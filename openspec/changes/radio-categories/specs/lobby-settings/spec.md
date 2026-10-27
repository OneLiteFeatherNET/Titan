# Spec Delta

## ADDED Requirements

### Requirement: Spieleroptionen können einen Stufenwert speichern
_Besides on/off switches a column may declare an option with a fixed list of whole-number levels; it is stored like a switch and is not shown in the settings menu._

Die Lobby MUSS neben Schaltern eine Spieleroption mit einer festen Liste ganzer Stufen anbieten können. Der Standard MUSS in der Liste stehen. Der gespeicherte Wert MUSS in Otis als JSON-Zahl liegen, unter demselben Schlüsselformat wie Schalter. Das Laden, das verzögerte Schreiben, die lokale Änderung vor dem Laden und das Verhalten ohne Otis MÜSSEN denselben Regeln folgen wie bei Schaltern. Stufenwerte DÜRFEN NICHT im Einstellungsmenü erscheinen. Eine geladene Zahl außerhalb der Liste MUSS verworfen werden: es gilt der Standard, und die Lobby protokolliert eine Warnung mit dem Schlüssel, nicht mit dem Wert.

#### Scenario: Stufe speichern

- **WHEN** ein Spieler eine Stufenoption auf 75 setzt
- **THEN** speichert die Lobby den Wert `75` unter dem Schlüssel der Option in Otis, und der Wert gilt sofort in der Lobby

#### Scenario: Unbekannte Zahl beim Laden

- **WHEN** Otis für den Schlüssel der Option den Wert `60` liefert, der nicht in der Liste steht
- **THEN** gilt der Standard, und die Lobby protokolliert eine Warnung mit dem Schlüssel

#### Scenario: Keine Stufe im Menü

- **WHEN** ein Spieler das Einstellungsmenü öffnet
- **THEN** gibt es darin keinen Eintrag für eine Stufenoption
