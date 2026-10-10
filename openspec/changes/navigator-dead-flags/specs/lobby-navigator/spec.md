# Spec Delta

## ADDED Requirements

### Requirement: Der Navigator kennt genau eine Feature-Flag
Die mitgelieferten Standardwerte des Navigators MÜSSEN im Abschnitt `features` genau die Flag `NAVIGATOR_SLENDER` enthalten, standardmäßig ausgeschaltet, und keine Flag, die kein Ziel schaltet. Setzt ein Betreiber eine Flag, die dort nicht steht (z. B. `features.NAVIGATOR_CREATIVE` in einer Datei oder `FEATURES_NAVIGATOR_MANIS` als Env-Variable), MUSS die Lobby sie als unbekannt behandeln: Sie startet ohne Fehler und ohne Warnung, die Flag ist aus, und der Navigator zeigt dieselben Ziele wie ohne den Eintrag. Der Anzeigename von Slender MUSS ein Farbverlauf von `#616161` nach `#e80000` sein.

#### Scenario: Standardwerte listen nur Slender
- **WHEN** die Lobby mit den mitgelieferten Standardwerten startet
- **THEN** kennt der Abschnitt `features` genau `NAVIGATOR_SLENDER`, und diese Flag ist aus

#### Scenario: Entfernte Flag in der Betreiber-Konfiguration
- **WHEN** `application.yaml` `features.NAVIGATOR_CREATIVE: true` und `features.NAVIGATOR_MANIS: true` setzt und die Lobby startet
- **THEN** startet die Lobby ohne Fehler und ohne Warnung, beide Flags gelten als unbekannt und aus, und der Navigator zeigt dieselben Ziele wie ohne diese Einträge

#### Scenario: Entfernte Flag per Env-Variable
- **WHEN** die Env-Variable `FEATURES_NAVIGATOR_SURVIVAL` auf `true` steht und ein Spieler den Navigator öffnet
- **THEN** erscheint Survival unverändert auf Platz 4, und die Flag `NAVIGATOR_SURVIVAL` gilt als unbekannt und aus

#### Scenario: Verlauf des Slender-Namens
- **WHEN** `features.NAVIGATOR_SLENDER` an ist und ein Spieler den Navigator öffnet
- **THEN** beginnt der Name „Slender“ in `#616161`, und sein letzter Buchstabe hat genau `#e80000`
