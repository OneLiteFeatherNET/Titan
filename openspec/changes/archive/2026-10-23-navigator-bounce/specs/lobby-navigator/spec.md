# Spec Delta

## RENAMED Requirements

- FROM: `### Requirement: Menü-Varianten bleiben acht geteilte Inventare`
- TO: `### Requirement: Menü-Varianten bleiben sechzehn geteilte Inventare`

## MODIFIED Requirements

### Requirement: Navigator-Ziele sind im Navigator-Modul festgelegt

Titel und Ziele des Navigators MÜSSEN fest im Navigator-Modul festgelegt sein. Die Lobby DARF Titel und Ziele NICHT aus der Konfiguration lesen, und andere Module DÜRFEN KEINE Ziele beisteuern. Werte unter `navigator.*` aus Dateien, Profilen, Env-Variablen oder System-Properties DÜRFEN den Navigator NICHT beeinflussen. Der Navigator MUSS einen Titel „Navigator“ im Verlauf zeigen, der in der Sprache des Spielers übersetzt ist, und eine Anzeige mit vier Reihen zu je neun Plätzen. ElytraRace MUSS auf Platz 11 liegen, Survival auf Platz 20, Creative auf Platz 22, Spawn mit einem Kompass auf Platz 31. Slender auf Platz 15 erscheint nur Spielern mit dem Recht `titan.navigator.slender` (siehe „Slender liegt hinter dem Recht titan.navigator.slender“). Bounce auf Platz 13 erscheint nur Spielern mit dem Recht `titan.navigator.bounce` (siehe „Bounce liegt hinter dem Recht titan.navigator.bounce“). Spielern mit dem Recht `titan.navigator.buildserver` MUSS er zusätzlich Build auf Platz 24 zeigen (siehe „Das Ziel Build erscheint nur mit Recht“). Die übrigen Plätze MÜSSEN mit grauen Glasscheiben gefüllt sein, die Rahmenplätze mit der Glasscheibe aus „Der Navigator hat einen Rahmen aus Glasscheiben“.

#### Scenario: Standardziele

- **WHEN** ein Spieler ohne das Recht `titan.navigator.buildserver`, ohne das Recht `titan.navigator.slender` und ohne das Recht `titan.navigator.bounce` den Navigator öffnet
- **THEN** sieht er den Titel „Navigator“, ElytraRace auf Platz 11, Survival auf Platz 20, Creative auf Platz 22 und Spawn auf Platz 31
- **AND** die Plätze 13, 15 und 24 sind graue Glasscheiben

#### Scenario: Standardziele mit Rechten

- **WHEN** ein Spieler mit den Rechten `titan.navigator.slender`, `titan.navigator.bounce` und `titan.navigator.buildserver` den Navigator öffnet
- **THEN** sieht er ElytraRace auf Platz 11, Bounce auf Platz 13, Slender auf Platz 15, Survival auf Platz 20, Creative auf Platz 22, Build auf Platz 24 und Spawn auf Platz 31

#### Scenario: Weiterleitungsziele

- **WHEN** ein Spieler im Navigator Survival oder Creative anklickt
- **THEN** wird eine Weiterleitung zum Ziel „Survival“ bzw. „MemberBuild“ angestoßen

#### Scenario: Navigator-Werte in der Konfiguration werden ignoriert

- **WHEN** die `application.yaml` des Betreibers `navigator.title` sowie einen Eintrag `navigator.entries.parkour` mit Platz 3 setzt und die Lobby startet
- **THEN** startet die Lobby ohne Fehler, der Titel bleibt „Navigator“, und auf Platz 3 liegt eine Glasscheibe des Rahmens

#### Scenario: Navigator-Wert per Env-Variable wird ignoriert

- **WHEN** die Env-Variable `NAVIGATOR_ENTRIES_SURVIVAL_DESTINATION` auf `Lobby2` steht und ein Spieler Survival anklickt
- **THEN** wird eine Weiterleitung zum Ziel „Survival“ angestoßen

### Requirement: Das Ziel Build erscheint nur mit Recht

Der Navigator MUSS das Ziel Build auf Platz 24 zeigen, wenn die Rechteprüfung für `titan.navigator.buildserver` beim Öffnen „erteilt“ ergibt. Ergibt sie „verboten“ oder „nicht gesetzt“, MUSS der Navigator genau das Menü zeigen, das Spieler ohne dieses Ziel sehen: kein Eintrag, keine Lücke, kein Hinweis auf Build. Ein Klick auf Build MUSS das Recht erneut prüfen. Ist es erteilt, MUSS die Lobby den Spieler an das Weiterleitungsziel „Team-Build“ übergeben und den Navigator schließen; sonst DARF sie keine Weiterleitung anstoßen, MUSS den Navigator schließen und DARF dem Spieler keine Chat-Nachricht senden. Welcher Server das Ziel „Team-Build“ bedient, entscheidet die Cloud; die Lobby zeigt keine einzelnen Server. Das Verhalten der übrigen Ziele MUSS für alle Menüs gleich bleiben, und die Rechte für Build, Slender und Bounce sind voneinander unabhängig. Für das Öffnen, egal wie oft und von wie vielen Spielern, DÜRFEN keine zusätzlichen Event-Listener oder dauerhaft gehaltenen Objekte entstehen.

#### Scenario: Spieler mit Recht sieht Build

- **WHEN** einem Spieler `titan.navigator.buildserver` erteilt ist und er den Navigator öffnet
- **THEN** liegt auf Platz 24 das Ziel Build, und die übrigen Ziele liegen unverändert auf ihren Plätzen

#### Scenario: Spieler mit Recht wählt Build

- **WHEN** einem Spieler `titan.navigator.buildserver` erteilt ist und er im Navigator auf Build klickt
- **THEN** wird eine Weiterleitung zum Ziel „Team-Build“ angestoßen, und der Navigator schließt

#### Scenario: Spieler ohne Recht sieht dasselbe Menü wie bisher

- **WHEN** einem Spieler `titan.navigator.buildserver` nicht gesetzt oder verboten ist und er den Navigator öffnet
- **THEN** liegt auf Platz 24 eine graue Glasscheibe, und das Menü ist in Titel, Zielen und Plätzen identisch mit dem Menü ohne Build, das ihm sein Slender-Recht zeigt

#### Scenario: Recht wird zwischen Öffnen und Klick entzogen

- **WHEN** ein Spieler mit Recht den Navigator öffnet, ihm danach das Recht `titan.navigator.buildserver` entzogen wird und er auf Build klickt
- **THEN** wird keine Weiterleitung angestoßen, der Navigator schließt, und der Spieler erhält keine Chat-Nachricht

#### Scenario: Ohne Permission-Plattform

- **WHEN** die Lobby ohne Permission-Plattform läuft und ein Spieler den Navigator öffnet
- **THEN** liegt auf Platz 24 eine graue Glasscheibe

#### Scenario: Kein Durchsickern bei wiederholtem Öffnen

- **WHEN** ein Spieler mit Recht und ein Spieler ohne Recht den Navigator abwechselnd 50 Mal öffnen und schließen
- **THEN** sieht jeder bei jedem Öffnen sein eigenes Menü, der Spieler ohne Recht nie Build, und die Anzahl angemeldeter Event-Listener ist unverändert

#### Scenario: Recht-Wechsel wirkt in allen Menüs

- **WHEN** das Recht `titan.navigator.slender` einem Spieler mit und einem Spieler ohne Build-Recht zwischen zwei Öffnungen erteilt oder entzogen wird
- **THEN** zeigt jedes Menü Slender auf Platz 15 entsprechend dem neuen Stand des Rechts, und Build bleibt unverändert

### Requirement: Modus-Einträge erklären die Klicks

Jeder Modus-Eintrag des Navigators MUSS seine Klickaktionen als Lore-Zeilen zeigen, in dieser Reihenfolge: eine Trennlinie in der Verlaufsfarbe des Modus, die Spielerzeile (sofern die Spielerzahl bekannt ist), eine Leerzeile und dann je Klick eine eigene Zeile. Ein Minispiel (ElytraRace, Slender, Bounce) zeigt die Zeilen „▸ Linksklick  Schnell beitreten“ und „▸ Rechtsklick  Server wählen“, auch wenn nur ein Dienst oder kein Dienst läuft. Ein Server (Survival, Creative, Build) zeigt die Zeile „▸ Linksklick  Verbinden“. Die Portal-Zeile „▸ Shift-Klick  Zum Portal laufen“ steht nur, wenn für das Ziel ein Portal in der Lobby existiert, und zuletzt. Der Rechtsklick auf einen Server hat keine eigene Zeile. Der Spawn-Eintrag DARF KEINE dieser Zeilen zeigen.

Die Spielerzahl folgt dem Snapshot der letzten abgeschlossenen Aktualisierung, wie bisher. Die Portal-Zeile folgt dagegen dem Stand beim Öffnen, und der Klick selbst liest die Dienste und das Portal beim Klick neu.

#### Scenario: Minispiel mit Portal

- **WHEN** ein Spieler den Navigator öffnet und für ElytraRace ein Portal in der Lobby existiert
- **THEN** zeigt der Eintrag ElytraRace die Zeilen „▸ Linksklick  Schnell beitreten“, „▸ Rechtsklick  Server wählen“ und zuletzt „▸ Shift-Klick  Zum Portal laufen“ untereinander

#### Scenario: Minispiel auf einem Server

- **WHEN** ein Spieler den Navigator öffnet und ElytraRace nur auf einem Server läuft
- **THEN** zeigt der Eintrag ElytraRace dieselben zwei Zeilen „▸ Linksklick  Schnell beitreten“ und „▸ Rechtsklick  Server wählen“, weil der Rechtsklick die Serverauswahl auch bei nur einem Server öffnet

#### Scenario: Minispiel ohne laufenden Server

- **WHEN** ein Spieler den Navigator öffnet und ElytraRace nirgends läuft
- **THEN** zeigt der Eintrag ElytraRace dieselben zwei Klickzeilen und keine Spielerzeile

#### Scenario: Bounce ohne laufenden Server

- **WHEN** ein Spieler mit dem Recht `titan.navigator.bounce` den Navigator öffnet und Bounce nirgends läuft
- **THEN** zeigt der Eintrag Bounce die Zeilen „▸ Linksklick  Schnell beitreten“ und „▸ Rechtsklick  Server wählen“ und keine Spielerzeile

#### Scenario: Minispiel ohne Portal

- **WHEN** ein Spieler den Navigator öffnet und für Slender kein Portal existiert
- **THEN** zeigt der Eintrag Slender die Zeilen „▸ Linksklick  Schnell beitreten“ und „▸ Rechtsklick  Server wählen“ und keine Shift-Zeile

#### Scenario: Server ohne Portal

- **WHEN** ein Spieler den Navigator öffnet und für Survival kein Portal existiert
- **THEN** zeigt der Eintrag Survival nur die Zeile „▸ Linksklick  Verbinden“

#### Scenario: Server mit Portal

- **WHEN** ein Spieler den Navigator öffnet und für Survival ein Portal existiert
- **THEN** zeigt der Eintrag Survival die Zeilen „▸ Linksklick  Verbinden“ und „▸ Shift-Klick  Zum Portal laufen“ untereinander

#### Scenario: Spielerzahl folgt dem Snapshot der letzten Aktualisierung

- **WHEN** ElytraRace nach dem letzten Aktualisierungszyklus des Navigators auf einen zweiten Server wechselt und ein Spieler den Navigator vor dem nächsten Zyklus öffnet
- **THEN** zeigt der Eintrag ElytraRace die Spielerzahl aus dem Snapshot der letzten Aktualisierung
- **AND** ein Linksklick auf ElytraRace liest die Dienste neu und tritt der besten bereiten Runde bei, die dann läuft

#### Scenario: Portal seit dem letzten Zyklus hinzugekommen

- **WHEN** ein Portal für Survival nach dem letzten Aktualisierungszyklus des Navigators angelegt wird und ein Spieler den Navigator danach öffnet, bevor der nächste Zyklus läuft
- **THEN** zeigt der Eintrag Survival sofort die Zeile „▸ Shift-Klick  Zum Portal laufen“

#### Scenario: Spawn ohne Hinweis

- **WHEN** ein Spieler den Navigator öffnet
- **THEN** hat der Spawn-Eintrag keine Lore-Zeile mit Klickhinweisen

#### Scenario: Portal wird entfernt

- **WHEN** das Portal eines Ziels entfernt wird und danach ein Spieler den Navigator öffnet oder der nächste Aktualisierungszyklus des Navigators läuft
- **THEN** verschwindet die Shift-Zeile aus dem Eintrag, und die übrigen Klickzeilen bleiben

### Requirement: Menü-Varianten bleiben sechzehn geteilte Inventare

Der Navigator MUSS je Kombination aus Rechten und Sprache genau ein geteiltes Inventar haben, beim Start angelegt, mit dem Titel in dieser Sprache. Die Rechte sind `titan.navigator.slender`, `titan.navigator.buildserver` und `titan.navigator.bounce`; jede Teilmenge dieser drei Rechte ergibt ein Menü, also acht je Sprache und sechzehn bei zwei Sprachen. Die Serverauswahl MUSS je Ziel und Sprache ein geteiltes Inventar haben. Das Öffnen DARF KEIN neues Inventar und keinen neuen Listener erzeugen.

#### Scenario: Sechzehn Inventare beim Start

- **WHEN** das Navigator-Modul startet
- **THEN** existieren genau sechzehn Navigator-Inventare, acht je Sprache, und jedes trägt die Ziele, die seiner Rechte-Kombination entsprechen

#### Scenario: Titel je Sprache

- **WHEN** ein Spieler mit deutschem Client den Navigator öffnet
- **THEN** zeigt das Inventar den Titel „Navigator“ im Verlauf und die deutschen Zeilen, und ein Spieler mit englischem Client sieht das zweite Inventar mit englischem Titel und englischen Zeilen

#### Scenario: Häufiges Öffnen der Serverauswahl

- **WHEN** ein Spieler die Serverauswahl 20 Mal öffnet und wieder schließt
- **THEN** ist die Anzahl angemeldeter Event-Listener unverändert

#### Scenario: Stoppen entfernt jede geöffnete Serverauswahl

- **WHEN** das Navigator-Modul gestoppt wird, nachdem Serverauswahlen auf Deutsch und Englisch geöffnet wurden
- **THEN** hat keine dieser Serverauswahlen mehr einen Klick-Listener

## ADDED Requirements

### Requirement: Bounce liegt hinter dem Recht titan.navigator.bounce

Das Ziel Bounce MUSS an das Recht `titan.navigator.bounce` gebunden sein, wie Slender an `titan.navigator.slender`. Ergibt die Rechteprüfung beim Öffnen nicht „erteilt“, DARF Bounce nicht erscheinen, und auf Platz 13 MUSS eine graue Glasscheibe liegen. Ein Klick auf Bounce MUSS das Recht erneut prüfen; ist es nicht erteilt, DARF keine Weiterleitung angestoßen werden, und der Navigator MUSS schließen, ohne dem Spieler eine Chat-Nachricht zu senden.

#### Scenario: Bounce ohne Recht

- **WHEN** einem Spieler `titan.navigator.bounce` nicht gesetzt oder verboten ist und er den Navigator öffnet
- **THEN** liegt auf Platz 13 eine graue Glasscheibe, und die übrigen Ziele sind unverändert

#### Scenario: Bounce mit Recht

- **WHEN** einem Spieler `titan.navigator.bounce` erteilt ist und er den Navigator öffnet
- **THEN** erscheint Bounce auf Platz 13, und ein Linksklick darauf tritt der besten bereiten Runde bei

#### Scenario: Recht wird zwischen Öffnen und Klick entzogen

- **WHEN** ein Spieler mit dem Recht den Navigator öffnet, ihm danach `titan.navigator.bounce` entzogen wird und er auf Bounce klickt
- **THEN** wird keine Weiterleitung angestoßen, der Navigator schließt, und der Spieler erhält keine Chat-Nachricht

#### Scenario: Recht wird zwischen zwei Öffnungen erteilt

- **WHEN** einem Spieler `titan.navigator.bounce` nach dem ersten Öffnen erteilt wird und er den Navigator danach erneut öffnet
- **THEN** erscheint Bounce auf Platz 13 bei diesem zweiten Öffnen, ohne Neustart der Lobby

#### Scenario: Ohne Permission-Plattform

- **WHEN** die Lobby ohne Permission-Plattform läuft und ein Spieler den Navigator öffnet
- **THEN** liegt auf Platz 13 eine graue Glasscheibe

### Requirement: Der Bounce-Name ist ein Verlauf von Grün nach Türkis

Der Anzeigename von Bounce MUSS ein Farbverlauf von `#00FA9A` nach `#40E0D0` sein: Der erste Buchstabe ist `#00FA9A`, der letzte genau `#40E0D0`.

#### Scenario: Verlauf des Bounce-Namens

- **WHEN** ein Spieler mit dem Recht `titan.navigator.bounce` den Navigator öffnet
- **THEN** beginnt der Name „Bounce“ in `#00FA9A`, und sein letzter Buchstabe hat genau `#40E0D0`
