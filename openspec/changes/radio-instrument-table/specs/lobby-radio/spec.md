## MODIFIED Requirements

### Requirement: MIDI-Noten werden auf Notenblock-Instrumente und Tonhöhen abgebildet
_Channels and programs choose the instrument, each track moves by whole octaves into its instrument's register, velocity scaled by channel volume and expression and the player's volume set the loudness._

Die Lobby MUSS jede Note eines MIDI-Songs einem der 20 Notenblock-Instrumente zuordnen. Schlagzeug auf Kanal 10 wird nach Taste zugeordnet, alle anderen Kanäle nach dem General-MIDI-Programm (0 bis 127). Jedes Instrument hat ein Register: Note 0 ist seine Grundtaste, und es reicht 25 Noten weit. Die Note eines Melodie-Tons MUSS so gewählt werden, dass ihre Taste gleich Grundtaste plus Note ist, damit die Oktave stimmt. Die Tonhöhe des Sounds MUSS `2^((n − 12) / 12)` sein.

Die Zuordnung der Programme MUSS nach Familien erfolgen:

| Programme | Instrument |
|---|---|
| 0–3 Klavier | `harp` |
| 4–5 E-Piano | `pling` |
| 6 Cembalo | `guitar` |
| 7 Clavinet | `banjo` |
| 8–9 Celesta, Glockenspiel | `bell` |
| 10 Spieldose | `chime` |
| 11 Vibraphon | `iron_xylophone` |
| 12–13 Marimba, Xylophon | `xylophone` |
| 14 Röhrenglocken | `chime` |
| 15 Hackbrett | `banjo` |
| 16–23 Orgeln, Akkordeon, Mundharmonika, Tango-Akkordeon | `flute` |
| 24–28, 31 Gitarren, Obertongitarre | `guitar` |
| 29–30 Verzerrte Gitarren | `bit` |
| 32–39 Bässe, Synth-Bässe | `bass` |
| 40, 41, 44 Violine, Viola, Tremolo-Streicher | `flute` |
| 42–43 Cello, Kontrabass | `bass` |
| 45–46 Pizzicato-Streicher, Konzertharfe | `harp` |
| 47 Pauke | `basedrum` (tonhöhig) |
| 48–54 Streicher-Ensembles, Synth-Streicher, Chor, Stimme | `flute` |
| 55 Orchestertreffer | `bit` |
| 56, 59, 61–63 Trompete, Posaune, Tuba, Flügelhorn, Waldhorn | `trumpet` |
| 57 Posaune | `trumpet_weathered` |
| 58 Tuba | `trumpet_oxidized` |
| 60 Waldhorn | `trumpet_exposed` |
| 64–69, 71 Saxophone, Oboe, Englischhorn, Klarinette | `flute` |
| 70 Fagott | `didgeridoo` |
| 72–79 Pfeifen: Piccolo, Flöte, Blockflöte, Panflöte, Ocarina | `flute` |
| 80–87 Synth-Leads | `bit` |
| 88–103 Synth-Pads, Synth-Effekte | `pling` |
| 104–106 Sitar, Banjo, Shamisen | `banjo` |
| 107 Koto | `harp` |
| 108, 114 Kalimba, Steel Drums | `iron_xylophone` |
| 109 Dudelsack | `didgeridoo` |
| 110–111 Fiedel, Shanai | `flute` |
| 112 Schellenglocke | `bell` |
| 113 Agogo | `cow_bell` |
| 115 Holzblock | `hat` |
| 116–118 Taiko, Melodic Tom, Synth-Trommel (tonhöhig) | `basedrum` |
| 119 Umgekehrtes Becken | `snare` |
| 120–127 Geräuscheffekte | `hat` |

Eine Spur ist die Gesamtheit der Melodie-Noten eines Kanals mit demselben Programm. Schlagzeug auf Kanal 10 ist keine Spur und wird nicht verschoben. Die Lobby MUSS jeder Spur eine Verschiebung um ganze Oktaven geben: ein Vielfaches von 12 zwischen −48 und +48, das die meisten Noten der Spur in das Register ihres Instruments bringt. Bei Gleichstand MUSS die kleinste Verschiebung gewinnen. Liegen Plus und Minus gleich weit von null, MUSS die Verschiebung gewinnen, deren Median der Spur der Mitte des Registers (Grundtaste plus 12) am nächsten liegt. Der Median ist das mittlere Element der unverschobenen Tasten; bei gerader Anzahl das untere der beiden mittleren. Die Verschiebung gilt für jede Note der Spur, sodass die Intervalle der Spur erhalten bleiben. Tonhöhige Schlagzeug-Programme (47, 116 bis 118) sind Melodie-Noten und folgen dieser Regel.

Liegt eine Melodie-Note nach der Verschiebung außerhalb des Registers ihres Instruments, MUSS die Lobby auf ein Erweiterungsinstrument wechseln: Bass, Gitarre, Flöte oder Glocke, deren Register die Taste enthält. Gibt es mehrere, MUSS das gewählt werden, dessen Grundtaste der des Instruments am nächsten liegt. Nur eine Note außerhalb des Gesamtbereichs von Taste 30 bis 102 DARF um Oktaven verschoben werden, bis sie darin liegt; ihre Tonhöhenklasse bleibt erhalten.

Schlagzeug auf Kanal 10 MUSS jede Taste auf ein festes Instrument und eine feste Note von 0 bis 24 abbilden, ohne Verschiebung und ohne Faltung. Die Zuordnung MUSS sein:

| Taste (GM-Schlagzeug) | Instrument | Note |
|---|---|---|
| 35, 36 Bass Drum | `basedrum` | 6 |
| 37 Side Stick | `hat` | 18 |
| 38, 40 Snare | `snare` | 12 |
| 39 Handclap | `snare` | 16 |
| 41, 43 Low Tom | `basedrum` | 10 |
| 42 Closed Hi-Hat | `hat` | 16 |
| 44 Pedal Hi-Hat | `hat` | 12 |
| 45, 47 Mid Tom | `basedrum` | 14 |
| 46 Open Hi-Hat | `hat` | 10 |
| 48, 50 High Tom | `basedrum` | 18 |
| 49, 57 Crash | `snare` | 20 |
| 51, 59 Ride | `hat` | 20 |
| 52 Chinese Cymbal | `snare` | 20 |
| 53 Ride Bell | `hat` | 20 |
| 54 Tambourine | `hat` | 22 |
| 55 Splash | `snare` | 20 |
| 56 Cowbell | `cow_bell` | 12 |
| 60–66 Bongos, Congas, Timbales | `basedrum` | 16 |
| 67, 68 Agogo | `cow_bell` | 16 |
| 69 Cabasa, 70 Maracas, 82 Shaker | `hat` | 22 |
| 75 Claves, 76, 77 Woodblocks | `hat` | 14 |
| 80, 81 Triangle | `chime` | 18 |
| alle übrigen Tasten | `hat` | 12 |

Die Anschlagstärke einer Note MUSS `velocity × cc7 / 127 × cc11 / 127` sein, gerundet auf die nächste ganze Zahl. `cc7` ist die Kanallautstärke (Control Change 7) und `cc11` der Ausdruck (Control Change 11) des Kanals der Note; beide gelten ab dem Zeitpunkt ihres Ereignisses und sind 127, solange der Kanal sie nicht gesetzt hat. Eine Note, deren Anschlagstärke 0 ergibt, MUSS wie ein Notenereignis mit Anschlagstärke 0 ignoriert werden. Die Lautstärke jeder Note MUSS die Anschlagstärke geteilt durch 127 mal der Lautstärke des Spielers sein, wobei die Lautstärke in Prozent geteilt durch 100 gerechnet wird. Ändert der Spieler die Lautstärke, MUSS die nächste Note mit der neuen Lautstärke klingen.

#### Scenario: Schlagzeug

- **WHEN** eine Note mit Taste 36 auf Kanal 10 gespielt wird
- **THEN** wird das Instrument `basedrum` gewählt und die Note ist 6

#### Scenario: Schlagzeug-Toms und Becken

- **WHEN** Kanal 10 die Tasten 41, 45, 48, 49 und 51 spielt
- **THEN** spielen die Tasten 41, 45 und 48 `basedrum` auf den Noten 10, 14 und 18, die Taste 49 `snare` auf Note 20 und die Taste 51 `hat` auf Note 20

#### Scenario: Unbekannte Schlagzeugtaste

- **WHEN** Kanal 10 die Taste 84 spielt
- **THEN** wird das Instrument `hat` auf Note 12 gewählt

#### Scenario: Tonhöhige Pauke bleibt auf dem Melodiekanal

- **WHEN** ein Melodiekanal das Programm 47 mit den Tasten 36 und 38 spielt
- **THEN** wird das Instrument `basedrum` gewählt und jede Note folgt der Spurregel mit Taste plus Verschiebung

#### Scenario: Tonhöhe

- **WHEN** die Note 12 gespielt wird
- **THEN** ist die Tonhöhe des Sounds 1,0

#### Scenario: Programmfamilien

- **WHEN** ein Titel die Programme 0, 4, 29, 42, 56, 104, 112 und 120 spielt
- **THEN** werden die Instrumente `harp`, `pling`, `bit`, `bass`, `trumpet`, `banjo`, `bell` und `hat` gewählt

#### Scenario: Jedes Programm hat ein Instrument

- **WHEN** ein Programm von 0 bis 127 gespielt wird
- **THEN** wählt die Lobby genau ein Instrument aus der Programmtabelle

#### Scenario: Lautstärke mit Anschlag und Spielerlautstärke

- **WHEN** eine Note mit Anschlag 127 gespielt wird und der Spieler 50 Prozent eingestellt hat
- **THEN** ist die Lautstärke des Sounds 0,5

#### Scenario: Kanallautstärke und Ausdruck skalieren den Anschlag

- **WHEN** ein Kanal Control Change 7 auf 64 und Control Change 11 auf 127 setzt und danach eine Note mit Anschlag 100 spielt
- **THEN** ist der Anschlag der Note 50

#### Scenario: Kanallautstärke eines anderen Kanals wirkt nicht

- **WHEN** Kanal 1 Control Change 7 auf 0 setzt und Kanal 0 danach eine Note mit Anschlag 100 spielt
- **THEN** bleibt der Anschlag der Note von Kanal 0 bei 100

#### Scenario: Skalierter Anschlag von 0 wird ignoriert

- **WHEN** ein Kanal Control Change 7 auf 1 setzt und danach eine Note mit Anschlag 1 spielt
- **THEN** wird die Note nicht gespielt

#### Scenario: Korobeiniki-Melodie auf der Harfe, eine Oktave tiefer

- **WHEN** eine Klavier-Spur (Programm 0) die Tasten 72 bis 88 spielt, also C5 bis E6
- **THEN** ist die Verschiebung −12, jede Note spielt die Harfe, und jede Note klingt eine Oktave tiefer als geschrieben, also wie Taste 60 bis 76

#### Scenario: Reine Bass-Spur bleibt unverschoben

- **WHEN** eine Bass-Spur (Programm 32) nur Taste 42 (F#2) hat
- **THEN** ist die Verschiebung 0, jede Note spielt den Bass mit Note 12 und klingt wie Taste 42

#### Scenario: Drei Oktaven behalten ihre Intervalle

- **WHEN** eine Klavier-Spur die Tasten 48, 60, 72 und 84 hat
- **THEN** ist die Verschiebung 0; die Tasten 60 und 72 spielen die Harfe, Taste 48 spielt die Gitarre mit Note 6, Taste 84 spielt die Flöte mit Note 18, und jede Note klingt wie ihre Taste

#### Scenario: Gleichstand zwischen Plus und Minus

- **WHEN** eine Klavier-Spur die Tasten 20, 43 und 85 hat
- **THEN** ist die Verschiebung +12, denn der Median 43 liegt nach −12 bei 31 und nach +12 bei 55, und 55 liegt näher an der Registermitte 66; die Taste 43 spielt die Harfe mit Note 1 und klingt wie Taste 55

#### Scenario: Zu tiefe Melodie-Note wird gefaltet

- **WHEN** eine Klavier-Spur die Tasten 66, 66, 66 und 20 hat
- **THEN** ist die Verschiebung 0, und die Taste 20 wird um eine Oktave auf Taste 32 gefaltet; sie spielt den Bass mit Note 2

#### Scenario: Sehr hohe Note über dem Flöten-Register auf der Glocke

- **WHEN** eine Klavier-Spur die Tasten 66, 66, 66 und 96 hat
- **THEN** ist die Verschiebung 0, und die Taste 96 spielt die Glocke mit Note 18

#### Scenario: Zu hohe Melodie-Note außerhalb des Gesamtbereichs

- **WHEN** eine Klavier-Spur die Tasten 66, 66, 66 und 110 hat
- **THEN** ist die Verschiebung 0, und die Taste 110 wird um eine Oktave auf 98 gefaltet; sie spielt die Glocke mit Note 20

#### Scenario: Blechbläser

- **WHEN** ein Titel die General-MIDI-Programme 56, 57, 58 und 60 spielt
- **THEN** werden die Instrumente `trumpet`, `trumpet_weathered`, `trumpet_oxidized` und `trumpet_exposed` gewählt
