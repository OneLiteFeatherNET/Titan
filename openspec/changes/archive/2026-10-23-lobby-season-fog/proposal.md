# Proposal

## Why

Zu Halloween soll die Lobby die Atmosphäre des Slender-Minispiels zeigen: den Nebel der wartenden Slender-Lobby. Heute kennt die Saison-Konfiguration nur eine eigene Welt. Die Stimmung lässt sich nur über eine Welt mit eigener Dimension setzen, und die Dimension muss beim Start registriert sein, bevor Spieler sich verbinden.

## What Changes

- Jede Saison bekommt ein optionales Feld `seasons.<id>.atmosphere`. Der einzige Wert ist `slender-fog`. Fehlt das Feld, läuft die Saison wie bisher in der Overworld.
- Ein unbekannter Wert loggt beim Start eine Warnung mit Schlüssel und bekannten Werten. Die Lobby läuft dann ohne Nebel, der Start wird nicht abgebrochen.
- Die Atmosphäre der aktiven Saison wird beim Start gewählt, zusammen mit der Welt, über die bestehende SPI `LobbyWorldChoice` (neue Default-Methode `atmosphere()`). Die Lobby registriert die Dimension einmal, bevor die Instanz entsteht, und legt die Instanz mit ihr an.
- Die Dimension trägt die Werte der Slender-Lobby: Granskoga-Atmosphäre zu 30 % in Richtung `BRIGHT` gemischt, mit der Overworld-Uhr als `defaultClock`, damit der Tagesverlauf (`daytime`) erhalten bleibt. Die Werte stehen als Konstanten im Code, mit Begründung und Quelle.
- Saisonwechsel bleiben beim Neustart: Die Atmosphäre gilt ab dem Start mit der Saison. Eine Änderung von `atmosphere` allein bei gleicher Welt löst keinen Neustart aus; sie wirkt beim nächsten Start.
- Beispiel `halloween` in der Dokumentation und in `season.yaml` als Kommentar.

Nicht Teil dieser Änderung:

- Live-Wechsel der Dimension im laufenden Betrieb. Saisons wirken nur über einen Neustart.
- Weitere Atmosphären, konfigurierbare Farb- oder Distanzwerte. Werte bleiben im Code, bis ein zweiter Fall sie braucht.
- Nebel für Spieler in Minispielen. Die Dimension gehört nur der Lobby.

## Delivery

Der Pull-Request-Titel und der Squash-Commit sind `feat(season): add the slender fog as a season atmosphere`. Die Änderung ist nicht breaking. Die OpenSpec-Artefakte stehen in einem eigenen Commit `docs(openspec): propose lobby-season-fog`.

## Capabilities

### New Capabilities

Keine. Die Atmosphäre ist eine Eigenschaft der Saison und gehört in die bestehende Capability.

### Modified Capabilities

- `lobby-seasons`: neue Anforderungen für die Atmosphäre je Saison, die festen Nebelwerte, die erhaltene Standarduhr und den unbekannten Wert. Bestehende Anforderungen bleiben unverändert.

## Impact

- **Code:**
  - `core`: `LobbyAtmosphere` (Enum mit Konfigurationsschlüssel), Default-Methode `atmosphere()` in `LobbyWorldChoice`.
  - `features/season`: Lesen von `seasons.<id>.atmosphere` (`SeasonAtmospheres`), `SeasonWorldChoice` liefert die Atmosphäre, `SeasonSettings` mit dem Feldnamen. Die Column hängt weiter nur an `core`.
  - `common`: `LobbyDimensions` registriert die Dimension über `Registries.dimensionType()` und baut sie aus den Konstanten.
  - `runtime`: `PlatformBeans` wählt die Atmosphäre aus derselben Auswahl wie die Welt, registriert die Dimension und legt die Instanz damit an.
- **Abhängigkeiten:** keine neuen Bibliotheken. Minestom bleibt bei 26.2 (`DimensionType.builder()`, `EnvironmentAttribute`).
- **Nutzertexte:** neue Warnung im Log (Englisch, wie die übrigen Season-Logs). Keine Spieltexte.
- **Betrieb:** Eine Saison mit Nebel braucht eine eigene Welt unter `worlds/` mit `map.json`, wie jede Saison. Der Saisonbeginn startet die Lobby ohnehin neu, weil die Welt wechselt; die Atmosphäre ist dann sofort da. Wird `atmosphere` an einer laufenden Saison geändert, wirkt es beim nächsten Start.
