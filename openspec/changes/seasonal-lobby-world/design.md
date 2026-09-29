# Design

## Context

Motivation: siehe proposal.md. Anforderungen: `specs/lobby-seasons`, `specs/app-variants`.

Ist-Zustand:

- `PlatformBeans` (`runtime`, `@Factory`) erzeugt `InstanceContainer`, `MapProvider` (`MapProvider.create(Path.of(""), instance)`), `LobbySpawn`, `Clock`, `Scheduler` und mehr. `MapProvider` lädt die Map-Daten im Konstruktor (`loadMapData`), also bevor irgendeine Column startet.
- `MapPool` (`common`) listet die Verzeichnisse unter `worlds/`, die eine `map.json` haben, und wählt daraus `-DTITAN_LOBBY_MAP` (Standard `world`); gibt es genau eine Welt, nimmt es diese unabhängig vom Namen. Fehlt der Name, wirft `orElseThrow()` ohne Hinweistext.
- Eine Column hängt nur an `core` (`titan.column`), deklariert ihre Plattform-Typen per `@InjectModule(requires = ...)` und darf keine andere Column importieren. `runtime` deklariert nur `provides` und hat kein `requires` (`docs/lobby-modules.md`).
- `titan.app-variant` bindet **jede** Column unter `features/` ein, außer die Variante ruft `titanVariant { exclude(...) }` auf. `expectedModules` und die zusammengeführte `application.yaml` folgen dieser Auswahl; eine ausgeschlossene Column verliert auch ihre Standardwerte.
- `DaytimeModule` ist die kleinste Column und Vorlage: `@Singleton`, `@PostConstruct`/`@PreDestroy`, Scheduler-Task, `Clock` injiziert, Werte live über `Config`, ungültige Startwerte brechen ab.
- `StopCommand` (`features/admin`) ruft `MinecraftServer.stopCleanly()` auf einem eigenen Thread `titan-stop` auf und danach `System.exit(0)`; `stopCleanly()` darf nicht auf dem Tick-Thread laufen. Der Scheduler läuft auf dem `TickSchedulerThread`.
- Die Konfigurationsüberwachung (`config.watch.enabled`, Standard aus) aktualisiert die `Config`-Fassade; Module lesen live.

Querbezug: Der offene Change `optional-extensions-bootstrap` (ungetrackt) ändert `apps/cloudnet/build.gradle.kts` und `apps/local/build.gradle.kts` (`platform("extensions")`/`platform("cloudnet")`, nur `cloudnet`) und führt in `VariantStartupCheck` den aktiven Bootstrap als erwartetes Modul ein. Dieser Change ändert dort nur `apps/local` (`exclude("season")`); die Zeilen widersprechen sich nicht, der Merge ist mechanisch. Er ändert `app-variants` nicht als Spec (`Modified Capabilities: Keine`), also kollidieren die Deltas nicht. Beide Changes lockern textlich „`local` hat dieselben Funktionen wie `cloudnet`“; das Delta hier formuliert nur die Saison-Ausnahme, und wer zweiter archiviert, gleicht den Wortlaut an.

## Goals / Non-Goals

**Goals:**
- Eine Saison ist eine Welt; die Lobby wechselt nur durch Neustart, nie zur Laufzeit.
- Ohne die Column oder ohne aktive Saison ändert sich nichts.
- Entscheidungslogik (Auswahl, Vormerkung, Stopp) ohne Server testbar.

**Non-Goals:**
- Effekte auf der laufenden Welt (Blöcke, Displays, Sounds), Navigator-Icons, Chat-Präfix, Release-Stufen, `/season list`, Spielerhinweis, Saisonen in `local` (siehe proposal.md).
- Beheben der Doppelung mit `StopCommand` (siehe D4).

## Decisions

### D1: Column `features/season` nach dem Muster von `daytime`

Standard-`titan.column`-Modul, `implementation(libs.avaje.config)` und `libs.slf4j.api` wie `daytime`. Pakete: `net.onelitefeather.titan.feature.season`. Klassen:

- `SeasonSettings` (Schlüsselnamen), `Season` (Record: `id`, `world`, `from`, `to`, `enabled`),
- `SeasonCalendar` (rein: aus `List<Season>`, `Instant` und `ZoneId` die gewünschte Saison; Überlappungsregel),
- `SeasonConfigReader` (liest `seasons.*` aus `Config`, validiert und nennt qualifizierte Schlüssel),
- `RestartPolicy` (rein: gestartete Welt, gewünschte Welt, Spielerzahl → `Decision` `NONE`/`PENDING`/`STOP`),
- `SeasonModule` (`@Singleton`, Verdrahtung: Minutentask, Disconnect-Listener, Stopp).

Standardwerte in `src/main/resources/titan/defaults/season.yaml`:

```yaml
seasons:
  # Zeitzone der Fenster (java.time-Id). Ungültig = Startabbruch.
  zone: Europe/Berlin
  # <id>:
  #   world: winter                 # Verzeichnis unter worlds/, mit map.json
  #   from: 2026-12-01T00:00:00     # einschließlich, lokale Zeit
  #   to: 2027-01-07T00:00:00       # ausschließlich
  #   enabled: true                 # Abschalter, wirkt ohne Neustart der Konfiguration
```

Built-in: `java.time.LocalDateTime`/`ZoneId`, avaje-config. Verworfen: eigene Konfigurationsklassen mit Datei-Loader (nur `Config`-Fassade wie alle Columns); Jackson/JSON wie in PR #225 (kein Grund, wenn `application.yaml` reicht).
Kein Zugriff auf `daytime.zone`: Eine Column importiert keine andere, und das Lesen ihres Schlüssels koppelt still an eine fremde Einstellung, die in einer Variante ohne `daytime` fehlt. Deshalb eigene `seasons.zone`, Standard `Europe/Berlin` (Annahme, siehe unten).
Die Id `zone` ist reserviert (kollidiert mit `seasons.zone`) und bricht den Start ab.
SOLID: SRP (Kalender, Leser, Politik getrennt), DIP. Test: Unit für `SeasonCalendar`, `SeasonConfigReader`, `RestartPolicy`; Integration für `SeasonModule` (D5).

### D2: Weltwahl beim Start über `LobbyWorldChoice` (ServiceLoader)

`core` bekommt

```java
public interface LobbyWorldChoice {
    Optional<String> worldName();   // leer = Standardwelt
}
```

`PlatformBeans.mapProvider(InstanceContainer)` löst die Wahl über `ServiceLoader.load(LobbyWorldChoice.class).findFirst().flatMap(LobbyWorldChoice::worldName)` auf und reicht das Ergebnis an `MapProvider.create(path, instance, worldName)` und `new MapPool(path, filter, worldName)`. Ein übergebener Name gilt immer (auch bei genau einer Welt) und wirft bei Fehlen mit klarem Text; `Optional.empty()` verhält sich wie heute. `features/season` meldet `SeasonWorldChoice` (öffentlicher Konstruktor ohne Argumente, `Clock.systemUTC()` und `Config`, teilt `SeasonSchedule`/`SeasonConfigReader` mit dem Modul) über `META-INF/services` an; `shadowJar` führt Service-Dateien bereits zusammen (`mergeServiceFiles()`). Das ist dasselbe Muster wie `ServerBootstraps.select` in `optional-extensions-bootstrap` D2.

**Spike-Ergebnis:** Eine `LobbyWorldChoice`-Bean aus der Saison-Column, die `Scheduler`/`Clock`/`EventNode` aus `runtime` verlangt, ist beim Bau von `mapProvider(...)` in `runtime` immer leer, weil Avaje den Modul-Deadlock zugunsten von `runtime` auflöst. Sie funktioniert nur aus einem eigenen Gradle-Modul ohne `requires`, und das erzwänge Column-zu-Column-Importe, die `lobby-modules` verbietet. Deshalb ist der `ServiceLoader` der gewählte Weg und kein Ausweichweg; eine Optional-Bean gibt es nicht.
Es gibt keinen statischen Zustand zwischen Wahl und Modul: `SeasonModule` berechnet die gestartete Welt beim Start selbst neu (`CalendarStartedWorld`); liegen beide Berechnungen um eine Fenstergrenze, ist die nächste Prüfung ein Neustart, der sich selbst heilt.
Test: Unit für die Auflösung in `PlatformBeans` (mit und ohne Wahl) und für `SeasonWorldChoice`; ein Integrationstest mit echtem `BeanScope` in `apps/cloudnet` (Task 4.3) sichert die Verdrahtung.
SOLID: DIP (`runtime` kennt nur die Schnittstelle), OCP (weitere Wahlquellen docken an).

### D3: Zeitzone, Überlappung, Gültigkeit (Annahmen, vom Nutzer noch nicht bestätigt)

- **Überlappung:** Die Saison mit dem früheren `from` gewinnt, bei Gleichstand die kleinere Id; Warnung beim Start mit beiden Ids. Verworfen: Startabbruch (eine Überlappung ist ein Konfigurationsfehler, aber kein Grund, die Lobby zu stoppen) und „letzte gewinnt“ (abhängig von der Reihenfolge der Schlüssel).
- **Zeitzone:** `seasons.zone`, Standard `Europe/Berlin` (siehe D1).
- **Ungültige Konfiguration:** Jede *aktivierte* Saison wird beim Start geprüft: `world`, `from`, `to` vorhanden und lesbar, `from < to`, `worlds/<world>/map.json` existiert. Fehler brechen den Start mit qualifiziertem Schlüssel und Grund ab (`lobby-module-config`), etwa über `IllegalStateException`, wie `DaytimeModule.start()` bei ungültiger Zone. Abgeschaltete Saisons werden nicht geprüft, damit ein noch unfertiger Weltbau geparkt werden kann. Zur Laufzeit gilt wie bei `lobby-module-config` keine erneute Validierung mit Abbruch: Ein live ungültig gewordener Wert, oder eine live aktivierte Saison ohne Welt, wird als WARN (einmal je Wert) geloggt und zählt als nicht aktiv. Sonst würde ein Vertipper im laufenden Betrieb einen Neustart auslösen, der beim nächsten Start abbricht, und CloudNet würde den Dienst in einer Schleife neu starten.
- **Fensterhälften:** `from` einschließlich, `to` ausschließlich, lokale Zeit in `seasons.zone`; bei Zeitumstellungen gilt die Wanduhr.

Die `map.json`-Prüfung liest den Dateinamen als eigene Konstante in der Column, weil `MapEntry.MAP_FILE_NAME` in `common` liegt und eine Column nur an `core` hängt. Ein Test in `apps/cloudnet` (sieht beide) sichert die Gleichheit beider Werte.
Test: Unit (`SeasonCalendarTest`, `SeasonConfigReaderTest` mit `@TempDir`-`worlds/`, Zeit als `Instant`).

### D4: Entscheidungen und Stopp, Minutentakt plus Disconnect

`RestartPolicy.decide(startedWorld, desiredWorld, onlinePlayers)`:
- gleiche Welt → `NONE` (Vormerkung aufgehoben),
- Abweichung und `onlinePlayers > 0` → `PENDING`,
- Abweichung und `onlinePlayers == 0` → `STOP`.

`SeasonModule` merkt sich nur `restartPendingSince` (`Instant`, `volatile`). Beim Übergang `NONE → PENDING/STOP` loggt es einmal INFO `Restart for season {} pending since {}` (bzw. für die Standardwelt `Restart to the default world pending since {}`), beim Übergang zurück INFO `Restart no longer needed`. Jede Prüfung liest die gewünschte Welt neu (D3, live).
Auslöser: (a) wiederkehrender Scheduler-Task alle 1200 Ticks (`TaskSchedule.tick(1200)`, nicht `minutes(1)`: Dauer-Schedules laufen nach der Wanduhr und lassen sich nicht mit `env.tick()` treiben; Minestom-Scheduler statt eigenem Timer, damit er mit dem Server endet, wie bei `daytime`); (b) `PlayerDisconnectEvent` am `titan`-Node. Befund: Beim `PlayerDisconnectEvent` ist der ausscheidende Spieler bereits aus dem `ConnectionManager` entfernt. Der Listener plant die Prüfung trotzdem mit `scheduler.scheduleNextTick(...)`; das ist harmlos und bleibt.
Stopp: Eine kleine Nahtstelle `ServerStop` (spaltenintern, Standardimplementierung als `@Singleton`) startet wie `StopCommand` einen `titan-stop`-Thread mit `MinecraftServer.stopCleanly()` und `System.exit(0)` — auf dem Tick-Thread würde `stopCleanly()` auf sich selbst warten. Der Aufruf geschieht höchstens einmal (`AtomicBoolean`), damit Minutentakt und Disconnect nicht doppelt stoppen. Spielerzahl kommt aus einer ebenso spaltenintern deklarierten Schnittstelle `OnlinePlayers` (Standard: `ConnectionManager#getOnlinePlayerCount()`); Tests injizieren Fakes über den Konstruktor, statt den Server zu stoppen.
`StopCommand` bleibt unverändert (Non-Goal): Die Dopplung sind zehn Zeilen in zwei Columns, die sich nicht importieren dürfen. Eine gemeinsame Nahtstelle in `core` wäre ein eigener Change (`refactor`).
Akzeptierte Kompromisse (siehe auch Risiken): kein oberes Zeitlimit für das Warten; kleines Rennen zwischen Leer-Prüfung und Stopp, der Spieler landet dann wie bei jedem Stopp über den Proxy woanders.
Built-in: `Scheduler`, `ConnectionManager`, `MinecraftServer.stopCleanly()`. Verworfen: Kick mit Countdown und Weltwechsel zur Laufzeit (Instanz tauschen, Spieler umsetzen, Lichtberechnung, Entitäten, Zustand der übrigen Columns; alles, was der Neustart umgeht).
SOLID: SRP (`RestartPolicy` reine Funktion), DIP (`ServerStop`, `OnlinePlayers`). Test: Unit `RestartPolicyTest`; Integration `SeasonModuleTest` (Cyano-`Env`, `AdjustableClock` wie in `daytime`, `env.tick()`; gefälschtes `ServerStop` zählt Aufrufe).

### D5: Einbindung nur in `cloudnet`

`apps/local/build.gradle.kts` bekommt `titanVariant { exclude("season") }`; `apps/cloudnet` ändert sich nicht, weil `titan.app-variant` jede Column automatisch aufnimmt. Damit erwartet `cloudnet` `seasonColumn` im Startcheck, `local` nicht, und `local` liefert `season.yaml` nicht aus. Ein Starttest je Variante prüft das (`cloudnet`: `seasonColumn` geladen, `local`: nicht auf dem Klassenpfad). `@InjectModule(name = "seasonColumn", requires = {Scheduler.class, Clock.class, EventNode.class}, requiresString = {"net.minestom.server.event.EventNode<net.minestom.server.event.Event>:titan"})` nach dem Muster aus `docs/lobby-modules.md`; `ColumnArchitectureTest` wie bei `daytime`.
Verworfen: eine Gradle-Property `titan.season` zum Einschalten in `local` (`local` hat keinen Supervisor; ein Stopp würde die Entwicklungslobby einfach beenden).

### D6: Logging, Metriken, Spans, Nutzertexte

- Logs (SLF4J, Englisch): INFO beim Start `Lobby world {} (season {})` bzw. Standardwelt; WARN bei Überlappung `Seasons {} and {} overlap, {} wins`; INFO bei Vormerkung und Aufhebung (einmal je Übergang); INFO vor dem Stopp `Stopping lobby for season change`; WARN bei live ungültigem Wert (einmal je Wert). Nichts davon läuft pro Tick oder pro Spieler.
- Keine neuen Metriken oder Spans: Ein Saisonwechsel kommt einmal pro Zeitfenster vor; das Log genügt. Nutzertexte: keine, also kein i18n.

## Risks / Trade-offs

- [Bean-Auflösung zwischen `runtime` und Column scheitert (D2)] → Spike-Ergebnis: `ServiceLoader` statt Bean; Integrationstest mit echtem `BeanScope`.
- [Eine dauerhaft belegte Lobby verzögert den Saisonwechsel beliebig lang] → akzeptiert; `/stop` bleibt als Betreibermittel.
- [Rennen zwischen Leer-Prüfung und Stopp] → akzeptiert; der Spieler wird wie bei jedem Stopp über den Proxy umgeleitet.
- [Neustart-Schleife bei kaputter Welt nach Konfigurationsänderung] → live nur validierte Saisons lösen einen Neustart aus (D3); Startvalidierung bricht ab, statt eine kaputte Welt zu laden.
- [Fenstergrenze zwischen Weltwahl und `SeasonModule`-Start ] → höchstens ein zusätzlicher Neustart bei leerer Lobby.
- [Zwei offene Changes ändern `apps/local/build.gradle.kts`] → andere Zeilen; der Hauptkontext prüft beim Mergen (siehe Kontext).

## Migration Plan

1. Saisonwelten (`worlds/<name>/` mit `map.json`) über den Setup-Server bauen und im Dienst-Template ablegen, dann `seasons.<id>.*` in `application.yaml` (oder Profil) eintragen. Ohne `seasons.*` bleibt alles wie heute.
2. `titan-cloudnet.jar` deployen (AOT-Cache neu trainieren). Die Lobby wechselt im Fenster beim ersten leeren Moment.
3. Zurück: `seasons.<id>.enabled: false` (wirkt mit der Konfigurationsüberwachung ohne Neustart der Konfiguration; die Lobby stoppt bei leerem Stand und startet in der Standardwelt) oder Revert des Squash-Commits.

## Open Questions

- Sollen `seasons.zone`, Überlappungsregel und Prüfumfang (D3) so bleiben? Sie sind als Annahmen gesetzt, ändern aber weder die Aufgabenliste noch die Struktur; eine Korrektur trifft `SeasonCalendar` und `SeasonConfigReader`.
