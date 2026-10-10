# Spec Delta

## MODIFIED Requirements

### Requirement: Schwierigkeit steigt mit dem Score
Nach der Aufstiegsphase MUSS die Schwierigkeit jedes neuen Sprungs aus einer festen, stetig steigenden Funktion des Scores folgen, die sich einem Höchstwert annähert, ohne ihn zu überschreiten. Die Schwierigkeit MUSS langsam steigen: Bis Score 10 MÜSSEN alle Blöcke Vollblöcke sein, Stufen und Falltüren DÜRFEN erst ab Score 10 vorkommen. Schmale Formen (Zaun oder Mauer, Glasscheibe oder Gitter, schmaler Pfosten, Kopf, Blumentopf, Kerze) DÜRFEN im Modus Medium bei keinem Score vorkommen; im Modus Hard DÜRFEN Zäune, Mauern, Scheiben, Gitter, Köpfe und Blumentöpfe erst ab Score 10 und Pfosten und Kerzen erst ab Score 20 vorkommen. Die Schwierigkeit MUSS sich in der Form des Blocks (Vollblock, Falltür, Stufe, Teppich, Schnee, Zaun oder Mauer, Glasscheibe oder Gitter, schmaler Pfosten, Kopf, Blumentopf, Kerze), in der Lückenbreite und im Höhenunterschied zeigen. Jeder erzeugte Sprung MUSS ohne Hilfsmittel schaffbar sein. Maßgeblich ist die Oberkante der Lauffläche, also z. B. bei einer Falltür knapp ein Fünftel, bei einer Stufe ein halber Block und bei einem Zaun oder einer Mauer anderthalb Blöcke über ihrer Blockposition. Die Oberkante des Ziels darf höchstens einen Block über der des Ausgangsblocks liegen. Die Lücke darf bei einem Aufstieg höchstens 3 Blöcke betragen, auf gleicher Höhe oder abwärts höchstens 4 Blöcke.

#### Scenario: Lange nur Vollblöcke
- **WHEN** der Score unter 10 liegt
- **THEN** sind alle erzeugten Blöcke Vollblöcke

#### Scenario: Leichter Anfang
- **WHEN** der Score 0 ist
- **THEN** sind die erzeugten Sprünge überwiegend Vollblöcke mit kurzer Lücke

#### Scenario: Später schwerer
- **WHEN** der Score hoch ist (z. B. 80) im Modus Medium
- **THEN** sind die erzeugten Sprünge im Mittel deutlich schwerer als bei Score 0, mit breiteren Lücken und mehr Aufstiegen, und keiner von ihnen ist eine schmale Form

#### Scenario: Medium ohne schmale Formen
- **WHEN** ein Lauf im Modus Medium Score 40 oder mehr erreicht
- **THEN** ist kein Block eine schmale Form, also kein Zaun, keine Mauer, keine Scheibe, kein Gitter, kein Pfosten, kein Kopf, kein Blumentopf und keine Kerze

#### Scenario: Hard mit schmalen Formen
- **WHEN** ein Lauf im Modus Hard Score 20 erreicht
- **THEN** kann jede schmale Form erscheinen, und über viele Läufe erscheinen alle

#### Scenario: Zaun nach Vollblock
- **WHEN** ein Zaun auf einen Vollblock folgt
- **THEN** liegt der Zaun auf derselben Blockhöhe oder tiefer, weil seine Oberkante sonst mehr als einen Block über der des Vollblocks läge

#### Scenario: Nie unmöglich
- **WHEN** beliebig viele Sprünge erzeugt werden
- **THEN** überschreitet kein Sprung die Grenzen für Lücke und Aufstieg, auch nicht von einem Zaun oder einer Stufe aus
