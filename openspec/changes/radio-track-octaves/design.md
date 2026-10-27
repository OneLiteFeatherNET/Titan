# Design

## Kontext

`features/radio` ordnet jeder MIDI-Note ein Notenblock-Instrument und eine Note 0 bis 24 zu (`InstrumentMapper`, `RadioInstrument`, `Pitch`). Seit `radio-instrument-registers` hat jedes Instrument sein Register (Grundtaste bis Grundtaste + 24). Liegt eine Melodie-Note außerhalb, wechselt `RadioInstrument.playing(key)` auf ein Erweiterungsinstrument. Die Melodie wird dabei nicht verschoben. Das führt dazu, dass eine Melodie über F#5 mitten in der Phrase von der Harfe auf Flöte und Glocke springt.

Die Anforderung „MIDI-Noten werden auf Notenblock-Instrumente und Tonhöhen abgebildet“ steht im MODIFIED-Abschnitt von `openspec/changes/radio-instrument-registers/specs/lobby-radio/spec.md`. Dieses Delta ist gegen diese Fassung geschrieben.

## Ziele / Nicht-Ziele

**Ziele**

- Eine Spur, deren Noten sich in ein Register bringen lassen, spielt auf einem einzigen Instrument, ohne dass Noten die Instrumente wechseln.
- Eine Bass-Spur bleibt auf dem Bass in ihrer Oktave.
- Deterministisch, ohne Zeit, Zufall oder Konfiguration.

**Nicht-Ziele**

- Keine Verschiebung über Spuren hinaus (kein Versatz des ganzen Songs).
- Keine Änderung an Schlagzeug, Registern, Erweiterungswahl oder Faltung.

## Entscheidungen

### D1 Spur = Kanal und Programm

Eine Spur ist die Menge der melodischen Noten mit demselben Kanal und Programm. Ihr Instrument ist `InstrumentMapper.of(channel, program, key)`; für melodische Kanäle hängt es nur vom Programm ab. Schlagzeug (Kanal 10) bildet keine Spur und bleibt unverändert.

### D2 Verschiebung je Spur, Suche über ganze Oktaven

Die Verschiebung `s` ist ein Vielfaches von 12 aus {−48, −36, …, +48}. Zielgröße ist die Anzahl der Noten der Spur, deren Taste `k + s` im Register des Spur-Instruments liegt (Grundtaste bis Grundtaste + 24). Die Suche ist vollständig: neun Kandidaten, kein Zufall.

### D3 Gleichstand

Bei gleicher Anzahl gewinnt in dieser Reihenfolge:

1. die kleinere Verschiebung `|s|`,
2. bei gleichem `|s|`, also Plus und Minus, die Verschiebung, deren Median nach dem Schieben der Registermitte (Grundtaste + 12) am nächsten liegt: `|median + s − (Grundtaste + 12)|` ist minimal,
3. danach das kleinere `s`, damit die Wahl auch bei Gleichstand eindeutig bleibt.

Der Median ist das mittlere Element der sortierten unverschobenen Tasten; bei gerader Anzahl das untere der beiden mittleren.

### D4 Anwendung

Die Verschiebung gilt für jede Note der Spur. Danach gilt die bisherige Logik: Liegt `k + s` außerhalb von 30 bis 102, faltet `Pitch.foldIntoSpan` um Oktaven hinein. `RadioInstrument.playing` wählt das Instrument. Die Intervalle der Spur bleiben erhalten.

### D5 Warum je Spur

- Je Note (Stand 4.11.0) zerreißt die Phrase, weil jede Note auf ein anderes Register fällt.
- Je Song (bisherige Verschiebung bis zum Median in der Mitte) schiebt eine Bass-Spur um zwei Oktaven nach oben, sobald eine Melodie darüber liegt.
- Je Spur passt jede Stimme in ihr Register, ohne die anderen Stimmen zu verändern.

### D6 Grenzen des Ziels

Eine Spur liegt nur dann vollständig in einem Register, wenn eine Verschiebung um ganze Oktaven das erlaubt. Eine Spur mit den Tasten 60 und 84 passt nie ganz in die Harfe: Die Verschiebung müsste −6 sein. Die Regel verspricht also keine vollständige Zuordnung für jede Spur, sondern die meisten Noten auf dem Instrument der Spur. Die übrigen Noten folgen den bestehenden Regeln (D4).

## Offen

Keine. Die Trompeten-Register aus `radio-instrument-registers` bleiben wie dort beschrieben.

## Tests

Alle Tests folgen F.I.R.S.T.: Unit-Tests ohne Server, ohne Zeit und ohne Dateisystem, jeder Test baut seine Fixtures selbst.

- `NoteMapperTest`:
  - Korobeiniki-ähnliche Melodie auf Klavier, Tasten 72 bis 88: alle Noten auf der Harfe, Verschiebung −12, Klang eine Oktave tiefer.
  - Reine Bass-Spur auf Taste 42: Verschiebung 0, Bass mit Note 12.
  - Drei Oktaven, Tasten 48, 60, 72, 84: Verschiebung 0, Intervalle bleiben, nur 48 und 84 wechseln das Instrument.
  - Gleichstand Plus/Minus: Tasten 20, 43, 85 ergeben +12, weil der Median 43 nach +12 (55) näher an 66 liegt als nach −12 (31). Der Test prüft außerdem, dass zwei Aufrufe mit denselben Eingaben dasselbe Ergebnis liefern.
  - Bestehende Erwartungen zu Schlagzeug, Tonhöhe, Faltung und Zeitstempeln bleiben. Der Test der Note 79 auf der Flöte wird durch die Verschiebung der Spur ersetzt.
