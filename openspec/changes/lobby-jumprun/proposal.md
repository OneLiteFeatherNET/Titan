# Proposal

## Why

Die Lobby hat außer Elytra, Sitzen und Kitzeln nichts, womit sich ein Spieler beim Warten allein beschäftigen kann. Ein zufällig generiertes Jump and Run, das nur der Spieler selbst sieht und das mit jedem Sprung schwerer wird, ist ein kleiner Zeitvertreib. Es braucht weder Map-Daten noch ein Backend und passt damit in die Zeit, bevor es den Stats-Dienst gibt.

## What Changes

- Neues Lobby-Feature „Jump and Run“ als eigene Column `features/jumprun`. Plattform und andere Features bleiben unverändert.
- Neues Hotbar-Item, das einen Lauf startet und bei erneuter Benutzung abbricht.
- Ein Lauf startet an der Stelle, an der der Spieler steht. Eine Vorab-Prüfung stellt sicher, dass von dort ein Weg aus dem Spawn-Bereich hinaus möglich ist. Sonst startet kein Lauf, und der Spieler bekommt eine Meldung.
- Die ersten Sprünge sind eine leichte Aufstiegsphase, die nach oben und vom Spawn weg führt. Sie zählt nicht zum Score.
- Die Blöcke existieren nur für den Spieler, als Blöcke auf dem Client. Andere Spieler und die echte Welt sehen sie nicht. Sichtbar sind immer genau 2 Blöcke hinter und 2 vor dem Block, auf dem der Spieler steht.
- Die Schwierigkeit steigt über eine mathematische Formel mit der Zahl geschaffter Sprünge. Sie zeigt sich in Blocktyp (Vollblock, Stufe, Zaun, …), Lückenbreite und Höhenunterschied.
- Neue Blöcke entstehen nur dort, wo in der echten Welt Platz ist und der Sprung schaffbar bleibt. Sackgassen werden vermieden.
- Ein Lauf endet bei einem Absturz, beim Gleiten mit der Elytra, bei einem Abbruch über das Item und beim Verlassen des Servers.
- Score in der Action Bar, Rekord pro Spieler im Speicher. Er geht beim Neustart verloren und wird später durch den Stats-Dienst ersetzt.
- Die Standardausstattung der Hotbar enthält zusätzlich das Jump-and-Run-Item.

## Capabilities

### New Capabilities
- `lobby-jumprun`: zufälliges Einzelspieler-Jump-and-Run in der Lobby, mit Start, Sichtfenster, Schwierigkeitsanstieg, Platzprüfung, Laufende und Score.

### Modified Capabilities
- `lobby-hotbar`: Das Szenario „Standardausstattung“ enthält zusätzlich das Jump-and-Run-Item.

## Impact

- **Code:** neues Gradle-Modul `features/jumprun`, das nur an `core` hängt. Die App-Varianten übernehmen es über den Verzeichnis-Scan ohne Änderung. Querschnitts-Tests in `apps/cloudnet` (Standardausstattung, Slot-Konflikte) bekommen das neue Item.
- **Abhängigkeiten:** keine neuen Laufzeit- oder Build-Abhängigkeiten. Benötigt werden nur Minestom (Block-Change-Pakete, Events) und Adventure.
- **Nutzertexte (neu):** Item-Name „Jump & Run“ (sprachneutral, ohne Beschreibung), Meldung „kein Platz zum Starten“, Score in der Action Bar, Meldung zum Laufende mit Score, Meldung „neuer Rekord“. Alle in den Sprachdateien mit Englisch als Fallback.
- **Spielerverhalten:** Ein Spieler im Lauf scheint für andere in der Luft zu schweben. Das ist bewusst so gewollt.

## Delivery

Pull-Request-Titel und Squash-Commit: `feat(jumprun): add a random single-player jump and run to the lobby`
