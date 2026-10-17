# Spec Delta

## REMOVED Requirements

### Requirement: Der Navigator kennt genau eine Feature-Flag

**Reason**: Slender wird nicht mehr über die Flag `NAVIGATOR_SLENDER` gesteuert, sondern über das Recht `titan.navigator.slender`. Der Navigator hat damit keine Feature-Flag mehr. Die Anforderung ist unten als „Der Navigator hat keine Feature-Flag“ neu gefasst; der Verlauf des Slender-Namens bleibt als eigene Anforderung erhalten.

**Migration**: Betreiber vergeben `titan.navigator.slender` an die Gruppen, die Slender sehen sollen, und entfernen `features.NAVIGATOR_SLENDER` aus `application*.yaml` bzw. `FEATURES_NAVIGATOR_SLENDER` aus der Umgebung.

### Requirement: Slender liegt hinter der Feature-Flag NAVIGATOR_SLENDER

**Reason**: Die Sichtbarkeit von Slender wird pro Spieler über das Recht `titan.navigator.slender` bestimmt, wie bei Build.

**Migration**: Siehe „Slender liegt hinter dem Recht titan.navigator.slender“.

## ADDED Requirements

### Requirement: Der Navigator hat keine Feature-Flag

Die mitgelieferten Standardwerte MÜSSEN für den Navigator keine Flag `NAVIGATOR_*` im Abschnitt `features` enthalten. Setzt ein Betreiber `features.NAVIGATOR_SLENDER` (oder `FEATURES_NAVIGATOR_SLENDER`), MUSS die Lobby den Eintrag als unbekannt behandeln: Sie startet ohne Fehler und ohne Warnung, und der Eintrag hat keinen Einfluss auf Slender. Slender erscheint allein über das Recht `titan.navigator.slender`.

#### Scenario: Standardwerte ohne Navigator-Flag

- **WHEN** die Lobby mit den mitgelieferten Standardwerten startet
- **THEN** enthält der Abschnitt `features` keine Flag, die mit `NAVIGATOR_` beginnt

#### Scenario: Alte Flag ohne Wirkung

- **WHEN** `application.yaml` `features.NAVIGATOR_SLENDER: true` setzt, die Lobby startet und ein Spieler ohne das Recht `titan.navigator.slender` den Navigator öffnet
- **THEN** startet die Lobby ohne Fehler und ohne Warnung, und Slender erscheint für diesen Spieler nicht

### Requirement: Slender liegt hinter dem Recht titan.navigator.slender

Das Ziel Slender MUSS an das Recht `titan.navigator.slender` gebunden sein, nach demselben Muster wie Build an `titan.navigator.buildserver`. Ergibt die Rechteprüfung beim Öffnen nicht „erteilt“ (also „verboten“ oder „nicht gesetzt“), DARF Slender für diesen Spieler nicht erscheinen, und auf Platz 5 MUSS eine graue Glasscheibe liegen. Ergibt sie „erteilt“, MUSS Slender auf Platz 5 erscheinen. Ein Klick auf Slender MUSS das Recht erneut prüfen. Ist es erteilt, leitet die Lobby an das Ziel „Slender“ weiter (bzw. öffnet die Dienstauswahl, wenn Slender auf mehreren Diensten läuft); sonst DARF sie keine Weiterleitung anstoßen, MUSS den Navigator schließen und DARF dem Spieler keine Chat-Nachricht senden. Der Zustand des Rechts MUSS bei jedem Öffnen des Navigators ausgewertet werden, ohne Neustart der Lobby.

#### Scenario: Slender ohne Recht

- **WHEN** einem Spieler `titan.navigator.slender` nicht gesetzt oder verboten ist und er den Navigator öffnet
- **THEN** liegt auf Platz 5 eine graue Glasscheibe, und die übrigen Ziele sind unverändert

#### Scenario: Slender mit Recht

- **WHEN** einem Spieler `titan.navigator.slender` erteilt ist und er den Navigator öffnet
- **THEN** erscheint Slender auf Platz 5, und ein Klick darauf leitet zum Ziel „Slender“ weiter und schließt den Navigator

#### Scenario: Recht wird zwischen Öffnen und Klick entzogen

- **WHEN** ein Spieler mit dem Recht den Navigator öffnet, ihm danach `titan.navigator.slender` entzogen wird und er auf Slender klickt
- **THEN** wird keine Weiterleitung angestoßen, der Navigator schließt, und der Spieler erhält keine Chat-Nachricht

#### Scenario: Recht wird zwischen zwei Öffnungen erteilt

- **WHEN** einem Spieler `titan.navigator.slender` nach dem ersten Öffnen erteilt wird und er den Navigator danach erneut öffnet
- **THEN** erscheint Slender auf Platz 5 bei diesem zweiten Öffnen, ohne Neustart der Lobby

### Requirement: Der Slender-Name ist ein Verlauf von Grau nach Rot

Der Anzeigename von Slender MUSS ein Farbverlauf von `#616161` nach `#e80000` sein: Der erste Buchstabe ist `#616161`, der letzte genau `#e80000`.

#### Scenario: Verlauf des Slender-Namens

- **WHEN** ein Spieler mit dem Recht `titan.navigator.slender` den Navigator öffnet
- **THEN** beginnt der Name „Slender“ in `#616161`, und sein letzter Buchstabe hat genau `#e80000`

## MODIFIED Requirements

### Requirement: Navigator-Ziele sind im Navigator-Modul festgelegt

Titel und Ziele des Navigators MÜSSEN fest im Navigator-Modul festgelegt sein. Die Lobby DARF Titel und Ziele NICHT aus der Konfiguration lesen, und andere Module DÜRFEN KEINE Ziele beisteuern. Werte unter `navigator.*` aus Dateien, Profilen, Env-Variablen oder System-Properties DÜRFEN den Navigator NICHT beeinflussen. Der Navigator MUSS einen Titel „Navigator“ und eine Reihe mit neun Plätzen zeigen: ElytraRace auf Platz 0, Spawn mit einem Kompass als Symbol auf Platz 2, Survival auf Platz 4, Slender auf Platz 5 und Creative auf Platz 8. Slender auf Platz 5 erscheint nur Spielern mit dem Recht `titan.navigator.slender` (siehe „Slender liegt hinter dem Recht titan.navigator.slender“). Spielern mit dem Recht `titan.navigator.buildserver` MUSS er zusätzlich Build auf Platz 7 zeigen (siehe „Das Ziel Build erscheint nur mit Recht“). Die übrigen Plätze MÜSSEN mit grauen Glasscheiben gefüllt sein.

#### Scenario: Standardziele

- **WHEN** ein Spieler ohne die Rechte `titan.navigator.slender` und `titan.navigator.buildserver` den Navigator öffnet
- **THEN** sieht er den Titel „Navigator“, ElytraRace auf Platz 0, Spawn auf Platz 2, Survival auf Platz 4, Creative auf Platz 8 und auf den Plätzen 1, 3, 5, 6 und 7 graue Glasscheiben

#### Scenario: Standardziele mit Rechten

- **WHEN** ein Spieler mit den Rechten `titan.navigator.slender` und `titan.navigator.buildserver` den Navigator öffnet
- **THEN** sieht er den Titel „Navigator“, ElytraRace auf Platz 0, Spawn auf Platz 2, Survival auf Platz 4, Slender auf Platz 5, Build auf Platz 7, Creative auf Platz 8 und auf den Plätzen 1, 3 und 6 graue Glasscheiben

#### Scenario: Weiterleitungsziele

- **WHEN** ein Spieler im Navigator ElytraRace, Survival, Slender oder Creative anklickt
- **THEN** wird eine Weiterleitung zum Ziel „ElytraRace“, „Survival“, „Slender“ bzw. „MemberBuild“ angestoßen

#### Scenario: Navigator-Werte in der Konfiguration werden ignoriert

- **WHEN** die `application.yaml` des Betreibers `navigator.title` sowie einen Eintrag `navigator.entries.parkour` mit Platz 3 setzt und die Lobby startet
- **THEN** startet die Lobby ohne Fehler, der Titel bleibt „Navigator“, und auf Platz 3 liegt eine graue Glasscheibe

#### Scenario: Navigator-Wert per Env-Variable wird ignoriert

- **WHEN** die Env-Variable `NAVIGATOR_ENTRIES_SURVIVAL_DESTINATION` auf `Lobby2` steht und ein Spieler Survival anklickt
- **THEN** wird eine Weiterleitung zum Ziel „Survival“ angestoßen

### Requirement: Das Ziel Build erscheint nur mit Recht

Der Navigator MUSS das Ziel Build auf Platz 7 zeigen, wenn die Rechteprüfung für `titan.navigator.buildserver` beim Öffnen „erteilt“ ergibt. Ergibt sie „verboten“ oder „nicht gesetzt“, MUSS der Navigator genau das Menü zeigen, das Spieler ohne dieses Ziel sehen: kein Eintrag, keine Lücke, kein Hinweis auf Build. Ein Klick auf Build MUSS das Recht erneut prüfen. Ist es erteilt, MUSS die Lobby den Spieler an das Weiterleitungsziel „Team-Build“ übergeben und den Navigator schließen; sonst DARF sie keine Weiterleitung anstoßen, MUSS den Navigator schließen und DARF dem Spieler keine Chat-Nachricht senden. Welcher Server das Ziel „Team-Build“ bedient, entscheidet die Cloud; die Lobby zeigt keine einzelnen Server. Das Verhalten der übrigen Ziele MUSS für alle Menüs gleich bleiben, und die Rechte für Build und Slender sind voneinander unabhängig. Für das Öffnen, egal wie oft und von wie vielen Spielern, DÜRFEN keine zusätzlichen Event-Listener oder dauerhaft gehaltenen Objekte entstehen.

#### Scenario: Spieler mit Recht sieht Build

- **WHEN** einem Spieler `titan.navigator.buildserver` erteilt ist und er den Navigator öffnet
- **THEN** liegt auf Platz 7 das Ziel Build, und die übrigen Ziele liegen unverändert auf ihren Plätzen

#### Scenario: Spieler mit Recht wählt Build

- **WHEN** einem Spieler `titan.navigator.buildserver` erteilt ist und er im Navigator auf Build klickt
- **THEN** wird eine Weiterleitung zum Ziel „Team-Build“ angestoßen, und der Navigator schließt

#### Scenario: Spieler ohne Recht sieht dasselbe Menü wie bisher

- **WHEN** einem Spieler `titan.navigator.buildserver` nicht gesetzt oder verboten ist und er den Navigator öffnet
- **THEN** liegt auf Platz 7 eine graue Glasscheibe, und das Menü ist in Titel, Zielen und Plätzen identisch mit dem Menü ohne Build, das ihm sein Slender-Recht zeigt

#### Scenario: Recht wird zwischen Öffnen und Klick entzogen

- **WHEN** ein Spieler mit Recht den Navigator öffnet, ihm danach das Recht `titan.navigator.buildserver` entzogen wird und er auf Build klickt
- **THEN** wird keine Weiterleitung angestoßen, der Navigator schließt, und der Spieler erhält keine Chat-Nachricht

#### Scenario: Ohne Permission-Plattform

- **WHEN** die Lobby ohne Permission-Plattform läuft und ein Spieler den Navigator öffnet
- **THEN** liegt auf Platz 7 eine graue Glasscheibe

#### Scenario: Kein Durchsickern bei wiederholtem Öffnen

- **WHEN** ein Spieler mit Recht und ein Spieler ohne Recht den Navigator abwechselnd 50 Mal öffnen und schließen
- **THEN** sieht jeder bei jedem Öffnen sein eigenes Menü, der Spieler ohne Recht nie Build, und die Anzahl angemeldeter Event-Listener ist unverändert

#### Scenario: Recht-Wechsel wirkt in allen Menüs

- **WHEN** das Recht `titan.navigator.slender` einem Spieler mit und einem Spieler ohne Build-Recht zwischen zwei Öffnungen erteilt oder entzogen wird
- **THEN** zeigt jedes Menü Slender auf Platz 5 entsprechend dem neuen Stand des Rechts, und Build bleibt unverändert
