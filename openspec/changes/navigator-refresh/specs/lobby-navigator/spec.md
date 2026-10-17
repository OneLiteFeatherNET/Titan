# Spec Delta

Die Plätze zählen ab 0, wie in der Basis-Spezifikation. „Platz 15“ ist der 16. Platz der Anzeige.

## MODIFIED Requirements

### Requirement: Navigator-Ziele sind im Navigator-Modul festgelegt

Titel und Ziele des Navigators MÜSSEN fest im Navigator-Modul festgelegt sein. Die Lobby DARF Titel und Ziele NICHT aus der Konfiguration lesen, und andere Module DÜRFEN KEINE Ziele beisteuern. Werte unter `navigator.*` aus Dateien, Profilen, Env-Variablen oder System-Properties DÜRFEN den Navigator NICHT beeinflussen. Der Navigator MUSS einen Titel „Navigator“ im Verlauf zeigen, der in der Sprache des Spielers übersetzt ist, und eine Anzeige mit vier Reihen zu je neun Plätzen. ElytraRace MUSS auf Platz 11 liegen, Slender auf Platz 15, Survival auf Platz 20, Creative auf Platz 22, Spawn mit einem Kompass auf Platz 31. Spielern mit dem Recht `titan.navigator.buildserver` MUSS er zusätzlich Build auf Platz 24 zeigen (siehe „Das Ziel Build erscheint nur mit Recht“). Die übrigen Plätze MÜSSEN mit grauen Glasscheiben gefüllt sein, die Rahmenplätze mit der Glasscheibe aus „Der Navigator hat einen Rahmen aus Glasscheiben“.

#### Scenario: Standardziele

- **WHEN** ein Spieler ohne das Recht `titan.navigator.buildserver` und ohne das Recht `titan.navigator.slender` den Navigator öffnet
- **THEN** sieht er den Titel „Navigator“, ElytraRace auf Platz 11, Survival auf Platz 20, Creative auf Platz 22 und Spawn auf Platz 31
- **AND** die Plätze 15 und 24 sind graue Glasscheiben

#### Scenario: Standardziele mit beiden Rechten

- **WHEN** ein Spieler mit den Rechten `titan.navigator.slender` und `titan.navigator.buildserver` den Navigator öffnet
- **THEN** sieht er ElytraRace auf Platz 11, Slender auf Platz 15, Survival auf Platz 20, Creative auf Platz 22, Build auf Platz 24 und Spawn auf Platz 31

#### Scenario: Weiterleitungsziele

- **WHEN** ein Spieler im Navigator Survival oder Creative anklickt
- **THEN** wird eine Weiterleitung zum Ziel „Survival“ bzw. „MemberBuild“ angestoßen

#### Scenario: Navigator-Werte in der Konfiguration werden ignoriert

- **WHEN** die `application.yaml` des Betreibers `navigator.title` sowie einen Eintrag `navigator.entries.parkour` mit Platz 3 setzt und die Lobby startet
- **THEN** startet die Lobby ohne Fehler, der Titel bleibt „Navigator“, und auf Platz 3 liegt eine Rahmen- oder graue Glasscheibe

#### Scenario: Navigator-Wert per Env-Variable wird ignoriert

- **WHEN** die Env-Variable `NAVIGATOR_ENTRIES_SURVIVAL_DESTINATION` auf `Lobby2` steht und ein Spieler Survival anklickt
- **THEN** wird eine Weiterleitung zum Ziel „Survival“ angestoßen

### Requirement: Slender liegt hinter dem Recht titan.navigator.slender

Das Ziel Slender MUSS an das Recht `titan.navigator.slender` gebunden sein, nach demselben Muster wie Build an `titan.navigator.buildserver`. Ergibt die Rechteprüfung beim Öffnen nicht „erteilt“ (also „verboten“ oder „nicht gesetzt“), DARF Slender für diesen Spieler nicht erscheinen, und auf Platz 15 MUSS eine graue Glasscheibe liegen. Ergibt sie „erteilt“, MUSS Slender auf Platz 15 erscheinen. Ein Klick auf Slender MUSS das Recht erneut prüfen. Ist es erteilt, leitet die Lobby nach den Regeln „Schnellbeitritt“ und „Klickzuordnung“ weiter; sonst DARF sie keine Weiterleitung anstoßen, MUSS den Navigator schließen und DARF dem Spieler keine Chat-Nachricht senden. Der Zustand des Rechts MUSS bei jedem Öffnen des Navigators ausgewertet werden, ohne Neustart der Lobby.

#### Scenario: Slender ohne Recht

- **WHEN** einem Spieler `titan.navigator.slender` nicht gesetzt oder verboten ist und er den Navigator öffnet
- **THEN** liegt auf Platz 15 eine graue Glasscheibe, und die übrigen Ziele sind unverändert

#### Scenario: Slender mit Recht

- **WHEN** einem Spieler `titan.navigator.slender` erteilt ist und er den Navigator öffnet
- **THEN** erscheint Slender auf Platz 15

#### Scenario: Recht wird zwischen Öffnen und Klick entzogen

- **WHEN** ein Spieler mit dem Recht den Navigator öffnet, ihm danach das Recht entzogen wird und er auf Slender klickt
- **THEN** wird keine Weiterleitung angestoßen, der Navigator schließt, und der Spieler erhält keine Chat-Nachricht

### Requirement: Das Ziel Build erscheint nur mit Recht

Der Navigator MUSS das Ziel Build auf Platz 24 zeigen, wenn die Rechteprüfung für `titan.navigator.buildserver` beim Öffnen „erteilt“ ergibt. Ergibt sie „verboten“ oder „nicht gesetzt“, MUSS der Navigator genau das Menü zeigen, das Spieler ohne dieses Ziel sehen: kein Eintrag, keine Lücke, kein Hinweis auf Build. Ein Klick auf Build MUSS das Recht erneut prüfen. Ist es erteilt, MUSS die Lobby den Spieler an das Weiterleitungsziel „Build“ übergeben und den Navigator schließen; sonst DARF sie keine Weiterleitung anstoßen, MUSS den Navigator schließen und DARF dem Spieler keine Chat-Nachricht senden. Welcher Server das Ziel „Build“ bedient, entscheidet die Cloud; die Lobby zeigt keine einzelnen Server. Für das Öffnen, egal wie oft und von wie vielen Spielern, DÜRFEN keine zusätzlichen Event-Listener oder dauerhaft gehaltenen Objekte entstehen.

#### Scenario: Spieler mit Recht sieht Build

- **WHEN** einem Spieler `titan.navigator.buildserver` erteilt ist und er den Navigator öffnet
- **THEN** liegt auf Platz 24 das Ziel Build, und die übrigen Ziele liegen unverändert auf ihren Plätzen

#### Scenario: Spieler ohne Recht sieht eine graue Glasscheibe

- **WHEN** einem Spieler `titan.navigator.buildserver` nicht gesetzt oder verboten ist und er den Navigator öffnet
- **THEN** liegt auf Platz 24 eine graue Glasscheibe

#### Scenario: Kein Durchsickern bei wiederholtem Öffnen

- **WHEN** ein Spieler mit Recht und ein Spieler ohne Recht den Navigator abwechselnd 50 Mal öffnen und schließen
- **THEN** sieht jeder bei jedem Öffnen sein eigenes Menü, der Spieler ohne Recht nie Build, und die Anzahl angemeldeter Event-Listener ist unverändert

### Requirement: Modus-Einträge erklären die Klicks

Jeder Modus-Eintrag des Navigators MUSS seine Klickaktionen als Lore-Zeilen zeigen, in dieser Reihenfolge: eine Trennlinie in der Verlaufsfarbe des Modus, die Spielerzeile (sofern die Spielerzahl bekannt ist), eine Leerzeile und dann je Klick eine eigene Zeile. Ein Minispiel (ElytraRace, Slender) zeigt die Zeilen „Linksklick / Schnell beitreten“ und „Rechtsklick / Server wählen“. Ein Server (Survival, Creative, Build) zeigt die Zeile „Linksklick / Verbinden“. Die Portal-Zeile „Shift-Klick / Zum Portal laufen“ steht nur, wenn für das Ziel ein Portal in der Lobby existiert, und zuletzt. Der Rechtsklick auf einen Server hat keine eigene Zeile. Der Spawn-Eintrag DARF KEINE dieser Zeilen zeigen.

Die Anzeige folgt dem Snapshot der letzten abgeschlossenen Aktualisierung, wie bisher. Die Portal-Zeile folgt dagegen dem Stand beim Öffnen, und der Klick selbst prüft das Portal beim Klick.

#### Scenario: Minispiel mit Portal

- **WHEN** ein Spieler den Navigator öffnet und für ElytraRace ein Portal in der Lobby existiert
- **THEN** zeigt der Eintrag ElytraRace die Zeilen „Linksklick / Schnell beitreten“, „Rechtsklick / Server wählen“ und „Shift-Klick / Zum Portal laufen“ untereinander

#### Scenario: Minispiel ohne Portal

- **WHEN** ein Spieler den Navigator öffnet und für Slender kein Portal existiert
- **THEN** zeigt der Eintrag Slender die Zeilen „Linksklick / Schnell beitreten“ und „Rechtsklick / Server wählen“ und keine Shift-Zeile

#### Scenario: Server mit Portal

- **WHEN** ein Spieler den Navigator öffnet und für Survival ein Portal existiert
- **THEN** zeigt der Eintrag Survival die Zeilen „Linksklick / Verbinden“ und „Shift-Klick / Zum Portal laufen“

#### Scenario: Server ohne Portal

- **WHEN** ein Spieler den Navigator öffnet und für Survival kein Portal existiert
- **THEN** zeigt der Eintrag Survival nur die Zeile „Linksklick / Verbinden“

#### Scenario: Portal seit dem letzten Zyklus hinzugekommen

- **WHEN** ein Portal für Survival nach dem letzten Aktualisierungszyklus angelegt wird und ein Spieler den Navigator danach öffnet, bevor der nächste Zyklus läuft
- **THEN** zeigt der Eintrag Survival sofort die Zeile „Shift-Klick / Zum Portal laufen“

#### Scenario: Spawn ohne Hinweis

- **WHEN** ein Spieler den Navigator öffnet
- **THEN** hat der Spawn-Eintrag keine Klickzeile

### Requirement: Klickhinweis ist übersetzt

Jede Lore-Zeile des Navigators MUSS in Deutsch oder Englisch erscheinen, je nach Sprache des Spielers, und DARF nie leer sein. Die Schlüssel für Tasten, Aktionen, die Spielerzeile und die Serverauswahl MÜSSEN in beiden Bundles stehen. Unbekannte Sprachen MÜSSEN Englisch zeigen.

#### Scenario: Deutsche Texte

- **WHEN** ein Spieler mit deutschem Client den Navigator öffnet
- **THEN** zeigt der Eintrag ElytraRace die Zeilen „▸ Linksklick  Schnell beitreten“ und „▸ Rechtsklick  Server wählen“, und keine Zeile ist leer

#### Scenario: Unbekannte Sprache

- **WHEN** ein Spieler mit einer Sprache ohne Bundle den Navigator öffnet
- **THEN** zeigt der Eintrag die englischen Texte „Left click  Quick join“ und „Right click  Choose a server“

#### Scenario: Spielerzeile in beiden Sprachen

- **WHEN** die Spielerzahl eines Ziels bekannt ist
- **THEN** steht „Spieler“ bzw. „Players“ mit der Zahl und, wenn das Maximum bekannt ist, „/ Maximum“ vor den Klickzeilen

## ADDED Requirements

### Requirement: Nur benannte Klicks wirken

Der Navigator und seine Serverauswahl MÜSSEN jeden Klick abbrechen. Gewirkt wird nur bei Links- und Rechtsklick, mit oder ohne Shift. Jeder andere Klicktyp (Zahlentasten, Q, F, Mittelklick, Doppelklick, Ziehen) DARF weder schnell beitreten, eine Serverauswahl öffnen, verbinden noch zum Portal laufen; das Inventar MUSS dabei offen bleiben. Das gilt für Minispiele, Server und die Einträge der Serverauswahl.

#### Scenario: Zahlentaste auf einem Minispiel

- **WHEN** ein Spieler auf ElytraRace die Zahlentaste drückt
- **THEN** passiert nichts: keine Übergabe, keine Serverauswahl, und der Navigator bleibt offen

#### Scenario: Q auf einem Server

- **WHEN** ein Spieler auf Survival Q drückt
- **THEN** wird niemand übergeben, und der Navigator bleibt offen

#### Scenario: Mittelklick auf einen Eintrag der Serverauswahl

- **WHEN** ein Spieler in der Serverauswahl mit dem Mittelklick auf einen Dienst klickt
- **THEN** wird er nicht übergeben, und die Serverauswahl bleibt offen

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
- **THEN** öffnet sich die Serverauswahl, die leer ist und in der Mitte „Kein Server läuft gerade.“ zeigt

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
- **THEN** zeigt sie in der Mitte einen Eintrag „Kein Server läuft gerade.“, der beim Klick nichts tut

#### Scenario: Gewählter Server wird beim Klick neu gelesen

- **WHEN** `ElytraRace-1` beim Öffnen bereit mit 3 von 50 Spielern gelistet war, danach aber mit 50 von 50 Spielern voll gemeldet wird, und der Spieler es anklickt
- **THEN** wird der Spieler nicht übergeben, die Serverauswahl bleibt offen, und er erhält im Chat „Servers » Dieser Server ist voll.“

#### Scenario: Gewählter Server läuft inzwischen nicht mehr

- **WHEN** ein Spieler einen Dienst aus der Serverauswahl anklickt, der seit dem Öffnen nicht mehr läuft
- **THEN** wird der Spieler nicht übergeben, und er erhält im Chat „Servers » Dieser Server ist noch nicht bereit.“

### Requirement: Menü-Varianten bleiben acht geteilte Inventare

Der Navigator MUSS je Kombination aus Rechten und Sprache genau ein geteiltes Inventar haben, beim Start angelegt, mit dem Titel in dieser Sprache. Die Serverauswahl MUSS je Ziel und Sprache ein geteiltes Inventar haben. Das Öffnen DARF KEIN neues Inventar und keinen neuen Listener erzeugen.

#### Scenario: Titel je Sprache

- **WHEN** ein Spieler mit deutschem Client den Navigator öffnet
- **THEN** zeigt das Inventar den Titel „Navigator“ im Verlauf und die deutschen Zeilen, und ein Spieler mit englischem Client sieht das zweite Inventar mit englischem Titel und englischen Zeilen

#### Scenario: Häufiges Öffnen der Serverauswahl

- **WHEN** ein Spieler die Serverauswahl 50 Mal öffnet und wieder schließt
- **THEN** ist die Anzahl angemeldeter Event-Listener unverändert

#### Scenario: Stoppen entfernt jede geöffnete Serverauswahl

- **WHEN** das Navigator-Modul gestoppt wird, nachdem Serverauswahlen auf Deutsch und Englisch geöffnet wurden
- **THEN** hat keine dieser Serverauswahlen mehr einen Klick-Listener
