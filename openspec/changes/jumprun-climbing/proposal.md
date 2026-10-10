# Proposal

## Why

Ein Lauf besteht heute nur aus Sprüngen mit höchstens einem Block Aufstieg (`Jump.MAX_RISE`). Der Kurs wächst dadurch fast nur in der Breite, und die Aufstiegsphase muss jede Höhe in Ein-Block-Sprüngen nehmen. Leitern und Ranken sind der naheliegende Ersatz, um in einem Abschnitt mehrere Blöcke zu gewinnen, und sie sind eine andere Aufgabe als Springen: Der Läufer klettert, statt zu landen.

Minestom simuliert das Klettern nicht, der Client tut es. Der Server sieht nur Positionen. Damit gilt für den Kurs:

- Fortschritt darf nur aus Positionen folgen, nicht aus einem „Klettern“-Zustand.
- Die Absturzerkennung (`Course.hasFallen`) darf beim Klettern nicht auslösen und muss einen echten Absturz vom Schacht trotzdem erkennen.
- Der Client klettert nur, wo am Standort des Läufers ein kletterbarer Block steht (Leiter, Ranke) und ein Block dahinter ihn sichtbar hält. Beides ist beim Kurs „Fake“: nur der Läufer sieht die Blöcke.

## What Changes

- Neuer Sprungtyp „Klettern“: ein Turm aus einem tragenden Pfeiler (Fake-Blöcke), einer Leiter oder Ranke an seiner Seite und einem Zielblock obenauf. Der Läufer steht auf dem Block davor, steigt hoch und tritt auf den Zielblock.
- Fortschritt: Der Zielblock zählt wie jeder andere Block als gelandet, sobald der Läufer darauf steht. Das Klettern selbst zählt nichts; ein Turm zählt als ein Sprung.
- Absturz: Der Schwellwert bleibt unverändert, ein Läufer im Schacht liegt immer darüber. Fällt er vom Turm bis zur Schwelle, endet der Lauf wie bei jedem Absturz.
- Der Generator erzeugt Klettern als eigenen Sprungtyp mit eigenen Kosten, eigener Freischaltung je Modus (nicht in Easy) und Prüfungen für den Raum: Pfeiler, Leiter und Kopffreiheit am Ende müssen in der echten Welt frei sein.
- Darstellung: Pfeiler und Leitersprossen sind Teil des Zielblocks und kommen und gehen mit ihm (Fake-Blöcke für den Läufer, fallende Darstellung für alle).
- Neue Konfiguration: `jumprun.climb.*` (Höhenbereich) und die Paletten `jumprun.palettes.ladder` und `jumprun.palettes.vine`.

## Capabilities

### New Capabilities

### Modified Capabilities
- `lobby-jumprun`: Neue Anforderungen „Klettern als Sprungtyp“, „Fortschritt und Absturz beim Klettern“ und „Platz für Kletterabschnitte“. Bestehende Anforderungen bleiben unverändert; der Turm zählt als ein Sprung der Anforderung „Fortschritt durch Landen“.

## Impact

- **Code:** `features/jumprun`: `Placement`/`CourseBlock`/`Spot` (Anbauten), `Jump` (Kletter-Erreichbarkeit und Kosten), `JumpRules`, `Clearance`, `CourseGenerator` (Kandidaten, `occupiedBy`), `Mode` (Freischaltung), `FakeBlocks`, `AnimatedBlock` (mehrere Zellen), `JumprunSettings` (Prüfung), Standard-Konfiguration.
- **Abhängigkeiten:** keine neuen. Baut auf `jumprun-more-surfaces` nicht inhaltlich auf, berührt aber dieselben Klassen; zuerst `jumprun-more-surfaces` mergen.
- **Nutzertexte:** keine.
- **Spielerverhalten:** ab Score 30 (Medium) bzw. 15 (Hard) können Türme mit Leiter oder Ranke vorkommen; Easy bleibt unverändert.

## Delivery

Pull-Request-Titel und Squash-Commit: `feat(jumprun): add climbing segments with ladders and vines`
