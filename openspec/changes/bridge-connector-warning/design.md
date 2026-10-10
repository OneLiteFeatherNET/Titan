# Design

## Context

Motivation: proposal.md. Anforderung: `specs/lobby-navigator`.

Ist-Zustand (geprüft):

- `DeliverProvider.create()` wählt bei `CloudNetEnvironment.isPresent()` (Verzeichnis `.wrapper`, `CloudNetEnvironment.java:34`) `MessageChannelDeliver`, sonst `DebugDeliver` (`DeliverProvider.java:32-37`).
- `MessageChannelDeliver.sendPlayer` (`MessageChannelDeliver.java:30-44`) ruft `TitanServerConnector.connectToTask/connectToServer`.
- `TitanServerConnector` (`TitanServerConnector.java:26-49`) hält `static volatile ServerConnector connector`. Ist er `null`, kehren beide Methoden ohne Log zurück (`:37-42`, `:44-49`).
- `TitanBridgePermissionExtension.initialize()` (`bridge/.../TitanBridgePermissionExtension.java:59-80`) installiert den Connector. Er hängt an `CloudNet_Bridge` (`@ExtensionInfo … dependencies`, `:53-55`). Ist der `PlayerManager` nicht registriert, tut auch der installierte Connector still nichts (`:82-85`).
- Was der Start heute prüft: `VariantStartupCheck.verify` (Avaje-Module, `Titan.java:59`) und `PermissionStartupLog.activeService` (`Titan.java:65`). Beide laufen im Konstruktor von `Titan`. Keine Prüfung betrifft den Connector.
- **Ladereihenfolge**: `ExtensionBootstrap.init()` (`TitanApplication.java:125/129`) lädt die Extensions bis `gotoPreInit()`. `initialize()` der Extensions läuft erst in `bootstrap.start(host, port)` als `extensions.gotoInit()` unmittelbar vor `server.start(...)` (minestom-extensions 2.2.0, `ExtensionBootstrap.start`), `gotoPostInit()` danach. `main` ruft `new Titan()` (`TitanApplication.java:59`) also **vor** `start` (`:71`). Der Connector ist im Konstruktor von `Titan` garantiert noch nicht installiert.

## Goals / Non-Goals

**Goals:**
- Der Betreiber sieht beim Start und bei jedem verpassten Klick, dass die Bridge fehlt.
- Der Halter bleibt statisch (Classloader-Grenze, siehe Javadoc `TitanServerConnector`).

**Non-Goals:**
- Chat-Feedback, Start-Abbruch, Warnung im `PlayerManager`-Fall der Extension.

## Decisions

### D1: `boolean` aus dem Halter, WARN in `MessageChannelDeliver`

`connectToTask`/`connectToServer` liefern `true`, wenn ein Connector den Aufruf übernommen hat, sonst `false`. `MessageChannelDeliver` kennt den `Player` und loggt bei `false`: `WARN Server connector missing: cannot send {} ({}) to {} {}` (Name, UUID, `task`/`server`, Ziel). Der Halter hat weder Namen noch Logger-Bedarf und bleibt JDK-only.
Rate: eine Zeile je verpasstem Klick, ohne Zustand. Verworfen: „einmal je Aufrufstelle“/Ratenbegrenzung. Beides braucht statischen Zustand (bricht Independent im Test) oder eine `Clock`; der Takt ist der eines menschlichen Klicks, wie bei `DebugDeliver` (`INFO` je Klick), und jeder Klick ist ein gestrandeter Spieler. Der Dauerhinweis kommt aus dem Start-ERROR (D2).
Built-in: SLF4J. SOLID: SRP (Halter überbrückt, Deliver berichtet). Test: Unit mit Logback-`ListAppender`, für beide `DeliverComponent`-Arten, mit und ohne Connector; `@AfterEach` setzt `setConnector(null)`.

### D2: Startprüfung nach `bootstrap.start`, ERROR

`ConnectorStartupCheck.verify(boolean cloudNetPresent, boolean connectorInstalled)` loggt bei `true, false` einmal `ERROR` (`CloudNet detected but no server connector installed - check the TitanCloudNetPermissions and CloudNet_Bridge extensions in extensions/; navigator transfers will do nothing`), sonst nichts. Die Überladung ohne Argumente liest `CloudNetEnvironment.isPresent()` und `TitanServerConnector.isInstalled()`. `main` ruft sie nach `bootstrap.start(...)` auf: dann sind `initialize()` und `postInit` der Extensions gelaufen (Context). Im Konstruktor von `Titan` wäre der Connector immer `null`, die Prüfung meldete immer fälschlich.
Ein Fehlschlag in `initialize()` der Extension bricht den Start nicht ab (Minestom loggt ihn nur), daher fängt die Prüfung auch diesen Fall.
Zeitpunkt: `server.start` hat zu diesem Zeitpunkt den Port schon geöffnet, ein Klick in den ersten Millisekunden bekäme also nur die WARN aus D1. Vertretbar; ein Hook zwischen `gotoInit` und `server.start` existiert nicht.
Level: `ERROR`, weil ein Betreiber handeln muss (die Lobby funktioniert, ihre Kernaufgabe nicht) und der Fehler sonst nur bei Spielern auffällt. Nicht `WARN`. Kein Start-Abbruch: Die Lobby ohne Weiterleitung ist besser als keine, und `VariantStartupCheck` prüft ohnehin nur Avaje-Module.
Ein Erfolgs-Log entfällt (KISS); die Extensions loggen ihr Laden selbst.
Built-in: SLF4J, kein Zustand. SOLID: SRP, reine Entscheidung mit injizierten Booleans. Test: Unit mit `ListAppender` (vier Kombinationen; nur `true,false` loggt genau einen ERROR, der `TitanCloudNetPermissions` nennt).

### D3: Chat-Feedback nur empfohlen

Empfehlung: `connectTo…` liefert schon `false` (D1), ein Folge-Change kann `MessageChannelDeliver` daran eine Chatzeile hängen lassen wie `DebugDeliver` (Inline-MiniMessage, `Placeholder.unparsed`, kein i18n-Bundle im Projekt). Nicht hier: neuer Nutzertext, und der Spieler kann nichts tun. Der Betreiber ist der Adressat.

## Abgrenzung zu `optional-extensions-bootstrap`

Der offene Change verschiebt `MessageChannelDeliver` und `CloudNetEnvironment` nach `platform/cloudnet` (dort D6, Task 3.2) und `main` in `TitanStartup` (Reihenfolge `init` → Scope → `start`). Kein Widerspruch, aber zwei Berührungspunkte:
- `ConnectorStartupCheck` liegt bei `TitanServerConnector`/`CloudNetEnvironment` in `common/deliver`. Wandert `CloudNetEnvironment` später nach `platform/cloudnet`, muss die Prüfung mit, und `runtime` darf sie nicht mehr direkt aufrufen. Dann braucht `TitanStartup` einen Schritt „nach `start`“, den `platform/cloudnet` befüllt (z. B. über den `ServerBootstrap`-Vertrag oder ein Bean). Der Change, der später landet, passt das an; die D2-Aussage („nach `start`, nicht davor“) gilt in beiden Fällen.
- Ohne die Extension-Plattform (`apps/local`) gibt es `.wrapper` nicht und damit keine Prüfung; das passt zu ihrem Szenario „ohne Cloud“.
Empfehlung: dieser Change zuerst (klein, entkoppelt), der Bootstrap-Change nimmt die Klasse mit und ergänzt den Nach-Start-Schritt.

## Risks / Trade-offs

- [`ERROR` in einem Setup mit `.wrapper`, aber bewusst ohne Bridge (z. B. lokaler CloudNet-Test)] → Hinweis nennt die Ursache; der Zustand ist im Betrieb ohnehin kaputt.
- [WARN je Klick füllt das Log, wenn jemand schnell klickt] → Menschlicher Takt, wie `DebugDeliver`; Navigator schließt beim Klick. Ratenbegrenzung nachrüsten, falls es auffällt.
- [Statischer Halter beeinträchtigt Tests] → Nur `setConnector(null)` in `@AfterEach`; die Prüfung selbst bekommt Booleans.
- [Reihenfolge-Annahme (`gotoInit` in `start`) hängt an minestom-extensions 2.2.0] → Manuelle Abnahme (Task 3.2) fängt eine Änderung; ein Test gegen die Bibliothek entfällt.

## Migration Plan

Kein Deploy-Schritt. Rollback: Revert.
