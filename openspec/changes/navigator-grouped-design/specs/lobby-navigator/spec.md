# Spec Delta

Dieses Delta setzt auf dem Stand nach `navigator-staging-on-demand` auf (Tamias, Staging-Hub, Untermenü „Staging“). Die Anforderungen unten ersetzen die gleichnamigen Anforderungen vollständig.

## MODIFIED Requirements

### Requirement: Navigator-Ziele sind im Navigator-Modul festgelegt

Titel und Ziele des Navigators MÜSSEN fest im Navigator-Modul festgelegt sein. Die Lobby DARF Titel und Ziele NICHT aus der Konfiguration lesen, und andere Module DÜRFEN KEINE Ziele beisteuern. Werte unter `navigator.*` aus Dateien, Profilen, Env-Variablen oder System-Properties DÜRFEN den Titel, die Ziele und die Plätze NICHT beeinflussen; ausgenommen sind die Zeitwerte `navigator.refreshSeconds` und `navigator.stagingIdleSeconds`. Der Navigator MUSS den Titel „✦ Navigator ✦“ als Farbverlauf im Verlauf zeigen, in der Sprache des Spielers übersetzt, und eine Anzeige mit vier Reihen zu je neun Plätzen. Die Ziele des Hauptmenüs MÜSSEN nach Gruppe platziert werden: die Minispiele ElytraRace, Bounce, Tamias und Slender in Reihe 1, die Server Survival, Creative und Build in Reihe 2 (siehe „Minispiele und Server stehen zentriert in ihrer Reihe“). Spawn MUSS auf Platz 31 liegen. Der Staging-Hub MUSS auf Platz 35 liegen, der untere rechte Eckplatz, und nur Spielern mit einem Staging-Recht erscheinen (siehe „Staging-Einträge liegen im Untermenü „Staging““). Ein Ziel, dessen Recht dem Spieler fehlt, DARF NICHT erscheinen. Die Plätze ohne Ziel MÜSSEN leer sein, außer den Rahmenplätzen, die die dunkle Glasscheibe aus „Der Navigator hat einen dunklen Rahmen“ zeigen.

#### Scenario: Standardziele

- **WHEN** ein Spieler ohne die Rechte `titan.navigator.buildserver`, `titan.navigator.slender`, `titan.navigator.bounce`, `titan.navigator.tamias` und ohne ein Staging-Recht den Navigator öffnet
- **THEN** sieht er den Titel „✦ Navigator ✦“, ElytraRace als einziges Minispiel auf Platz 13, Survival auf Platz 21, Creative auf Platz 22 und Spawn auf Platz 31
- **AND** Platz 35 ist eine Rahmenscheibe

#### Scenario: Standardziele mit Rechten

- **WHEN** ein Spieler mit den Rechten `titan.navigator.slender`, `titan.navigator.bounce`, `titan.navigator.tamias`, `titan.navigator.buildserver` und `bounce.staging.join` den Navigator öffnet
- **THEN** liegen ElytraRace, Bounce, Tamias und Slender auf den Plätzen 11, 12, 13 und 14 in dieser Reihenfolge
- **AND** Survival, Creative und Build liegen auf den Plätzen 21, 22 und 23, Spawn auf Platz 31 und der Staging-Hub auf Platz 35

#### Scenario: Weiterleitungsziele

- **WHEN** ein Spieler im Navigator Survival oder Creative anklickt
- **THEN** wird eine Weiterleitung zum Ziel „Survival“ bzw. „MemberBuild“ angestoßen

#### Scenario: Navigator-Werte in der Konfiguration werden ignoriert

- **WHEN** die `application.yaml` des Betreibers `navigator.title` sowie einen Eintrag `navigator.entries.parkour` mit Platz 3 setzt und die Lobby startet
- **THEN** startet die Lobby ohne Fehler, der Titel bleibt „✦ Navigator ✦“, und auf Platz 3 liegt eine Rahmenscheibe

#### Scenario: Navigator-Wert per Env-Variable wird ignoriert

- **WHEN** die Env-Variable `NAVIGATOR_ENTRIES_SURVIVAL_DESTINATION` auf `Lobby2` steht und ein Spieler Survival anklickt
- **THEN** wird eine Weiterleitung zum Ziel „Survival“ angestoßen

### Requirement: Modus-Einträge erklären die Klicks

Jeder Modus-Eintrag des Hauptmenüs MUSS in dieser Reihenfolge zeigen: eine Statuszeile mit der Spielerzahl („● 12 Spieler“, bzw. „● 12 players“ auf Englisch), eine Beschreibungszeile, eine Leerzeile und dann je Klick eine eigene Zeile. Der Punkt der Statuszeile MUSS grün sein, wenn mindestens ein Spieler auf dem Ziel ist, und grau bei null Spielern. Die Beschreibungszeile MUSS je Ziel in beiden Sprachen vorhanden sein und DARF NICHT leer sein. Eine Trennlinie in der Verlaufsfarbe DARF NICHT erscheinen. Ein Minispiel (ElytraRace, Slender, Bounce, Tamias) zeigt die Klickzeilen „▸ Linksklick  Schnell beitreten“ und „▸ Rechtsklick  Server wählen“, auch wenn nur ein Dienst oder kein Dienst läuft. Ein Server (Survival, Creative, Build sowie Slender (Staging)) zeigt die Zeile „▸ Linksklick  Verbinden“. Die Portal-Zeile „▸ Shift-Klick  Zum Portal laufen“ steht nur, wenn für das Ziel ein Portal in der Lobby existiert, und zuletzt. Der Rechtsklick auf einen Server hat keine eigene Zeile. Der Spawn-Eintrag DARF KEINE dieser Zeilen zeigen.

Die Spielerzahl folgt dem Snapshot der letzten abgeschlossenen Aktualisierung. Die Portal-Zeile folgt dagegen dem Stand beim Öffnen, und der Klick selbst liest die Dienste und das Portal beim Klick neu.

#### Scenario: Minispiel mit Portal

- **WHEN** ein Spieler den Navigator öffnet, drei Spieler auf ElytraRace sind und für ElytraRace ein Portal in der Lobby existiert
- **THEN** zeigt der Eintrag ElytraRace die grüne Zeile „● 3 Spieler“, die Beschreibungszeile, eine Leerzeile und danach „▸ Linksklick  Schnell beitreten“, „▸ Rechtsklick  Server wählen“ und zuletzt „▸ Shift-Klick  Zum Portal laufen“

#### Scenario: Minispiel auf einem Server

- **WHEN** ein Spieler den Navigator öffnet und ElytraRace nur auf einem Server läuft
- **THEN** zeigt der Eintrag ElytraRace dieselben zwei Zeilen „▸ Linksklick  Schnell beitreten“ und „▸ Rechtsklick  Server wählen“, weil der Rechtsklick die Serverauswahl auch bei nur einem Server öffnet

#### Scenario: Minispiel ohne laufenden Server

- **WHEN** ein Spieler den Navigator öffnet und ElytraRace nirgends läuft
- **THEN** zeigt der Eintrag ElytraRace die graue Zeile „● 0 Spieler“ und dieselben zwei Klickzeilen

#### Scenario: Bounce ohne laufenden Server

- **WHEN** ein Spieler mit dem Recht `titan.navigator.bounce` den Navigator öffnet und Bounce nirgends läuft
- **THEN** zeigt der Eintrag Bounce die graue Zeile „● 0 Spieler“ und die Zeilen „▸ Linksklick  Schnell beitreten“ und „▸ Rechtsklick  Server wählen“

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
- **THEN** hat der Spawn-Eintrag keine Lore-Zeile mit Klickhinweisen und keine Statuszeile

#### Scenario: Portal wird entfernt

- **WHEN** das Portal eines Ziels entfernt wird und danach ein Spieler den Navigator öffnet oder der nächste Aktualisierungszyklus des Navigators läuft
- **THEN** verschwindet die Shift-Zeile aus dem Eintrag, und die übrigen Klickzeilen bleiben

### Requirement: Das Ziel Build erscheint nur mit Recht

Der Navigator MUSS das Ziel Build in Reihe 2 zeigen, wenn die Rechteprüfung für `titan.navigator.buildserver` beim Öffnen „erteilt“ ergibt (Platzierung nach „Minispiele und Server stehen zentriert in ihrer Reihe“). Ergibt sie „verboten“ oder „nicht gesetzt“, MUSS der Navigator genau das Menü zeigen, das Spieler ohne dieses Ziel sehen: kein Eintrag, keine Lücke, kein Hinweis auf Build. Ein Klick auf Build MUSS das Recht erneut prüfen. Ist es erteilt, MUSS die Lobby den Spieler an das Weiterleitungsziel „Team-Build“ übergeben und den Navigator schließen; sonst DARF sie keine Weiterleitung anstoßen, MUSS den Navigator schließen und DARF dem Spieler keine Chat-Nachricht senden. Welcher Server das Ziel „Team-Build“ bedient, entscheidet die Cloud; die Lobby zeigt keine einzelnen Server. Das Verhalten der übrigen Ziele MUSS für alle Menüs gleich bleiben, und die Rechte für Build, Slender, Slender (Staging), Bounce und Tamias sind voneinander unabhängig. Für das Öffnen, egal wie oft und von wie vielen Spielern, DÜRFEN keine zusätzlichen Event-Listener oder dauerhaft gehaltenen Objekte entstehen.

#### Scenario: Spieler mit Recht sieht Build

- **WHEN** einem Spieler `titan.navigator.buildserver` erteilt ist und er den Navigator öffnet
- **THEN** liegt Build in Reihe 2 und die übrigen Server der Reihe sind zentriert neu verteilt

#### Scenario: Spieler mit Recht wählt Build

- **WHEN** einem Spieler `titan.navigator.buildserver` erteilt ist und er im Navigator auf Build klickt
- **THEN** wird eine Weiterleitung zum Ziel „Team-Build“ angestoßen, und der Navigator schließt

#### Scenario: Spieler ohne Recht sieht dasselbe Menü wie bisher

- **WHEN** einem Spieler `titan.navigator.buildserver` nicht gesetzt oder verboten ist und er den Navigator öffnet
- **THEN** erscheint Build nicht, und das Menü ist in Titel, Zielen und Plätzen identisch mit dem Menü ohne Build, das ihm sein Slender-Recht zeigt

#### Scenario: Recht wird zwischen Öffnen und Klick entzogen

- **WHEN** ein Spieler mit Recht den Navigator öffnet, ihm danach das Recht `titan.navigator.buildserver` entzogen wird und er auf Build klickt
- **THEN** wird keine Weiterleitung angestoßen, der Navigator schließt, und der Spieler erhält keine Chat-Nachricht

#### Scenario: Ohne Permission-Plattform

- **WHEN** die Lobby ohne Permission-Plattform läuft und ein Spieler den Navigator öffnet
- **THEN** erscheint Build nicht

#### Scenario: Kein Durchsickern bei wiederholtem Öffnen

- **WHEN** ein Spieler mit Recht und ein Spieler ohne Recht den Navigator abwechselnd 50 Mal öffnen und schließen
- **THEN** sieht jeder bei jedem Öffnen sein eigenes Menü, der Spieler ohne Recht nie Build, und die Anzahl angemeldeter Event-Listener ist unverändert

#### Scenario: Recht-Wechsel wirkt in allen Menüs

- **WHEN** das Recht `titan.navigator.slender` einem Spieler mit und einem Spieler ohne Build-Recht zwischen zwei Öffnungen erteilt oder entzogen wird
- **THEN** zeigt jedes Menü Slender entsprechend dem neuen Stand des Rechts in Reihe 1, und Build bleibt unverändert

### Requirement: Slender liegt hinter dem Recht titan.navigator.slender

Das Ziel Slender MUSS an das Recht `titan.navigator.slender` gebunden sein, nach demselben Muster wie Build an `titan.navigator.buildserver`. Ergibt die Rechteprüfung beim Öffnen nicht „erteilt“ (also „verboten“ oder „nicht gesetzt“), DARF Slender für diesen Spieler nicht erscheinen, und die übrigen Minispiele MÜSSEN in Reihe 1 zentriert neu verteilt sein. Ergibt sie „erteilt“, MUSS Slender in Reihe 1 erscheinen (Platzierung nach „Minispiele und Server stehen zentriert in ihrer Reihe“). Ein Klick auf Slender MUSS das Recht erneut prüfen. Ist es erteilt, leitet die Lobby nach den Regeln „Schnellbeitritt tritt der besten bereiten Runde bei“ und „Klickzuordnung“ weiter; sonst DARF sie keine Weiterleitung anstoßen, MUSS den Navigator schließen und DARF dem Spieler keine Chat-Nachricht senden. Der Zustand des Rechts MUSS bei jedem Öffnen des Navigators ausgewertet werden, ohne Neustart der Lobby.

#### Scenario: Slender ohne Recht

- **WHEN** einem Spieler `titan.navigator.slender` nicht gesetzt oder verboten ist und er den Navigator öffnet
- **THEN** erscheint Slender nicht, und die übrigen Ziele sind in ihrer Gruppe zentriert verteilt

#### Scenario: Slender mit Recht

- **WHEN** einem Spieler `titan.navigator.slender` erteilt ist und er den Navigator öffnet
- **THEN** erscheint Slender in Reihe 1, und ein Linksklick darauf tritt der besten bereiten Runde bei

#### Scenario: Recht wird zwischen Öffnen und Klick entzogen

- **WHEN** ein Spieler mit dem Recht den Navigator öffnet, ihm danach `titan.navigator.slender` entzogen wird und er auf Slender klickt
- **THEN** wird keine Weiterleitung angestoßen, der Navigator schließt, und der Spieler erhält keine Chat-Nachricht

#### Scenario: Recht wird zwischen zwei Öffnungen erteilt

- **WHEN** einem Spieler `titan.navigator.slender` nach dem ersten Öffnen erteilt wird und er den Navigator danach erneut öffnet
- **THEN** erscheint Slender in Reihe 1 bei diesem zweiten Öffnen, ohne Neustart der Lobby

### Requirement: Bounce liegt hinter dem Recht titan.navigator.bounce

Das Ziel Bounce MUSS an das Recht `titan.navigator.bounce` gebunden sein, wie Slender an `titan.navigator.slender`. Ergibt die Rechteprüfung beim Öffnen nicht „erteilt“, DARF Bounce nicht erscheinen, und die übrigen Minispiele MÜSSEN in Reihe 1 zentriert neu verteilt sein. Ein Klick auf Bounce MUSS das Recht erneut prüfen; ist es nicht erteilt, DARF keine Weiterleitung angestoßen werden, und der Navigator MUSS schließen, ohne dem Spieler eine Chat-Nachricht zu senden.

#### Scenario: Bounce ohne Recht

- **WHEN** einem Spieler `titan.navigator.bounce` nicht gesetzt oder verboten ist und er den Navigator öffnet
- **THEN** erscheint Bounce nicht, und die übrigen Ziele sind in ihrer Gruppe zentriert verteilt

#### Scenario: Bounce mit Recht

- **WHEN** einem Spieler `titan.navigator.bounce` erteilt ist und er den Navigator öffnet
- **THEN** erscheint Bounce in Reihe 1, und ein Linksklick darauf tritt der besten bereiten Runde bei

#### Scenario: Recht wird zwischen Öffnen und Klick entzogen

- **WHEN** ein Spieler mit dem Recht den Navigator öffnet, ihm danach `titan.navigator.bounce` entzogen wird und er auf Bounce klickt
- **THEN** wird keine Weiterleitung angestoßen, der Navigator schließt, und der Spieler erhält keine Chat-Nachricht

#### Scenario: Recht wird zwischen zwei Öffnungen erteilt

- **WHEN** einem Spieler `titan.navigator.bounce` nach dem ersten Öffnen erteilt wird und er den Navigator danach erneut öffnet
- **THEN** erscheint Bounce in Reihe 1 bei diesem zweiten Öffnen, ohne Neustart der Lobby

#### Scenario: Ohne Permission-Plattform

- **WHEN** die Lobby ohne Permission-Plattform läuft und ein Spieler den Navigator öffnet
- **THEN** erscheint Bounce nicht

### Requirement: Tamias ist ein Minispiel hinter dem Recht titan.navigator.tamias

Der Navigator MUSS den Eintrag Tamias in Reihe 1 zeigen, und zwar nur Spielern mit dem Recht `titan.navigator.tamias`. Ohne das Recht DARF Tamias NICHT erscheinen, und die übrigen Minispiele MÜSSEN zentriert neu verteilt sein. Tamias MUSS den CloudNet-Task `Tamias` ansprechen, den Namen „Tamias“ ohne Übersetzung zeigen und ein Symbol und einen Verlauf aus Vanilla-Items verwenden, ohne Ressourcenpaket. Tamias gilt als Minispiel: Schnellbeitritt, Serverauswahl, Klickzeilen, Portal-Zeile und Spielerzahl folgen den Regeln von „Modus-Einträge erklären die Klicks“, „Schnellbeitritt tritt der besten bereiten Runde bei“ und „Serverauswahl im Stil des Navigators“. Schnellbeitritt und Serverauswahl von Tamias lesen nur die Dienste des Tasks `Tamias`, nicht die Dienste von `Tamias-Staging`.

#### Scenario: Tamias mit Recht

- **WHEN** ein Spieler mit `titan.navigator.tamias` den Navigator öffnet und auf Tamias links klickt
- **THEN** liegt Tamias in Reihe 1, und der Spieler wird dem besten bereiten Dienst des Tasks `Tamias` zugeordnet

#### Scenario: Tamias ohne Recht

- **WHEN** ein Spieler ohne `titan.navigator.tamias` den Navigator öffnet
- **THEN** erscheint Tamias nicht, und ein Klick auf den betroffenen Platz löst nichts aus

#### Scenario: Staging-Dienst erscheint nicht im Tamias-Schnellbeitritt

- **WHEN** `Tamias-Staging-1` als einziger Dienst freie Plätze hat und ein Spieler mit `titan.navigator.tamias` auf Tamias links klickt
- **THEN** wird niemand an `Tamias-Staging-1` übergeben

### Requirement: Staging-Einträge liegen im Untermenü „Staging“

Der Navigator MUSS auf Platz 35 den Eintrag „Staging“ zeigen, und zwar nur Spielern mit mindestens einem der Rechte `cygnus.staging.join`, `bounce.staging.join`, `elytrarace.staging.join` oder `tamias.staging.join`. Ohne eines dieser Rechte DARF der Eintrag nicht erscheinen, und Platz 35 MUSS eine Rahmenscheibe zeigen. Ein Klick auf „Staging“ MUSS das Untermenü öffnen. Das Untermenü MUSS genau neun Plätze haben: Slender (Staging) auf Platz 0, Bounce (Staging) auf Platz 1, ElytraRace (Staging) auf Platz 2, Tamias (Staging) auf Platz 3 und auf Platz 8 eine Zurück-Taste, die in den Navigator führt. Die übrigen Plätze MÜSSEN graue Glasscheiben sein. Ein Staging-Eintrag, für den das Recht fehlt, MUSS auf seinem Platz eine graue Glasscheibe zeigen. Jeder Staging-Eintrag MUSS die Klickzeile „▸ Linksklick  Starten und verbinden“ und die Zeile „Startet bei Bedarf“ zeigen. Er hat keinen Schnellbeitritt und keine Serverauswahl. Jeder Klick auf einen Staging-Eintrag MUSS das Recht des Eintrags erneut prüfen.

#### Scenario: Untermenü mit einem Staging-Recht

- **WHEN** ein Spieler nur `bounce.staging.join` hat und auf Platz 35 klickt
- **THEN** öffnet sich das Untermenü, Bounce (Staging) liegt auf Platz 1, die übrigen Staging-Plätze sind graue Glasscheiben, und auf Platz 8 liegt die Zurück-Taste

#### Scenario: Staging ohne Recht

- **WHEN** ein Spieler ohne `cygnus.staging.join` den Navigator öffnet und das Untermenü nicht erreichen kann
- **THEN** liegt auf Platz 35 eine Rahmenscheibe, und die übrigen Ziele sind unverändert

#### Scenario: Zurück in den Navigator

- **WHEN** ein Spieler im Untermenü auf die Zurück-Taste klickt
- **THEN** öffnet sich der Navigator mit den Zielen, die zu seinen Rechten passen

#### Scenario: Recht während des Untermenüs entzogen

- **WHEN** ein Spieler das Recht `bounce.staging.join` verliert, während das Untermenü offen ist, und danach auf Platz 1 klickt
- **THEN** wird niemand gestartet oder verbunden, und der Spieler erhält keine Chat-Nachricht

### Requirement: Menü-Varianten bleiben sechsundneunzig geteilte Inventare

_The navigator MUST keep one shared inventory per permission key and language, and the server list one per destination, language, order, page and row count; opening, paging and sorting MUST NOT create a new inventory or listener once it exists._

Der Navigator MUSS je Schlüssel und Sprache genau ein geteiltes Inventar haben, beim Start angelegt, mit dem Titel in dieser Sprache. Das Hauptmenü hat den Schlüssel aus den Rechten `titan.navigator.slender`, `titan.navigator.bounce`, `titan.navigator.tamias` und `titan.navigator.buildserver` sowie dem abgeleiteten Merkmal „mindestens ein Staging-Recht“. Das ergibt zweiunddreißig Menüs je Sprache, also vierundsechzig. Das Untermenü „Staging“ hat den Schlüssel aus den Rechten `cygnus.staging.join`, `bounce.staging.join`, `elytrarace.staging.join` und `tamias.staging.join`. Das ergibt sechzehn Menüs je Sprache, also zweiunddreißig. Insgesamt MÜSSEN es sechsundneunzig Inventare sein. Die Serverauswahl MUSS je Ziel, Sprache, Ordnung, Seite und Zeilenzahl höchstens ein geteiltes Inventar haben. Ein Serverauswahl-Inventar entsteht, wenn seine Ansicht zum ersten Mal gebraucht wird, und wird danach wiederverwendet. Das Öffnen, Blättern und Sortieren DARF, wenn das Inventar schon existiert, KEIN neues Inventar und keinen neuen Listener erzeugen.

#### Scenario: Zweiunddreißig Inventare beim Start

- **WHEN** das Navigator-Modul startet
- **THEN** existieren je Sprache zweiunddreißig Hauptmenü-Inventare, und zusammen mit dem Untermenü mit je sechzehn Inventaren sind es sechsundneunzig; jedes trägt die Ziele, die seinem Schlüssel entsprechen

#### Scenario: Titel je Sprache

- **WHEN** ein Spieler mit deutschem Client den Navigator öffnet
- **THEN** zeigt das Inventar den Titel „✦ Navigator ✦“ im Verlauf und die deutschen Zeilen, und ein Spieler mit englischem Client sieht das zweite Inventar mit englischem Titel und englischen Zeilen

#### Scenario: Häufiges Öffnen der Serverauswahl

- **WHEN** ein Spieler die Serverauswahl 20 Mal öffnet und wieder schließt
- **THEN** ist die Anzahl angemeldeter Event-Listener unverändert

#### Scenario: Häufiges Blättern und Sortieren

- **WHEN** ein Spieler alle vier Ordnungen einmal durchläuft, danach jede Ordnung ein zweites Mal aufruft und zwischen den Seiten hin- und herblättert
- **THEN** ist die Anzahl angemeldeter Event-Listener nach dem ersten Durchlauf unverändert

#### Scenario: Stoppen entfernt jede geöffnete Serverauswahl

- **WHEN** das Navigator-Modul gestoppt wird, nachdem Serverauswahlen auf Deutsch und Englisch geöffnet wurden
- **THEN** hat keine dieser Serverauswahlen mehr einen Klick-Listener

#### Scenario: Hub nur bei Staging-Recht

- **WHEN** ein Spieler ohne Staging-Recht, aber mit `titan.navigator.bounce` den Navigator öffnet
- **THEN** zeigt sein Hauptmenü Bounce in Reihe 1 und auf Platz 35 eine Rahmenscheibe, und sonst ist das Menü identisch mit dem Menü eines Spielers ohne Bounce-Recht

#### Scenario: Häufiges Öffnen des Navigators

- **WHEN** Spieler das Hauptmenü und das Untermenü wiederholt öffnen und schließen
- **THEN** wird kein neues Inventar und kein neuer Listener angelegt

## REMOVED Requirements

### Requirement: Der Navigator hat einen Rahmen aus Glasscheiben

**Reason**: Der Rahmen wird einheitlich dunkel, und die Plätze ohne Ziel bleiben leer statt grau gefüllt. Die Anforderung wird durch „Der Navigator hat einen dunklen Rahmen“ ersetzt.

**Migration**: Die neue Anforderung unten gilt ab diesem Stand; es gibt keine Datenmigration.

## ADDED Requirements

### Requirement: Der Navigator hat einen dunklen Rahmen

Der Navigator MUSS vier Reihen zu neun Plätzen haben. Die Plätze der ersten und letzten Reihe sowie der ersten und letzten Spalte MÜSSEN mit einer einzigen dunklen Glasscheibe (schwarz) gefüllt sein, die keinen Namen hat. Die Plätze ohne Ziel im Inneren MÜSSEN leer sein; graue Füllscheiben DÜRFEN NICHT erscheinen. Spawn MUSS auf Platz 31 und der Staging-Hub auf Platz 35 die Rahmenscheibe ersetzen, sobald sie erscheinen.

#### Scenario: Rahmen ist einheitlich dunkel

- **WHEN** ein Spieler den Navigator öffnet
- **THEN** sind die Plätze 0, 4, 9, 17 und 27 dieselbe schwarze Glasscheibe ohne Namen

#### Scenario: Innere Plätze ohne Ziel sind leer

- **WHEN** ein Spieler ohne Build-Recht den Navigator öffnet
- **THEN** sind die Plätze 10 und 12 leer, und es liegt keine graue Glasscheibe im Menü

#### Scenario: Ziele liegen im Inneren

- **WHEN** ein Spieler den Navigator öffnet
- **THEN** liegt kein Ziel auf einem Rahmenplatz außer Spawn auf Platz 31 und dem Staging-Hub auf Platz 35

### Requirement: Minispiele und Server stehen zentriert in ihrer Reihe

Die Minispiele MÜSSEN in Reihe 1 auf den Plätzen 10 bis 16 liegen, die Server in Reihe 2 auf den Plätzen 19 bis 25. Die sichtbaren Ziele einer Gruppe MÜSSEN in der Reihenfolge ihrer Aufzählung auf aufeinanderfolgenden Plätzen liegen, und die Gruppe MUSS für `n` sichtbare Ziele bei einer Breite von sieben Plätzen mit dem ersten Platz `Anfang + ⌊(7 − n) / 2⌋` beginnen. Ein Ziel, dessen Recht fehlt, belegt keinen Platz, und die übrigen Ziele rücken nach, ohne Lücke. Dieselbe Regel gilt bei jedem Öffnen aus den aktuellen Rechten.

#### Scenario: Ein Minispiel

- **WHEN** nur ElytraRace sichtbar ist
- **THEN** liegt ElytraRace auf Platz 13

#### Scenario: Zwei Minispiele

- **WHEN** ElytraRace und Slender sichtbar sind
- **THEN** liegen sie auf den Plätzen 12 und 13

#### Scenario: Drei Minispiele

- **WHEN** ElytraRace, Bounce und Slender sichtbar sind
- **THEN** liegen sie auf den Plätzen 12, 13 und 14

#### Scenario: Vier Minispiele

- **WHEN** ElytraRace, Bounce, Tamias und Slender sichtbar sind
- **THEN** liegen sie auf den Plätzen 11, 12, 13 und 14

#### Scenario: Drei Server

- **WHEN** Survival, Creative und Build sichtbar sind
- **THEN** liegen sie auf den Plätzen 21, 22 und 23

### Requirement: Stapelanzahl und Glanz zeigen die Spielerzahl

Die Stapelanzahl eines Hauptmenü-Ziels MUSS seiner Spielerzahl entsprechen, auf den Bereich 1 bis 64 begrenzt; ohne Spieler MUSS sie 1 sein. Ein Ziel MUSS glänzen, solange mindestens ein Spieler auf ihm ist, und DARF NICHT glänzen, wenn niemand darauf ist. Die Zahlen, die Anzahl und der Glanz MÜSSEN beim bestehenden Aktualisierungstakt `navigator.refreshSeconds` neu gesetzt werden und zwischen zwei Takten unverändert bleiben. Dafür DARF kein neuer Takt, keine neue Konfiguration und kein neuer Listener entstehen.

#### Scenario: Anzahl folgt der Spielerzahl

- **WHEN** 12 Spieler auf Survival sind und der Takt läuft
- **THEN** zeigt Survival die Stapelanzahl 12 und glänzt

#### Scenario: Leeres Ziel

- **WHEN** niemand auf Creative ist und der Takt läuft
- **THEN** zeigt Creative die Stapelanzahl 1 und glänzt nicht

#### Scenario: Obergrenze

- **WHEN** 100 Spieler auf Survival sind und der Takt läuft
- **THEN** zeigt Survival die Stapelanzahl 64, und die Zeile „● 100 Spieler“ bleibt unverändert im Lore

#### Scenario: Zwischen den Takten

- **WHEN** ein Spieler nach einem Takt den Server verlässt und vor dem nächsten Takt das Menü betrachtet
- **THEN** zeigt das Menü noch den Stand des letzten Taktes
