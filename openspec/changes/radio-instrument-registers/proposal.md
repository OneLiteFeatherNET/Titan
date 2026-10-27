# Proposal

## Why

Im Radio klingen MIDI-Songs stellenweise falsch. Steigt eine Melodie über F#5, springt sie mitten in der Phrase eine Oktave nach unten. Eine Bass-Stimme klingt zwei Oktaven zu tief, eine Gitarre eine Oktave zu tief, eine Flöte eine Oktave zu hoch, und Glocke, Glockenspiel und Xylophon klingen zwei Oktaven zu hoch. Die Ursache liegt in `features/radio`: Jede Note wird in den Bereich der Harfe (F#3 bis F#5) gefaltet, als wären alle Notenblock-Instrumente Harfen. Die Instrumente haben aber verschiedene Register.

## What Changes

- **Register je Instrument:** Jedes Notenblock-Instrument kennt seine Grundtaste, also die MIDI-Taste seiner Note 0. Harfe, Banjo, Pling, Bit und Eisen-Xylophon haben 54, Bass und Didgeridoo 30, Gitarre 42, Flöte und Kuhglocke 66, Glocke, Glockenspiel und Xylophon 78. Die Note ist `Taste − Grundtaste`, damit die Oktave stimmt.
- **Erweiterungsinstrumente statt Falten:** Liegt eine Melodie-Note außerhalb des Registers ihres Instruments, spielt sie das Erweiterungsinstrument (Bass, Gitarre, Flöte oder Glocke), dessen Register die Taste enthält. Gibt es mehrere, gewinnt das Register, dessen Grundtaste der des Instruments am nächsten liegt.
- **Letzte Instanz:** Nur Noten außerhalb von 30 bis 102 (der Gesamtbereich aller Register) werden um Oktaven gefaltet, und zwar in diesen Bereich hinein. Die Tonhöhenklasse bleibt erhalten.
- **Blechbläser:** Die Notenblock-Instrumente `trumpet`, `trumpet_exposed`, `trumpet_weathered` und `trumpet_oxidized` aus der aktuellen Minecraft-Version kommen dazu, mit der Grundtaste 54 (Register der Harfe, angenommen). General-MIDI-Programme 56 (Trompete), 59 (gedämpfte Trompete) und 61 bis 63 (Blechbläser-Satz, Synth-Blech) spielen `trumpet`, 57 (Posaune) `trumpet_weathered`, 58 (Tuba) `trumpet_oxidized`, 60 (Waldhorn) `trumpet_exposed`. Die bisherige Zuordnung von 56 bis 63 zum Didgeridoo entfällt. Trompete und Horn haben laut Wiki das Register F#3 bis F#5. Posaune und Tuba sind angenommen und müssen im Spiel per Ohr geprüft werden.
- **Keine Verschiebung des Songs:** Ein Song wird nicht mehr als Ganzes um Oktaven verschoben, bis der Median der Melodie in der Mitte liegt. Die Register decken F#1 bis F#7 ab, also spielt jede Note in der Oktave, die sie im MIDI-Stück hat; die Verschiebung hatte Noten von ihrer geschriebenen Oktave weggeschoben. Schlagzeug auf Kanal 10 bleibt wie bisher, die Lautstärke ebenso. Keine neue Konfiguration.

## Capabilities

### New Capabilities

Keine.

### Modified Capabilities

- `lobby-radio`: Die Anforderung „MIDI-Noten werden auf Notenblock-Instrumente und Tonhöhen abgebildet“ ändert sich: Instrumentwahl je Register, Erweiterungsinstrumente, Faltung nur außerhalb des Gesamtbereichs, Blechbläser-Programme. Siehe `specs/lobby-radio/spec.md`.

## Impact

- **Code:** `features/radio`. `RadioInstrument` bekommt die Grundtaste und die vier Trompeten, `Pitch` faltet nur noch in den Gesamtbereich und verliert die Verschiebung, `InstrumentMapper` bekommt die Blechbläser-Gruppen, `NoteMapper` wählt je Note das Instrument und die Note ohne Verschiebung des Songs.
- **Tests:** Die Mapper-Tests und die Pitch-Tests ändern ihre Erwartungen. Die Tests der Verschiebung entfallen. Neue Unit-Tests für Register, Erweiterung und Faltung sowie für einen reinen Bass-Song in seiner Oktave.
- **Nutzertexte:** keine.
- **Konfiguration:** keine.
- **Abhängigkeit:** Die Anforderung steht heute in `openspec/changes/radio-categories/specs/lobby-radio/spec.md` (MODIFIED-Abschnitt) und ist dort noch nicht archiviert. Dieses Delta ist gegen diese Fassung geschrieben. Die Archivreihenfolge ist `lobby-radio`, dann `radio-categories`, dann diese Änderung.
- **Nicht brechend:** Vorhandene Songs bleiben abspielbar. Die Tonlage einzelner Noten ändert sich, und Songs, deren Melodie nicht in der Mitte lag, klingen nun in ihrer geschriebenen Oktave. Die Instrumente der Blechbläser ändern sich.
- **Minecraft-Version:** Die Trompeten-Sounds gibt es ab Minecraft 26.1. Ein Client ohne diese Sounds spielt die Töne nicht. Das muss mit dem Server-Stand abgeglichen werden.

## Type and Scope

Die Umsetzung wird als **`fix(radio)`** ausgeliefert, mit dem Pull-Request-Titel `fix(radio): keep notes in their octave by using the instrument registers`. Die Änderung ist nicht brechend.

Die Vorschlags-Artefakte stehen im Commit `docs(openspec): propose radio-instrument-registers`. Der Code-Pull-Request folgt erst nach dem Merge dieser Vorschlags-PR.

## Nicht Teil dieser Änderung

- Keine Lautstärke- oder Pitch-Anpassung an den Sound selbst.
- Keine Mob-Köpfe als Instrumente (Zombie, Skelett, Creeper, Drache, Wither-Skelett, Piglin, Kopf). Sie haben keine Tonhöhe.
- Kein Resource Pack.
- Keine Änderung an Telemetrie, Warteschlange oder Takt.
