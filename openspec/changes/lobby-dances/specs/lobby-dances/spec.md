# Spec Delta

## ADDED Requirements

### Requirement: Die Schnellaktion öffnet die Tanzauswahl
_The quick action is a static entry; the per-player choice follows on click._

Die Lobby MUSS beim Start den Dialog `titan:dances` mit genau einem Knopf in den Tag `minecraft:quick_actions` eintragen, bevor ein Spieler sich verbinden kann. Der Knopf löst `titan:dances/open` aus. Der Text ist neutral und nicht übersetzt, weil Registry-Daten für alle Clients gleich sind.

#### Scenario: Schnellaktion im Client
- **WHEN** ein Client mit Version 1.21.6 oder neuer die Schnellaktion öffnet
- **THEN** erscheint der Eintrag der Lobby-Tänze mit genau einem Knopf

#### Scenario: Klick auf den Schnellaktions-Knopf
- **WHEN** ein Spieler den Knopf `titan:dances/open` auslöst
- **THEN** zeigt die Lobby ihm die Tanzauswahl, und kein anderer Spieler erhält sie

### Requirement: Die Tanzauswahl zeigt nur erlaubte Tänze
_The per-player dialog lists only the dances this player may use, plus Stop._

Die Tanzauswahl MUSS jedem Spieler nur die Tänze anbieten, die er nutzen darf, und immer einen Stopp-Knopf enthalten. Die Texte MUSS sie in der Sprache des Spielers zeigen, Deutsch oder Englisch; ohne passende Sprache gilt Englisch. Jeder Tanz-Knopf MUSS die Aktion `titan:dance` mit der Id des Tanzes als Payload senden.

#### Scenario: Spieler ohne Sonderrecht
- **WHEN** ein Spieler ohne `titan.dance.spin` die Tanzauswahl öffnet
- **THEN** enthält sie die Knöpfe Winken, Groove und Party sowie Stopp, aber keinen Knopf für Drehen

#### Scenario: Spieler mit Sonderrecht
- **WHEN** ein Spieler mit `titan.dance.spin` die Tanzauswahl öffnet
- **THEN** enthält sie alle vier Tänze sowie Stopp

#### Scenario: Sprache des Spielers
- **WHEN** ein deutscher Spieler die Tanzauswahl öffnet
- **THEN** sind die Texte auf Deutsch

### Requirement: Der Befehl öffnet dieselbe Auswahl
_The /tanz command is the fallback for clients without quick actions._

Der Befehl `/tanz` (Alias `/dance`) MUSS dem Spieler dieselbe Tanzauswahl öffnen wie die Schnellaktion. Ein Befehl aus der Konsole MUSS eine verständliche Meldung erhalten und nichts öffnen.

#### Scenario: Befehl im Spiel
- **WHEN** ein Spieler `/tanz` eingibt
- **THEN** öffnet sich ihm dieselbe Tanzauswahl wie über die Schnellaktion

#### Scenario: Befehl aus der Konsole
- **WHEN** die Konsole `dance` ausführt
- **THEN** öffnet nichts, und die Konsole erhält eine Meldung, dass nur Spieler tanzen können

### Requirement: Ein gesendeter Tanz wird vor dem Start geprüft
_The server never trusts a dance id from the client: unknown ids and missing permissions are ignored._

Die Lobby MUSS die Id aus einer Aktion `titan:dance` prüfen, bevor sie einen Tanz startet. Eine unbekannte Id, ein fehlendes Payload oder eine Id ohne Berechtigung MUSS sie ignorieren und nur im Debug-Log erwähnen. Ein Tanz, der nicht erlaubt ist, MUSS dabei den Spieler nicht verändern.

#### Scenario: Unbekannte Id
- **WHEN** ein Client `titan:dance` mit der Id `unknown` sendet
- **THEN** startet kein Tanz, und die Lobby schreibt eine Debug-Zeile

#### Scenario: Fehlendes Payload
- **WHEN** ein Client `titan:dance` ohne Payload sendet
- **THEN** startet kein Tanz

#### Scenario: Gesperrter Tanz
- **WHEN** ein Spieler ohne `titan.dance.spin` die Id `spin` sendet
- **THEN** startet der Tanz nicht, und seine Pose und Kamera bleiben unverändert

### Requirement: Jeder Tanz ist eine feste Folge von Schritten
_Each dance is a fixed sequence of tick steps, played by one scheduled task per dancer._

Ein Tanz MUSS aus festen Schritten bestehen, die innerhalb seiner Länge in Ticks liegen. Es gibt die vier Tänze `wave`, `groove`, `party` und `spin`; nur `spin` braucht eine Berechtigung.

#### Scenario: Winken
- **WHEN** ein Spieler `wave` startet
- **THEN** schwingt er in festen Abständen den Arm und nickt mit dem Kopf

#### Scenario: Drehen
- **WHEN** ein Spieler mit Berechtigung `spin` startet
- **THEN** nimmt er die Drehpose ein und sieht Herzpartikel

#### Scenario: Ende des Tanzes
- **WHEN** ein Tanz die letzte Tick seiner Länge erreicht
- **THEN** endet er, und die Pose kehrt auf stehend zurück

### Requirement: Ein Spieler tanzt nur einen Tanz gleichzeitig, mit Abklingzeit
_At most one dance at a time; a fixed cooldown of three seconds between starts._

Ein Spieler MUSS höchstens einen Tanz gleichzeitig ausführen. Zwischen zwei Starten desselben Spielers liegen mindestens drei Sekunden; die Abklingzeit ist eine feste Konstante im Code. Ein abgelehnter Start DARF den Spieler nicht verändern.

#### Scenario: Zweiter Tanz während eines laufenden Tanzes
- **WHEN** ein Spieler einen Tanz läuft hat und einen zweiten startet
- **THEN** läuft der erste Tanz weiter, der zweite startet nicht

#### Scenario: Neustart innerhalb der Abklingzeit
- **WHEN** ein Tanz endet und der Spieler nach zwei Sekunden erneut startet
- **THEN** startet der Tanz nicht

#### Scenario: Neustart nach der Abklingzeit
- **WHEN** ein Tanz endet und der Spieler nach drei Sekunden erneut startet
- **THEN** startet der Tanz

### Requirement: Die Kamera zeigt den Tanzenden von außen
_While dancing, the dancer views his NPC from a fixed camera entity that is removed again on stop._

Beim Start eines Tanzes MUSS die Lobby eine unsichtbare Kameraentität erzeugen, die nur dem Tanzenden sichtbar ist, etwa 3,5 Blöcke vor dem NPC auf Augenhöhe steht und auf dessen Augen blickt. Die Kameraentität MUSS vom Typ `ITEM_DISPLAY` ohne Item sein, weil dieser Typ keine Größe hat und nichts rendert. Die Lobby MUSS dem Spieler mit `CameraPacket` diese Entität als Kamera setzen. Jeder Stopp-Pfad MUSS die Kamera mit `CameraPacket` wieder auf den Spieler setzen und die Entität entfernen.

#### Scenario: Kamera beim Start
- **WHEN** ein Spieler einen Tanz startet
- **THEN** sieht er über die Kameraentität seinen NPC, die Kamera steht davor und blickt auf dessen Augen, und nur er kennt diese Entität

#### Scenario: Kamera nach dem Stopp
- **WHEN** ein Tanz stoppt
- **THEN** ist die Kamera wieder der Spieler, und die Kameraentität existiert nicht mehr

### Requirement: Ein NPC tanzt in der Sicht des Tanzenden
_The dancer sees a mannequin in his skin dance in front of the camera; other players only see the real player._

Beim Start eines Tanzes MUSS die Lobby genau einen NPC vom Typ `MANNEQUIN` an der Position und mit der Blickrichtung des Tanzenden erzeugen. Der NPC MUSS das Profil des Tanzenden tragen (seinen Skin, sonst Name und Id), unbeweglich sein und nur dem Tanzenden angezeigt werden. Jeder Tanzschritt MUSS sowohl den Spieler als auch den NPC erreichen. Jeder Stopp-Pfad MUSS den NPC entfernen.

#### Scenario: NPC beim Start
- **WHEN** ein Spieler einen Tanz startet
- **THEN** existiert genau ein NPC an seiner Position, mit seinem Skin, und nur er sieht ihn

#### Scenario: Schritt erreicht den NPC
- **WHEN** der Tanz einen Armschwung oder das Ducken ausführt
- **THEN** schwingt der NPC den Arm beziehungsweise duckt sich, während der echte Spieler die gleiche Bewegung ausführt

#### Scenario: Der NPC verschwindet mit dem Tanz
- **WHEN** ein Tanz stoppt, aus welchem Grund auch immer
- **THEN** ist der NPC aus der Instanz entfernt, und andere Spieler haben ihn nie gesehen

### Requirement: Stopp-Auslöser beenden den Tanz
_Movement, input, attacks, interactions, leaving the instance, disconnects, the stop button and errors all end a dance._

Ein Tanz MUSS enden, wenn der Spieler sich über eine kleine Schwelle bewegt, eine Bewegungseingabe sendet, angreift oder interagiert, die Instanz verlässt, die Verbindung trennt, den Stopp-Knopf auslöst oder ein Fehler auftritt. Das Ducken des Spielers selbst ist keine Bewegung.

#### Scenario: Bewegung beendet den Tanz
- **WHEN** ein tanzender Spieler sich über die Schwelle bewegt
- **THEN** endet der Tanz, die Pose ist stehend, die Kamera gehört dem Spieler, und die Kameraentität ist entfernt

#### Scenario: Bewegungseingabe beendet den Tanz
- **WHEN** ein tanzender Spieler die Taste für vorwärts sendet
- **THEN** endet der Tanz wie bei einer Bewegung

#### Scenario: Angriff beendet den Tanz
- **WHEN** ein tanzender Spieler einen Block oder eine Entität angreift
- **THEN** endet der Tanz

#### Scenario: Verbindungsabbruch
- **WHEN** ein tanzender Spieler die Verbindung trennt
- **THEN** ist die Kameraentität entfernt und der Tanz beendet, ohne dass die Lobby einen Fehler meldet

#### Scenario: Stopp-Knopf
- **WHEN** ein tanzender Spieler den Stopp-Knopf auslöst
- **THEN** endet der Tanz wie bei einer Bewegung

#### Scenario: Fehler im Tanz
- **WHEN** ein Schritt des Tanzes einen Fehler auslöst
- **THEN** endet der Tanz, der Spieler hat wieder seine Kamera, und die Kameraentität ist entfernt

### Requirement: Jeder Stopp stellt Pose, Kamera und Aufgabe wieder her
_Whatever ends a dance, the player is left standing with his own camera, and a failure never leaves the camera behind._

Bei jedem Ende MUSS die Pose auf `STANDING` stehen, der Spieler wieder die eigene Kamera haben, die Kameraentität und der NPC entfernt sein. Ein Fehler im Tanz DARF weder die Kamera noch die Aufgabe des Tanzes zurücklassen; ein doppelter Stopp DARF nichts verändern.

#### Scenario: Scheitert das Zurücksetzen der Pose
- **WHEN** das Zurücksetzen der Pose beim Stopp eine Ausnahme wirft
- **THEN** gehört dem Spieler trotzdem wieder seine Kamera, und die Kameraentität ist entfernt

#### Scenario: Doppelter Stopp
- **WHEN** ein Tanz bereits gestoppt ist und ein weiterer Stopp-Auslöser eintritt
- **THEN** ändert sich nichts mehr, und die Lobby meldet keinen Fehler

### Requirement: Der Start eines Tanzes wird als Zähler erfasst
_A started dance counts once, labelled with its id, so the lobby can see which dances are used._

Jeder erfolgreiche Start MUSS den Zähler `titan.dance.started` um eins erhöhen, mit dem Attribut `dance` als Id des Tanzes. Abgelehnte Starts zählen nicht.

#### Scenario: Erfolgreicher Start
- **WHEN** ein Spieler `party` startet
- **THEN** steigt der Zähler `titan.dance.started` für `dance=party` um eins

#### Scenario: Abgelehnter Start
- **WHEN** ein Spieler während der Abklingzeit einen Tanz startet
- **THEN** steigt kein Zähler
