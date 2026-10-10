# Proposal

## Why

Unter CloudNet schickt `MessageChannelDeliver` Spieler über den statischen Halter `TitanServerConnector` zur Bridge-Extension. Fehlt die Extension oder scheitert ihr `initialize()`, ist dort kein Connector installiert, und `connectToTask`/`connectToServer` tun ohne jede Ausgabe nichts. Ein Spieler klickt ein Navigator-Ziel (auch das neue „Build“), und nichts passiert; niemand sieht warum. Eine falsch ausgelieferte Lobby fällt so erst durch Spielerbeschwerden auf.

## What Changes

- **Warnung pro verpasster Weiterleitung**: `TitanServerConnector.connectToTask`/`connectToServer` melden per `boolean`, ob sie weitergegeben haben. `MessageChannelDeliver` loggt bei `false` eine `WARN`-Zeile mit Spielername, UUID, Art (Task/Server) und Ziel. Der Halter bleibt statisch und JDK-only (die Classloader-Grenze bleibt begründet).
- **Startprüfung**: Nach dem Laden der Extensions prüft die Lobby, ob unter CloudNet (`.wrapper` vorhanden) ein Connector installiert wurde, und loggt sonst einmal `ERROR` mit Hinweis auf die Extensions `TitanCloudNetPermissions` und `CloudNet_Bridge`. Ohne CloudNet loggt sie nichts. Der Aufruf steht in `TitanApplication.main` nach `bootstrap.start(...)`, weil die Extensions erst dort initialisiert werden (design.md D2); eine Prüfung im Konstruktor von `Titan` liefe zu früh.
- Kein Spieler-Chat, keine neue Konfiguration, keine neue Abhängigkeit.

## Capabilities

### New Capabilities

_Keine._

### Modified Capabilities

- `lobby-navigator`: Neue Anforderung „Fehlende CloudNet-Anbindung wird gemeldet“ (ADDED). Sie ergänzt „Auswahl eines Ziels leitet weiter“ um den Fall „CloudNet läuft, die Bridge fehlt“; die Anforderung dort und ihr Szenario „Weiterleitung ohne Cloud“ bleiben unverändert.

## Impact

- **Code**: `common/.../deliver/TitanServerConnector.java` (Rückgabewert), `common/.../deliver/MessageChannelDeliver.java` (WARN), neu `common/.../deliver/ConnectorStartupCheck.java`, Aufruf in `runtime/.../TitanApplication.java`. Tests in `common`.
- **Abhängigkeiten**: keine neuen.
- **Nutzertexte**: keine (Logzeilen für Betreiber, Englisch).
- **Verhalten**: Nur Läufe mit `.wrapper` ohne Connector ändern sich, und nur durch Logzeilen. Der ERROR erscheint dort, wo bisher nichts auffiel.
- **Zusammenspiel mit `optional-extensions-bootstrap`**: siehe design.md, „Abgrenzung“.

## Delivery

Typ `feat`, Scope `bridge`: neue Diagnose, kein geändertes Weiterleitungsverhalten. „Bridge fehlt“ ist ein Betriebsfehler, den die Lobby bislang nicht meldete; sie leitet danach genauso wenig weiter wie vorher, also keine Fehlerkorrektur.

PR-Titel: `feat(bridge): warn when the server connector is missing`

Nicht-Ziele: Chat-Nachricht an den Spieler (Empfehlung in design.md, Folge-Change), Warnung in der Bridge-Extension bei fehlendem `PlayerManager`, Abbruch des Starts, Wiederholungsversuche.
