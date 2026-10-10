# Spec Delta

## ADDED Requirements

### Requirement: Portal-Transfers sind sichtbar
Die Lobby MUSS für jedes Portal, das einen Spieler ausliefert, einen kurzen Span `portal.transfer` mit Portal-ID, Ziel-Task, Spieler-UUID und Ergebnis erzeugen. Die Auslieferung über `Deliver` MUSS einen Kind-Span `deliver.send_player` mit Zieltyp, Ziel und Ergebnis tragen. Die Portalprüfung bei `PlayerMoveEvent` DARF KEINEN Span erzeugen.

#### Scenario: Erfolgreicher Transfer
- **WHEN** ein Spieler ein Portal betritt und die Auslieferung gelingt
- **THEN** gibt es einen Span `portal.transfer` mit `portal.id`, `portal.task`, `portal.result=delivered` und darunter `deliver.send_player` mit `titan.deliver.result=ok`

#### Scenario: Auslieferung scheitert
- **WHEN** `Deliver.sendPlayer` eine Ausnahme wirft
- **THEN** tragen `deliver.send_player` und `portal.transfer` die Ausnahme und den Fehlerstatus, `portal.transfers{result=error}` steigt, und die Ausnahme geht weiter

#### Scenario: Keine Spans bei Bewegung ohne Portal
- **WHEN** ein Spieler sich bewegt, ohne ein Portal zu berühren
- **THEN** entsteht kein Span

### Requirement: Portal-Zähler
Die Lobby MUSS `portal.transfers{result}` bei jedem Transfer und `portal.denied{portal.id}` bei jedem verweigerten Betreten erhöhen. Der Zähler DARF KEIN `user.id` tragen.

#### Scenario: Rechte verweigert
- **WHEN** ein Spieler ein Portal betritt, für das er kein Recht hat
- **THEN** steigt `portal.denied{portal.id}` um eins und es entsteht kein Span `portal.transfer`

### Requirement: Spielerzahl-Abfragen sind sichtbar
Die Lobby MUSS je Label-Aktualisierungszyklus einen Span `portal.labels.refresh` mit der Zahl der Labels und der Zahl fehlgeschlagener Abfragen erzeugen, und `portal.player_count.lookups{result}` je Abfrage erhöhen.

#### Scenario: Eine Abfrage scheitert
- **WHEN** die Spielerzahl-Abfrage für ein Label eine Ausnahme wirft
- **THEN** zeigt der Span `portal.labels.refresh` eine fehlgeschlagene Abfrage, `portal.player_count.lookups{result=error}` steigt, und die übrigen Labels werden weiter aktualisiert
