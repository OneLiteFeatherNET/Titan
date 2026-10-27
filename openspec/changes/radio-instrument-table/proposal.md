# Proposal

## Why

Die Instrumentwahl im Radio ist grob. Alle 16 General-MIDI-Gruppen laufen auf eine Handvoll Instrumente: ein E-Piano klingt wie eine Harfe, ein Cello wie eine Flöte, eine verzerrte Gitarre wie eine Gitarre ohne Verzerrung. Im Schlagzeug kennt die Lobby nur Bass Drum, Snare, Cowbell und Hat, und jede Trommel spielt die Hi-Hat auf derselben Note 12. Ein Titel mit Toms, Becken oder Congas klingt dadurch wie ein einziges Becken.

## What Changes

- **Programmtabelle:** Jedes der 128 General-MIDI-Programme bekommt ein Instrument. Die Zuordnung ist eine feste Tabelle in `InstrumentMapper`, nach Familien geordnet (Klavier, Orgel, Gitarre, Bass, Streicher, Blech, Holzbläser, Synth, Percussion, Effekte).
- **Schlagzeug nach Taste:** Jede GM-Schlagzeugtaste auf Kanal 10 spielt ein festes Instrument mit einer festen Note, damit Bass Drum, Toms, Snare, Hi-Hat, Becken, Congas und Cowbell unterscheidbar klingen. Die Noten sind fix und werden nicht über die Registerlogik verschoben. Nicht zugeordnete Tasten spielen wie bisher die Hi-Hat, jetzt auf Note 12.
- **Tonhöhige Schlagzeug-Programme:** Pauke (47) und die tonhöhigen Trommeln (116 bis 118) sind Melodie-Noten und folgen der Spur- und Registerlogik wie alle anderen.
- **Kanallautstärke und Ausdruck:** Der Anschlag einer Note wird mit Control Change 7 (Kanallautstärke) und 11 (Ausdruck) des Kanals skaliert. Fehlen sie, gelten 127. Notenereignisse, die dabei auf 0 fallen, werden übersprungen.
- **Keine neue Konfiguration.** Die Änderung liegt in `InstrumentMapper`, `NoteMapper` und `MidiSongParser`. `RadioInstrument` und `Pitch` bleiben unverändert.

## Capabilities

### New Capabilities

Keine.

### Modified Capabilities

- `lobby-radio`: Die Anforderung „MIDI-Noten werden auf Notenblock-Instrumente und Tonhöhen abgebildet“ ändert sich. Sie gilt für alle 128 Programme, die feste Schlagzeugtabelle und die Anschlagstärke mit Kanallautstärke und Ausdruck. Siehe `specs/lobby-radio/spec.md`.

## Impact

- **Code:** `features/radio`: `InstrumentMapper` (Programmtabelle, Schlagzeugtabelle), `NoteMapper` (feste Schlagzeugnote), `MidiSongParser` (CC 7 und 11 je Kanal).
- **Tests:** `InstrumentMapperTest` (Stichproben je Familie, Schlagzeug mit Noten), `NoteMapperTest` (Schlagzeugnote statt 12), `MidiSongParserTest` (CC 7, CC 11, Kanaltrennung, Anschlag 0 nach Skalierung).
- **Nutzertexte:** keine.
- **Konfiguration:** keine.
- **Abhängigkeit:** Das Delta ist gegen die Fassung aus `openspec/changes/radio-track-octaves/specs/lobby-radio/spec.md` geschrieben, die auf `origin/main` liegt, aber noch nicht archiviert ist. Die Archivreihenfolge ist `radio-track-octaves`, dann diese Änderung.
- **Nicht brechend:** Vorhandene Songs bleiben abspielbar. Sie klingen anders: Instrumente wechseln, Schlagzeug und leise Passagen (CC 7 oder 11 unter 127) ändern sich hörbar.

## Type and Scope

Die Umsetzung wird als **`feat(radio)`** ausgeliefert, mit dem Pull-Request-Titel `feat(radio): map every midi program and drum key to a fitting note block sound`. Die Änderung ist nicht brechend.

Die Vorschlags-Artefakte stehen im Commit `docs(openspec): propose radio-instrument-table`. Der Code-Pull-Request folgt erst nach dem Merge dieser Vorschlags-PR.

## Nicht Teil dieser Änderung

- Keine Änderung an Registern, Grundtasten, Erweiterungswahl oder Faltung in `RadioInstrument` und `Pitch`.
- Keine Verschiebung des Schlagzeugs in Oktaven.
- Keine Auswertung weiterer Controller (Pan, Modulation, Pitch Bend, Sustain) und keine Umsetzung von Program Change in Bänken.
- Kein Resource Pack und keine Anpassung der Sounds selbst.
