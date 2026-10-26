# Design

## Kontext

Siehe `proposal.md`, Abschnitt „Why“. Der Navigator ist ein Enum (`Destination`) mit Slot, Icon, Verlauf, Anzeigename, CloudNet-Task und optionalem Recht. `NavigatorModule` baut beim Start je Teilmenge der gebundenen Ziele und Sprache ein geteiltes Inventar. Die Dienste je Task liest das Modul über `TaskPlayers` und `PlayerCounts` (Core-Port). Die CloudNet-Seite ist zweigeteilt. `platform/cloudnet` hat keine CloudNet-Abhängigkeit und exponiert nur den Halter `TitanServerConnector`. Die CloudNet-Typen liegen ausschließlich in `bridge`, das als Extension in der Classloader-Welt von CloudNet läuft und über JDK-typisierte Halter in `common` mit der Lobby spricht (`TitanServerConnector`, `TitanPlayerCountLookup`, `TitanPermissionBridge`).

Die CloudNet-Driver-API ist `4.0.0-RC18-SNAPSHOT` (`settings.gradle.kts`). Geprüft wurde das Jar aus dem Gradle-Cache per `javap`:

- `CloudServiceFactory.createCloudService(ServiceConfiguration)` liefert ein `ServiceCreateResult` (`created`, `deferred` oder `FAILED`).
- `CloudServiceProvider.servicesByTask(String)` liefert `ServiceInfoSnapshot`s. `serviceProvider(UUID)` und `serviceProviderByName(String)` liefern einen `SpecificCloudServiceProvider` mit `start()` und `stop()`.
- `ServiceLifeCycle` hat genau vier Werte: `PREPARED`, `RUNNING`, `STOPPED`, `DELETED`. **Es gibt kein `STARTING`.** Der Brief nennt STARTING; in diesem Stand ist „startend“ also `PREPARED`.
- `CloudServiceLifecycleChangeEvent` existiert als Event.

Offen bleibt, ob `createCloudService` den Dienst direkt startet oder nur vorbereitet, und wie `ServiceConfiguration` für einen Task gebaut wird. Das klärt der erste Spike (Task 2.0).

Die Bridge filtert derzeit in `ServiceListing.running(...)` nach `ServiceReading.running`, also nach `RUNNING`. PREPARED-Dienste sind für die Lobby unsichtbar, und genau das würde einen Doppelstart auslösen (siehe D5).

## Ziele / Nicht-Ziele

**Ziele:**

- Ein Klick auf einen Staging-Eintrag startet bei Bedarf genau einen Dienst und verbindet, sobald er bereit ist.
- Staging-Dienste laufen nur, wenn sie gebraucht werden. Live-Tasks werden nie gestoppt.
- Das Menü bleibt geteilt. Die Anzahl der Inventare wächst nur linear mit den Ansichten, nicht mit 2 hoch der Rechte.
- Die Logik lässt sich mit Fake-Ports, einem explizit fortgeschalteten `Clock` und explizit gesteuerten Ticks testen.

**Nicht-Ziele:**

- Keine Änderung an Spiellogik, Portalen oder Schnellbeitritt von Tamias.
- Keine neue Prozessverwaltung neben CloudNet. Titan erzeugt und stoppt nur über die CloudNet-API.
- Keine verteilte Sperre zwischen den Lobbys (D6).

## Entscheidungen

### D1 Layout: Untermenü hinter dem Eintrag „Staging“

Der Navigator behält vier Reihen. Die Belegung ist:

| Platz | Eintrag | Recht |
| --- | --- | --- |
| 11 | ElytraRace (live) | keins |
| 13 | Bounce (live) | `titan.navigator.bounce` |
| 14 | Tamias (live, neu) | `titan.navigator.tamias` |
| 15 | Slender (live) | `titan.navigator.slender` |
| 16 | Staging (Untermenü, neu) | mindestens eines der vier Staging-Rechte |
| 20 / 22 / 24 | Survival / Creative / Build | keins / keins / `titan.navigator.buildserver` |
| 31 | Spawn | keins |

Das Untermenü ist eine Ein-Reihen-Kiste (neun Plätze): Platz 0 Slender (Staging), 1 Bounce (Staging), 2 ElytraRace (Staging), 3 Tamias (Staging), 8 Zurück in den Navigator. Die übrigen Plätze sind graue Glasscheiben. Staging-Einträge, für die das Recht fehlt, zeigen eine graue Glasscheibe auf ihrem festen Platz, so wie das Hauptmenü es für Build macht.

Die Platzwahl für Tamias (14) ist vorläufig. Platz 12 bleibt frei, damit Elytra und Bounce nicht eng zusammenhängen. Die Position ist im Test festgelegt, nicht im Code.

Alternativen:

- **Eigene Staging-Reihe.** Die Kiste hat keine freie Reihe; Reihe 0 und 3 sind Rahmen beziehungsweise Spawn. Eine fünfte Reihe ändert die Anforderung „vier Reihen“ und zeigt allen Spielern eine leere graue Reihe. Dazu verdoppelt sich die Zahl der Hauptmenüs (Reihenzahl als zusätzlicher Schlüssel).
- **Staging mitten im Hauptmenü (Reihe 1).** Die vierzehn freien Innenplätze würden ausreichen, aber Staging-Einträge wären für alle sichtbar und in die Minispiele gemischt. Das verwirrt, weil die Minispiele für alle da sind.
- **Größere Kiste.** Löst den Platz, aber nicht das Inventarproblem, und ändert die Reihenzahl wie oben.

Gewählt wird das Untermenü, weil es das Hauptmenü für Spieler ohne Staging-Recht unverändert lässt, nur einen Platz belegt und die Team-Einträge trennt.

Test: `NavigatorLayoutTest` (Belegung der Plätze mit und ohne Rechte), `NavigatorStagingMenuTest` (Untermenü zeigt nur die Plätze, Zurück-Taste führt in den Navigator).

SOLID: Open/Closed. Das Untermenü ist eine weitere Ansicht über denselben Mechanismus `SharedNavigator`, und kein Eintrag wird im Hauptmenü verändert.

### D2 Inventare: eigene Ansicht pro Menü, Schlüssel nur über die Rechte dieser Ansicht

Heute ergibt die Teilmengen-Schleife über alle gebundenen Ziele 2 hoch der Rechte je Sprache. Mit acht gebundenen Zielen wären das 256 × 2 = 512 Inventare, jedes mit 54 Items. Zwei Schlüsselräume lösen das:

- **Hauptmenü**: Schlüssel sind die vier Live-Rechte (Slender, Bounce, Tamias, Build) plus ein abgeleitetes Bit „mindestens ein Staging-Recht“ (der Hub-Eintrag). 2^5 = 32 je Sprache, also 64.
- **Untermenü**: Schlüssel sind die vier Staging-Rechte. 2^4 = 16 je Sprache, also 32.

Insgesamt 96 Inventare, alle beim Start angelegt, wie heute. Es entsteht kein Inventar beim Öffnen, kein neuer Listener und kein pro-Spieler-Objekt, daher bleibt die Anforderung „Öffnen häuft nichts an“ gültig.

Alternativen:

- **Lazy Cache pro Teilmenge** (`computeIfAbsent`, Aufbau beim ersten Öffnen auf dem Tick-Thread). Generisch, bleibt bei 512 Einträgen begrenzt, aber der erste Öffnen-Klick baut, und der Cache muss gegen Zugriffe aus dem Lesethread geschützt sein, weil Menüs nur auf dem Tick-Thread entstehen. Das wäre die richtige Wahl, sobald ein fünftes Live-Recht dazukommt. Heute ist der Gewinn zu klein.
- **Ein Menü pro Spieler.** Verletzt die bestehende Anforderung der geteilten Inventare und erzeugt bei jedem Öffnen Objekte.
- **Alle 512 vorab.** Kosten rund 27 000 Items beim Start für Kombinationen, die nie gebraucht werden.

Built-in first: Aves stellt die Inventare bereit, und die Schlüssel sind reine Mengen. Kein eigenes Cache-Framework.

Test: `NavigatorPermissionTest` (96 Inventare beim Start, 64 davon Hauptmenü, Zuordnung je Rechteset), `NavigatorModuleLeakTest` (Listener beim Öffnen unverändert).

SOLID: Single Responsibility. Die Schlüsselbildung je Ansicht steht in einer kleinen Funktion, die `NavigatorModule` nur aufruft.

### D3 Ziele: Tamias und vier Staging-Einträge

Die Einträge als Vorschlag. Namen, Symbole und Verläufe sind **anpassbar**:

| Eintrag | Task | Recht | Symbol | Verlauf | Staging |
| --- | --- | --- | --- | --- | --- |
| Tamias (live) | `Tamias` | `titan.navigator.tamias` | `AMETHYST_SHARD` | `<gradient:#b388ff:#ff80ab>` | nein, Minispiel |
| Slender (Staging) | `Slender-Staging` | `cygnus.staging.join` | `STRUCTURE_BLOCK` | `<gradient:#616161:#ffaa00>` | ja |
| Bounce (Staging) | `Bounce-Staging` | `bounce.staging.join` | `SLIME_BLOCK` | `<gradient:#616161:#00FA9A>` | ja |
| ElytraRace (Staging) | `ElytraRace-Staging` | `elytrarace.staging.join` | `FIREWORK_ROCKET` | `<gradient:#616161:#fcba03>` | ja |
| Tamias (Staging) | `Tamias-Staging` | `tamias.staging.join` | `AMETHYST_BLOCK` | `<gradient:#616161:#b388ff>` | ja |

Tamias live ist ein Minispiel: Es hat Schnellbeitritt und Serverauswahl und steht in `ServerPicker.DESTINATIONS`. Die Staging-Einträge stehen nicht darin, wie beim heutigen Slender (Staging). Der Name „Tamias“ wird nicht übersetzt, wie bei den anderen Eigennamen.

Die Staging-Einträge tragen jeweils ihr eigenes Recht, das dem `requiredPermission` des Tasks entspricht. Ein Recht schaltet Navigator und Proxy gleichzeitig frei.

Test: `NavigatorDestinationTest` (Slot, Recht, Task und Staging-Markierung je Eintrag, `DESTINATIONS` enthält Tamias nicht als Staging), `ServerPickerTest` (Tamias in `DESTINATIONS`).

SOLID: Open/Closed. Neue Einträge sind neue Enum-Werte mit demselben Muster wie Slender und Bounce.

### D4 CloudNet-Port in `core`, Adapter in `bridge`

Die Richtung des Pfades bleibt wie beim Zählen der Spieler: Feature → Core-Port → Platform-Glue → JDK-Halter in `common` → CloudNet-Adapter in der Extension.

- **Core** (`net.onelitefeather.titan.core.service`, neu):

  ```java
  public interface OnDemandServices {
      Optional<List<ServiceState>> services(String task); // leer = nicht lesbar
      boolean start(String task);                          // true = Anfrage angenommen
      boolean stop(String serviceName);                    // idempotent, true = Anfrage angenommen
  }
  public record ServiceState(String name, Phase phase, int online, int max) {
      public enum Phase { STARTING, RUNNING, STOPPED }
      public boolean ready() { return phase == Phase.RUNNING && max > 0; }
  }
  ```

  Dazu `FakeOnDemandServices` und `OnDemandServicesContract` in `core/testFixtures`, analog zu `FakePlayerCounts` und `PlayerCountsContract`. `ServiceState.ready()` nutzt dieselbe Regel wie `TaskJoin.joinable`, damit Schnellbeitritt und Staging dieselbe Definition von „bereit“ haben.
- **Common**: `TitanServiceControl` ist ein JDK-only Halter mit einem Adapter-Interface, wie `TitanServerConnector`. Ohne installierten Adapter melden `services` leer, `start` und `stop` `false`.
- **Platform** (`platform/cloudnet`): `HolderOnDemandServices implements OnDemandServices`, Bean nur im Profil `CLOUDNET`. Das Modul kennt keine CloudNet-Typen, weil das Modul das nicht darf (`ModuleBoundariesTest`). Der Brief nennt den CloudNet-Adapter für Platform, hier liegt er stattdessen in `bridge`. Platform hält nur die JDK-Seite.
- **Bridge**: `TitanBridgePermissionExtension` installiert den Adapter, der `CloudServiceProvider.servicesByTask`, `CloudServiceFactory.createCloudService` und `serviceProvider(uuid).stop()` aufruft. Die Abbildung von `ServiceLifeCycle` auf `Phase` steht in einer reinen Klasse ohne CloudNet-Typen und ist testbar.

Built-in first: Die CloudNet-API ist die einzige Quelle für Dienste. Ein eigener Prozessmanager oder Shell-Aufrufe wurden verworfen. CloudNets `ServiceTaskProvider` wurde nicht gewählt, weil Titan nur Dienste eines Tasks braucht, nicht die Task-Konfiguration.

Alternativen:

- **Adapter direkt in `platform/cloudnet`.** Nicht möglich, weil das Modul keine CloudNet-Abhängigkeit hat und die CloudNet-Classloader-Welt nur über die Extension erreichbar ist (wie bei `PlayerManager`).
- **CloudNet-Events statt Abfrage.** Siehe D7.

SOLID: Dependency Inversion. Das Feature hängt nur vom Port in `core` ab. Interface Segregation: der Port umfasst nur die drei Aufrufe, die das Feature braucht.

Test: `OnDemandServicesContract` (für Fake und Holder gleich), `HolderOnDemandServicesTest` (ohne Adapter: leere Liste, keine Anfrage), `ServiceStateMappingTest` in `bridge` (PREPARED → STARTING, RUNNING → RUNNING, STOPPED und DELETED → STOPPED, unbekannte Werte → STARTING). Die Verbindung zu einem echten CloudNet läuft nur manuell (Task 7.2).

### D5 Zustände: `PREPARED` ist „startet“, `ServiceListing` muss PREPARED mitliefern

- `PREPARED` ist „startet“. `STARTING` gibt es in der RC18-API nicht.
- Unbekannte oder künftige Zustände zählen als `STARTING`. Ein Fehler in dieser Richtung verhindert einen Doppelstart, und der Leerlauf-Stopp räumt einen Fehlstart wieder auf.
- `RUNNING` plus angekündigtes Spielerlimit (`max > 0`) ist „bereit“. Das Limit meldet Titan selbst beim Hochfahren über `TitanPlayerLimit`. Wer nur `RUNNING` sieht, sieht noch keine fertige Lobby.
- `ServiceListing.running` filtert heute auf `RUNNING`. Für den Start muss die Liste auch `STARTING` liefern, sonst zählt eine laufende Anfrage als „nichts läuft“ und löst einen zweiten Start aus. Der bestehende Schnellbeitritt und die Serverauswahl lesen weiterhin nur `RUNNING`, damit sie sich nicht ändern.

Test: `ServiceStateMappingTest` (Bridge), `OnDemandServicesContract` (`STARTING` wird gezählt, aber nicht als bereit).

### D6 Start und Doppelstart

Für jeden Staging-Klick, der die Dienste off-tick liest, gilt diese Reihenfolge:

- Lesen schlägt fehl oder der Port ist leer: Ablehnung im Chat („Servers » …“), kein Start, Navigator bleibt offen. Das folgt der bestehenden Regel „Unlesbare Dienste werden im Chat abgelehnt“. Fehlende Information darf nie als „läuft nirgends“ gelten.
- Ein Dienst im Zustand `STARTING` oder `RUNNING` existiert: kein Start. Der Spieler wird einsortiert: bei `RUNNING` und bereit sofort verbunden, sonst wartend (D7).
- Kein Dienst existiert, aber der lokale Cooldown für diesen Task läuft (30 Sekunden seit dem letzten eigenen Start): kein Start, der Spieler wartet.
- Sonst: `start(task)`, Cooldown setzen, der Spieler wartet.

Der Cooldown ist lokal und überbrückt die Verzögerung, mit der CloudNet einen frisch angelegten Dienst in der Liste zeigt.

Zwischen Lobby-1 und Lobby-2 gibt es keine Sperre. Zwei Spieler, die im selben Lesefenster (Millisekunden bis wenige Sekunden) auf zwei Lobbys klicken, können zwei Dienste anlegen. Der Schaden ist klein: ein zusätzlicher leerer Dienst, den der Leerlauf-Stopp nach zehn Minuten wieder beendet. Eine verteilte Sperre über Persistenz wurde verworfen, weil sie für diesen Fall zu viel Infrastruktur kostet.

Test: `StagingStartsTest` (Entscheidungstabelle mit Fake-Port: Lesefehler, `PREPARED`, `RUNNING`, Cooldown, kein Dienst), `NavigatorStagingStartTest` (Integration mit Cyano-Env: ein Klick startet genau einen Dienst, ein zweiter Klick startet keinen).

SOLID: Single Responsibility. Die Entscheidung liegt in einer Klasse ohne Minestom-Typen (`StagingStarts`), die `NavigatorModule` nur verdrahtet.

### D7 Warten auf Bereitschaft

- **Prüfzeitpunkt**: der bestehende Refresh-Zyklus des Navigators (`navigator.refreshSeconds`, Standard 5). Ein Zyklus liest ohnehin alle Tasks off-tick. Es entsteht kein zweiter Timer, und die Verzögerung bis zur Verbindung ist höchstens ein Zyklus.
- **CloudNet-Events** (`CloudServiceLifecycleChangeEvent`) wären schneller, verlangen aber einen Listener in `bridge`, einen Thread-Wechsel und Verwaltung der Abonnements. Verworfen für diesen Schritt. Revisit, wenn die Verzögerung stört.
- **Warteliste** je Task: Eine Menge von Spieler-UUIDs. Wer ankommt, wird eingetragen. Bereit: jeder noch wartende Spieler, der weiter online ist und das Recht noch hat, wird per `connectToTask` verbunden (wie heute). Verliert er das Recht, wird er still gestrichen, ohne Chat (die Regel „Recht entzogen“ des Navigators).
- **Zeitlimit**: 90 Sekunden seit dem Start, gemessen am injizierten `Clock`. Nach Ablauf erhalten alle Wartenden eine Ablehnung im Chat („Servers » … nicht rechtzeitig bereit“). Der Dienst wird nicht gestoppt, der Leerlauf-Stopp übernimmt das, falls niemand mehr da ist. Das Limit steht im Code, nicht in der Konfiguration (offene Frage 3).
- **Logout**: Ein Spieler, der während des Wartens die Verbindung verliert, wird aus der Warteliste entfernt. Der Dienst läuft weiter, weil andere ihn brauchen können. Dazu genügt ein einziger Listener für `PlayerDisconnectEvent`, der beim Start registriert wird, nicht einer pro Spieler.
- **Zweiter Klick** desselben Spielers: keine zweite Anfrage. Die Aktionsleiste zeigt den Wartehinweis erneut. Ein anderer Spieler wird der Warteliste desselben Tasks hinzugefügt.
- **Aktionsleiste**: Der Hinweis wird beim Klick gesendet und bei jedem Refresh erneut, solange der Spieler wartet. Die Aktionsleiste verschwindet nach wenigen Sekunden, deshalb bleibt sie nur für Wartende sichtbar. Offene Frage 4.
- **Ablauf**: Das Ergebnis des Lesens kommt auf dem Lesethread an und wird auf dem Tick-Thread entschieden, so wie es die Serverauswahl heute macht (`scheduleNextTick`). Minestom-Spieleraktionen laufen nur auf dem Tick-Thread.

Test: `StagingStartsTest` (Zeitlimit mit fortgeschaltetem Clock, Logout, Recht während des Wartens entzogen, zweiter Klick ohne zweiten Start), `NavigatorStagingStartTest` (Integration: Dienst wird bereit, Spieler wird verbunden; Ticks explizit).

SOLID: Single Responsibility. Warteliste und Zeitlimit sind eine Klasse, die Aktionsleiste ist ein Ausgabeport (`Messages`).

### D8 Leerlauf-Stopp

- **Markierung im Code**: `Destination` bekommt eine Eigenschaft `onDemand`. Nur die vier Staging-Einträge tragen sie. Die Konfiguration kann sie nicht setzen. So kann ein falscher Eintrag in `navigator.yaml` niemals einen Live-Task stoppen.
- **Konfiguration**: `navigator.stagingIdleSeconds` in `features/navigator/src/main/resources/titan/defaults/navigator.yaml`, Standard `600`, zulässig 60 bis 86400. Ein ungültiger Wert bricht den Start ab, so wie `lobbyswitcher.refreshSeconds` (Spec `lobby-switcher`). Der Wert wirkt nur auf die Leerlaufzeit. Titel, Ziele und Plätze bleiben fest.
- **Algorithmus** je Refresh-Zyklus, für jeden Staging-Task:
  1. Dienst läuft (`RUNNING`), `online == 0`, noch nicht gemerkt: Zeitpunkt des ersten Leers merken (`Clock`).
  2. Dienst hat wieder Spieler oder verschwindet: Merkmal löschen.
  3. Dienst ist seit `stagingIdleSeconds` leer: vor dem Stopp frisch lesen. Ist er dann immer noch leer und `RUNNING`, `stop(name)` aufrufen und den Dienst in eine kurze Sperrliste (60 Sekunden) setzen, damit der nächste Zyklus nicht erneut stoppt.
- **Lesefehler** friert alles ein: kein Stopp, keine Fortschreibung der Leerlaufzeit. Ein unbekannter Zustand wird nie als leer gezählt.
- **Zwei Lobbys**: Beide lesen dieselben CloudNet-Daten und führen ihre eigene Uhr. Der zweite `stop` trifft auf einen gestoppten Dienst und ist ein No-op (`stop` liefert `false`, nur der erste Aufruf loggt INFO). Das macht `stop` idempotent.
- **Stopp leert nie einen besetzten Dienst**, weil die Entscheidung nur auf `online == 0` beruht und der frische Lesestand kurz vor dem Aufruf geprüft wird.

Alternativen:

- **Zählen über Refresh-Zyklen** statt `Clock`. Nicht gewählt, weil das die Leerlaufzeit an `refreshSeconds` koppelt und die Projektregel verletzt, dass Zeit injiziert wird.
- **Eigener Timer je Dienst.** Nicht nötig; der Refresh-Zyklus besteht schon.

Test: `StagingIdleTest` (Unit mit fortgeschaltetem Clock: nach 600 s gestoppt, vor 600 s nicht, Spieler setzt zurück, Lesefehler friert ein, zwei Stopps hintereinander ergeben einen Aufruf mit Erfolg, Live-Tasks werden nie gestoppt, `stop` in der Schleife doppelt), `NavigatorSettingsTest` (Standard 600, Werte 0, 59, 86401 und Text brechen den Start ab).

SOLID: Single Responsibility. Die Uhr für Leerlauf ist eine Klasse (`StagingIdle`) und kennt keine Menüs.

### D9 Telemetrie und Logs

Metriken (Instrumente werden einmal beim Start angelegt, Attribute sind feste Werte):

| Name | Instrument | Einheit | Attribute (Kardinalität) | Frage, die sie beantwortet |
| --- | --- | --- | --- | --- |
| `titan.navigator.staging.starts` | Counter | `{start}` | `destination` (4), `result` (`requested`, `skipped_starting`, `cooldown`, `failed`) | Wie oft startet Titan, und greift die Sperre gegen Doppelstarts? Alarm bei `failed` > 0 über 10 Minuten. |
| `titan.navigator.staging.wait` | Histogramm | `s` | `destination` (4), `result` (`ready`, `timeout`, `left`) | Liegt die Wartezeit nahe am Limit von 90 Sekunden? Zeigt, ob das Limit passt. |
| `titan.navigator.staging.stops` | Counter | `{stop}` | `destination` (4), `result` (`requested`, `failed`) | Wie oft stoppt der Leerlauf? Viele Stopps bei wenigen Starts deuten auf einen falsch gesetzten `minServiceCount`. |

Keine Spieler-UUIDs, keine Namen als Attribute. Der Dienstname kommt nur in die Logs.

Logs (Englisch, Platzhalter, Level nach den Vorgaben des Schemas):

- INFO bei jeder angenommenen Start- und Stopp-Anfrage mit Task und Dienstname. Das ist eine Zustandsänderung eines Dienstes, nicht eine Spieleraktion, daher INFO erlaubt. Ohne Spielernamen.
- WARN, wenn ein Start ins Zeitlimit läuft oder ein Stopp fehlschlägt (degradiert, aber erholbar).
- DEBUG für jeden Klick und jede Entscheidung.
- ERROR nur, wenn der Operator eingreifen muss. Das ist hier nicht der Fall.

Spans: keine neuen. Ein Klick wird vom bestehenden Span `navigator.select` erfasst, um das Ergebnis `started` oder `pending` erweitert.

Test: `NavigatorStagingTelemetryTest` mit `OpenTelemetryExtension` (In-Memory-Metrik-Reader und Log-Appender): Name, Einheit, Wert und Attribute je Ergebnis. Die INFO-Zeile wird über den Log-Appender geprüft, nicht über Konsolen-Ausgabe.

### D10 Texte

Neue Schlüssel, je in `messages_de.properties` und `messages_en.properties`, und in `NavigatorMessages.KEYS`:

- `titan.navigator.staging.menu.title`: „Staging“ / „Staging“.
- `titan.navigator.staging.starting`: Aktionsleiste. DE „Die Umgebung „{0}“ startet – du wirst verbunden, sobald sie bereit ist.“ EN „Starting „{0}“ – you will be connected once it is ready.“
- `titan.navigator.staging.timeout`: Chat-Ablehnung mit Präfix „Servers »“. DE „Die Umgebung „{0}“ ist nicht rechtzeitig bereit. Versuch es später noch einmal.“ EN „The environment „{0}“ was not ready in time. Try again later.“
- `titan.navigator.action.start`: Klickzeile „▸ Linksklick  Starten und verbinden“ (DE) und „▸ Left click  Start and connect“ (EN).
- `titan.navigator.hint.staging.ondemand`: zusätzliche Lore-Zeile „Startet bei Bedarf“ bei den Staging-Einträgen.

Der Name „{0}“ ist der Anzeigename des Eintrags und nicht übersetzt. Der Schlüssel `titan.navigator.hint.staging` bleibt bestehen.

Test: die vorhandene i18n-Prüfung (Schlüssel in jedem Bundle, verwendete Schlüssel im Fallback), erweitert um die neuen Schlüssel.

SOLID: Interface Segregation. `NavigatorMessages` bleibt der einzige Ort für Texte.

## Risiken / Abwägungen

- **Die CloudNet-API weicht von der Annahme ab** (Start-Semantik, `ServiceConfiguration` für einen Task). → Spike Task 2.0 gegen das RC18-Jar mit `javap` und einem kurzen Lauf in der Test-CloudNet-Instanz. Ergebnis fließt in den Adapter, nicht in den Port.
- **Doppelstart zwischen Lobbys** (D6). → Lokaler Cooldown, Leerlauf-Stopp räumt auf, Metrik `starts{result=skipped_starting}` zeigt, ob die Sperre greift.
- **Staging-Task mit `minServiceCount` größer als 0.** CloudNet startet den gestoppten Dienst neu, und der Leerlauf-Stopp wiederholt sich. → Ops-Checkliste (Abschnitt Migration). Die Metrik `stops` steigt dann auffällig; Alarm.
- **Ein Staging-Task ohne Titan-Markierung im Code**, etwa wenn die Liste erweitert wird. → Test: die Liste der `onDemand`-Einträge ist exakt die vier Staging-Einträge, und kein Live-Eintrag trägt die Markierung.
- **Alte Bridge mit neuer Lobby oder umgekehrt.** → Ohne Adapter liest der Port leer, und der Start wird abgelehnt. Es wird nichts gestartet, aber Staging-Klicks schlagen fehl. Deshalb `bridge` und Lobby zusammen ausrollen.
- **Zu kurzes Zeitlimit** bei langsamen Starts. → Histogramm `wait`. Das Limit ist im Code und lässt sich nach Messung anpassen.
- **Ein Wartender verliert das Recht** → still gestrichen, wie beim Rechte-Entzug heute.
- **Aktionsleiste flackert** (sie verschwindet nach wenigen Sekunden und kommt beim nächsten Refresh wieder). → Akzeptiert, offene Frage 4.
- **Viele Menüs wachsen weiter mit der Zahl der Rechte.** → Bei einem fünften Live-Recht wird D2 neu bewertet (Lazy Cache).

## Migrationsplan

Reihenfolge beim Rollout (alles durch Ops, außer Titan-Release):

1. CloudNet-Tasks anlegen: `Tamias` (live), `Bounce-Staging`, `ElytraRace-Staging`, `Tamias-Staging`. `Slender-Staging` prüfen. Alle vier Staging-Tasks mit `minServiceCount: 0`, `requiredPermission` wie in D3. Das Recht im Proxy muss dem im Code entsprechen.
2. LuckPerms-Rechte setzen (zunächst nur an das Team).
3. `bridge` und Lobby gemeinsam ausrollen (Rolling Restart wie im Betriebsablauf für mc01). Vorher prüfen, dass der Adapter in beiden Lobbys installiert wird (Log „Service control installed“).
4. Smoke-Test (Task 7.2) auf einer Staging-Umgebung.

Rollback: Titan auf die alte Version zurücksetzen. Die alte Version kennt keinen Leerlauf-Stopp. Laufende Staging-Dienste müssen deshalb per CloudNet-Befehl gestoppt werden, sonst laufen sie dauerhaft. Die Tasks dürfen bleiben.

## Offene Fragen

Diese Fragen können nach der Umsetzung geklärt werden, ohne Spec oder Aufgabenliste zu ändern:

1. Ob `createCloudService` den Dienst direkt startet oder nur vorbereitet, und die genaue `ServiceConfiguration`-Erzeugung für einen Task. Wird in Task 2.0 geklärt, der Port bleibt gleich.
2. Ob der Bridge-Aufruf `connectToTask` das `requiredPermission` des Tasks beim Verbinden prüft. Titan verlässt sich nicht darauf, weil der Navigator das Recht schon vor dem Klick prüft.
3. Ob das Zeitlimit von 90 Sekunden konfigurierbar sein soll. Aktuell: fest im Code.
4. Ob die Aktionsleiste bei langen Wartezeiten reicht oder ein Titel nötig ist.
5. Feinheiten der Platzwahl (Tamias auf 14) und der Symbole/Verläufe, siehe D3. Die Namen der Tasks und Rechte sind laut Auftrag vorläufig.
6. Ob die Lobby schon vor der Ankündigung des Limits Spieler annehmen kann. Das bestimmt, ob „bereit“ mit `max > 0` korrekt ist. Falls nicht, wird die Bereitschaftsregel in D5 und in der Spec angepasst. Prüfung im Spike (Task 2.0).
