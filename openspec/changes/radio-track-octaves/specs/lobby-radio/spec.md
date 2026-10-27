## MODIFIED Requirements

### Requirement: MIDI-Noten werden auf Notenblock-Instrumente und Tonhöhen abgebildet
_Channels and programs choose the instrument, each track moves by whole octaves into its instrument's register, velocity and the player's volume set the loudness._

Die Lobby MUSS jede Note eines MIDI-Songs einem der 20 Notenblock-Instrumente zuordnen, Schlagzeug auf Kanal 10 nach Taste, alle anderen Kanäle nach dem General-MIDI-Programm. Jedes Instrument hat ein Register: Note 0 ist seine Grundtaste, und es reicht 25 Noten weit. Die Note eines Melodie-Tons MUSS so gewählt werden, dass ihre Taste gleich Grundtaste plus Note ist, damit die Oktave stimmt. Die Tonhöhe des Sounds MUSS `2^((n − 12) / 12)` sein. Die Lautstärke jeder Note MUSS `velocity / 127` mal der Lautstärke des Spielers sein, wobei die Lautstärke in Prozent geteilt durch 100 gerechnet wird. Notenereignisse mit Anschlagstärke 0 MÜSSEN ignoriert werden. Ändert der Spieler die Lautstärke, MUSS die nächste Note mit der neuen Lautstärke klingen.

Eine Spur ist die Gesamtheit der Melodie-Noten eines Kanals mit demselben Programm. Schlagzeug auf Kanal 10 ist keine Spur und wird nicht verschoben. Die Lobby MUSS jeder Spur eine Verschiebung um ganze Oktaven geben: ein Vielfaches von 12 zwischen −48 und +48, das die meisten Noten der Spur in das Register ihres Instruments bringt. Bei Gleichstand MUSS die kleinste Verschiebung gewinnen. Liegen Plus und Minus gleich weit von null, MUSS die Verschiebung gewinnen, deren Median der Spur der Mitte des Registers (Grundtaste plus 12) am nächsten liegt. Der Median ist das mittlere Element der unverschobenen Tasten; bei gerader Anzahl das untere der beiden mittleren. Die Verschiebung gilt für jede Note der Spur, sodass die Intervalle der Spur erhalten bleiben.

Liegt eine Melodie-Note nach der Verschiebung außerhalb des Registers ihres Instruments, MUSS die Lobby auf ein Erweiterungsinstrument wechseln: Bass, Gitarre, Flöte oder Glocke, deren Register die Taste enthält. Gibt es mehrere, MUSS das gewählt werden, dessen Grundtaste der des Instruments am nächsten liegt. Nur eine Note außerhalb des Gesamtbereichs von Taste 30 bis 102 DARF um Oktaven verschoben werden, bis sie darin liegt; ihre Tonhöhenklasse bleibt erhalten.

#### Scenario: Schlagzeug

- **WHEN** eine Note mit Taste 36 auf Kanal 10 gespielt wird
- **THEN** wird das Instrument `basedrum` gewählt und die Note bleibt 12

#### Scenario: Tonhöhe

- **WHEN** die Note 12 gespielt wird
- **THEN** ist die Tonhöhe des Sounds 1,0

#### Scenario: Lautstärke mit Anschlag und Spielerlautstärke

- **WHEN** eine Note mit Anschlag 127 gespielt wird und der Spieler 50 Prozent eingestellt hat
- **THEN** ist die Lautstärke des Sounds 0,5

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
