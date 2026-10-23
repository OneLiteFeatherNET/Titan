# Spec Delta

## ADDED Requirements

### Requirement: Die Schnellaktion öffnet die Tanzliste direkt
_The quick action is the dance list itself: one step, in the player's language._

Die Lobby MUSS jedem Spieler beim ersten Spawn, bei einer Änderung seiner Sprache und bei jedem `/dance` ein eigenes `TagsPacket` für die Dialog-Registry senden, das den Tag `minecraft:quick_actions` genau auf die Tanzliste seiner Variante setzt. Eine Variante ist die Sprache (Deutsch, sonst Englisch) zusammen mit den Tänzen, die der Spieler nutzen darf. Die Listen MUSS die Lobby beim Start einmal registrieren; jeder Text ist in der Sprache der Liste gerendert, weil der Client unsere Schlüssel nicht übersetzt.

#### Scenario: Schnellaktion im Client
- **WHEN** ein Client mit Version 1.21.6 oder neuer die Schnellaktion öffnet
- **THEN** erscheint direkt die Tanzliste mit den Tänzen des Spielers, ohne einen weiteren Schritt

#### Scenario: Sprache des Spielers
- **WHEN** ein deutscher Spieler die Schnellaktion öffnet
- **THEN** ist die Liste auf Deutsch, und ein Spieler mit einer nicht unterstützten Sprache sieht Englisch

#### Scenario: Sonderrecht
- **WHEN** ein Spieler mit `titan.dance.spin` die Schnellaktion öffnet
- **THEN** enthält die Liste Drehen, ein Spieler ohne das Recht nicht

#### Scenario: Sprachwechsel
- **WHEN** ein Spieler seine Sprache ändert
- **THEN** sendet die Lobby ihm die Schnellaktion für die neue Sprache erneut

### Requirement: Die Tanzauswahl zeigt nur erlaubte Tänze
_The per-player dialog lists only the dances this player may use, plus Stop._

Die Tanzliste MUSS jedem Spieler nur die Tänze anbieten, die er nutzen darf, und immer einen Stopp-Knopf enthalten. Die Texte MUSS sie in der Sprache des Spielers zeigen, Deutsch oder Englisch; ohne passende Sprache gilt Englisch. Jeder Tanz-Knopf MUSS die Aktion `titan:dance` mit der Id des Tanzes als Payload senden.

#### Scenario: Spieler ohne Sonderrecht
- **WHEN** ein Spieler ohne `titan.dance.spin` die Tanzauswahl öffnet
- **THEN** enthält sie die Knöpfe Winken, Groove und Party sowie Stopp, aber keinen Knopf für Drehen

#### Scenario: Spieler mit Sonderrecht
- **WHEN** ein Spieler mit `titan.dance.spin` die Tanzauswahl öffnet
- **THEN** enthält sie alle vier Tänze sowie Stopp

#### Scenario: Sprache des Spielers
- **WHEN** ein deutscher Spieler die Tanzauswahl öffnet
- **THEN** sind die Texte auf Deutsch

### Requirement: Der Befehl öffnet dieselbe Liste
_The /dance command is the fallback for clients without quick actions. It is English on every server._

Der Befehl `/dance` MUSS dem Spieler dieselbe Tanzliste öffnen wie die Schnellaktion und dabei seine Schnellaktion neu senden. Es gibt keinen weiteren Befehl. Ein Befehl aus der Konsole MUSS eine verständliche Meldung erhalten und nichts öffnen.

#### Scenario: Befehl im Spiel
- **WHEN** ein Spieler `/dance` eingibt
- **THEN** öffnet sich ihm dieselbe Tanzliste wie über die Schnellaktion

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
- **THEN** nimmt der NPC die Drehpose ein, hebt sich einen Block und mehr über den Boden, landet nach 40 Ticks und sieht Herzpartikel

#### Scenario: Ende des Tanzes
- **WHEN** ein Tanz die letzte Tick seiner Länge erreicht
- **THEN** endet er, und der NPC steht wieder auf dem Boden

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

### Requirement: Ein NPC tanzt an Stelle des Spielers
_While dancing, the real player is hidden from everyone; a mannequin in his skin is what everyone sees dance._

Beim Start eines Tanzes MUSS die Lobby genau einen NPC vom Typ `MANNEQUIN` an der Position und mit der Blickrichtung des Tanzenden erzeugen. Der NPC MUSS das Profil des Tanzenden tragen (seinen Skin, sonst Name und Id), unbeweglich sein, seinen Namen über dem Kopf zeigen und für alle Spieler der Instanz sichtbar sein, auch für den Tanzenden. Der echte Spieler MUSS für alle anderen Spieler unsichtbar sein, auch für solche, die während des Tanzes die Instanz betreten. Jeder Tanzschritt MUSS nur den NPC bewegen, nie den Spieler. Jeder Stopp-Pfad MUSS den NPC entfernen und den Spieler wieder für alle sichtbar machen.

#### Scenario: NPC beim Start
- **WHEN** ein Spieler einen Tanz startet
- **THEN** existiert genau ein NPC an seiner Position, mit seinem Skin und seinem Namen darüber, und alle Spieler der Instanz sehen ihn

#### Scenario: Der echte Spieler ist unsichtbar
- **WHEN** ein anderer Spieler während des Tanzes die Instanz betritt
- **THEN** sieht er den NPC, aber nicht den echten Tanzenden

#### Scenario: Schritt erreicht den NPC
- **WHEN** der Tanz einen Armschwung, das Ducken, den Sprung oder die Drehung ausführt
- **THEN** führt der NPC die Bewegung aus, und der echte Spieler erhält keine Geschwindigkeit und keine Pose

#### Scenario: Der NPC verschwindet mit dem Tanz
- **WHEN** ein Tanz stoppt, aus welchem Grund auch immer
- **THEN** ist der NPC aus der Instanz entfernt, und der echte Spieler ist wieder für alle sichtbar

### Requirement: Stopp-Auslöser beenden den Tanz
_Movement, input, attacks, interactions, leaving the instance, disconnects, the stop button and errors all end a dance._

Ein Tanz MUSS enden, wenn der Spieler sich über eine kleine Schwelle bewegt, eine Bewegungseingabe sendet, angreift oder interagiert, die Instanz verlässt, die Verbindung trennt, den Stopp-Knopf auslöst oder ein Fehler auftritt. Das Ducken des Spielers selbst ist keine Bewegung.

#### Scenario: Bewegung beendet den Tanz
- **WHEN** ein tanzender Spieler sich über die Schwelle bewegt
- **THEN** endet der Tanz, der Spieler ist wieder für alle sichtbar, die Kamera gehört ihm, und die Kameraentität ist entfernt

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

### Requirement: Jeder Stopp stellt Kamera, Sichtbarkeit und Aufgabe wieder her
_Whatever ends a dance, the player is seen again and has his own camera, and a failure never leaves either behind._

Bei jedem Ende MUSS der Spieler wieder die eigene Kamera haben, die Kameraentität und der NPC entfernt sein und der echte Spieler für alle wieder sichtbar sein. Ein Fehler im Tanz DARF weder die Kamera noch die Sichtbarkeit noch die Aufgabe des Tanzes zurücklassen; ein doppelter Stopp DARF nichts verändern.

#### Scenario: Scheitert das Zurücksetzen des NPC
- **WHEN** das Zurücksetzen des NPC beim Stopp eine Ausnahme wirft
- **THEN** gehört dem Spieler trotzdem wieder seine Kamera, der NPC und die Kameraentität sind entfernt, und der Spieler ist wieder für alle sichtbar

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
