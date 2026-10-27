# Design

## Kontext

`features/radio` ordnet jeder MIDI-Note ein Notenblock-Instrument und eine Note 0 bis 24 zu. Heute kommt das Instrument aus 16 groben Programmgruppen (`InstrumentMapper`). Schlagzeug auf Kanal 10 kennt vier Klänge, und alle übrigen Tasten spielen die Hi-Hat auf Note 12 (`NoteMapper`). Die Anschlagstärke geht ungeändert in die Lautstärke (`MidiSongParser`, `MidiNote.volume`).

## Ziele / Nicht-Ziele

**Ziele**

- Jedes der 128 Programme hat ein Instrument, das der Familie entspricht.
- Jede Schlagzeugtaste hat einen festen Klang, der sich von den anderen unterscheidet.
- Kanallautstärke und Ausdruck werden berücksichtigt.
- Deterministisch, ohne Zeit, Zufall oder Konfiguration.

**Nicht-Ziele**

- Keine Änderung an Registern, Grundtasten, Verschiebung je Spur oder Faltung.
- Keine weiteren Controller außer CC 7 und CC 11.

## Entscheidungen

### D1 Programmtabelle als `switch`

`InstrumentMapper` hat für die Programmnummer ein `switch` mit einer Zeile je Instrument und den Programmen als Liste oder Bereich. Das ist lesbar, und jede Zuordnung steht an ihrer Stelle. Ein Programm ohne Fall läuft in den `default`-Zweig, der eine Ausnahme wirft. Ein Test prüft alle 128 Programme.

Verworfen: ein Array aus Bereichen. Es ist kürzer, aber die Überlappung der Bereiche (Programm 14 gegen 10 bis 15) ist schwer zu lesen.

### D2 Schlagzeug als Tabelle von Taste zu Klang

`InstrumentMapper.drum(key)` liefert ein `DrumSound(instrument, semitone)`. Die Noten sind feste Töne 0 bis 24 und gehen nicht durch die Registerlogik. `NoteMapper` nutzt die Note direkt, ohne Verschiebung und ohne Faltung.

Die Zuordnung steht in der Spezifikation. Nicht zugeordnete Tasten fallen auf Hi-Hat, Note 12.

### D3 Tonhöhige Schlagzeugprogramme

Die Programme 47 (Pauke), 116 (Taiko), 117 (Melodic Tom) und 118 (Synth Drum) sind keine Schlagzeugtaste, sondern Melodie-Noten auf einem Melodiekanal. Sie bekommen `BASEDRUM` und laufen durch `NoteMapper` wie jede andere Spur.

### D4 Anschlag mit CC 7 und CC 11

`MidiSongParser` merkt sich je Kanal den letzten Wert von CC 7 und CC 11, Startwert 127. Eine Note bekommt den Anschlag `round(velocity × cc7 / 127 × cc11 / 127)`. Ergibt das 0, wird die Note verworfen, wie ein Anschlag 0. Der Wert bleibt im Bereich 1 bis 127, weil jeder Faktor höchstens 1 ist. `RawNote` und `MidiNote` bleiben unverändert: der Parser liefert den fertigen Anschlag.

Ein Control Change wirkt ab seinem Tick auf alle folgenden Noten desselben Kanals.

## Offen

Keine.

## Tests

Alle Tests folgen F.I.R.S.T.: Unit-Tests ohne Server, ohne Zeit und ohne Dateisystem. Jeder Test baut seine Fixtures selbst.

- `InstrumentMapperTest`: je Programmfamilie eine Stichprobe (Klavier, E-Piano, Orgel, Gitarre, Verzerrung, Bass, Cello, Blech, Holzbläser, Synth-Lead, Synth-Pad, Sitar, Glocke, Percussion, Effekt). Ein Test für alle 128 Programme. Schlagzeug: Stichproben je Klang mit Instrument und Note.
- `NoteMapperTest`: Schlagzeugnote 36 spielt `basedrum` auf Note 6, nicht mehr 12. Schlagzeug bleibt unverschoben.
- `MidiSongParserTest`: CC 7 halbiert den Anschlag, CC 11 ebenso, beide multiplizieren sich, CC auf anderem Kanal wirkt nicht, Anschlag nach Skalierung 0 wird übersprungen.
