## MODIFIED Requirements

### Requirement: MIDI-Noten werden auf Notenblock-Instrumente und Tonhöhen abgebildet
_Channels and programs choose the instrument, each note sounds on the instrument whose register holds it, velocity and the player's volume set the loudness._

Die Lobby MUSS jede Note eines MIDI-Songs einem der 20 Notenblock-Instrumente zuordnen, Schlagzeug auf Kanal 10 nach Taste, alle anderen Kanäle nach dem General-MIDI-Programm. Jedes Instrument hat ein Register: Note 0 ist seine Grundtaste, und es reicht 25 Noten weit. Die Note eines Melodie-Tons MUSS so gewählt werden, dass ihre Taste gleich Grundtaste plus Note ist, damit die Oktave stimmt. Die Tonhöhe des Sounds MUSS `2^((n − 12) / 12)` sein. Die Lautstärke jeder Note MUSS `velocity / 127` mal der Lautstärke des Spielers sein, wobei die Lautstärke in Prozent geteilt durch 100 gerechnet wird. Notenereignisse mit Anschlagstärke 0 MÜSSEN ignoriert werden. Ändert der Spieler die Lautstärke, MUSS die nächste Note mit der neuen Lautstärke klingen.

Liegt eine Melodie-Note außerhalb des Registers ihres Instruments, MUSS die Lobby auf ein Erweiterungsinstrument wechseln: Bass, Gitarre, Flöte oder Glocke, deren Register die Taste enthält. Gibt es mehrere, MUSS das gewählt werden, dessen Grundtaste der des Instruments am nächsten liegt. Nur eine Note außerhalb des Gesamtbereichs von Taste 30 bis 102 DARF um Oktaven verschoben werden, bis sie darin liegt; ihre Tonhöhenklasse bleibt erhalten.

Die Melodie eines Songs MUSS wie bisher um ganze Oktaven verschoben werden, sodass ihr Median in der Mitte des Bereichs liegt. Schlagzeug wird davon nicht verschoben.

#### Scenario: Schlagzeug

- **WHEN** eine Note mit Taste 36 auf Kanal 10 gespielt wird
- **THEN** wird das Instrument `basedrum` gewählt und die Note bleibt 12

#### Scenario: Tonhöhe

- **WHEN** die Note 12 gespielt wird
- **THEN** ist die Tonhöhe des Sounds 1,0

#### Scenario: Lautstärke mit Anschlag und Spielerlautstärke

- **WHEN** eine Note mit Anschlag 127 gespielt wird und der Spieler 50 Prozent eingestellt hat
- **THEN** ist die Lautstärke des Sounds 0,5

#### Scenario: Hohe Melodie-Note über F#5 bleibt in der Oktave

- **WHEN** eine Klavier-Stimme (Programm 0) die Taste 79 spielt und der Median des Songs in der Mitte liegt
- **THEN** wird die Flöte mit Note 13 gewählt, und die Note klingt eine Oktave über F#5 wie Taste 79, nicht eine Oktave tiefer

#### Scenario: Sehr hohe Melodie-Note auf der Glocke

- **WHEN** eine Klavier-Stimme die Taste 96 spielt und der Median des Songs in der Mitte liegt
- **THEN** wird die Glocke mit Note 18 gewählt, und die Note klingt wie Taste 96

#### Scenario: Bass-Stimme bleibt in ihrer Oktave

- **WHEN** eine Bass-Stimme (Programm 32) die Taste 42 spielt und der Median des Songs in der Mitte liegt
- **THEN** wird der Bass mit Note 12 gewählt, und die Note klingt wie Taste 42

#### Scenario: Blechbläser

- **WHEN** ein Titel das General-MIDI-Programm 56 spielt, dann 59, dann 60
- **THEN** wird `trumpet` gewählt, dann `trumpet_oxidized`, dann `trumpet_weathered`

#### Scenario: Zu tiefe Melodie-Note

- **WHEN** eine Melodie-Note die Taste 20 hat, der Median in der Mitte liegt
- **THEN** wird sie um eine Oktave auf Taste 32 gefaltet, und sie spielt den Bass mit Note 2

#### Scenario: Zu hohe Melodie-Note außerhalb des Gesamtbereichs

- **WHEN** eine Melodie-Note die Taste 110 hat, der Median in der Mitte liegt
- **THEN** wird sie um eine Oktave auf Taste 98 gefaltet, und sie spielt die Glocke mit Note 20
