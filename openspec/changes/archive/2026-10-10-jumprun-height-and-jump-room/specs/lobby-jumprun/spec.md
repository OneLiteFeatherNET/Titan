# Spec Delta

## MODIFIED Requirements

### Requirement: Sprünge nur, wo Platz ist
Ein neuer Block nach der Aufstiegsphase DARF NUR an einer Stelle entstehen, die all diese Bedingungen erfüllt:
- Sie ist waagrecht mindestens 16 Blöcke vom Lobby-Spawn entfernt.
- Unter ihr sind in der echten Welt mindestens 6 Blöcke Luft.
- An ihr ist in der echten Welt Luft.
- Über ihrer Oberkante ist so viel freier Raum, dass der Spieler dort stehen und von dort aus springen kann. Das sind seine Körperhöhe plus die Höhe eines Sprungs.
- Ihre Flugbahn vom vorigen Block aus ist bis zur Scheitelhöhe des Sprungs frei.
- Sie überschneidet keinen sichtbaren Block des eigenen Laufs.
- Sie liegt innerhalb der Höhengrenzen der Lobby, mit dem Abstand aus der Anforderung „Kein Teleport durch die Höhengrenzen“.

Ein neuer Block und die Flugbahn dorthin DÜRFEN NICHT in einem Lobby-Portal oder näher als 3 Blöcke an einem liegen, damit ein Läufer nie versehentlich auf einen anderen Server geschickt wird. Ein neuer Block DARF NUR gewählt werden, wenn von ihm aus mindestens ein weiterer gültiger Sprung möglich ist. Unter den gültigen Stellen MUSS die Lobby solche mit mehr Luft darunter, vor allem in der Senkrechten, und drumherum bevorzugen, ohne dass dadurch die Schwierigkeit nach dem Score verloren geht. Findet die Lobby trotzdem keinen gültigen nächsten Block, MUSS der Lauf mit dem erreichten Score enden.

#### Scenario: Nicht über Wegen
- **WHEN** eine Stelle direkt über einem Weg liegt, sodass unter ihr weniger als 6 Blöcke Luft sind
- **THEN** entsteht dort nach der Aufstiegsphase kein Block

#### Scenario: Nicht zurück zum Spawn
- **WHEN** eine sonst gültige Stelle waagrecht weniger als 16 Blöcke vom Spawn entfernt liegt
- **THEN** entsteht dort nach der Aufstiegsphase kein Block

#### Scenario: Ins Leere bevorzugt
- **WHEN** zwei gleich schwere Stellen möglich sind, eine über offenem Raum und eine dicht neben einem Gebäude
- **THEN** wählt die Lobby die Stelle über offenem Raum

#### Scenario: Portal in der Nähe
- **WHEN** in Laufrichtung ein Lobby-Portal liegt
- **THEN** führt kein Block und keine Flugbahn in das Portal oder näher als 3 Blöcke daran

#### Scenario: Wand im Weg
- **WHEN** in Laufrichtung eine Wand der Lobby steht
- **THEN** entsteht kein Block in oder hinter der Wand, sondern der Lauf führt an ihr vorbei

#### Scenario: Keine Sackgasse
- **WHEN** ein Kandidat zwar frei ist, von ihm aus aber kein weiterer Sprung möglich wäre
- **THEN** wird er nicht gewählt

#### Scenario: Gar kein Platz mehr
- **WHEN** von der aktuellen Stelle aus kein gültiger Block mehr möglich ist
- **THEN** endet der Lauf mit dem erreichten Score

#### Scenario: Nicht unter eine Decke
- **WHEN** eine sonst gültige Stelle 2 Blöcke unter der Unterseite einer Insel oder eines Überhangs liegt, sodass der Spieler dort stehen, aber nicht springen könnte
- **THEN** entsteht dort kein Block

#### Scenario: Flugbahn unter einem Überhang
- **WHEN** zwischen zwei Blöcken ein Überhang so tief hängt, dass er die Scheitelhöhe des Sprungs schneidet
- **THEN** wird dieser Sprung nicht erzeugt

#### Scenario: Jeder Block ist ein Absprung
- **WHEN** ein Spieler auf einem beliebigen Block eines Laufs steht
- **THEN** ist über ihm bis zur Scheitelhöhe eines Sprungs nur Luft

## ADDED Requirements

### Requirement: Kein Teleport durch die Höhengrenzen
Ein Lauf DARF einen Läufer NIE so hoch oder so tief führen, dass die Lobby ihn wegen ihrer Höhengrenzen zum Spawn teleportiert. Jeder Block eines Laufs, auch in der Aufstiegsphase, MUSS so weit über der unteren Höhengrenze liegen, dass ein Absturz von ihm den Lauf beendet und den Spieler an den Startpunkt zurücksetzt, bevor er die Grenze erreicht. Jeder Block MUSS so weit unter der oberen Höhengrenze liegen, dass der Spieler auch im Scheitelpunkt eines Sprungs darunter bleibt. Kann ein Lauf an der Stelle des Spielers nicht so beginnen, MUSS der Start mit der Meldung „kein Platz zum Starten“ ausbleiben. Die Höhengrenzen sind die der Lobby; ändert der Betreiber sie, MUSS der nächste neue Block die neuen Grenzen beachten.

#### Scenario: Absturz knapp über der Untergrenze
- **WHEN** ein Läufer vom tiefsten erlaubten Block senkrecht abstürzt
- **THEN** endet der Lauf, und er steht am Startpunkt des Laufs, nicht am Lobby-Spawn

#### Scenario: Kein Block nahe der Untergrenze
- **WHEN** eine sonst gültige Stelle so tief liegt, dass ein Absturz von ihr die untere Höhengrenze erreichen würde, bevor der Lauf endet
- **THEN** entsteht dort kein Block

#### Scenario: Kein Block nahe der Obergrenze
- **WHEN** eine sonst gültige Stelle so hoch liegt, dass der Spieler im Scheitelpunkt eines Sprungs von ihr die obere Höhengrenze überschreiten würde
- **THEN** entsteht dort kein Block

#### Scenario: Start zu nah an einer Grenze
- **WHEN** ein Spieler einen Lauf an einer Stelle starten will, an der die Aufstiegsphase das erlaubte Höhenband verlassen würde
- **THEN** startet kein Lauf, und er erhält die Meldung „kein Platz zum Starten“ in seiner Sprache
