# Design

## Context

Motivation steht in `proposal.md`. Ausgangslage auf `origin/main` (`bacd6ba`):

- **Zahlen:** `PlayerCounts` (`core/.../portal/PlayerCounts.java`) liefert `count(SourceType, name)` als Summe über alle laufenden Dienste eines Tasks, einer Gruppe oder eines Dienstes. Im CloudNet-Profil ist `HolderPlayerCounts` (`common/.../deliver`) die Implementierung, sie reicht über `TitanPlayerCountLookup` (JDK-Typen, Holder auf dem App-Classloader) an die Bridge weiter. Ohne Bridge liest jede Quelle als nicht laufend. Die Bridge (`bridge/.../TitanBridgePermissionExtension`) fragt den `CloudServiceProvider` über `InjectionLayer.ext()` ab (nicht über `ServiceRegistry`, s. Memory „CloudNet v4 CloudServiceProvider“), und liest `ONLINE_COUNT`/`MAX_PLAYERS` je Dienst (`ServiceReadings`, `ServiceTotals`).
- **Fehlt:** Eine Liste einzelner Dienste mit Zahlen. `ServiceReadings.Source` kennt die Einzelwerte bereits, `ServiceTotals` summiert sie aber sofort. Kein Abstraktionspunkt liefert „die laufenden Dienste von Task X“. Ebenso fehlt die Identität der eigenen Lobby (Task und Dienstname) in der App; `CloudNetEnvironment` erkennt nur, ob CloudNet läuft (`.wrapper`-Verzeichnis).
- **Zustellung:** `Deliver.sendPlayer(player, DeliverComponent)` (`core/.../api/deliver`). `DeliverComponent.serverBuilder().serverName(...)` leitet an einen bestimmten Dienst (`connectToServer`), `taskBuilder()` an einen Task (`connectToTask`, `LOWEST_PLAYERS`). Der Navigator nutzt `taskBuilder`, die Portale ebenfalls.
- **Hotbar:** `HotbarLobbyItems` baut die Item-Maps einmal im Konstruktor, `equip(player)` platziert sie beim Beitritt. Belegte Hotbar-Slots: 0 (`jumprun`), 4 (`navigator`). Slot 8 ist frei.
- **Inventare:** Der Navigator nutzt pro Modul einen `GlobalInventoryBuilder` (`SharedNavigator`), `register()` in `@PostConstruct`, `unregister()` in `@PreDestroy`. Aves 1.16 bietet zusätzlich `GlobalTranslatedInventoryBuilder` (`getInventory(Locale)`, übersetzter Titel), `setOpenFunction`/`setCloseFunction` und `setDataLayoutFunction`/`invalidateDataLayout`.
- **Aktualisierung:** `portal/LabelRefresh` liest Zahlen periodisch: `Scheduler` mit `TaskSchedule.tick(...)`, Lesung auf einem virtuellen Thread (`titan.portal.label-read`), Anwendung über `scheduleNextTick`, Überspringen, wenn die letzte Lesung noch läuft. Intervall `portal.labelRefreshSeconds`, Standard `5`, Grenzen 1 bis 3600 (`PortalSettings`).
- **Telemetrie:** Spans `<modul>.<operation>` ohne `titan.`-Präfix (`navigator.open`, `navigator.select`), Zähler `titan.<modul>.<größe>{ergebnis}`. Metriken tragen nie `user.id` oder Namen (Kardinalität, s. `docs/lobby-modules.md`, „Datenschutz und Attribute“).
- **Texte:** `titan/<modul>/messages_{en,de}.properties` über `MiniMessageTranslationStore` und `GlobalTranslator`, explizit gerendert (`SpawnMessages`, `RunMessages`). Ein Bundle-Test prüft, dass beide Dateien dieselben Schlüssel haben.
- **Feature-Flags:** `features.<NAME>` in `ConfigFeatureFlags`. Die bisherigen Flags (`NAVIGATOR_*`) sind alle `false` in den Standardwerten. `README.md` sagt, dass kein Modul beim Ändern neu startet, und liest Flags und Einstellungen zur Laufzeit. `DefaultsMerger` erlaubt nur eine Datei pro Top-Level-Abschnitt; deshalb muss der Abschnitt `features` in eine gemeinsame Datei.
- **Varianten:** `apps/cloudnet` und `apps/local` binden dieselben Columns ein (nur `season` fehlt in `local`). Die Bean-Profile `BeanProfiles.CLOUDNET` (`runtime`) steuern, welche Beans nur als CloudNet-Dienst existieren.

## Goals / Non-Goals

**Goals:**
- Eine Liste der laufenden Lobbys des eigenen Tasks mit Namen und Spielerzahl, aktuell gehalten, solange sie offen ist.
- Ein Wechsel per Klick über den bestehenden `Deliver`, ohne neuen Weg zum Server.
- Keine zweite Abfrage-Schiene für Zahlen; die Erweiterung liegt an der Stelle, an der die Portale bereits lesen.
- Lokale Variante ohne Verhaltensänderung und ohne Item.

**Non-Goals:** siehe `proposal.md`.

## Decisions

### D1 Column, Flag und Profil

```
features/lobbyswitcher   (Paket net.onelitefeather.titan.feature.lobbyswitcher)
  LobbySwitcherModule     @Singleton, @Profile(BeanProfiles.CLOUDNET)
  LobbySwitcherItems      @Factory, @Profile(BeanProfiles.CLOUDNET), liefert LobbyItem oder nichts
  lobbyswitcher.yaml      lobbyswitcher.refreshSeconds: 5
runtime/.../titan/defaults/features.yaml   features.NAVIGATOR_* und features.LOBBYSWITCHER: true
```

- **Flag:** `features.LOBBYSWITCHER`, Standard `true` (Q4). Name folgt dem Modul (`lobbyswitcher`) in Großbuchstaben, ohne Unterstrich, wie `NAVIGATOR` als Präfix. Umgebungsvariable `FEATURES_LOBBYSWITCHER`. Die lokale Variante hat kein CloudNet-Profil und damit kein Item; der Standardwert wirkt dort nicht.
- **Wirkzeitpunkt (Q1, entschieden):** Das Flag wird beim Start gelesen und entscheidet, ob das Item als Bean existiert. Der Grund ist `HotbarLobbyItems`: Die Item-Maps werden im Konstruktor einmal gebaut, ein Item lässt sich nicht zur Laufzeit aus der Leiste nehmen, ohne Spieler neu auszustatten. Umschalten braucht einen Neustart. Die Einstellung `lobbyswitcher.refreshSeconds` dagegen wird bei jedem Öffnen bzw. jeder Aktualisierung gelesen.
- **Profil:** Lokal (`apps/local`) gibt es kein Item, kein Inventar und keine Abfragen. Der Portal-Fallback `NoPlayerCounts` zeigt Labels als offline, ein Wechselinventar nur mit der eigenen Lobby ist aber ohne Ziel (der `DebugDeliver` wechselt nie) nutzlos.
- **Identität fehlt (Korrektur):** Der Bean-Scope entsteht, bevor die Bridge-Extension die Identität (D3) setzt. Das Item wird deshalb immer beigesteuert, solange Flag und CloudNet-Profil aktiv sind; die Identität wird erst beim Öffnen aufgelöst. Fehlt sie dann noch, öffnet sich nichts (ohne Task gibt es nichts zu lesen). Die Bridge loggt den Grund einmal als WARN.
- **Priorität:** `FeatureNode.attach(..., priority)` mit freiem Wert zwischen `navigator` (400) und `portal` (900); Wert in `FeatureNode`-Tabelle prüfen.
- **Alternativen:** (a) Flag live beim Klick prüfen, Item immer da: Spieler sähen ein wirkungsloses Item, wenn das Flag aus ist. Verworfen (Q1). (b) Standard `false` wie `NAVIGATOR_*`: verworfen (Q4), das Item soll nach dem Deploy ohne Konfigurationsänderung da sein.

### D2 Auflistung über denselben Provider

`PlayerCounts` bekommt eine Default-Methode:

```java
default List<ServiceCount> running(SourceType type, String name) { return List.of(); }
record ServiceCount(String name, int online, int max) {}   // core.portal, nur laufende Dienste
```

- `HolderPlayerCounts` reicht sie über `TitanPlayerCountLookup` durch (JDK-Typen: `List<String[]>`-Zeilen oder ein `common`-Record; die Bridge kennt `common` bereits, da sie `PlayerCountLookup` implementiert, daher ist ein `common`-Record zulässig).
- Die Bridge implementiert die Auflistung auf denselben `ServiceReadings`-Quellen und denselben `BridgeDocProperties`-Lesungen. `ServiceTotals` bleibt unverändert.
- Ohne Bridge liefert die Default-Methode eine leere Liste, der Switcher ist dann ohnehin nicht gebaut (D1).
- **Warum keine zweite Schnittstelle:** Die Zahlen sollen nicht zwei Quellen mit möglicherweise verschiedenem Stand haben. Ein eigener `LobbyDirectory`-SPI wäre sauberer getrennt, hätte aber einen zweiten Holder, einen zweiten Bridge-Pfad und einen zweiten Konfigurationspunkt.
- **Alternative:** Pro Dienstnamen `count(SERVICE, name)` aufrufen. Braucht die Namensliste, die es nicht gibt. Verworfen.
- **Test:** Unit-Test für die Zuordnung `ServiceReading -> ServiceCount` und die Filterung auf laufende Dienste in der Bridge (ohne CloudNet-Typen testbar, wie `ServiceTotals`).

### D3 Identität der eigenen Lobby

```java
// core.lobby
record LobbyIdentity(String task, String serviceName) {}
interface LobbyIdentities { Optional<LobbyIdentity> self(); }
```

- Die Bridge liest die eigene Dienst-Info über `InjectionLayer.ext()`. Im Wrapper-API (`wrapper-jvm-api` 4.0.0-RC16) gibt es `WrapperConfiguration.serviceInfoSnapshot()` und `ServiceInfoHolder.serviceInfo()`; `serviceInfo().serviceId().taskName()` liefert den Task, `name()` den Dienstnamen. Ob diese Instanz über `InjectionLayer.ext()` erreichbar ist, entscheidet der Spike (Task 1.1, Q2); das Ergebnis steht unter „Spike-Ergebnis“.
- Der Holder folgt `TitanPlayerCountLookup` (statisch, volatile, JDK-Typen). Ohne Bridge: `Optional.empty()`.
- **Alternative:** Task und Dienstname als Konfiguration (`lobbyswitcher.task`, `lobbyswitcher.service`). Einfach, aber der Betreiber pflegt Werte, die CloudNet schon kennt, und sie können beim Kopieren einer Konfiguration falsch werden. Als Rückfall vorgesehen, falls der Spike negativ ausfällt.

### D4 Inventar: ein Aves-Builder pro Modul

- `GlobalTranslatedInventoryBuilder` (eine Instanz pro Modul), `register()` in `@PostConstruct`, `unregister()` in `@PreDestroy`. Das folgt der Konvention „ein Builder pro Modul, register/unregister im Lebenszyklus“ und dem Navigator.
- Übersetzte Texte je Spieler-Locale über `getInventory(Locale)`, damit Titel und Itemnamen in der Sprache des Viewers erscheinen. Ob Aves die Item-Namen je Locale rendert, entscheidet der Spike (Q7). Rückfall: übersetzter Titel, englische Itemnamen.
- Größe: Zeilen nach Anzahl Einträge, mindestens eine, höchstens sechs (54 Plätze). Mehr Einträge werden abgeschnitten und protokolliert (Q6).
- Layout: Einträge über `setDataLayoutFunction`, Aktualisierung über `invalidateDataLayout()`. Ob `invalidateDataLayout()` den Inhalt eines geöffneten Inventars aktualisiert, entscheidet der Spike (Task 1.1).
- **Alternative:** `PersonalTranslatedInventoryBuilder` je Öffnen: passt zu „ein Viewer, ein Inventar“, bricht aber die Regel „ein Builder pro Modul“ und erzeugt Listener je Öffnen. Verworfen.
- **Alternative:** `GlobalInventoryBuilder` mit englischen Texten wie der Navigator: einfacher, widerspricht der i18n-Regel. Verworfen.

### D5 Aktualisierung nur bei offenem Inventar

- Öffnen/Schließen über `setOpenFunction`/`setCloseFunction` zählt die Viewer. Der erste Viewer startet die Aktualisierung (`scheduler.scheduleTask`, `TaskSchedule.tick(refreshSeconds * ServerFlag.SERVER_TICKS_PER_SECOND)`), der letzte beendet sie (`Task.cancel()`). Keine Aktualisierung ohne Viewer, auch wenn das Modul läuft.
- Ein Öffnen liest sofort neu, damit der Stand nicht bis zur nächsten Periode alt ist.
- Muster wie `LabelRefresh`: Lesung auf einem virtuellen Thread (eigener `@Named`-Executor im Modul), Überspringen, wenn die letzte Lesung läuft, Anwendung über `scheduleNextTick`; nur auf dem Tick-Thread werden Inventare verändert.
- Intervall `lobbyswitcher.refreshSeconds`, Standard `5`, gültig 1 bis 3600, Validierung wie `PortalSettings.refreshSeconds` (Aufzählung der Fehler mit Schlüssel). Eigener Schlüssel, damit Portal und Switcher getrennt justierbar bleiben (Q8).
- **D5b Kein Refactor (Q3, entschieden):** `LabelRefresh` bleibt unverändert. Der Switcher hat einen eigenen, kleinen Refresh im Modul nach demselben Muster. Das ist bewusst eine Kopie von etwa zwanzig Zeilen; ein gemeinsamer Baustein in `common` lohnt erst, wenn ein dritter Nutzer dazukommt (KISS). Entfällt damit: der vorgelagerte `refactor(common)`-PR.

### D6 Zustände und Klick

Jeder Eintrag hat genau einen Zustand, aus dem Zählerstand und der eigenen Identität bestimmt (reine Funktion, ohne Minestom, unit-getestet). Die Liste zeigt nur laufende Dienste; `GONE` (Dienst läuft beim Klick nicht mehr) entsteht nur in der Klick-Prüfung.

| Zustand | Bedingung | Anzeige | Klick |
| --- | --- | --- | --- |
| `CURRENT` | Dienstname = eigener Dienst | markiert (Glanz), „hier“ | keine Aktion |
| `FULL` | `max > 0` und `online >= max` | „voll“ | Meldung, keine Aktion |
| `NOT_READY` | `max == 0` (läuft, hat aber noch keine Werte gemeldet) | „nicht bereit“ | Meldung, keine Aktion |
| `JOINABLE` | sonst | `online/max` | Klick-Prüfung, dann `Deliver.sendPlayer(...serverBuilder().serverName(name)...)`, Inventar schließen |

- Die Sortierung ist nach Dienstname, aufsteigend, unabhängig vom Zustand.
- **Klick-Listener (Korrektur):** Die Protection-Column bricht jeden `InventoryPreClickEvent` bei Priorität 100 ab. Der Switcher hört deshalb mit `onIncludingCancelled` (Priorität 450) und behandelt abgebrochene Klicks. Verlässt ein Spieler mit offener Liste den Server, gibt `PlayerDisconnectEvent` den Betrachter frei, da Minestom dann kein Close-Event feuert.
- **Klick-Prüfung (Q5, entschieden):** Vor dem Wechsel liest der Switcher den Zieldienst frisch über denselben Provider (`running(TASK, task)`, auf dem Lese-Thread, Anwendung auf dem nächsten Tick). Ist er jetzt `FULL`, `NOT_READY` oder `GONE` (nicht mehr gelistet), wird nicht weitergeleitet, die passende Meldung geht an den Spieler, und das Inventar liest neu. Schlägt die Prüfung selbst fehl, wird nicht weitergeleitet (Ergebnis `error`, Meldung `unavailable`).
- Der Server bleibt die letzte Instanz: Ein Wettlauf zwischen Prüfung und Verbindung wird akzeptiert. Es gibt keine Warteschlange und keine Reservierung.
- Der Wechsel nutzt `serverBuilder` (ein bestimmter Dienst) und nicht `taskBuilder` (Lastverteilung), weil der Spieler eine bestimmte Lobby gewählt hat.

### D7 Telemetrie

- Span `lobbyswitcher.open`: Attribute `user.id`, `lobbyswitcher.entries` (Anzahl Einträge). Umgibt das Öffnen, nicht die Lesung.
- Span `lobbyswitcher.select`: Attribute `user.id`, `lobbyswitcher.target` (Dienstname; auf dem Span erlaubt, weil Spannamen des Navigators auch Ziele tragen), `lobbyswitcher.result` (`sent`, `current`, `full`, `not_ready`, `gone`, `error`). Bei `error` Span-Status `ERROR`.
- Zähler `titan.lobbyswitcher.selections{result}` mit denselben Werten. Abweichung vom ersten Entwurf: **kein** `target` im Zähler. Dienstnamen sind Kardinalität, die Lobby-Liste wächst mit den Instanzen; `docs/lobby-modules.md` verbietet Namen in Metriken.
- Keine Spans und keine Zähler für die periodische Aktualisierung; Fehler der Lesung gehen einmal als `WARN` ins Log (siehe D8).

### D8 Fehler der Lesung

- Wirft der Provider oder liefert er keinen Stand: der letzte gute Stand bleibt, das Inventar zeigt weiter diesen Stand. Ohne jeden Stand zeigt das Öffnen eine Meldung „Lobbys nicht verfügbar“ (übersetzt) und enthält nur die eigene Lobby, sofern die Identität bekannt ist.
- Log wie `LabelReader`: der erste Fehler einer Art als `WARN` je Quelle, danach nur `DEBUG`, Rückkehr zum Normalzustand meldet sich wieder neu.
- Keine eigene Fehlermetrik (Q9): Fehler beim Klick erscheinen als `result=error` im Zähler und als Span-Status; Lesefehler bei der Aktualisierung nur im Log.

### D9 Texte

- `titan/lobbyswitcher/messages_en.properties` und `messages_de.properties`, Schlüssel unter `titan.lobbyswitcher.*`: Titel, Zustandsnamen (`current`, `full`, `not_ready`), Zeilen für Zähler, Meldungen für die Klicks (`sent`, `current`, `full`, `not_ready`, `gone`) und für Fehler (`unavailable`, auch bei fehlgeschlagener Klick-Prüfung). Log-Zeilen bleiben englisch.
- Gerendert wie `SpawnMessages`: eigener `MiniMessageTranslationStore`, `GlobalTranslator` mit explizitem Render (Minestom 26.1 zeigt übersetzbare Komponenten ohne Render-Flag leer, s. Memory „Minestom 26.1 Thread-/Übersetzungs-Fallen“).
- Bundle-Test: beide Dateien haben dieselben Schlüssel, und jeder Schlüssel aus `KEYS` steht im englischen Bundle.

### D10 Konfiguration

`features/lobbyswitcher/src/main/resources/titan/defaults/lobbyswitcher.yaml` nur mit dem Abschnitt `lobbyswitcher` (`refreshSeconds`). Einstellungsklasse `LobbySwitcherSettings` nach Vorbild `PortalSettings`.

Der Flag-Abschnitt `features` steht heute in `features/navigator/.../navigator.yaml`. `DefaultsMerger` lehnt jede zweite Datei ab, die den Top-Level-Abschnitt `features` setzt (auch mit anderen Schlüsseln, s. Kommentar in `DefaultsMerger.flatten`). Deshalb wandert der Abschnitt `features` in eine eigene Datei `runtime/src/main/resources/titan/defaults/features.yaml` (runtime besitzt `ConfigFeatureFlags`), mit `NAVIGATOR_*` und `LOBBYSWITCHER: true`. Die Werte und Kommentare der Navigator-Flags bleiben gleich; die Verschiebung ist ein eigener `refactor(runtime)`-Commit vor dem Feature-Commit, weil ohne sie das neue Flag nicht ladbar ist.

### D11 Hotbar-Item

- `LobbyItem(featureId = "lobbyswitcher", key = Key("titan:lobbyswitcher"), itemStack = CLOCK "Lobbys", placement = ItemSlot.hotbar(8), onUse = open)`.
- Material: Uhr (`CLOCK`, Q10). Unterscheidet sich von Navigator (Feder) und Jump-and-Run (Schleimblock).

## Architektur

```
Spieler klickt Item (Slot 8)
   -> LobbySwitcherModule.open(player)
        -> SwitcherInventory (GlobalTranslatedInventoryBuilder, pro Modul)
             Öffnen: Viewer +1, erster Viewer startet Refresh-Task
             Refresh-Task (Tick): Lesung off-tick -> PlayerCounts.running(TASK, task)
                                  -> HolderPlayerCounts -> Bridge (ServiceReadings)
        -> Klick: Klick-Prüfung (D6, frische Lesung) -> Zustand -> Deliver.sendPlayer(serverBuilder(name))
Schließen: Viewer -1, letzter Viewer stoppt Refresh-Task
```

Die Bridge bleibt der einzige Ort, der CloudNet-Typen kennt. `core` bekommt nur Records und Interfaces, `common` die Holder, `features/lobbyswitcher` kennt CloudNet nicht.

## Tests (F.I.R.S.T., Test zuerst)

- **Unit:** Zustandsfunktion D6 (jede Zeile der Tabelle, Grenzfälle `max == 0`, `online == max`, eigener Dienst mit voller Lobby); Sortierung; Zeilenzahl für Inventargröße; `LobbySwitcherSettings.refreshSeconds` (Grenzen, Text, Schlüssel im Fehler); Viewer-Zähler (erster startet, letzter stoppt, kein Viewer ohne Task); Lesefehler-Verhalten D8 mit Captured Appender; Bundle-Test D9; Bridge-Zuordnung D2 ohne CloudNet; Klick-Prüfung als reine Funktion (frischer Zustand -> Aktion).
- **Integration (Cyano-`Env`, `env.tick()`):** Mit Flag und Profil wird das Item auf Slot 8 gelegt; ohne Flag oder Profil nicht. Öffnen zeigt die Dienste aus einem Fake-`PlayerCounts`; Klick auf `JOINABLE` ruft einen Fake-`Deliver` mit `serverName` auf; Klick auf `CURRENT`/`FULL` ruft ihn nicht auf; `@PreDestroy` entfernt das Inventar aus dem Aves-Listener. Refresh: nach `refreshSeconds * 20` Ticks ist der neue Stand sichtbar, ohne Viewer keine Lesung (über Zähler des Fakes).
- **Keine Sleeps, keine Systemzeit:** `Clock` und Scheduler über `env` bzw. explizite Ticks; Lesungen auf dem Test-Executor, der synchron läuft.
- **Verdrahtung:** `apps/cloudnet` (`WiringTest`) und `apps/local` (`VariantStartTest`) bleiben grün; lokale Variante startet ohne Lobbyswitcher-Beans.

## Risks / Trade-offs

- Bridge-Änderung ist nur gegen CloudNet-Snapshot testbar (Bridge läuft nicht im Test). Mitigation: Zuordnung als reine Funktion, Smoke-Test auf CloudNet (Task 12.1).
- Aves-Datenschicht und übersetzte Itemnamen sind nicht vollständig verifiziert (Q7); der Spike (Task 1.1) entscheidet, ob D4 so bleibt.
- Das Flag wirkt nicht live (D1, Q1). Das weicht von „kein Neustart“ in `README.md` ab; die README-Passage muss das ausdrücklich nennen.
- Die Klick-Prüfung kostet eine zusätzliche Abfrage je Klick auf eine andere Lobby; das ist selten und billig.
- Viele Viewer lösen keine zusätzlichen Lesungen aus (eine Aktualisierung je Modul), aber jeder Viewer sieht den Stand des Moduls. Kein Problem bei den erwarteten Zahlen.

## Entscheidungen (Open Questions geschlossen)

- **Q1 Flag wirkt beim Start.** Entschieden: Item-Präsenz wird beim Start festgelegt, Umschalten braucht Neustart. Die README-Ausnahme ist Teil von Task 11.1.
- **Q2 Eigene Identität.** Offen bis zum Spike (Task 1.1). Ergebnis unter „Spike-Ergebnis“. Fällt der Spike negativ aus, gilt der Konfigurationsrückfall (`lobbyswitcher.task`/`lobbyswitcher.service`) aus D3.
- **Q3 Muster teilen.** Entschieden: kein Refactor, eigene kleine Aktualisierung im Modul (D5b). Der vorgelagerte Refactor-PR entfällt.
- **Q4 Standard des Flags.** Entschieden: `true` im CloudNet-Profil (`features.LOBBYSWITCHER: true` in `runtime/.../features.yaml`).
- **Q5 Klick-Wettlauf.** Entschieden: Der Zieldienst wird beim Klick frisch geprüft; voll, nicht bereit oder nicht mehr gelistet heißt: nicht senden, Meldung, Ansicht aktualisieren (D6).
- **Q6 Mehr als 54 Lobbys.** Entschieden: Abschneiden nach Namen sortiert, Log-Zeile, keine Paginierung.
- **Q7 Aves und Locale.** Offen bis zum Spike (Task 1.1). Ergebnis unter „Spike-Ergebnis“.
- **Q8 Intervall.** Entschieden: eigener Schlüssel `lobbyswitcher.refreshSeconds` (Standard `5`).
- **Q9 Fehlerzähler.** Entschieden: kein eigener Lookup-Zähler. Fehler sind über `titan.lobbyswitcher.selections{result=error}` und den Span-Status sichtbar.
- **Q10 Materialwahl und Icon.** Entschieden: `CLOCK` in Slot 8.

## Abweichungen bei der Umsetzung

- Das Inventar hat immer sechs Zeilen, weil ein Aves-Builder eine feste Größe hat; ungenutzte Slots tragen eine Glasscheibe (D4 sah Zeilen nach Eintragsanzahl vor).
- Die eigene Lobby ist ein `NETHER_STAR`, kein verzaubertes Item, weil Aves `TranslatedItem` den Glint verwirft (D6 sah markiert/Glint vor).
- Klick- und Schließ-Events leitet der `FeatureNode` des Moduls an `SwitcherInventory` weiter, weil Aves-Listener bei Inventaren je Locale nie feuern (der Spike nahm das Gegenteil an); die Liste aktualisiert sich dadurch einen Tick später.
- Die Telemetrie liegt beim Klick- und Refresh-Code, nicht in einem eigenen Commit.
- Die Identitäts-Bean gibt es nur im Profil CLOUDNET; ein Unit-Test für den Profil-Guard fehlt, die Container-Wiring-Tests decken ihn ab.
- `ServerFlag.SERVER_TICKS_PER_SECOND` ist veraltet, `ViewerCounter` nutzt eine benannte Konstante 20.
- Der Navigator-Guard-Test (`DefaultNavigatorFeatureFlagsTest`) liest `features.yaml` aus `runtime` über eine `testRuntimeOnly`-Abhängigkeit auf `:runtime`.
- Hotbar: Nach `@PreDestroy` bleibt die Uhr in der Hotbar-Map (die Hotbar baut sie einmal), tut aber nichts.

## Spike-Ergebnis

Stand: Task 1.1, ohne laufenden CloudNet-Knoten (nur Quellen gelesen, Spike-Code verworfen).

- **Q2 Eigene Identität: erreichbar (Quellenlage, Smoke-Test in 12.1 bestätigt es).**
  - `ServiceInfoSnapshot.name()` und `serviceId().taskName()` existieren in `driver-api` 4.0.0-RC18-SNAPSHOT (dem Katalog-Stand) und liefern Dienstname bzw. Task.
  - `ServiceInfoHolder.serviceInfo()` und `WrapperConfiguration.serviceInfoSnapshot()` sind in `wrapper-jvm-api` RC18 Schnittstellen. Laut DeepWiki zu `CloudNetService/CloudNet` werden `ServiceInfoHolder` (`WrapperServiceInfoHolder`, `@Provides`/`@Singleton`) und `WrapperConfiguration` (`DocumentWrapperConfiguration`, `@Factory`) im Boot-Layer gebunden. `InjectionLayer.ext()` enthält laut Javadoc alle Bindungen des Boot-Layers.
  - Nicht im Quelltext nachgeprüft (die Wrapper-Implementierung liegt nicht im Gradle-Cache). Die Bridge nutzt `InjectionLayer.ext().instance(ServiceInfoHolder.class)`, Fehler oder `null` ergeben `Optional.empty()` (Ausschlussfall: kein Item, Warnung im Start-Log).
  - Rückfall (Konfigurationsschlüssel `lobbyswitcher.task`/`lobbyswitcher.service`) wird nur umgesetzt, wenn der Smoke-Test 12.1 zeigt, dass die Bindung fehlt.
- **Q7 Aves und Locale: ja.** `GlobalTranslatedInventoryBuilder` (Aves 1.16.6) hält je Locale ein eigenes `CustomInventory`. Der Titel wird je Locale mit `GlobalTranslator.render` gerendert. Die Datenschicht (`DataLayoutFunction.applyLayout(contents, locale)`) bekommt die Locale des Viewers. Damit sind übersetzte Itemnamen je Viewer möglich.
- **Aktualisierung (D4):** `invalidateDataLayout()` ruft bei offenen Inventaren `retrieveDataLayout()` auf; `updateInventory` zieht offene Viewer nach (`updateViewer` -> `inventory.update()`). Ein geöffnetes Inventar wird also aktualisiert. Geschlossene Inventare werden erst beim nächsten Öffnen neu aufgebaut.
- **Nebenbefund:** `unregister()` schließt alle offenen Viewer (relevant für 7.6).
