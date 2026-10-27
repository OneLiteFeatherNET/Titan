# Design

## Kontext

`features/radio` ordnet jeder MIDI-Note ein Notenblock-Instrument und eine Note 0 bis 24 zu. Heute gilt für alle Instrumente derselbe Bereich, F#3 (Taste 54) bis F#5. `NoteMapper` verschiebt den Song um ganze Oktaven, bis der Median der Melodie in der Mitte liegt, und faltet danach jede Note in diesen Bereich (`Pitch.fold`). Das ist falsch, weil Minecraft die Notenblock-Instrumente in verschiedenen Registern spielt: Bass und Didgeridoo F#1 bis F#3, Gitarre F#2 bis F#4, Flöte F#4 bis F#6, Kuhglocke F#4 bis F#6, Glocke, Glockenspiel und Xylophon F#5 bis F#7, die übrigen F#3 bis F#5. Quelle der Register: minecraft.wiki, Seite „Note Block“ (Notenblock).

Die Anforderung „MIDI-Noten werden auf Notenblock-Instrumente und Tonhöhen abgebildet“ steht heute im MODIFIED-Abschnitt von `openspec/changes/radio-categories/specs/lobby-radio/spec.md`. Diese Änderung ist ein Delta dagegen.

Zusätzlich kommen in der aktuellen Minecraft-Version vier Trompeten-Instrumente dazu (`trumpet`, `trumpet_exposed`, `trumpet_weathered`, `trumpet_oxidized`). Sie werden in dieser Änderung mit eingeführt und den Blechbläser-Programmen zugeordnet.

## Ziele / Nicht-Ziele

**Ziele**

- Jede Melodie-Note klingt in der Oktave, die im MIDI-Stück steht, solange das Register eines Instruments sie hält.
- Erweiterungsinstrumente statt Falten, sodass keine Note mitten in der Phrase eine Oktave springt.
- Pure Funktionen, keine neue Konfiguration.

**Nicht-Ziele**

- Keine Änderung an der Verschiebung des ganzen Songs.
- Keine Änderung am Schlagzeug.
- Keine Mob-Köpfe als Instrumente.

## Entscheidungen

### D1 Grundtasten je Instrument

`RadioInstrument` bekommt die Grundtaste (MIDI-Taste der Note 0). Die Note eines Melodie-Tons auf seinem Instrument ist `Taste − Grundtaste`, sein Register reicht von der Grundtaste bis Grundtaste + 24.

| Instrument | Grundtaste | Register |
| --- | --- | --- |
| harp, banjo, bit, pling, iron_xylophone | 54 | F#3 bis F#5 |
| bass, didgeridoo | 30 | F#1 bis F#3 |
| guitar | 42 | F#2 bis F#4 |
| flute, cow_bell | 66 | F#4 bis F#6 |
| bell, chime, xylophone | 78 | F#5 bis F#7 |
| trumpet, trumpet_exposed | 54 | F#3 bis F#5 (laut Wiki) |
| trumpet_weathered, trumpet_oxidized | 54 | F#3 bis F#5 (angenommen, siehe D5) |

Schlagzeug (basedrum, snare, hat) hat kein Register; es wird über die Taste auf Kanal 10 gewählt wie bisher. Die Kuhglocke (GM 112 bis 119) ist ein Melodie-Instrument mit Register 66, auch wenn sie auf Kanal 10 als Drum-Taste 56 erscheint.

### D2 Erweiterungsinstrument statt Falten

Liegt die (verschobene) Taste einer Melodie-Note außerhalb des Registers ihres Instruments, wählt `RadioInstrument` aus der Erweiterungsgruppe {Bass, Gitarre, Flöte, Glocke} das Instrument, dessen Register die Taste enthält. Gibt es mehrere, gewinnt das, dessen Grundtaste der Grundtaste des Instruments am nächsten liegt. Bei Gleichstand gilt die Reihenfolge der Aufzählung. Die vier Register decken Taste 30 bis 102 lückenlos ab, sodass für jede Taste in diesem Bereich ein Instrument existiert.

Beispiele:

- Harfe, Taste 50: Bass (30 bis 54) und Gitarre (42 bis 66) enthalten sie. Die Gitarre liegt mit Grundtaste 42 näher an 54 als der Bass mit 30, also Gitarre mit Note 8.
- Harfe, Taste 79: Flöte (66 bis 90) und Glocke (78 bis 102). Die Flöte mit Grundtaste 66 liegt näher, also Flöte mit Note 13.
- Harfe, Taste 96: nur die Glocke enthält sie, also Glocke mit Note 18.
- Glocke, Taste 60: nur die Gitarre enthält sie, also Gitarre mit Note 18.

Die Gruppe ist bewusst klein: Die vier Register decken den Gesamtbereich ab, die Harfe muss nicht Erweiterung sein.

### D3 Faltung nur außerhalb des Gesamtbereichs

Liegt die verschobene Taste außerhalb von 30 bis 102, faltet `Pitch` sie um Oktaven in diesen Bereich. Die Tonhöhenklasse bleibt. Danach wählt D2 das Instrument. Diese Faltung ist die letzte Instanz; im Normalfall greift sie nicht.

### D4 Verschiebung des Songs bleibt

`NoteMapper.transposition` bleibt unverändert: Der Median der Melodie-Tasten wird um ganze Oktaven in die Mitte gelegt (Note 12 der Harfe, also Taste 66). Die Verschiebung gilt für die Taste, bevor D1 bis D3 greifen.

Das hat eine Grenze, die vor dem Merge geklärt werden muss (siehe „Offen“).

### D5 Blechbläser und Trompeten

Es kommen vier Notenblock-Instrumente dazu: `trumpet`, `trumpet_exposed`, `trumpet_weathered`, `trumpet_oxidized`. Ihre Sound-Schlüssel sind `minecraft:block.note_block.trumpet`, `…trumpet_exposed`, `…trumpet_weathered`, `…trumpet_oxidized`. Die Sounds sind im Data-Jar nachgewiesen. Die Wiki-Quelle nennt die Varianten als eigene Instrumente: Trompete, Horn, Posaune und Tuba.

Die Zuordnung der General-MIDI-Programme:

- 56 (Trompete), 59 (gedämpfte Trompete), 61 (Blechbläser-Satz), 62 und 63 (Synth-Blech) → `trumpet`
- 57 (Posaune) → `trumpet_weathered`
- 58 (Tuba) → `trumpet_oxidized`
- 60 (Waldhorn) → `trumpet_exposed`

Die Register der Trompeten: `trumpet` und `trumpet_exposed` haben laut minecraft.wiki (Seite „Note Block“) die Grundtaste 54 (F#3 bis F#5). Für `trumpet_weathered` und `trumpet_oxidized` war die Wiki-Zeile nicht lesbar; ihre Grundtaste 54 ist angenommen. Eine Tuba-Sample liegt unter Umständen tiefer; dann ist die Grundtaste von `trumpet_oxidized` anzupassen. Die Grundtaste steht in einer Konstante, damit sie bei einer Korrektur an einer Stelle geändert wird. Der Abnahmeschritt in `tasks.md` prüft im Spiel per Ohr, ob die Oktave stimmt.

Das Didgeridoo verliert damit seine General-MIDI-Programme 56 bis 63. Das Instrument bleibt im Enum mit Grundtaste 30, wird aber von keinem Programm mehr gewählt.

### D6 Nicht genutzte Instrumente

Die Mob-Köpfe des Notenblocks (Zombie, Skelett, Creeper, Drache, Wither-Skelett, Piglin, eigener Kopf) haben keine Tonhöhe und bleiben ungenutzt. Sie kommen nicht ins Enum.

## Offen

- **Verschiebung bei tiefen Songs (Owner-Entscheidung).** D4 verschiebt den ganzen Song, sobald der Median nicht in der Mitte liegt. Eine reine Bass-Stimme mit Median F#2 (Taste 42) wird um zwei Oktaven nach oben verschoben. Dann landen ihre Noten auf Gitarre und Flöte statt auf dem Bass. Die Anforderung verlangt die Verschiebung ausdrücklich unverändert. Vor dem Merge soll der Owner entscheiden, ob die Verschiebung für diesen Fall entfällt oder pro Instrument gilt. Der Code-Pull-Request ändert das nicht ohne diese Entscheidung.
- **Trompeten-Register.** `trumpet` und `trumpet_exposed` sind laut Wiki auf 54. `trumpet_weathered` und `trumpet_oxidized` sind mit 54 angenommen (D5); die Tuba-Variante kann tiefer liegen.
- **Minecraft-Version.** Die Trompeten-Sounds gibt es nur in einer passenden Minecraft-Version. Ob der Server und der Client-Stand dazu passen, prüft diese Änderung nicht.

## Tests

Alle Tests folgen F.I.R.S.T.: Unit-Tests ohne Server, ohne Zeit und ohne Dateisystem, jeder Test baut seine Fixtures selbst.

- `RadioInstrumentTest` (neu): Grundtasten (Tabelle D1 inklusive Trompeten), Registergrenzen, Erweiterungswahl nach D2 (Harfe 50 bis Gitarre, Harfe 35 bis Bass, Harfe 79 bis Flöte, Harfe 96 bis Glocke, Glocke 60 bis Gitarre).
- `PitchTest`: `foldIntoSpan` faltet nur außerhalb von 30 bis 102, Tonhöhenklasse bleibt. Die Transposition bleibt getestet.
- `NoteMapperTest`: Hohe Note über F#5 bleibt in der Oktave (Flöte, Note 13), Glocke bei 96, Bass-Stimme in ihrer Oktave (Taste 42 auf Bass, Note 12), zu tiefe Note (20 auf 32), zu hohe Note (110 auf 98), ganzer Song wird verschoben, Schlagzeug unverändert.
- `InstrumentMapperTest`: Blechbläser-Programme 56 bis 63 nach D5; Schlagzeug bleibt.
