# Spec Delta

## MODIFIED Requirements

### Requirement: Auswahl eines Ziels leitet weiter
Klickt ein Spieler ein Server-Ziel an, MUSS die Lobby ihn an das Weiterleitungsziel dieses Ziels übergeben und den Navigator schließen. Klickt er auf „Spawn“, MUSS die Lobby ihn zum Spawn-Punkt der Lobby zurückbringen, wie es die Capability `lobby-spawn-return` festlegt, und den Navigator schließen; dabei DARF KEINE Weiterleitung angestoßen werden. Der Klick DARF NICHT das Symbol ins Spielerinventar verschieben. Ein Klick auf eine graue Glasscheibe DARF NICHTS auslösen.

#### Scenario: Survival wählen
- **WHEN** ein Spieler im Navigator auf Survival klickt
- **THEN** wird eine Weiterleitung zum Ziel „Survival“ angestoßen, und das Symbol bleibt im Navigator

#### Scenario: Weiterleitung ohne Cloud
- **WHEN** die Lobby ohne CloudNet läuft und ein Spieler ein Ziel anklickt
- **THEN** passiert keine Weiterleitung, es entsteht kein Fehler, der Spieler erhält im Chat eine Nachricht, die Art (Task oder Server) und Ziel unverändert nennt, und die Lobby loggt dieselbe Information für den Betreiber

#### Scenario: Klick auf leeren Platz
- **WHEN** ein Spieler im Navigator auf Platz 1 klickt
- **THEN** wird keine Weiterleitung angestoßen, der Navigator bleibt offen, und die Glasscheibe bleibt im Navigator

#### Scenario: Spawn wählen
- **WHEN** ein Spieler im Navigator auf Spawn klickt
- **THEN** steht er am Spawn-Punkt der Lobby, der Navigator ist geschlossen, es wird keine Weiterleitung angestoßen, und das Symbol bleibt im Navigator

### Requirement: Navigator-Ziele sind im Navigator-Modul festgelegt
Titel und Ziele des Navigators MÜSSEN fest im Navigator-Modul festgelegt sein. Die Lobby DARF Titel und Ziele NICHT aus der Konfiguration lesen, und andere Module DÜRFEN KEINE Ziele beisteuern. Werte unter `navigator.*` aus Dateien, Profilen, Env-Variablen oder System-Properties DÜRFEN den Navigator NICHT beeinflussen. Der Navigator MUSS einen Titel „Navigator“ und eine Reihe mit neun Plätzen zeigen: ElytraRace auf Platz 0, Spawn mit einem Kompass als Symbol auf Platz 2, Survival auf Platz 4, Slender auf Platz 5 und Creative auf Platz 8. Spielern mit dem Recht `titan.navigator.buildserver` MUSS er zusätzlich Build auf Platz 7 zeigen (siehe „Das Ziel Build erscheint nur mit Recht“). Die übrigen Plätze MÜSSEN mit grauen Glasscheiben gefüllt sein.

#### Scenario: Standardziele
- **WHEN** `features.NAVIGATOR_SLENDER` an ist und ein Spieler ohne das Recht `titan.navigator.buildserver` den Navigator öffnet
- **THEN** sieht er den Titel „Navigator“, ElytraRace auf Platz 0, Spawn auf Platz 2, Survival auf Platz 4, Slender auf Platz 5, Creative auf Platz 8 und auf den Plätzen 1, 3, 6 und 7 graue Glasscheiben

#### Scenario: Standardziele mit Recht
- **WHEN** `features.NAVIGATOR_SLENDER` an ist und ein Spieler mit dem Recht `titan.navigator.buildserver` den Navigator öffnet
- **THEN** sieht er den Titel „Navigator“, ElytraRace auf Platz 0, Spawn auf Platz 2, Survival auf Platz 4, Slender auf Platz 5, Build auf Platz 7, Creative auf Platz 8 und auf den Plätzen 1, 3 und 6 graue Glasscheiben

#### Scenario: Weiterleitungsziele
- **WHEN** ein Spieler im Navigator ElytraRace, Survival, Slender oder Creative anklickt
- **THEN** wird eine Weiterleitung zum Ziel „ElytraRace“, „Survival“, „cygnus“ bzw. „MemberBuild“ angestoßen

#### Scenario: Navigator-Werte in der Konfiguration werden ignoriert
- **WHEN** die `application.yaml` des Betreibers `navigator.title` sowie einen Eintrag `navigator.entries.parkour` mit Platz 3 setzt und die Lobby startet
- **THEN** startet die Lobby ohne Fehler, der Titel bleibt „Navigator“, und auf Platz 3 liegt eine graue Glasscheibe

#### Scenario: Navigator-Wert per Env-Variable wird ignoriert
- **WHEN** die Env-Variable `NAVIGATOR_ENTRIES_SURVIVAL_DESTINATION` auf `Lobby2` steht und ein Spieler Survival anklickt
- **THEN** wird eine Weiterleitung zum Ziel „Survival“ angestoßen
