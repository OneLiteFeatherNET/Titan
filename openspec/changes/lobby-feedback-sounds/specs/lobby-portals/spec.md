# Spec Delta

## ADDED Requirements

### Requirement: Ein Portal-Durchgang spielt einen Ton für den Spieler
_One pass, one sound, heard by the passing player only._

Passiert ein Spieler ein Portal, für das er das Recht hat und das die Lobby weiterleitet, MUSS die Lobby genau einen Ton für diesen Spieler spielen, bevor sie ihn übergibt. Der Ton MUSS nur an den Spieler selbst gehen; andere Spieler in der Nähe hören ihn nicht. Bei `box` MUSS der Ton `entity.enderman.teleport` mit Lautstärke 0,5 und Tonhöhe 1,0 sein. Bei `disc` (Ring) MUSS der Ton `entity.experience_orb.pickup` mit Lautstärke 0,6 und Tonhöhe 1,3 sein; der Teleport-Ton DARF dabei NICHT zusätzlich erklingen.

#### Scenario: Box-Portal

- **WHEN** ein Spieler mit Recht in eine Box läuft, die ihn an einen Task weiterleitet
- **THEN** hört nur er genau einen Teleport-Ton, und die Weiterleitung erfolgt danach

#### Scenario: Ring-Portal

- **WHEN** ein Spieler mit Recht mit der Elytra durch eine Scheibe fliegt, die ihn weiterleitet
- **THEN** hört nur er genau einen Ring-Ton und keinen Teleport-Ton

#### Scenario: Umstehende hören nichts

- **WHEN** ein Spieler ein Portal passiert und ein anderer Spieler im Umkreis steht
- **THEN** erhält der andere Spieler keinen Portal-Ton

### Requirement: Ein abgewiesener Betritt spielt einen Ton
_A refused entry is signalled once, quietly, to the player only._

Betritt ein Spieler den Bereich eines Portals, für das er das Recht nicht hat (`NOT_SET` oder `DENIED`), MUSS die Lobby einmal den Ton `block.note_block.bass` mit Lautstärke 0,8 und Tonhöhe 0,7 nur für diesen Spieler spielen. Der Ton DARF NICHT während der Abklingzeit erklingen, und er DARF NICHT erneut erklingen, solange der Spieler im Bereich bleibt.

#### Scenario: Recht fehlt

- **WHEN** ein Spieler ohne Recht in den Bereich eines Portals mit Recht läuft
- **THEN** hört nur er einmal den Abweisungston, und er wird nicht weitergeleitet

#### Scenario: Abweisung während der Abklingzeit

- **WHEN** ein Spieler innerhalb von 3 Sekunden nach einer Weiterleitung in den Bereich eines Portals mit Recht läuft, für das er das Recht nicht hat
- **THEN** erklingt kein Abweisungston

## MODIFIED Requirements

### Requirement: Ein Recht am Portal schützt die Weiterleitung

Hat ein Portal ein Recht, MUSS die Lobby nur Spieler weiterleiten, für die die Rechteprüfung `ALLOWED` ergibt. Bei `NOT_SET` und `DENIED` DARF keine Weiterleitung passieren und KEINE Chat-Nachricht erscheinen. Der Spieler hört dann nur den Abweisungston aus „Ein abgewiesener Betritt spielt einen Ton“. Ein Portal ohne Recht steht allen offen.

#### Scenario: Recht fehlt

- **WHEN** ein Spieler ohne das Recht eines Portals dessen Bereich betritt
- **THEN** wird er nicht weitergeleitet und erhält keine Chat-Nachricht

#### Scenario: Recht vorhanden

- **WHEN** ein Spieler mit dem Recht eines Portals dessen Bereich betritt
- **THEN** wird er an den Task des Portals weitergeleitet

#### Scenario: Abgewiesener Spieler startet keine Abklingzeit

- **WHEN** ein Spieler ohne Recht ein Portal betritt und danach mit Recht ein anderes Portal
- **THEN** wird er am zweiten Portal weitergeleitet
