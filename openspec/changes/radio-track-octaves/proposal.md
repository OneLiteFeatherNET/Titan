# Proposal

## Why

Im Radio springt eine Melodie mitten in der Phrase das Instrument, sobald sie über F#5 steigt. Die Tonfolge stimmt, aber Flöte oder Glocke klingen anders als die Harfe, und die Melodie wirkt zerrissen. Bei Korobeiniki, dessen Melodie um C5 bis E6 liegt, passiert das fast in jedem Takt. Die Anforderung aus `radio-instrument-registers` hat die Verschiebung des ganzen Songs entfernt, damit jede Note in ihrer geschriebenen Oktave klingt. Ergebnis: Eine Melodie oberhalb des Harfen-Registers wechselt Note für Note das Instrument.

## What Changes

- **Spuren:** Die Melodie-Noten werden nach Kanal und Programm gruppiert. Eine Spur ist eine Stimme, zum Beispiel alle Klavier-Noten auf Kanal 1.
- **Verschiebung je Spur:** Jede Spur bekommt eine ganze Oktavverschiebung (Vielfaches von 12, zwischen −48 und +48). Gewählt wird die, die die meisten Noten der Spur in das Register ihres Instruments bringt. Bei Gleichstand gewinnt die kleinste Verschiebung. Liegen Plus und Minus gleich weit von null, gewinnt die, deren Median der Registermitte näher liegt.
- **Eine Verschiebung je Spur:** Alle Noten einer Spur bekommen dieselbe Verschiebung, die Intervalle der Spur bleiben erhalten. Eine Bass-Spur bleibt auf dem Bass.
- **Ausreißer:** Noten, die auch nach der Verschiebung außerhalb ihres Registers liegen, bleiben bei den bestehenden Regeln: Faltung nur außerhalb von 30 bis 102, dann Erweiterungsinstrument.
- **Keine neue Konfiguration.** Die Änderung liegt in `NoteMapper`.

## Capabilities

### New Capabilities

Keine.

### Modified Capabilities

- `lobby-radio`: Die Anforderung „MIDI-Noten werden auf Notenblock-Instrumente und Tonhöhen abgebildet“ ändert sich. Die Verschiebung kehrt zurück, aber je Spur statt je Song. Siehe `specs/lobby-radio/spec.md`.

## Impact

- **Code:** `features/radio`, `NoteMapper` (Spuren, Wahl der Verschiebung, Anwendung). `RadioInstrument`, `Pitch` und `InstrumentMapper` bleiben unverändert.
- **Tests:** `NoteMapperTest`. Der Test der Note 79 auf der Flöte wird an die Verschiebung der Spur angepasst; die übrigen Erwartungen bleiben.
- **Nutzertexte:** keine.
- **Konfiguration:** keine.
- **Abhängigkeit:** Das Delta ist gegen `openspec/changes/radio-instrument-registers/specs/lobby-radio/spec.md` geschrieben, die noch nicht archiviert ist. Die Archivreihenfolge ist `lobby-radio`, dann `radio-categories`, dann `radio-instrument-registers`, dann diese Änderung.
- **Nicht brechend:** Vorhandene Songs bleiben abspielbar. Die Tonlage einer Spur kann sich um Oktaven gegenüber der Partitur verschieben. Das hebt die Entscheidung „keine Verschiebung des Songs“ (D4 in `radio-instrument-registers`) für die Spur auf, nicht für den ganzen Song.

## Type and Scope

Die Umsetzung wird als **`fix(radio)`** ausgeliefert, mit dem Pull-Request-Titel `fix(radio): shift each track by octaves into its instrument's register`. Die Änderung ist nicht brechend.

Die Vorschlags-Artefakte stehen im Commit `docs(openspec): propose radio-track-octaves`. Der Code-Pull-Request folgt erst nach dem Merge dieser Vorschlags-PR.

## Nicht Teil dieser Änderung

- Keine Änderung an Registern, Grundtasten, Erweiterungswahl oder Faltung in `RadioInstrument` und `Pitch`.
- Schlagzeug auf Kanal 10 bleibt unverschoben.
- Keine Lautstärke- oder Pitch-Anpassung am Sound selbst, kein Resource Pack.
- Kein Versatz des ganzen Songs über seine Spuren hinaus.
