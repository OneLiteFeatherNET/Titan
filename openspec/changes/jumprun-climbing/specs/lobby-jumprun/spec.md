# Spec Delta

## ADDED Requirements

### Requirement: Klettern als Sprungtyp
Ab einem Mindestscore je Modus MUSS die Lobby Kletterabschnitte erzeugen können: einen Turm aus einem tragenden Pfeiler, einer Leiter oder Ranke und einem Zielblock obenauf. Der Turm MUSS an einen Block des Laufs in einer der vier Achsenrichtungen anschließen, der Zielblock MUSS 3 bis 5 Blöcke höher liegen (Oberkante gegen Oberkante, Standard, einstellbar mit `jumprun.climb.minHeight`/`maxHeight`), und die Leiter oder Ranke MUSS in der Spalte über dem Ausgangsblock stehen, sodass der Läufer vom Ausgangsblock aus klettert. Pfeiler, Leiter oder Ranke und Zielblock sind für den Läufer sichtbar und tragend und für alle anderen eine nicht begehbare Darstellung, und sie kommen und gehen mit dem Zielblock des Sichtfensters. Die Auswahl der Leitern und Ranken MUSS je Art im Abschnitt `jumprun.palettes.ladder` bzw. `jumprun.palettes.vine` stehen (Block-Schlüssel und ganzzahliges Gewicht ≥ 0); ein Block, der nicht kletterbar ist, MUSS beim Start den Start abbrechen, mit vollständigem Schlüssel und Grund. Im Modus Easy DÜRFEN keine Türme vorkommen. Leitern DÜRFEN in Medium erst ab Score 30 und in Hard ab Score 15, Ranken in Medium erst ab Score 40 und in Hard ab Score 25 vorkommen. In der Aufstiegsphase DÜRFEN keine Türme vorkommen. Ein Turm MUSS die Schwierigkeit des Sprungs erhöhen und zählt bei der Schwierigkeit wie ein Sprung mit eigenen Kosten, die mit der Höhe steigen; die Obergrenze der Schwierigkeit MUSS weiter gelten. Die Höhe eines Turms MUSS mit dem Score wachsen, sodass seine Kosten der Zielschwierigkeit des Modus folgen und Türme bei jedem Score ab ihrer Freischaltung zur Wahl stehen; die Höhe bleibt im Bereich `jumprun.climb.minHeight` bis `maxHeight` und im Höhenband der Lobby.

#### Scenario: Turm ab Freischaltung
- **WHEN** in Medium der Score 29 ist
- **THEN** ist der nächste Block kein Turm

#### Scenario: Easy ohne Türme
- **WHEN** ein Lauf im Modus Easy Score 80 erreicht
- **THEN** war kein Block ein Turm

#### Scenario: Höhe des Turms
- **WHEN** ein Turm erzeugt wird
- **THEN** liegt die Oberkante seines Zielblocks 3 bis 5 Blöcke über der Oberkante des Blocks, an den er anschließt, und die Leiter steht in der Spalte über diesem Block

#### Scenario: Bei Score 80 können Kletterstrecken gewählt werden
- **WHEN** ein Lauf im Modus Medium mit festen Seeds Score 80 erreicht
- **THEN** kommt mindestens ein Turm vor

#### Scenario: Turmhöhe wächst monoton mit dem Score
- **WHEN** der Score in Medium oder Hard steigt
- **THEN** ist die Höhe eines Turms nie kleiner als bei einem niedrigeren Score

#### Scenario: Kosten folgen dem Ziel
- **WHEN** ein Turm bei Score 40 oder 80 erzeugt wird und die Höhe im Bereich 3 bis 8 liegt
- **THEN** weicht seine Kosten um höchstens eine halbe Höhenstufe (1,05) von der Zielschwierigkeit ab

#### Scenario: Höhe bleibt im erlaubten Band
- **WHEN** die Zielschwierigkeit eine Höhe über `jumprun.climb.maxHeight` verlangt oder der Zielblock sonst nicht in das Höhenband passt
- **THEN** wird die Höhe auf die Grenze oder um Stufen verkürzt, bis der Zielblock passt, und nie unter `jumprun.climb.minHeight`

#### Scenario: Kein Turm in der Aufstiegsphase
- **WHEN** die Aufstiegsphase noch läuft
- **THEN** ist kein Block ein Turm

#### Scenario: Nur für den Läufer begehbar
- **WHEN** Spieler A einen Turm vor sich hat und Spieler B in der Nähe ist
- **THEN** sieht B Pfeiler, Leiter und Zielblock als Darstellung, kann sie nicht erklettern, und in der echten Welt ist an diesen Stellen weiter Luft

#### Scenario: Kein kletterbarer Block
- **WHEN** `jumprun.palettes.ladder` den Block `stone` enthält
- **THEN** startet die Lobby nicht, und die Meldung nennt `jumprun.palettes.ladder` und den Grund

### Requirement: Fortschritt und Absturz beim Klettern
Der Lobby-Server sieht vom Klettern nur die Positionen des Spielers, und der Fortschritt MUSS deshalb allein aus der Position folgen. Ein Turm MUSS als geschafft gelten, sobald der Spieler auf seinem Zielblock steht, und zählt einen Sprung, egal wie hoch er ist. Auf dem Weg nach oben DARF der Läufer weder Punkte erhalten noch einen Absturz auslösen, solange er nicht unter den Absturz-Schwellwert des Laufs fällt. Fällt er vom Turm bis unter diesen Schwellwert, MUSS der Lauf wie bei jedem Absturz enden.

#### Scenario: Klettern und oben ankommen
- **WHEN** der Läufer die Leiter hinaufklettert und auf dem Zielblock steht
- **THEN** zählt genau ein Sprung, und das Sichtfenster rückt um einen Block vor

#### Scenario: Halb oben
- **WHEN** der Läufer auf halber Höhe der Leiter gemeldet wird
- **THEN** zählt kein Sprung, und der Lauf endet nicht durch einen Absturz

#### Scenario: Fall vom Turm
- **WHEN** der Läufer von der Leiter fällt und die Absturz-Schwelle des Laufs unterschreitet
- **THEN** endet der Lauf wie bei jedem Absturz, mit dem bis dahin erreichten Score

#### Scenario: Zurück auf den Ausgangsblock
- **WHEN** der Läufer ein Stück hochklettert und wieder auf den Ausgangsblock zurückkommt
- **THEN** ist der Lauf unverändert, und es zählt nichts

### Requirement: Platz für Kletterabschnitte
Ein Turm DARF NUR an einer Stelle entstehen, an der die Zellen für Pfeiler, Leiter oder Ranke und der Zielblock in der echten Welt frei sind, über der Leiterspalte bis zur Sprunghöhe über dem Zielblock freier Raum ist, der Zielblock die Bedingungen der Anforderung „Sprünge nur, wo Platz ist“ erfüllt und alle Zellen des Turms innerhalb der Höhengrenzen der Lobby liegen. Ein Turm MUSS zu den sichtbaren Blöcken des Laufs denselben Abstand halten wie ein Sprung, wobei alle Zellen des Turms zählen. Kein neuer Block DARF in den Zellen eines sichtbaren Turms entstehen.

#### Scenario: Pfeiler im Weg
- **WHEN** eine Zelle, in die der Pfeiler käme, in der echten Welt ein Block ist
- **THEN** entsteht dort kein Turm

#### Scenario: Decke über der Leiter
- **WHEN** über der Leiterspalte weniger als die Sprunghöhe über dem Zielblock frei ist
- **THEN** entsteht dort kein Turm

#### Scenario: Zu hoch für die Lobby
- **WHEN** der Zielblock die obere Höhengrenze der Lobby mit dem Abstand der Anforderung „Kein Teleport durch die Höhengrenzen“ überschreiten würde
- **THEN** entsteht dort kein Turm

#### Scenario: Nichts im sichtbaren Turm
- **WHEN** ein Turm im Sichtfenster steht
- **THEN** entsteht kein neuer Block in einer seiner Zellen
