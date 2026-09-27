# Spec Delta

## ADDED Requirements

### Requirement: Navigator-Ziele sind im Navigator-Modul festgelegt
Titel und Ziele des Navigators MÜSSEN fest im Navigator-Modul festgelegt sein. Die Lobby DARF Titel und Ziele NICHT aus der Konfiguration lesen, und andere Module DÜRFEN KEINE Ziele beisteuern. Werte unter `navigator.*` aus Dateien, Profilen, Env-Variablen oder System-Properties DÜRFEN den Navigator NICHT beeinflussen. Der Navigator MUSS einen Titel „Navigator“ und eine Reihe mit neun Plätzen zeigen: ElytraRace auf Platz 0, Survival auf Platz 4, Slender auf Platz 5 und Creative auf Platz 8. Die übrigen Plätze MÜSSEN mit grauen Glasscheiben gefüllt sein.

#### Scenario: Standardziele
- **WHEN** `features.NAVIGATOR_SLENDER` an ist und ein Spieler den Navigator öffnet
- **THEN** sieht er den Titel „Navigator“, ElytraRace auf Platz 0, Survival auf Platz 4, Slender auf Platz 5, Creative auf Platz 8 und auf den Plätzen 1, 2, 3, 6 und 7 graue Glasscheiben

#### Scenario: Weiterleitungsziele
- **WHEN** ein Spieler im Navigator ElytraRace, Survival, Slender oder Creative anklickt
- **THEN** wird eine Weiterleitung zum Ziel „ElytraRace“, „Survival“, „cygnus“ bzw. „MemberBuild“ angestoßen

#### Scenario: Navigator-Werte in der Konfiguration werden ignoriert
- **WHEN** die `application.yaml` des Betreibers `navigator.title` sowie einen Eintrag `navigator.entries.parkour` mit Platz 2 setzt und die Lobby startet
- **THEN** startet die Lobby ohne Fehler, der Titel bleibt „Navigator“, und auf Platz 2 liegt eine graue Glasscheibe

#### Scenario: Navigator-Wert per Env-Variable wird ignoriert
- **WHEN** die Env-Variable `NAVIGATOR_ENTRIES_SURVIVAL_DESTINATION` auf `Lobby2` steht und ein Spieler Survival anklickt
- **THEN** wird eine Weiterleitung zum Ziel „Survival“ angestoßen

### Requirement: Slender liegt hinter der Feature-Flag NAVIGATOR_SLENDER
Das Ziel Slender MUSS an die Feature-Flag `NAVIGATOR_SLENDER` gebunden sein. Ist die Flag aus, DARF Slender für niemanden erscheinen, und auf Platz 5 liegt eine graue Glasscheibe. Ist sie an, erscheint Slender für alle Spieler. Der Zustand der Flag MUSS bei jedem Öffnen des Navigators ausgewertet werden. Eine Änderung in der Konfiguration MUSS, sobald die Dateiüberwachung sie übernommen hat, beim nächsten Öffnen sichtbar sein, ohne Neustart der Lobby oder des Navigators.

#### Scenario: Slender bei ausgeschalteter Flag
- **WHEN** `features.NAVIGATOR_SLENDER` aus ist oder nirgends gesetzt ist und ein Spieler den Navigator öffnet
- **THEN** liegt auf Platz 5 eine graue Glasscheibe, und die übrigen Ziele sind unverändert

#### Scenario: Slender bei eingeschalteter Flag
- **WHEN** `features.NAVIGATOR_SLENDER` an ist und ein Spieler den Navigator öffnet
- **THEN** erscheint Slender auf Platz 5, und ein Klick darauf leitet zum Ziel „cygnus“ weiter

#### Scenario: Flag wird zur Laufzeit umgeschaltet
- **WHEN** der Betreiber im Betrieb `features.NAVIGATOR_SLENDER: true` in `application.yaml` setzt und die Dateiüberwachung die Änderung übernimmt
- **THEN** zeigt der Navigator Slender beim nächsten Öffnen, ohne dass die Lobby oder der Navigator neu startet

## MODIFIED Requirements

### Requirement: Auswahl eines Ziels leitet weiter
Klickt ein Spieler ein Ziel an, MUSS die Lobby ihn an das Weiterleitungsziel dieses Ziels übergeben und den Navigator schließen. Der Klick DARF NICHT das Symbol ins Spielerinventar verschieben. Ein Klick auf eine graue Glasscheibe DARF NICHTS auslösen.

#### Scenario: Survival wählen
- **WHEN** ein Spieler im Navigator auf Survival klickt
- **THEN** wird eine Weiterleitung zum Ziel „Survival“ angestoßen, und das Symbol bleibt im Navigator

#### Scenario: Weiterleitung ohne Cloud
- **WHEN** die Lobby ohne CloudNet läuft und ein Spieler ein Ziel anklickt
- **THEN** passiert keine Weiterleitung, und es entsteht kein Fehler

#### Scenario: Klick auf leeren Platz
- **WHEN** ein Spieler im Navigator auf Platz 2 klickt
- **THEN** wird keine Weiterleitung angestoßen, der Navigator bleibt offen, und die Glasscheibe bleibt im Navigator

## REMOVED Requirements

### Requirement: Navigator-Ziele kommen aus der Konfiguration
**Reason**: Die Ziele ändern sich selten und nur zusammen mit einem Release. Die Konfigurierbarkeit brachte eigenen Parse- und Validierungscode, ohne im Betrieb gebraucht zu werden. Ersetzt durch „Navigator-Ziele sind im Navigator-Modul festgelegt“.
**Migration**: `navigator.title` und `navigator.entries.*` aus eigener `application.yaml`, Profilen und Env-Variablen/System-Properties entfernen; sie haben keine Wirkung mehr. Ein neues oder geändertes Ziel wird im Navigator-Modul eingetragen.

### Requirement: Module können Navigator-Ziele beisteuern
**Reason**: Kein Modul außer dem Navigator selbst hat je ein Ziel beigesteuert. Die dafür gebaute Registry mit Versionszähler, Konfliktprüfung und Sichtbarkeitslogik war der größte Teil der Komplexität. Weiterleitungsziele sind eine Navigator-Angelegenheit.
**Migration**: Ein Feature, das im Navigator erscheinen soll, trägt sein Ziel im Navigator-Modul ein.

### Requirement: Doppelt belegte Navigator-Plätze werden beim Start erkannt
**Reason**: Ohne Beiträge anderer Module und ohne Konfiguration legt nur noch das Navigator-Modul selbst Plätze fest. Ein Doppelbeleg ist ein Programmierfehler im selben Code und fällt im Unit-Test auf, nicht erst beim Start.
**Migration**: Keine.

### Requirement: Navigator-Ziele können hinter einer Feature-Flag liegen
**Reason**: Der allgemeine Flag-Mechanismus für beliebige, auch fremde Einträge samt Startprüfung unbekannter Flags entfällt mit der Registry. Ersetzt durch „Slender liegt hinter der Feature-Flag NAVIGATOR_SLENDER“ mit unverändertem Verhalten für Spieler und Betreiber.
**Migration**: Keine. `features.NAVIGATOR_SLENDER` wirkt wie bisher.
