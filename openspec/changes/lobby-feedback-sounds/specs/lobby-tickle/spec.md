# Spec Delta

## ADDED Requirements

### Requirement: Ein Kitzeln spielt einen verspielten Vanilla-Ton
_A tickle plays one short vanilla sound, positioned at the tickled player._

Wenn ein Spieler einen anderen mit einer Feder kitzelt, MUSS die Lobby den Vanilla-Ton `entity.allay.item_given` mit der Lautstärke 0,6 spielen. Die Tonhöhe MUSS zufällig zwischen 0,9 und 1,2 liegen. Der Ton MUSS an der Position des gekitzelten Spielers liegen. Er MUSS an alle Spieler gehen, die sich höchstens 16 Blöcke von dieser Position entfernt aufhalten; dazu zählt der Kitzelnde, solange er in dieser Reichweite steht.

#### Scenario: Kitzeln hörbar für beide und für Nahe

- **WHEN** Spieler A Spieler B mit einer Feder kitzelt und ein dritter Spieler C 5 Blöcke von B entfernt steht
- **THEN** hören A, B und C genau einen Kitzel-Ton an der Position von B

#### Scenario: Fernen Spieler erreicht der Ton nicht

- **WHEN** Spieler A Spieler B kitzelt und ein Spieler D 30 Blöcke von B entfernt steht
- **THEN** erhält D keinen Kitzel-Ton

#### Scenario: Ohne Feder kein Ton

- **WHEN** Spieler A Spieler B ohne Feder angreift
- **THEN** wird kein Kitzel-Ton gespielt

### Requirement: Der Kitzel-Ton ist pro Paar begrenzt
_Spamming one pair does not spam audio._

Die Lobby MUSS für jedes geordnete Paar (Kitzelnder, Ziel) höchstens alle 2 Sekunden einen Kitzel-Ton spielen. Ein Kitzeln innerhalb dieser 2 Sekunden MUSS weiterhin ausgeführt werden (Nachricht, Abklingzeit); nur der Ton entfällt. Andere Paare sind davon nicht betroffen.

#### Scenario: Zweites Kitzeln innerhalb von 2 Sekunden ohne Ton

- **WHEN** Spieler A Spieler B kitzelt, die konfigurierte Kitzel-Abklingzeit kürzer als 2 Sekunden ist, und A Spieler B 1 Sekunde später erneut kitzelt
- **THEN** wird der Ton nur einmal gespielt, und der zweite Kitzel wird trotzdem ausgeführt (Nachricht, Abklingzeit)

#### Scenario: Nach 2 Sekunden wieder ein Ton

- **WHEN** Spieler A Spieler B kitzelt und 2 Sekunden oder mehr später erneut kitzelt
- **THEN** wird der Ton erneut gespielt

#### Scenario: Paar ist getrennt gezählt

- **WHEN** Spieler A Spieler B kitzelt und danach Spieler C Spieler B kitzelt, innerhalb von 2 Sekunden
- **THEN** wird für beide Kitzel-Paare ein Ton gespielt
