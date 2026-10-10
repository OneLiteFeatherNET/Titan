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
- **Feature-Flags:** `features.<NAME>` in `ConfigFeatureFlags`. Die bisherigen Flags (`NAVIGATOR_*`) sind alle `false` in den Standardwerten. `README.md` sagt, dass kein Modul beim Ändern neu startet, und liest Flags und Einstellungen zur Laufzeit.
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
  lobbyswitcher.yaml      features.LOBBYSWITCHER: false, lobbyswitcher.refreshSeconds: 5
```

- **Flag:** `features.LOBBYSWITCHER`, Standard `false`. Name folgt dem Modul (`lobbyswitcher`) in Großbuchstaben, ohne Unterstrich, wie `NAVIGATOR` als Präfix. Umgebungsvariable `FEATURES_LOBBYSWITCHER`.
- **Wirkzeitpunkt (Abweichung, s. Open Questions Q1):** Das Flag wird beim Start gelesen und entscheidet, ob das Item als Bean existiert. Der Grund ist `HotbarLobbyItems`: Die Item-Maps werden im Konstruktor einmal gebaut, ein Item lässt sich nicht zur Laufzeit aus der Leiste nehmen, ohne Spieler neu auszustatten. Die Einstellungen `lobbyswitcher.refreshSeconds` dagegen werden bei jedem Öffnen bzw. jeder Aktualisierung gelesen.
- **Profil:** Lokal (`apps/local`) gibt es kein Item, kein Inventar und keine Abfragen. Der Portal-Fallback `NoPlayerCounts` zeigt Labels als offline, ein Wechselinventar nur mit der eigenen Lobby ist aber ohne Ziel (der `DebugDeliver` wechselt nie) nutzlos.
- **Identität fehlt:** Ist die eigene Identität (D3) nicht verfügbar, wird ebenfalls kein Item gebaut; eine Warnung mit Grund geht einmal in den Start-Log.
- **Priorität:** `FeatureNode.attach(..., priority)` mit freiem Wert zwischen `navigator` (400) und `portal` (900); Wert in `FeatureNode`-Tabelle prüfen.
- **Alternativen:** (a) Flag live beim Klick prüfen, Item immer da: Spieler sähen ein wirkungsloses Item während des Rollouts. Verworfen. (b) Standard `true` im CloudNet-Profil: Die Navigator-Ziele sind ebenfalls `false`; ein neuer Eintrag in der Leiste sollte erst nach Smoke-Test sichtbar werden. Offen, s. Q4.

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

- Die Bridge liest die eigene Dienst-Info über `InjectionLayer.ext()`. Im Wrapper-API (`wrapper-jvm-api` 4.0.0-RC16) gibt es `WrapperConfiguration.serviceInfoSnapshot()` und `ServiceInfoHolder.serviceInfo()`; `serviceInfo().serviceId().taskName()` liefert den Task, `name()` den Dienstnamen. Ob diese Instanz über `InjectionLayer.ext()` erreichbar ist, muss im Spike (Task 1.1) bestätigt werden (Q2).
- Der Holder folgt `TitanPlayerCountLookup` (statisch, volatile, JDK-Typen). Ohne Bridge: `Optional.empty()`.
- **Alternative:** Task und Dienstname als Konfiguration (`lobbyswitcher.task`, `lobbyswitcher.service`). Einfach, aber der Betreiber pflegt Werte, die CloudNet schon kennt, und sie können beim Kopieren einer Konfiguration falsch werden. Als Rückfall denkbar, Q2.

### D4 Inventar: ein Aves-Builder pro Modul

- `GlobalTranslatedInventoryBuilder` (eine Instanz pro Modul), `register()` in `@PostConstruct`, `unregister()` in `@PreDestroy`. Das folgt der Konvention „ein Builder pro Modul, register/unregister im Lebenszyklus“ und dem Navigator.
- Übersetzte Texte je Spieler-Locale über `getInventory(Locale)`, damit Titel und Itemnamen in der Sprache des Viewers erscheinen. Ob Aves die Item-Namen je Locale rendert, ist vor dem Bau zu prüfen (Q7); Rückfall sind englische Namen.
- Größe: Zeilen nach Anzahl Einträge, mindestens eine, höchstens sechs (54 Plätze). Mehr Einträge werden abgeschnitten und protokolliert (Non-Goal Paginierung).
- Layout: Einträge über `setDataLayoutFunction`, Aktualisierung über `invalidateDataLayout()`. Ob Aves die Datenschicht je Viewer neu aufbaut, ist im Spike zu prüfen.
- **Alternative:** `PersonalTranslatedInventoryBuilder` je Öffnen: passt zu „ein Viewer, ein Inventar“, bricht aber die Regel „ein Builder pro Modul“ und erzeugt Listener je Öffnen. Verworfen.
- **Alternative:** `GlobalInventoryBuilder` mit englischen Texten wie der Navigator: einfacher, widerspricht der i18n-Regel. Verworfen.

### D5 Aktualisierung nur bei offenem Inventar

- Öffnen/Schließen über `setOpenFunction`/`setCloseFunction` zählt die Viewer. Der erste Viewer startet die Aktualisierung (`scheduler.scheduleTask`, `TaskSchedule.tick(refreshSeconds * ServerFlag.SERVER_TICKS_PER_SECOND)`), der letzte beendet sie (`Task.cancel()`). Keine Aktualisierung ohne Viewer, auch wenn das Modul läuft.
- Ein Öffnen liest sofort neu, damit der Stand nicht bis zur nächsten Periode alt ist.
- Muster wie `LabelRefresh`: Lesung auf einem virtuellen Thread (`@Named`-Executor wie `PortalBeans.labelReads`), Überspringen, wenn die letzte Lesung läuft, Anwendung über `scheduleNextTick`, nur auf dem Tick-Thread werden Inventare verändert.
- Intervall `lobbyswitcher.refreshSeconds`, Standard `5`, gültig 1 bis 3600, Validierung wie `PortalSettings.refreshSeconds` (Aufzählung der Fehler mit Schlüssel). Eigener Schlüssel, damit Portal und Switcher getrennt justierbar bleiben (Q8).
- **D5b Wiederverwendung des Musters (Vorschlag):** Die Logik „Periode, Überspringen, Lesung off-tick, Anwendung auf dem nächsten Tick“ ist in `LabelRefresh` schon vorhanden und hat dieselbe Bedeutung. Vorgeschlagen: vorgelagerter `refactor(common)`-PR, der sie als `OffTickRefresh` nach `common` hebt; `LabelRefresh` und der Switcher nutzen sie. Alternative: Kopie. Verworfen wegen DRY, aber als Entscheidung offen (Q3).

### D6 Zustände und Klick

Jeder Eintrag hat genau einen Zustand, aus dem Zählerstand und der eigenen Identität bestimmt (reine Funktion, ohne Minestom, unit-getestet):

| Zustand | Bedingung | Anzeige | Klick |
| --- | --- | --- | --- |
| `CURRENT` | Dienstname = eigener Dienst | markiert (Glanz), „hier“ | keine Aktion |
| `FULL` | `max > 0` und `online >= max` | „voll“ | Meldung, keine Aktion |
| `NOT_READY` | `max == 0` (läuft, hat aber noch keine Werte gemeldet) | „nicht bereit“ | Meldung, keine Aktion |
| `JOINABLE` | sonst | `online/max` | `Deliver.sendPlayer(...serverBuilder().serverName(name)...)`, Inventar schließen |

- Die Sortierung ist nach Dienstname, aufsteigend, unabhängig vom Zustand.
- Der Klick arbeitet mit dem letzten gelesenen Stand. Ein Wettlauf (Lobby wird zwischen Lesung und Klick voll) wird akzeptiert; der Server bleibt die letzte Instanz. Q5.
- Der Wechsel nutzt `serverBuilder` (ein bestimmter Dienst) und nicht `taskBuilder` (Lastverteilung), weil der Spieler eine bestimmte Lobby gewählt hat.

### D7 Telemetrie

- Span `lobbyswitcher.open`: Attribute `user.id`, `lobbyswitcher.entries` (Anzahl Einträge). Umgibt das Öffnen, nicht die Lesung.
- Span `lobbyswitcher.select`: Attribute `user.id`, `lobbyswitcher.target` (Dienstname; auf dem Span erlaubt, weil Spannamen des Navigators auch Ziele tragen), `lobbyswitcher.result` (`sent`, `current`, `full`, `not_ready`).
- Zähler `titan.lobbyswitcher.selections{result}`. Abweichung vom ersten Entwurf: **kein** `target` im Zähler. Dienstnamen sind Kardinalität, die Lobby-Liste wächst mit den Instanzen; `docs/lobby-modules.md` verbietet Namen in Metriken.
- Keine Spans und keine Zähler für die periodische Aktualisierung; Fehler der Lesung gehen einmal als `WARN` ins Log (siehe D8).

### D8 Fehler der Lesung

- Wirft der Provider oder liefert er keinen Stand: der letzte gute Stand bleibt, das Inventar zeigt weiter diesen Stand. Ohne jeden Stand zeigt das Öffnen eine Meldung „Lobbys nicht verfügbar“ (übersetzt) und enthält nur die eigene Lobby, sofern die Identität bekannt ist.
- Log wie `LabelReader`: der erste Fehler einer Art als `WARN` je Quelle, danach nur `DEBUG`, Rückkehr zum Normalzustand meldet sich wieder neu.
- Keine Metrik in diesem Change (Q9).

### D9 Texte

- `titan/lobbyswitcher/messages_en.properties` und `messages_de.properties`, Schlüssel unter `titan.lobbyswitcher.*`: Titel, Zustandsnamen (`current`, `full`, `not_ready`), Zeilen für Zähler, Meldungen für die Klicks (`sent`, `current`, `full`, `not_ready`) und für Fehler der Lesung (`unavailable`). Log-Zeilen bleiben englisch.
- Gerendert wie `SpawnMessages`: eigener `MiniMessageTranslationStore`, `GlobalTranslator` mit explizitem Render (Minestom 26.1 zeigt übersetzbare Komponenten ohne Render-Flag leer, s. Memory „Minestom 26.1 Thread-/Übersetzungs-Fallen“).
- Bundle-Test: beide Dateien haben dieselben Schlüssel, und jeder Schlüssel aus `KEYS` steht im englischen Bundle.

### D10 Konfiguration

`features/lobbyswitcher/src/main/resources/titan/defaults/lobbyswitcher.yaml` nur mit dem Abschnitt `lobbyswitcher` (`refreshSeconds`). Einstellungsklasse `LobbySwitcherSettings` nach Vorbild `PortalSettings`.

Der Flag-Abschnitt `features` steht heute in `features/navigator/.../navigator.yaml`. `DefaultsMerger` lehnt jede zweite Datei ab, die den Top-Level-Abschnitt `features` setzt (auch mit anderen Schlüsseln, s. Kommentar in `DefaultsMerger.flatten`). Deshalb wandert der Abschnitt `features` in eine eigene Datei `runtime/src/main/resources/titan/defaults/features.yaml` (runtime besitzt `ConfigFeatureFlags`), mit `NAVIGATOR_*` und `LOBBYSWITCHER`. Die Werte und Kommentare bleiben gleich; das gehört zum selben `feat`-Commit, weil ohne es das Flag nicht ladbar ist.

### D11 Hotbar-Item

- `LobbyItem(featureId = "lobbyswitcher", key = Key("titan:lobbyswitcher"), itemStack = ENDER_EYE "Lobbys", placement = ItemSlot.hotbar(8), onUse = open)`.
- Material und Name sind Vorschlag; das Icon sollte sich von Navigator (Feder) und Jump-and-Run (Schleimblock) unterscheiden.

## Architektur

```
Spieler klickt Item (Slot 8)
   -> LobbySwitcherModule.open(player)
        -> SwitcherInventory (GlobalTranslatedInventoryBuilder, pro Modul)
             Öffnen: Viewer +1, erster Viewer startet Refresh-Task
             Refresh-Task (Tick): OffTickRefresh -> PlayerCounts.running(TASK, task)
                                  -> HolderPlayerCounts -> Bridge (ServiceReadings)
        -> Klick: Zustand (D6) -> Deliver.sendPlayer(serverBuilder(name))
Schließen: Viewer -1, letzter Viewer stoppt Refresh-Task
```

Die Bridge bleibt der einzige Ort, der CloudNet-Typen kennt. `core` bekommt nur Records und Interfaces, `common` die Holder, `features/lobbyswitcher` kennt CloudNet nicht.

## Tests (F.I.R.S.T., Test zuerst)

- **Unit:** Zustandsfunktion D6 (jede Zeile der Tabelle, Grenzfälle `max == 0`, `online == max`, eigener Dienst mit voller Lobby); Sortierung; Zeilenzahl für Inventargröße; `LobbySwitcherSettings.refreshSeconds` (Grenzen, Text, Schlüssel im Fehler); Viewer-Zähler (erster startet, letzter stoppt, kein Viewer ohne Task); Lesung-Fehler-Verhalten D8 mit Captured Appender; Bundle-Test D9; Bridge-Zuordnung D2 ohne CloudNet.
- **Integration (Cyano-`Env`, `env.tick()`):** Mit Flag und Profil wird das Item auf Slot 8 gelegt; ohne Flag oder Profil nicht. Öffnen zeigt die Dienste aus einem Fake-`PlayerCounts`; Klick auf `JOINABLE` ruft einen Fake-`Deliver` mit `serverName` auf; Klick auf `CURRENT`/`FULL` ruft ihn nicht auf; `@PreDestroy` entfernt das Inventar aus dem Aves-Listener. Refresh: nach `refreshSeconds * 20` Ticks ist der neue Stand sichtbar, ohne Viewer keine Lesung (über Zähler des Fakes).
- **Keine Sleeps, keine Systemzeit:** `Clock` und Scheduler über `env` bzw. explizite Ticks; Lesungen auf dem Test-Executor, der synchron läuft.
- **Verdrahtung:** `apps/cloudnet` (`WiringTest`) und `apps/local` (`VariantStartTest`) bleiben grün; lokale Variante startet ohne Lobbyswitcher-Beans.

## Risks / Trade-offs

- Bridge-Änderung ist nur gegen CloudNet-Snapshot testbar (Bridge läuft nicht im Test). Mitigation: Zuordnung als reine Funktion, Smoke-Test auf CloudNet (Task 12.1).
- Aves-Datenschicht und übersetzte Itemnamen sind nicht vollständig verifiziert (Q7); der Spike (Task 1.1) entscheidet, ob D4 so bleibt.
- Das Flag wirkt nicht live (D1). Das weicht von „kein Neustart“ in `README.md` ab; die README-Passage muss das ausdrücklich nennen.
- Viele Viewer lösen keine zusätzlichen Lesungen aus (eine Aktualisierung je Modul), aber jeder Viewer sieht den Stand des Moduls. Kein Problem bei den erwarteten Zahlen.

## Open Questions

- **Q1 Flag wirkt beim Start.** Abweichung von der Regel „Flags live“. Alternative: Item immer da, Klick prüft das Flag live und meldet „deaktiviert“. Entscheidung vor dem Apply bestätigen.
- **Q2 Eigene Identität.** Ist `ServiceInfoHolder`/`WrapperConfiguration` über `InjectionLayer.ext()` erreichbar? Falls nein: Konfigurationsrückfall (`lobbyswitcher.task`/`lobbyswitcher.service`) oder kein Item. Spike Task 1.1 entscheidet.
- **Q3 Muster teilen (D5b).** `refactor(common)`-PR vorab (Vorschlag) oder Kopie in den Switcher?
- **Q4 Standard des Flags im CloudNet-Profil.** `false` wie `NAVIGATOR_*` (Vorschlag) oder `true`? Mit `true` ist das Item nach dem Deploy sofort sichtbar.
- **Q5 Klick-Wettlauf.** Vor dem Wechsel den Stand frisch lesen (zweite Abfrage am Tick-Thread, sofern günstig) oder den letzten Stand nutzen (Vorschlag)?
- **Q6 Mehr als 54 Lobbys.** Abschneiden mit Log (Vorschlag) oder Paginierung in einem Folge-Change?
- **Q7 Aves und Locale.** Rendert `GlobalTranslatedInventoryBuilder` übersetzte Itemnamen je Viewer? Falls nicht: englische Namen für Items, übersetzter Titel über `getInventory(Locale)`.
- **Q8 Intervall.** Eigener Schlüssel `lobbyswitcher.refreshSeconds` (Vorschlag) oder gemeinsam mit `portal.labelRefreshSeconds`?
- **Q9 Fehlerzähler.** Soll ein Zähler `titan.lobbyswitcher.lookups{result}` wie `titan.portal.player_count.lookups` dazukommen? Vorschlag: nein in diesem Change, Folge-Entscheidung nach dem Smoke-Test.
- **Q10 Materialwahl und Icon.** `ENDER_EYE` ist Vorschlag; Designentscheidung.
