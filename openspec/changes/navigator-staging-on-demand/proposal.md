# Proposal

## Why

Die Staging-Umgebungen laufen dauerhaft, obwohl das Team sie nur selten braucht. Slender (Staging) ist seit 4.7.0 der einzige Eintrag dieser Art. Bounce, ElytraRace und das neue Minispiel Tamias sollen je eine eigene Staging-Umgebung bekommen. Damit das nicht jedes Mal einen Dauerdienst kostet, startet Titan eine Staging-Umgebung erst beim Klick und stoppt sie nach Leerlauf wieder. Tamias bekommt zusätzlich einen normalen Live-Eintrag mit Schnellbeitritt.

## What Changes

- Neue Staging-Einträge für **Bounce** (Task `Bounce-Staging`, Recht `bounce.staging.join`), **ElytraRace** (Task `ElytraRace-Staging`, Recht `elytrarace.staging.join`) und **Tamias** (Task `Tamias-Staging`, Recht `tamias.staging.join`) neben Slender (Staging). Jeder Staging-Eintrag hat genau ein Recht, das dem `requiredPermission` seines CloudNet-Tasks entspricht. Die Namen sind vorläufig.
- Alle vier Staging-Einträge liegen in einem **Untermenü**. Der Navigator zeigt auf Platz 16 einen Eintrag „Staging“, der nur bei mindestens einem Staging-Recht erscheint. Slender (Staging) verlässt Platz 16 und wird Teil des Untermenüs.
- **Tamias** als Live-Minispiel: Task `Tamias`, Recht `titan.navigator.tamias`, Platz 14, Schnellbeitritt und Serverauswahl wie bei Slender. Symbol und Verlauf sind Vanilla-Items ohne Ressourcenpaket.
- **Start bei Bedarf** für alle Staging-Einträge: Ist kein Dienst des Tasks gestartet oder im Start, startet Titan einen über die CloudNet-API, zeigt dem Spieler einen Hinweis in der Aktionsleiste (Deutsch und Englisch) und verbindet ihn, sobald der Dienst bereit ist. Nach 90 Sekunden ohne Bereitschaft endet der Versuch mit einer Meldung.
- **Leerlauf-Stopp** für Staging-Dienste: Ein Dienst ohne Spieler über die Dauer von `navigator.stagingIdleSeconds` (Standard 600) wird gestoppt. Nur Tasks, die im Code als Staging markiert sind, werden gestoppt. Live-Tasks sind davon ausgeschlossen, egal was die Konfiguration sagt.
- **Menü-Varianten** wachsen von zweiunddreißig auf sechsundneunzig geteilte Inventare. Das Hauptmenü hält sich an fünf Schlüsselbits, das Untermenü an vier.
- **Telemetrie**: Zähler für Start- und Stopp-Anfragen, Histogramm für die Wartezeit bis zur Bereitschaft, INFO-Logs für jede Start- und Stopp-Anfrage mit Task- und Dienstname.
- Bestehende Ziele, Klicks und Rechte für Survival, Creative, Build, ElytraRace, Slender und Bounce bleiben unverändert. Ohne eines der neuen Rechte sieht kein Spieler etwas Neues außer der grauen Glasscheibe auf Platz 14.

## Capabilities

### New Capabilities

Keine.

### Modified Capabilities

- `lobby-navigator`: Tamias wird als rechtegebundenes Minispiel aufgenommen. Slender (Staging) wandert in ein Untermenü und startet bei Bedarf. Die Staging-Einträge für Bounce, ElytraRace und Tamias kommen hinzu. Platz-Anforderungen, die Anzahl der Menü-Varianten und die Konfiguration des Navigators ändern sich entsprechend. Siehe `specs/lobby-navigator/spec.md`.

## Type and Scope

Die Implementierung wird als **`feat(navigator)`** ausgeliefert, mit dem Pull-Request-Titel `feat(navigator): add on-demand staging entries and tamias`. Die Änderung ist nicht brechend. Die Spieler sehen nur neue Einträge, die bestehende Ziele nicht verschieben. Betrieblich ist sie dennoch nicht harmlos (siehe Impact). Alle Teile sind `feat`, daher gibt es keine zweite Änderung für Fehlerbehebungen oder Refactorings.

## Impact

- **Code**:
  - `core`: neuer Port `OnDemandServices` mit Testdouble und Vertragstest in `testFixtures`.
  - `common`: JDK-only Halter `TitanServiceControl`, analog zu `TitanServerConnector`.
  - `platform/cloudnet`: Verbindung vom Port zum Halter (nur JDK-Typen, keine CloudNet-Typen).
  - `bridge`: CloudNet-Adapter mit `CloudServiceProvider` und `CloudServiceFactory`. Die Dienstliste liest zusätzlich PREPARED-Dienste.
  - `features/navigator`: `Destination` (vier Staging-Einträge und Tamias, Markierung `onDemand`), `NavigatorModule` (Untermenü, Hub-Eintrag, Menüschlüssel), neue Klassen für Start-Warteschlange und Leerlauf-Stopp, `NavigatorSettings`, Telemetrie, Texte.
  - `apps/cloudnet`: Verdrahtungstest für den neuen Bean.
- **Abhängigkeiten**: keine neuen Laufzeit- oder Build-Abhängigkeiten. Die CloudNet-API ist in `bridge` bereits als `compileOnly` eingebunden.
- **Betrieb** (vor dem Rollout durch Ops, nicht durch Titan):
  - Tasks `Bounce-Staging`, `ElytraRace-Staging`, `Tamias`, `Tamias-Staging` anlegen. Die vier Staging-Tasks bekommen `minServiceCount: 0`. Für `Slender-Staging` muss das geprüft werden.
  - `requiredPermission` jedes Staging-Tasks auf das Recht setzen, das im Code steht.
  - LuckPerms-Rechte setzen: `titan.navigator.tamias`, `bounce.staging.join`, `elytrarace.staging.join`, `tamias.staging.join`.
  - `bridge` und Lobby gemeinsam ausrollen. Eine Lobby mit altem `bridge` findet keine Dienste und lehnt Staging-Klicks ab, ohne einen Dienst zu starten.
- **Nutzertexte**: neue Schlüssel für Aktionsleiste, Wartezeit-Hinweis, Zeitüberschreitung, Untermenü-Titel, Klickzeile „Starten und verbinden“ und Lore-Zeile „Startet bei Bedarf“. Alle Schlüssel gibt es in `de` und `en`, mit Englisch als Fallback.
- **Speicher**: sechsundneunzig statt zweiunddreißig kleine geteilte Inventare, einmal beim Start.

## Non-goals

- Keine Menüs pro Spieler und kein Cache-Eviction-Mechanismus; die Menüs bleiben geteilt.
- Keine Konfiguration, die Tasks oder die Staging-Markierung festlegt. Nur die Leerlaufzeit ist einstellbar.
- Kein Stopp von Live-Tasks, auch nicht bei Leerlauf.
- Keine verteilte Sperre zwischen Lobby-1 und Lobby-2. Ein doppelter Start ist selten und wird durch den Leerlauf-Stopp wieder aufgeräumt.
- Keine Änderung an der Prüfung des `requiredPermission` im Proxy.
- Keine Spiellogik für Tamias.

## Delivery

PR-Titel (Conventional Commit, Typ `feat`, Scope `navigator`, nicht brechend):

`feat(navigator): add on-demand staging entries and tamias`
