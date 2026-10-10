# Proposal

## Why

Beim lokalen Test ist der Kurs nach vielen Sprüngen unter eine schwebende Insel gewandert. Dort setzte der Generator einen Block direkt unter die Unterseite der Insel, von dem aus der Spieler nicht mehr springen konnte. Zwei Lücken im Generator stecken dahinter:

- Er verlangt über einer Lauffläche nur die Standhöhe des Spielers (1,8 Blöcke), nicht die Höhe, die ein Sprung braucht.
- Er hält sich nur an die Grenzen der Dimension, nicht an die Höhengrenzen der Lobby (`spawn.minHeight`/`maxHeight`). Verlässt ein Läufer diese Grenzen, teleportiert ihn das Spawn-Modul zum Spawn, statt dass der Jump-and-Run-Reset greift.

## What Changes

- Jeder neue Block braucht über seiner Lauffläche so viel freien Raum, dass der Spieler von dort aus springen kann (Standhöhe plus Sprunghöhe). Auch die Flugbahn dorthin muss bis zur Scheitelhöhe des Sprungs frei sein.
- Neue Blöcke entstehen nur in einem Höhenband innerhalb der Lobby-Grenzen. Der Abstand zur Untergrenze ist so groß, dass ein Absturz den Jump-and-Run-Reset auslöst, bevor der Spieler `minHeight` erreicht. Zur Obergrenze bleibt Abstand für den Scheitelpunkt eines Sprungs.
- Die Vorab-Prüfung beim Start und die Aufstiegsphase halten sich an dieselben Regeln. Wo der Aufstieg nicht in das Band passt, startet kein Lauf.
- Das Spawn-Modul stellt seine Höhengrenzen als Bean über `core` bereit, damit jumprun sie lesen kann, ohne den Konfigurationsabschnitt eines anderen Moduls zu lesen.

## Capabilities

### New Capabilities

### Modified Capabilities
- `lobby-jumprun`: Die Anforderung „Sprünge nur, wo Platz ist“ verlangt Sprungfreiheit statt nur Kopffreiheit und ein Höhenband innerhalb der Lobby-Grenzen. Neu ist die Anforderung „Kein Teleport durch die Höhengrenzen“.

## Impact

- **Code:**
  - `core` bekommt ein kleines Interface für die Höhengrenzen der Lobby.
  - `features/spawn` stellt es als Bean bereit, aus seinen vorhandenen Einstellungen.
  - `features/jumprun` nutzt es in `JumpRules`, bei der Startprüfung und in der Aufstiegsphase, und rechnet die Sprungfreiheit in `Surface`/`JumpRules`.
- **Abhängigkeiten:** keine neuen.
- **Nutzertexte:** keine neuen und keine geänderten. Ein Lauf, der wegen des Höhenbands nicht startet, nutzt die vorhandene Meldung „kein Platz zum Starten“.
- **Spielerverhalten:** Kurse führen nicht mehr unter Decken und Überhänge, und ein Absturz endet immer am Startpunkt des Laufs statt am Lobby-Spawn. In engen Bereichen enden Läufe eventuell früher („kein Platz mehr“).

## Delivery

Pull-Request-Titel und Squash-Commit: `fix(jumprun): keep runs inside the lobby height and leave room to jump`
