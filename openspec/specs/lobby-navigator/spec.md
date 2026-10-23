# lobby-navigator Specification

## Purpose
Legt fest, welche Ziele der Lobby-Navigator zeigt und wohin sie weiterleiten: Titel und Ziele stehen fest im Navigator-Modul, Slender und Build hängen an Rechten, Minispiele bieten Schnellbeitritt und Serverauswahl, und das Öffnen häuft keine Ressourcen an.

## Requirements

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

### Requirement: Öffnen des Navigators häuft nichts an
Das Öffnen des Navigators DARF NICHT dazu führen, dass pro Spieler oder pro Öffnung zusätzliche Event-Listener oder dauerhaft gehaltene Objekte entstehen. Nach dem Verlassen der Lobby DARF der Navigator keine Referenz mehr auf den Spieler halten.

#### Scenario: Häufiges Öffnen
- **WHEN** ein Spieler den Navigator 50 Mal öffnet und wieder schließt
- **THEN** ist die Anzahl angemeldeter Event-Listener unverändert

#### Scenario: Spieler verlässt die Lobby
- **WHEN** ein Spieler, der den Navigator geöffnet hatte, die Lobby verlässt
- **THEN** hält der Navigator keine Daten mehr zu diesem Spieler

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

### Requirement: Klickhinweis ist übersetzt

Jede Lore-Zeile des Navigators MUSS in Deutsch oder Englisch erscheinen, je nach Sprache des Spielers, und DARF nie leer sein. Die Schlüssel für Tasten, Aktionen, die Spielerzeile und die Serverauswahl MÜSSEN in beiden Bundles stehen. Unbekannte Sprachen MÜSSEN Englisch zeigen.

#### Scenario: Deutsche Texte

- **WHEN** ein Spieler mit deutschem Client den Navigator öffnet
- **THEN** zeigt der Eintrag ElytraRace die Zeilen „▸ Linksklick  Schnell beitreten“ und „▸ Rechtsklick  Server wählen“, und keine Zeile ist leer

#### Scenario: Unbekannte Sprache

- **WHEN** ein Spieler mit einer Sprache ohne Bundle den Navigator öffnet
- **THEN** zeigt der Eintrag die englischen Zeilen „▸ Left click  Quick join“ und „▸ Right click  Choose a server“

#### Scenario: Spielerzeile in beiden Sprachen

- **WHEN** die Spielerzahl eines Ziels bekannt ist
- **THEN** steht „Spieler“ bzw. „Players“ mit der Zahl und, wenn das Maximum bekannt ist, „/ Maximum“ vor den Klickzeilen

### Requirement: Der Navigator hat keine Feature-Flag

Die mitgelieferten Standardwerte MÜSSEN für den Navigator keine Flag `NAVIGATOR_*` im Abschnitt `features` enthalten. Setzt ein Betreiber `features.NAVIGATOR_SLENDER` (oder `FEATURES_NAVIGATOR_SLENDER`), MUSS die Lobby den Eintrag als unbekannt behandeln: Sie startet ohne Fehler und ohne Warnung, und der Eintrag hat keinen Einfluss auf Slender. Slender erscheint allein über das Recht `titan.navigator.slender`.

#### Scenario: Standardwerte ohne Navigator-Flag

- **WHEN** die Lobby mit den mitgelieferten Standardwerten startet
- **THEN** enthält der Abschnitt `features` keine Flag, die mit `NAVIGATOR_` beginnt

#### Scenario: Alte Flag ohne Wirkung

- **WHEN** `application.yaml` `features.NAVIGATOR_SLENDER: true` setzt, die Lobby startet und ein Spieler ohne das Recht `titan.navigator.slender` den Navigator öffnet
- **THEN** startet die Lobby ohne Fehler und ohne Warnung, und Slender erscheint für diesen Spieler nicht

### Requirement: Slender liegt hinter dem Recht titan.navigator.slender

Das Ziel Slender MUSS an das Recht `titan.navigator.slender` gebunden sein, nach demselben Muster wie Build an `titan.navigator.buildserver`. Ergibt die Rechteprüfung beim Öffnen nicht „erteilt“ (also „verboten“ oder „nicht gesetzt“), DARF Slender für diesen Spieler nicht erscheinen, und auf Platz 15 MUSS eine graue Glasscheibe liegen. Ergibt sie „erteilt“, MUSS Slender auf Platz 15 erscheinen. Ein Klick auf Slender MUSS das Recht erneut prüfen. Ist es erteilt, leitet die Lobby nach den Regeln „Schnellbeitritt tritt der besten bereiten Runde bei“ und „Klickzuordnung“ weiter; sonst DARF sie keine Weiterleitung anstoßen, MUSS den Navigator schließen und DARF dem Spieler keine Chat-Nachricht senden. Der Zustand des Rechts MUSS bei jedem Öffnen des Navigators ausgewertet werden, ohne Neustart der Lobby.

#### Scenario: Slender ohne Recht

- **WHEN** einem Spieler `titan.navigator.slender` nicht gesetzt oder verboten ist und er den Navigator öffnet
- **THEN** liegt auf Platz 15 eine graue Glasscheibe, und die übrigen Ziele sind unverändert

#### Scenario: Slender mit Recht

- **WHEN** einem Spieler `titan.navigator.slender` erteilt ist und er den Navigator öffnet
- **THEN** erscheint Slender auf Platz 15, und ein Linksklick darauf tritt der besten bereiten Runde bei

#### Scenario: Recht wird zwischen Öffnen und Klick entzogen

- **WHEN** ein Spieler mit dem Recht den Navigator öffnet, ihm danach `titan.navigator.slender` entzogen wird und er auf Slender klickt
- **THEN** wird keine Weiterleitung angestoßen, der Navigator schließt, und der Spieler erhält keine Chat-Nachricht

#### Scenario: Recht wird zwischen zwei Öffnungen erteilt

- **WHEN** einem Spieler `titan.navigator.slender` nach dem ersten Öffnen erteilt wird und er den Navigator danach erneut öffnet
- **THEN** erscheint Slender auf Platz 15 bei diesem zweiten Öffnen, ohne Neustart der Lobby

### Requirement: Der Slender-Name ist ein Verlauf von Grau nach Rot

Der Anzeigename von Slender MUSS ein Farbverlauf von `#616161` nach `#e80000` sein: Der erste Buchstabe ist `#616161`, der letzte genau `#e80000`.

#### Scenario: Verlauf des Slender-Namens

- **WHEN** ein Spieler mit dem Recht `titan.navigator.slender` den Navigator öffnet
- **THEN** beginnt der Name „Slender“ in `#616161`, und sein letzter Buchstabe hat genau `#e80000`

### Requirement: Nur benannte Klicks wirken
_The navigator and its server list MUST cancel every click, act only on left and right clicks, and apply the same rule to the sort item, the page arrows and the back button._

Der Navigator und seine Serverauswahl MÜSSEN jeden Klick abbrechen. Gewirkt wird nur bei Links- und Rechtsklick, mit oder ohne Shift. Jeder andere Klicktyp (Zahlentasten, Q, F, Mittelklick, Doppelklick, Ziehen) DARF weder schnell beitreten, eine Serverauswahl öffnen, verbinden, zum Portal laufen, den Navigator öffnen, die Ordnung wechseln noch eine Seite wechseln; das Inventar MUSS dabei offen bleiben. Das gilt für Minispiele, Server, die Einträge der Serverauswahl, das Sortier-Item, die Pfeile und die Zurück-Taste.

#### Scenario: Zahlentaste auf einem Minispiel

- **WHEN** ein Spieler auf ElytraRace die Zahlentaste drückt
- **THEN** passiert nichts: keine Übergabe, keine Serverauswahl, und der Navigator bleibt offen

#### Scenario: Q auf einem Server

- **WHEN** ein Spieler auf Survival Q drückt
- **THEN** wird niemand übergeben, und der Navigator bleibt offen

#### Scenario: Mittelklick auf einen Eintrag der Serverauswahl

- **WHEN** ein Spieler in der Serverauswahl mit dem Mittelklick auf einen Dienst klickt
- **THEN** wird er nicht übergeben, und die Serverauswahl bleibt offen

#### Scenario: Zahlentaste auf dem Sortier-Item

- **WHEN** ein Spieler in der Serverauswahl auf das Sortier-Item die Zahlentaste drückt
- **THEN** bleibt die Ordnung unverändert, und die Serverauswahl bleibt offen

#### Scenario: Zahlentaste auf der Zurück-Taste

- **WHEN** ein Spieler in der Serverauswahl auf die Zurück-Taste die Zahlentaste drückt
- **THEN** öffnet sich der Navigator nicht, und die Serverauswahl bleibt offen

### Requirement: Unlesbare Dienste werden im Chat abgelehnt

Kann die Lobby die laufenden Dienste eines Ziels nicht lesen, weil kein Zähl-Anbieter vorhanden ist oder die Abfrage fehlschlägt, MUSS sie das von dem Ergebnis „läuft nirgends“ unterscheiden. Dann DARF sie weder die Serverauswahl öffnen noch jemanden übergeben, MUSS den Navigator offen lassen und dem Spieler eine übersetzte Ablehnung im Chat senden. Die Ablehnung trägt das Präfix „Servers »“ wie die übrigen Ablehnungen.

#### Scenario: Schnellbeitritt bei fehlgeschlagener Abfrage

- **WHEN** die Abfrage der Dienste fehlschlägt und ein Spieler links auf ElytraRace klickt
- **THEN** wird niemand übergeben, es öffnet sich keine Serverauswahl, und der Spieler erhält im Chat „Servers » Die Serverliste ist gerade nicht verfügbar.“

#### Scenario: Rechtsklick bei fehlgeschlagener Abfrage

- **WHEN** die Abfrage der Dienste fehlschlägt und ein Spieler rechts auf ElytraRace klickt
- **THEN** öffnet sich keine Serverauswahl, und der Spieler erhält im Chat „Servers » Die Serverliste ist gerade nicht verfügbar.“

#### Scenario: Ohne Zähl-Anbieter

- **WHEN** kein Zähl-Anbieter vorhanden ist und ein Spieler links auf ElytraRace klickt
- **THEN** wird niemand übergeben, und der Spieler erhält im Chat „Servers » Die Serverliste ist gerade nicht verfügbar.“

### Requirement: Der Navigator hat einen Rahmen aus Glasscheiben

Der Navigator MUSS vier Reihen zu neun Plätzen haben. Die Plätze der ersten und letzten Reihe sowie der ersten und letzten Spalte MÜSSEN mit Glasscheiben in Hellblau und Cyan gefüllt sein, die sich nach der Parität von Reihe plus Spalte abwechseln, und einen leeren Namen haben. Die übrigen Plätze ohne Ziel MÜSSEN graue Glasscheiben sein. Spawn MUSS auf Platz 31 die Rahmenscheibe in der unteren Mitte ersetzen.

#### Scenario: Rahmen wechselt die Farbe

- **WHEN** ein Spieler den Navigator öffnet
- **THEN** ist Platz 0 hellblau, Platz 1 cyan, Platz 9 cyan und Platz 10 grau

#### Scenario: Ziele liegen im Inneren

- **WHEN** ein Spieler den Navigator öffnet
- **THEN** liegt kein Ziel auf einem Rahmenplatz außer Spawn auf Platz 31

### Requirement: Schnellbeitritt tritt der besten bereiten Runde bei

Ein Linksklick auf ein Minispiel MUSS aus den laufenden Diensten des Ziels den Dienst wählen, der bereit ist (Limit gemeldet) und nicht voll ist, und zwar den mit den meisten Spielern. Bei Gleichstand MUSS der Dienst mit dem kleineren Namen gewählt werden. Die Lobby MUSS den Spieler direkt zu diesem Dienst übergeben und den Navigator schließen. Gibt es keinen solchen Dienst, MUSS die Lobby die Serverauswahl öffnen. Die Dienste MÜSSEN beim Klick neu gelesen werden.

#### Scenario: Schnellbeitritt mit mehreren Diensten

- **WHEN** ElytraRace auf drei Diensten läuft, `ElytraRace-2` mit 12 Spielern, `ElytraRace-1` mit 12 Spielern und `ElytraRace-3` mit 4 Spielern, alle bereit und nicht voll, und ein Spieler auf ElytraRace links klickt
- **THEN** wird der Spieler zu `ElytraRace-1` übergeben

#### Scenario: Volle Dienste werden übersprungen

- **WHEN** `ElytraRace-1` mit 20 von 20 Spielern voll ist und `ElytraRace-2` mit 5 von 20 bereit ist, und ein Spieler links klickt
- **THEN** wird der Spieler zu `ElytraRace-2` übergeben

#### Scenario: Nicht bereite Dienste werden übersprungen

- **WHEN** `ElytraRace-1` kein Limit gemeldet hat und `ElytraRace-2` bereit ist, und ein Spieler links klickt
- **THEN** wird der Spieler zu `ElytraRace-2` übergeben

#### Scenario: Keine bereite Runde öffnet die Serverauswahl

- **WHEN** alle laufenden Dienste voll oder nicht bereit sind und ein Spieler links auf ElytraRace klickt
- **THEN** öffnet sich die Serverauswahl, die die Zustände der Dienste zeigt

#### Scenario: Ohne laufenden Dienst

- **WHEN** ElytraRace nirgends läuft und ein Spieler links klickt
- **THEN** öffnet sich die Serverauswahl, die leer ist und in der Mitte „Gerade läuft kein Server.“ zeigt

### Requirement: Klickzuordnung

Ein Klick MUSS nach dieser Tabelle wirken. Shift hat nur dann eine Wirkung, wenn für das Ziel ein Portal existiert; sonst gilt der Klick wie ohne Shift.

| Klick | Minispiel | Server |
| --- | --- | --- |
| Links | Schnellbeitritt | Verbinden |
| Rechts | Serverauswahl, immer | Verbinden |
| Shift-Links oder Shift-Rechts mit Portal | zum Portal laufen | zum Portal laufen |

Eine Serverauswahl nach dem Rechtsklick MUSS sich auch dann öffnen, wenn nur ein Dienst läuft, und diesen Dienst zeigen. Das Portal führt den Spieler vor den Portal-Auslöser, wie bisher. Jeder andere Klicktyp wirkt nicht, siehe „Nur benannte Klicks wirken“.

#### Scenario: Rechtsklick auf Minispiel mit einem Dienst

- **WHEN** ElytraRace nur auf `ElytraRace-1` läuft und ein Spieler rechts klickt
- **THEN** öffnet sich die Serverauswahl mit `ElytraRace-1`

#### Scenario: Rechtsklick auf Server verbindet

- **WHEN** ein Spieler rechts auf Survival klickt, ohne Shift
- **THEN** wird eine Weiterleitung zum Ziel „Survival“ angestoßen

#### Scenario: Shift-Klick auf Minispiel ohne Portal

- **WHEN** für Slender kein Portal existiert und ein Spieler Shift-Links klickt
- **THEN** wird wie beim Linksklick schnell beigetreten

#### Scenario: Shift-Klick auf Server mit Portal

- **WHEN** für Survival ein Portal existiert und ein Spieler Shift-Rechts klickt
- **THEN** läuft der Spieler zum Portal, und es wird keine Weiterleitung angestoßen

### Requirement: Serverauswahl im Stil des Navigators

Die Serverauswahl MUSS den Titel „Server wählen“ im Verlauf zeigen, übersetzt, und für jeden Dienst einen Eintrag mit dem Namen in der Verlaufsfarbe des Ziels. Die Lore eines Eintrags MUSS die Spielerzeile in dem Format des Navigators zeigen (mit Maximum, wenn bekannt) und danach eine Zustandszeile `▸ Zustand` in Rot für „Voll“, Gelb für „Noch nicht bereit“ oder Grün für „Klicken zum Beitreten“. Die Zustände und die Ablehnung MÜSSEN übersetzt sein.

#### Scenario: Eintrag im Stil des Navigators

- **WHEN** die Serverauswahl für ElytraRace mit `ElytraRace-1` mit 13 von 40 Spielern geöffnet wird
- **THEN** zeigt der Eintrag den Namen in der Verlaufsfarbe von ElytraRace, die Zeile „Spieler  13 / 40“ und die Zeile „▸ Klicken zum Beitreten“

#### Scenario: Ablehnung auf Deutsch

- **WHEN** ein Spieler mit deutschem Client einen vollen Dienst wählt
- **THEN** erhält er im Chat „Servers » Dieser Server ist voll.“

#### Scenario: Leere Serverauswahl erklärt sich

- **WHEN** die Serverauswahl geöffnet wird, während das Ziel nirgends läuft
- **THEN** zeigt sie in der Mitte einen Eintrag „Gerade läuft kein Server.“, der beim Klick nichts tut

#### Scenario: Gewählter Server wird beim Klick neu gelesen

- **WHEN** `ElytraRace-1` beim Öffnen bereit mit 3 von 50 Spielern gelistet war, danach aber mit 50 von 50 Spielern voll gemeldet wird, und der Spieler es anklickt
- **THEN** wird der Spieler nicht übergeben, die Serverauswahl bleibt offen, und er erhält im Chat „Servers » Dieser Server ist voll.“

#### Scenario: Gewählter Server läuft inzwischen nicht mehr

- **WHEN** ein Spieler einen Dienst aus der Serverauswahl anklickt, der seit dem Öffnen nicht mehr läuft
- **THEN** wird der Spieler nicht übergeben, und er erhält im Chat „Servers » Dieser Server ist noch nicht bereit.“

### Requirement: Menü-Varianten bleiben sechzehn geteilte Inventare
_The navigator MUST keep one shared inventory per combination of permissions and language, and the server list one per destination, language, order, page and row count; opening, paging and sorting MUST NOT create a new inventory or listener once it exists._

Der Navigator MUSS je Kombination aus Rechten und Sprache genau ein geteiltes Inventar haben, beim Start angelegt, mit dem Titel in dieser Sprache. Die Rechte sind `titan.navigator.slender`, `titan.navigator.buildserver` und `titan.navigator.bounce`; jede Teilmenge dieser drei Rechte ergibt ein Menü, also acht je Sprache und sechzehn bei zwei Sprachen. Die Serverauswahl MUSS je Ziel, Sprache, Ordnung, Seite und Zeilenzahl höchstens ein geteiltes Inventar haben. Ein Serverauswahl-Inventar entsteht, wenn seine Ansicht zum ersten Mal gebraucht wird, und wird danach wiederverwendet. Das Öffnen, Blättern und Sortieren DARF, wenn das Inventar schon existiert, KEIN neues Inventar und keinen neuen Listener erzeugen.

#### Scenario: Sechzehn Inventare beim Start

- **WHEN** das Navigator-Modul startet
- **THEN** existieren genau sechzehn Navigator-Inventare, acht je Sprache, und jedes trägt die Ziele, die seiner Rechte-Kombination entsprechen

#### Scenario: Titel je Sprache

- **WHEN** ein Spieler mit deutschem Client den Navigator öffnet
- **THEN** zeigt das Inventar den Titel „Navigator“ im Verlauf und die deutschen Zeilen, und ein Spieler mit englischem Client sieht das zweite Inventar mit englischem Titel und englischen Zeilen

#### Scenario: Häufiges Öffnen der Serverauswahl

- **WHEN** ein Spieler die Serverauswahl 20 Mal öffnet und wieder schließt
- **THEN** ist die Anzahl angemeldeter Event-Listener unverändert

#### Scenario: Häufiges Blättern und Sortieren

- **WHEN** ein Spieler alle vier Ordnungen einmal durchläuft, danach jede Ordnung ein zweites Mal aufruft und zwischen den Seiten hin- und herblättert
- **THEN** ist die Anzahl angemeldeter Event-Listener nach dem ersten Durchlauf unverändert

#### Scenario: Stoppen entfernt jede geöffnete Serverauswahl

- **WHEN** das Navigator-Modul gestoppt wird, nachdem Serverauswahlen auf Deutsch und Englisch geöffnet wurden
- **THEN** hat keine dieser Serverauswahlen mehr einen Klick-Listener

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

### Requirement: Serverauswahl blättert durch alle Dienste
_The server list MUST show every running service on pages of at most 45 entries, with the navigation row at the bottom._

Die Serverauswahl MUSS jeden laufenden Dienst des Ziels zeigen, auf Seiten zu höchstens 45 Einträgen. Die unterste Reihe ist immer die Navigationszeile, auch bei leerer Liste. Auf ihrem Startslot steht die Zurück-Taste, danach folgen der Vorige-Pfeil auf Startslot + 3, das Sortier-Item auf Startslot + 4 und der Nächste-Pfeil auf Startslot + 5. Ein Pfeil erscheint nur, wenn es in seine Richtung eine Seite gibt. Ab zwei Seiten nennen die Pfeile ihre Zielseite, und die Lore des Sortier-Items zeigt „Seite x/y“. Eine nicht volle Reihe, also die letzte oder einzige Reihe einer Seite, MUSS zentriert sein: ihre `n` Einträge beginnen in Spalte `(9 − n) / 2`, ganzzahlig. Volle Reihen bleiben unverschoben.

#### Scenario: Wenige Dienste ohne Seiten
- **WHEN** sechs Dienste eines Minispiels laufen und ein Spieler die Serverauswahl öffnet
- **THEN** zeigt das Inventar eine Reihe Einträge in den Slots 1 bis 6 und darunter die Navigationszeile mit der Zurück-Taste in Slot 9 und dem Sortier-Item in Slot 13
- **AND** es erscheinen keine Pfeile und keine Seitenanzeige in der Lore

#### Scenario: Ein Dienst steht in der Mitte
- **WHEN** genau ein Dienst läuft und ein Spieler die Serverauswahl öffnet
- **THEN** steht der Dienst in Slot 4, der Mitte der ersten Reihe
- **AND** die Zurück-Taste steht in Slot 9, das Sortier-Item in Slot 13, der Mitte der Navigationszeile, und es erscheinen keine Pfeile

#### Scenario: Mehr Dienste als eine Seite fasst
- **WHEN** 100 Dienste laufen und ein Spieler die Serverauswahl öffnet
- **THEN** zeigt Seite 1 die ersten 45 Einträge der aktiven Ordnung, in den Slots 0 bis 44
- **AND** der Nächste-Pfeil in Slot 50 nennt „Nächste Seite · Seite 2/3“, das Sortier-Item in Slot 49 zeigt in seiner Lore „Seite 1/3“, die Zurück-Taste steht in Slot 45, und es gibt keinen Vorige-Pfeil in Slot 48

#### Scenario: Seite zurück
- **WHEN** der Spieler auf Seite 2 den Pfeil nach links in Slot 48 anklickt
- **THEN** zeigt die Serverauswahl Seite 1 wieder, mit der aktiven Ordnung

#### Scenario: Letzte Seite
- **WHEN** der Spieler auf der letzten Seite steht
- **THEN** fehlt der Pfeil nach rechts

#### Scenario: Kein Dienst geht verloren
- **WHEN** 200 Dienste laufen und der Spieler alle fünf Seiten nacheinander aufruft
- **THEN** erscheint jeder der 200 Dienste genau einmal

#### Scenario: Seite verschwindet beim Neulesen
- **WHEN** der Spieler auf Seite 2 von 2 den Pfeil nach rechts anklickt, die Dienste inzwischen auf eine Seite geschrumpft sind
- **THEN** zeigt die Serverauswahl die einzige vorhandene Seite, ohne Fehler

#### Scenario: Mehr als 54 Dienste werden nicht abgeschnitten
- **WHEN** 60 Dienste laufen und der Spieler auf Seite 2 wechselt
- **THEN** zeigt Seite 2 die restlichen 15 Dienste und die Lore des Sortier-Items „Seite 2/2“

### Requirement: Sortier-Item schaltet die Ordnung weiter
_The sort item MUST cycle through four orders on each named click, start at name and return to page one on every change._

Die Navigationszeile der Serverauswahl MUSS ein Sortier-Item haben. Ein benannter Klick darauf schaltet die Ordnung weiter: Name, Meiste Spieler, Freie Plätze, Status, danach wieder Name. Beim Öffnen ist Name aktiv. Nach jedem Ordnungswechsel MUSS die Serverauswahl auf Seite 1 springen. Die Lore des Sortier-Items MUSS alle vier Ordnungen nennen und die aktive hervorheben.

#### Scenario: Standardordnung
- **WHEN** die Dienste `Lobby-2`, `Lobby-1` und `Lobby-3` laufen und ein Spieler die Serverauswahl öffnet
- **THEN** stehen die Einträge in der Reihenfolge `Lobby-1`, `Lobby-2`, `Lobby-3`

### Requirement: Ordnungen sind eindeutig festgelegt
_Each order MUST be fully defined, and ties MUST be broken by the service name ascending._

Die Ordnungen sind: **Name** (Dienstname aufsteigend), **Meiste Spieler** (Spielerzahl absteigend), **Freie Plätze** (Zahl der freien Plätze absteigend; Dienste ohne gemeldetes Limit stehen am Ende) und **Status** (beitretbare Dienste, dann nicht bereite, dann volle). Gleichstand entscheidet in jeder Ordnung der Dienstname aufsteigend (A–Z).

#### Scenario: Meiste Spieler
- **WHEN** `A` 12 von 100, `B` 40 von 100 und `C` 0 von 100 Spielern hat und der Spieler auf Meiste Spieler wechselt
- **THEN** stehen die Einträge in der Reihenfolge `B`, `A`, `C`

#### Scenario: Gleichstand bei Spielerzahl
- **WHEN** `ElytraRace-2` und `ElytraRace-1` jeweils 12 Spieler haben und die Ordnung Meiste Spieler aktiv ist
- **THEN** steht `ElytraRace-1` vor `ElytraRace-2`

#### Scenario: Freie Plätze
- **WHEN** `A` 12 von 100, `B` 40 von 50 und `C` ohne gemeldetes Limit hat und der Spieler auf Freie Plätze wechselt
- **THEN** stehen die Einträge in der Reihenfolge `A`, `B`, `C`, und `C` steht am Ende

#### Scenario: Status
- **WHEN** `A` beitretbar, `B` voll (10 von 10) und `C` nicht bereit ist und der Spieler auf Status wechselt
- **THEN** stehen die Einträge in der Reihenfolge `A`, `C`, `B`

#### Scenario: Ein Klick nach dem anderen durch alle Ordnungen
- **WHEN** der Spieler viermal auf das Sortier-Item klickt
- **THEN** ist wieder Name aktiv, und die Reihenfolge der Einträge ist die der Standardordnung

#### Scenario: Wechsel springt auf Seite 1
- **WHEN** der Spieler auf Seite 2 steht und das Sortier-Item anklickt
- **THEN** zeigt die Serverauswahl Seite 1 in der neuen Ordnung

#### Scenario: Lore zeigt die aktive Ordnung
- **WHEN** Status aktiv ist
- **THEN** nennt die Lore des Sortier-Items alle vier Ordnungen und hebt Status als aktive Ordnung hervor

#### Scenario: Erneutes Öffnen beginnt mit Name
- **WHEN** der Spieler die Ordnung auf Status gestellt hat, die Serverauswahl schließt und sie danach erneut öffnet
- **THEN** ist Name aktiv und die Liste zeigt Seite 1

### Requirement: Serverauswahl liest beim Öffnen und bei jeder Interaktion neu
_The server list MUST read the services on open, on every page or order change and on every entry click, and MUST NOT refresh itself in between._

Die Serverauswahl liest die laufenden Dienste beim Öffnen, bei jedem Seitenwechsel, bei jedem Ordnungswechsel und beim Klick auf einen Eintrag neu. Zwischen diesen Ereignissen aktualisiert sie sich nicht von selbst. Der Klick auf einen Eintrag prüft den Dienst weiterhin vor dem Wechsel.

#### Scenario: Stand bleibt bis zum nächsten Klick
- **WHEN** ein Dienst nach dem Öffnen der Serverauswahl Plätze verliert, der Spieler aber keine Taste drückt
- **THEN** zeigt die Liste bis zum nächsten Klick den Stand vom Öffnen

#### Scenario: Seitenwechsel liest neu
- **WHEN** ein Dienst nach dem Öffnen gestoppt wurde und der Spieler auf die nächste Seite wechselt
- **THEN** erscheint der gestoppte Dienst auf der neuen Seite nicht mehr

### Requirement: Ordnung und Seite gelten nur für das offene Inventar
_The server list MUST keep no order or page per player; the open inventory is the only state, and a player who leaves leaves nothing behind._

Die Serverauswahl DARF Ordnung und Seite eines Spielers nicht speichern. Die Ordnung und die Seite, die ein Spieler sieht, sind die des Inventars, das er offen hat. Ändert ein Spieler die Ordnung, bleibt die Ansicht eines anderen Spielers unverändert.

#### Scenario: Zwei Spieler mit verschiedenen Ordnungen
- **WHEN** Spieler A die Ordnung auf Status stellt und Spieler B danach die Serverauswahl öffnet
- **THEN** sieht Spieler B die Ordnung Name auf Seite 1

#### Scenario: Spieler verlässt die Lobby bei offener Serverauswahl
- **WHEN** ein Spieler mit offener Serverauswahl die Lobby verlässt
- **THEN** hält die Serverauswahl keine Daten mehr zu diesem Spieler

### Requirement: Zurück-Taste führt in den Navigator
_The back button MUST open the navigator menu that matches the player's current permissions and language, through the same path as the feather._

Die Navigationszeile der Serverauswahl MUSS auf Navigationsstart + 0 eine Zurück-Taste haben, auf jeder Seite und auch bei leerer Liste. Ein benannter Klick öffnet den Navigator wie das Feder-Item, mit den Rechten und der Sprache des Spielers im Moment des Klicks.

#### Scenario: Zurück von der ersten Seite

- **WHEN** ein Spieler mit dem Recht `titan.navigator.slender` die Serverauswahl von ElytraRace auf Seite 1 offen hat und die Zurück-Taste in Slot 45 anklickt
- **THEN** zeigt sein Inventar den Navigator mit dem Eintrag Slender, also das Menü seiner Rechte

#### Scenario: Zurück von der zweiten Seite

- **WHEN** der Spieler auf Seite 2 einer Serverauswahl mit 100 Diensten die Zurück-Taste anklickt
- **THEN** zeigt sein Inventar den Navigator, nicht Seite 1 der Serverauswahl

#### Scenario: Zurück bei leerer Liste

- **WHEN** die Serverauswahl geöffnet ist, während das Ziel nirgends läuft, und der Spieler die Zurück-Taste in Slot 9 anklickt
- **THEN** zeigt sein Inventar den Navigator

#### Scenario: Entzogenes Recht vor dem Zurück

- **WHEN** ein Spieler die Serverauswahl offen hat, danach das Recht `titan.navigator.slender` verliert und die Zurück-Taste anklickt
- **THEN** zeigt sein Inventar den Navigator ohne den Eintrag Slender

#### Scenario: Zurück auf Deutsch

- **WHEN** ein Spieler mit deutschem Client die Zurück-Taste anklickt
- **THEN** zeigt sein Inventar das deutsche Navigator-Menü mit dem Titel „Navigator“

#### Scenario: Häufiges Zurück ohne neuen Listener

- **WHEN** ein Spieler die Serverauswahl 20 Mal öffnet und jedes Mal über die Zurück-Taste den Navigator wieder öffnet
- **THEN** ist die Anzahl angemeldeter Event-Listener am Navigator-Modul und an der Serverauswahl unverändert
