# Spec Delta

## ADDED Requirements

### Requirement: Das Ziel Build erscheint nur mit Recht
Der Navigator MUSS das Ziel Build auf Platz 7 zeigen, wenn die Rechteprüfung für `titan.navigator.buildserver` beim Öffnen „erteilt“ ergibt. Ergibt sie „verboten“ oder „nicht gesetzt“, MUSS der Navigator genau das Menü zeigen, das Spieler ohne dieses Ziel sehen: kein Eintrag, keine Lücke, kein Hinweis auf Build. Ein Klick auf Build MUSS das Recht erneut prüfen. Ist es erteilt, MUSS die Lobby den Spieler an das Weiterleitungsziel „Build“ übergeben und den Navigator schließen; sonst DARF sie keine Weiterleitung anstoßen, MUSS den Navigator schließen und DARF dem Spieler keine Chat-Nachricht senden. Welcher Server das Ziel „Build“ bedient, entscheidet die Cloud; die Lobby zeigt keine einzelnen Server. Das Verhalten der übrigen Ziele und der Feature-Flag von Slender MUSS für beide Menüs gleich bleiben. Für das Öffnen, egal wie oft und von wie vielen Spielern, DÜRFEN keine zusätzlichen Event-Listener oder dauerhaft gehaltenen Objekte entstehen.

#### Scenario: Spieler mit Recht sieht Build
- **WHEN** einem Spieler `titan.navigator.buildserver` erteilt ist und er den Navigator öffnet
- **THEN** liegt auf Platz 7 das Ziel Build, und die übrigen Ziele liegen unverändert auf ihren Plätzen

#### Scenario: Spieler mit Recht wählt Build
- **WHEN** einem Spieler `titan.navigator.buildserver` erteilt ist und er im Navigator auf Build klickt
- **THEN** wird eine Weiterleitung zum Ziel „Build“ angestoßen, und der Navigator schließt

#### Scenario: Spieler ohne Recht sieht dasselbe Menü wie bisher
- **WHEN** einem Spieler `titan.navigator.buildserver` nicht gesetzt oder verboten ist und er den Navigator öffnet
- **THEN** liegt auf Platz 7 eine graue Glasscheibe, und das Menü ist in Titel, Zielen und Plätzen identisch mit dem Menü vor dieser Änderung

#### Scenario: Recht wird zwischen Öffnen und Klick entzogen
- **WHEN** ein Spieler mit Recht den Navigator öffnet, ihm danach das Recht `titan.navigator.buildserver` entzogen wird und er auf Build klickt
- **THEN** wird keine Weiterleitung angestoßen, der Navigator schließt, und der Spieler erhält keine Chat-Nachricht

#### Scenario: Ohne Permission-Plattform
- **WHEN** die Lobby ohne Permission-Plattform läuft und ein Spieler den Navigator öffnet
- **THEN** liegt auf Platz 7 eine graue Glasscheibe

#### Scenario: Kein Durchsickern bei wiederholtem Öffnen
- **WHEN** ein Spieler mit Recht und ein Spieler ohne Recht den Navigator abwechselnd 50 Mal öffnen und schließen
- **THEN** sieht jeder bei jedem Öffnen sein eigenes Menü, der Spieler ohne Recht nie Build, und die Anzahl angemeldeter Event-Listener ist unverändert

#### Scenario: Flag-Wechsel wirkt in beiden Menüs
- **WHEN** `features.NAVIGATOR_SLENDER` zur Laufzeit umgeschaltet wird und je ein Spieler mit und ohne Recht den Navigator danach öffnet
- **THEN** zeigen beide Menüs Slender auf Platz 5 entsprechend dem neuen Zustand der Flag

## MODIFIED Requirements

### Requirement: Navigator-Ziele sind im Navigator-Modul festgelegt
Titel und Ziele des Navigators MÜSSEN fest im Navigator-Modul festgelegt sein. Die Lobby DARF Titel und Ziele NICHT aus der Konfiguration lesen, und andere Module DÜRFEN KEINE Ziele beisteuern. Werte unter `navigator.*` aus Dateien, Profilen, Env-Variablen oder System-Properties DÜRFEN den Navigator NICHT beeinflussen. Der Navigator MUSS einen Titel „Navigator“ und eine Reihe mit neun Plätzen zeigen: ElytraRace auf Platz 0, Survival auf Platz 4, Slender auf Platz 5 und Creative auf Platz 8. Spielern mit dem Recht `titan.navigator.buildserver` MUSS er zusätzlich Build auf Platz 7 zeigen (siehe „Das Ziel Build erscheint nur mit Recht“). Die übrigen Plätze MÜSSEN mit grauen Glasscheiben gefüllt sein.

#### Scenario: Standardziele
- **WHEN** `features.NAVIGATOR_SLENDER` an ist und ein Spieler ohne das Recht `titan.navigator.buildserver` den Navigator öffnet
- **THEN** sieht er den Titel „Navigator“, ElytraRace auf Platz 0, Survival auf Platz 4, Slender auf Platz 5, Creative auf Platz 8 und auf den Plätzen 1, 2, 3, 6 und 7 graue Glasscheiben

#### Scenario: Standardziele mit Recht
- **WHEN** `features.NAVIGATOR_SLENDER` an ist und ein Spieler mit dem Recht `titan.navigator.buildserver` den Navigator öffnet
- **THEN** sieht er den Titel „Navigator“, ElytraRace auf Platz 0, Survival auf Platz 4, Slender auf Platz 5, Build auf Platz 7, Creative auf Platz 8 und auf den Plätzen 1, 2, 3 und 6 graue Glasscheiben

#### Scenario: Weiterleitungsziele
- **WHEN** ein Spieler im Navigator ElytraRace, Survival, Slender oder Creative anklickt
- **THEN** wird eine Weiterleitung zum Ziel „ElytraRace“, „Survival“, „cygnus“ bzw. „MemberBuild“ angestoßen

#### Scenario: Navigator-Werte in der Konfiguration werden ignoriert
- **WHEN** die `application.yaml` des Betreibers `navigator.title` sowie einen Eintrag `navigator.entries.parkour` mit Platz 2 setzt und die Lobby startet
- **THEN** startet die Lobby ohne Fehler, der Titel bleibt „Navigator“, und auf Platz 2 liegt eine graue Glasscheibe

#### Scenario: Navigator-Wert per Env-Variable wird ignoriert
- **WHEN** die Env-Variable `NAVIGATOR_ENTRIES_SURVIVAL_DESTINATION` auf `Lobby2` steht und ein Spieler Survival anklickt
- **THEN** wird eine Weiterleitung zum Ziel „Survival“ angestoßen
