# Spec Delta

<!-- Baut auf dem Stand nach dem Archivieren von `standardized-config-profiles` und
     `avaje-config-facade` auf. -->

## MODIFIED Requirements

### Requirement: Navigator-Ziele können hinter einer Feature-Flag liegen
Ein Navigator-Ziel MUSS optional an eine Feature-Flag gebunden werden können. Ist die Flag aus, DARF das Ziel für niemanden im Navigator erscheinen, und der Platz wird wie ein leerer Platz gefüllt. Ist sie an, erscheint das Ziel für alle Spieler. Titel und Einträge des Navigators MÜSSEN bei jedem Öffnen neu aus der Konfiguration gelesen werden. Eine Änderung des Flag-Zustands in der Konfiguration MUSS, sobald die Dateiüberwachung sie übernommen hat, beim nächsten Öffnen des Navigators sichtbar sein, ohne Neustart der Lobby oder des Navigators. Eine unbekannte Flag in einem Navigator-Eintrag MUSS beim Start den Start abbrechen. Eine unbekannte Flag oder ein ungültiger Eintrag, die erst zur Laufzeit auftreten, werden nicht geprüft; der Navigator liest den Eintrag unvalidiert über die Fassade. Das Standardziel Slender MUSS an die Flag `NAVIGATOR_SLENDER` gebunden sein.

#### Scenario: Slender bei ausgeschalteter Flag
- **WHEN** `features.NAVIGATOR_SLENDER` aus ist oder nirgends gesetzt ist und ein Spieler den Navigator öffnet
- **THEN** ist auf Platz 5 kein Slender-Ziel, sondern eine graue Glasscheibe, und die übrigen Ziele sind unverändert

#### Scenario: Slender bei eingeschalteter Flag
- **WHEN** `features.NAVIGATOR_SLENDER` an ist und ein Spieler den Navigator öffnet
- **THEN** erscheint Slender auf Platz 5, und ein Klick darauf leitet zum Ziel „cygnus“ weiter

#### Scenario: Flag wird zur Laufzeit umgeschaltet
- **WHEN** der Betreiber im Betrieb `features.NAVIGATOR_SLENDER: true` in `application.yaml` setzt und die Dateiüberwachung die Änderung übernimmt
- **THEN** zeigt der Navigator Slender beim nächsten Öffnen, ohne dass die Lobby oder der Navigator neu startet

#### Scenario: Unbekannte Flag in der Konfiguration
- **WHEN** ein Navigator-Eintrag in `application.yaml` `feature: GIBT_ES_NICHT` enthält und die Lobby startet
- **THEN** startet die Lobby nicht, und die Fehlermeldung nennt `navigator.entries` und die unbekannte Flag

#### Scenario: Unbekannte Flag nach Dateiänderung
- **WHEN** im Betrieb ein Navigator-Eintrag auf `feature: GIBT_ES_NICHT` geändert wird und die Dateiüberwachung die Änderung übernimmt
- **THEN** wird das zur Laufzeit nicht geprüft
