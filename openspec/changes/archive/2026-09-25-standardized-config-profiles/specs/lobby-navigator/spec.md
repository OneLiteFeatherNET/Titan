# Spec Delta

## MODIFIED Requirements

### Requirement: Navigator-Ziele kommen aus der Konfiguration
Der Navigator MUSS seine Ziele aus seinem Konfigurationsabschnitt lesen. Jedes Ziel hat einen eindeutigen Namen und besteht aus Symbol, angezeigtem Namen, Platz im Navigator und Weiterleitungsziel. Ein neues Ziel MUSS sich durch einen Konfigurationseintrag hinzufügen lassen, ohne Codeänderung. Über seinen Namen MUSS sich ein einzelnes Ziel in einem Profil oder per Override ändern lassen, ohne die übrigen Ziele zu wiederholen.

#### Scenario: Standardziele
- **WHEN** ein Spieler ohne angepasste Konfiguration den Navigator öffnet
- **THEN** sieht er eine Reihe mit ElytraRace auf Platz 0, Survival auf Platz 4, Slender auf Platz 5 und Creative auf Platz 8, die übrigen Plätze sind mit grauen Glasscheiben gefüllt

#### Scenario: Zusätzliches Ziel per Konfiguration
- **WHEN** der Betreiber im Abschnitt `navigator` ein Ziel `parkour` („Parkour“) auf Platz 2 ergänzt und die Lobby neu startet
- **THEN** zeigt der Navigator „Parkour“ auf Platz 2 an

#### Scenario: Profil ändert ein einzelnes Ziel
- **WHEN** `application-dev.yaml` nur für das Ziel `survival` ein anderes Weiterleitungsziel setzt und das Profil `dev` aktiv ist
- **THEN** leitet Survival zum neuen Ziel weiter, und alle anderen Ziele bleiben wie in `application.yaml`

### Requirement: Navigator-Ziele können hinter einer Feature-Flag liegen
Ein Navigator-Ziel MUSS optional an eine Feature-Flag gebunden werden können. Ist die Flag aus, DARF das Ziel für niemanden im Navigator erscheinen, und der Platz wird wie ein leerer Platz gefüllt. Ist sie an, erscheint das Ziel für alle Spieler. Eine Änderung des Flag-Zustands MUSS beim nächsten Öffnen des Navigators sichtbar sein, ohne Neustart. Eine unbekannte Flag in der Konfiguration MUSS den Start abbrechen. Das Standardziel Slender MUSS an die Flag `NAVIGATOR_SLENDER` gebunden sein.

#### Scenario: Slender bei ausgeschalteter Flag
- **WHEN** die Flag `NAVIGATOR_SLENDER` aus ist oder in `flags.properties` fehlt und ein Spieler den Navigator öffnet
- **THEN** ist auf Platz 5 kein Slender-Ziel, sondern eine graue Glasscheibe, und die übrigen Ziele sind unverändert

#### Scenario: Slender bei eingeschalteter Flag
- **WHEN** die Flag `NAVIGATOR_SLENDER` an ist und ein Spieler den Navigator öffnet
- **THEN** erscheint Slender auf Platz 5, und ein Klick darauf leitet zum Ziel „cygnus“ weiter

#### Scenario: Flag wird zur Laufzeit umgeschaltet
- **WHEN** die Flag `NAVIGATOR_SLENDER` während des Betriebs eingeschaltet wird
- **THEN** zeigt der Navigator Slender beim nächsten Öffnen, ohne dass die Lobby neu startet

#### Scenario: Unbekannte Flag in der Konfiguration
- **WHEN** ein Navigator-Eintrag in `application.yaml` `feature: GIBT_ES_NICHT` enthält
- **THEN** startet die Lobby nicht, und die Fehlermeldung nennt `navigator.entries` und die unbekannte Flag
