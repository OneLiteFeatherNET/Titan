# Spec Delta

## RENAMED Requirements

- FROM: `### Requirement: Menü-Varianten bleiben zweiunddreißig geteilte Inventare`
- TO: `### Requirement: Menü-Varianten bleiben sechsundneunzig geteilte Inventare`

## MODIFIED Requirements

### Requirement: Navigator-Ziele sind im Navigator-Modul festgelegt

Titel und Ziele des Navigators MÜSSEN fest im Navigator-Modul festgelegt sein. Die Lobby DARF Titel und Ziele NICHT aus der Konfiguration lesen, und andere Module DÜRFEN KEINE Ziele beisteuern. Werte unter `navigator.*` aus Dateien, Profilen, Env-Variablen oder System-Properties DÜRFEN den Titel, die Ziele und die Plätze NICHT beeinflussen; ausgenommen sind die Zeitwerte `navigator.refreshSeconds` und `navigator.stagingIdleSeconds`. Der Navigator MUSS einen Titel „Navigator“ im Verlauf zeigen, der in der Sprache des Spielers übersetzt ist, und eine Anzeige mit vier Reihen zu je neun Plätzen. ElytraRace MUSS auf Platz 11 liegen, Survival auf Platz 20, Creative auf Platz 22, Spawn mit einem Kompass auf Platz 31. Bounce auf Platz 13 erscheint nur Spielern mit dem Recht `titan.navigator.bounce`. Tamias auf Platz 14 erscheint nur Spielern mit dem Recht `titan.navigator.tamias` (siehe „Tamias ist ein Minispiel hinter dem Recht titan.navigator.tamias“). Slender auf Platz 15 erscheint nur Spielern mit dem Recht `titan.navigator.slender`. Auf Platz 16 MUSS der Eintrag „Staging“ liegen, der nur Spielern mit mindestens einem Staging-Recht erscheint (siehe „Staging-Einträge liegen im Untermenü „Staging““). Spielern mit dem Recht `titan.navigator.buildserver` MUSS er zusätzlich Build auf Platz 24 zeigen (siehe „Das Ziel Build erscheint nur mit Recht“). Die übrigen Plätze MÜSSEN mit grauen Glasscheiben gefüllt sein, die Rahmenplätze mit der Glasscheibe aus „Der Navigator hat einen Rahmen aus Glasscheiben“.

#### Scenario: Standardziele

- **WHEN** ein Spieler ohne die Rechte `titan.navigator.buildserver`, `titan.navigator.slender`, `titan.navigator.bounce`, `titan.navigator.tamias` und ohne ein Staging-Recht den Navigator öffnet
- **THEN** sieht er den Titel „Navigator“, ElytraRace auf Platz 11, Survival auf Platz 20, Creative auf Platz 22 und Spawn auf Platz 31
- **AND** die Plätze 13, 14, 15, 16 und 24 sind graue Glasscheiben

#### Scenario: Standardziele mit Rechten

- **WHEN** ein Spieler mit den Rechten `titan.navigator.slender`, `titan.navigator.bounce`, `titan.navigator.tamias`, `titan.navigator.buildserver` und `bounce.staging.join` den Navigator öffnet
- **THEN** sieht er ElytraRace auf Platz 11, Bounce auf Platz 13, Tamias auf Platz 14, Slender auf Platz 15, Staging auf Platz 16, Survival auf Platz 20, Creative auf Platz 22, Build auf Platz 24 und Spawn auf Platz 31

#### Scenario: Weiterleitungsziele

- **WHEN** ein Spieler im Navigator Survival oder Creative anklickt
- **THEN** wird eine Weiterleitung zum Ziel „Survival“ bzw. „MemberBuild“ angestoßen

#### Scenario: Navigator-Werte in der Konfiguration werden ignoriert

- **WHEN** die `application.yaml` des Betreibers `navigator.title` sowie einen Eintrag `navigator.entries.parkour` mit Platz 3 setzt und die Lobby startet
- **THEN** startet die Lobby ohne Fehler, der Titel bleibt „Navigator“, und auf Platz 3 liegt eine Glasscheibe des Rahmens

#### Scenario: Navigator-Wert per Env-Variable wird ignoriert

- **WHEN** die Env-Variable `NAVIGATOR_ENTRIES_SURVIVAL_DESTINATION` auf `Lobby2` steht und ein Spieler Survival anklickt
- **THEN** wird eine Weiterleitung zum Ziel „Survival“ angestoßen

### Requirement: Slender (Staging) liegt hinter dem Recht cygnus.staging.join

Das Ziel Slender (Staging) MUSS an das Recht `cygnus.staging.join` gebunden sein, dasselbe Recht, das der Proxy für den Beitritt zum Staging prüft. Es MUSS im Untermenü „Staging“ liegen (siehe „Staging-Einträge liegen im Untermenü „Staging““), mit einem Strukturblock als Symbol und dem Namen „Slender (Staging)“ in einem Verlauf von `#616161` nach `#ffaa00`. Ergibt die Rechteprüfung beim Öffnen des Untermenüs nicht „erteilt“, DARF Slender (Staging) nicht erscheinen, und sein Platz MUSS eine graue Glasscheibe zeigen. Der Eintrag MUSS nach seinen Klickzeilen eine Leerzeile und die Zeile „Testumgebung – nur fürs Team“ zeigen. Abweichend von „Modus-Einträge erklären die Klicks“ zeigt er die Klickzeile „▸ Linksklick  Starten und verbinden“ und die Zeile „Startet bei Bedarf“ (siehe „Staging-Ziele starten bei Bedarf“). Ein Klick auf Slender (Staging) MUSS das Recht erneut prüfen. Ist es erteilt, MUSS die Lobby den Spieler an den CloudNet-Task `Slender-Staging` übergeben, und zwar über den Ablauf „Staging-Ziele starten bei Bedarf“, ohne Schnellbeitritt und ohne Serverauswahl; sonst DARF sie keine Weiterleitung anstoßen, MUSS den Navigator schließen und DARF dem Spieler keine Chat-Nachricht senden. Slender (Staging) ist kein Minispiel: Der Schnellbeitritt und die Serverauswahl von Slender lesen nur die Dienste des Tasks `Slender`, und die Dienste eines Tasks MÜSSEN über den genauen Tasknamen gelesen werden, nicht über einen Namenspräfix.

#### Scenario: Staging ohne Recht

- **WHEN** ein Spieler ohne `cygnus.staging.join` den Navigator öffnet und das Untermenü nicht erreichen kann
- **THEN** liegt auf Platz 16 keine Staging-Schaltfläche, sondern eine graue Glasscheibe, und die übrigen Ziele sind unverändert

#### Scenario: Staging mit Recht

- **WHEN** ein Spieler mit `cygnus.staging.join` das Untermenü öffnet und auf Platz 0 links klickt, während `Slender-Staging` keinen Dienst hat
- **THEN** wird der Task `Slender-Staging` gestartet, der Spieler sieht die Aktionsleiste „startet“, und er wird verbunden, sobald der Dienst bereit ist

#### Scenario: Staging nach Entzug des Rechts

- **WHEN** ein Spieler das Recht verliert, während das Untermenü offen ist, und danach auf Slender (Staging) klickt
- **THEN** wird niemand weitergeleitet, nichts gestartet, der Navigator schließt sich, und dem Spieler wird keine Chat-Nachricht gesendet

#### Scenario: Slender sieht den Staging-Dienst nicht

- **WHEN** `Slender-Staging-1` als einziger Dienst freie Plätze hat und ein Spieler mit dem Recht `titan.navigator.slender` auf Slender links klickt oder dessen Serverauswahl öffnet
- **THEN** wird niemand an `Slender-Staging-1` übergeben, und die Serverauswahl von Slender zeigt `Slender-Staging-1` nicht

### Requirement: Menü-Varianten bleiben sechsundneunzig geteilte Inventare

_The navigator MUST keep one shared inventory per permission key and language, and the server list one per destination, language, order, page and row count; opening, paging and sorting MUST NOT create a new inventory or listener once it exists._

Der Navigator MUSS je Schlüssel und Sprache genau ein geteiltes Inventar haben, beim Start angelegt, mit dem Titel in dieser Sprache. Das Hauptmenü hat den Schlüssel aus den Rechten `titan.navigator.slender`, `titan.navigator.bounce`, `titan.navigator.tamias` und `titan.navigator.buildserver` sowie dem abgeleiteten Merkmal „mindestens ein Staging-Recht“. Das ergibt zweiunddreißig Menüs je Sprache, also vierundsechzig. Das Untermenü „Staging“ hat den Schlüssel aus den Rechten `cygnus.staging.join`, `bounce.staging.join`, `elytrarace.staging.join` und `tamias.staging.join`. Das ergibt sechzehn Menüs je Sprache, also zweiunddreißig. Insgesamt MÜSSEN es sechsundneunzig Inventare sein. Die Serverauswahl MUSS je Ziel, Sprache, Ordnung, Seite und Zeilenzahl höchstens ein geteiltes Inventar haben. Ein Serverauswahl-Inventar entsteht, wenn seine Ansicht zum ersten Mal gebraucht wird, und wird danach wiederverwendet. Das Öffnen, Blättern und Sortieren DARF, wenn das Inventar schon existiert, KEIN neues Inventar und keinen neuen Listener erzeugen.

#### Scenario: Zweiunddreißig Inventare beim Start

- **WHEN** das Navigator-Modul startet
- **THEN** existieren je Sprache zweiunddreißig Hauptmenü-Inventare, und zusammen mit dem Untermenü mit je sechzehn Inventaren sind es sechsundneunzig; jedes trägt die Ziele, die seinem Schlüssel entsprechen

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

#### Scenario: Hub nur bei Staging-Recht

- **WHEN** ein Spieler ohne Staging-Recht, aber mit `titan.navigator.bounce` den Navigator öffnet
- **THEN** zeigt sein Hauptmenü Bounce auf Platz 13 und eine graue Glasscheibe auf Platz 16, und sonst ist das Menü identisch mit dem Menü eines Spielers ohne Bounce-Recht

#### Scenario: Häufiges Öffnen des Navigators

- **WHEN** Spieler das Hauptmenü und das Untermenü wiederholt öffnen und schließen
- **THEN** wird kein neues Inventar und kein neuer Listener angelegt

## ADDED Requirements

### Requirement: Tamias ist ein Minispiel hinter dem Recht titan.navigator.tamias

Der Navigator MUSS den Eintrag Tamias auf Platz 14 zeigen, und zwar nur Spielern mit dem Recht `titan.navigator.tamias`. Ohne das Recht MUSS auf Platz 14 eine graue Glasscheibe liegen. Tamias MUSS den CloudNet-Task `Tamias` ansprechen, den Namen „Tamias“ ohne Übersetzung zeigen und ein Symbol und einen Verlauf aus Vanilla-Items verwenden, ohne Ressourcenpaket. Tamias gilt als Minispiel: Schnellbeitritt, Serverauswahl, Klickzeilen, Portal-Zeile und Spielerzahl folgen den Regeln von „Modus-Einträge erklären die Klicks“, „Schnellbeitritt tritt der besten bereiten Runde bei“ und „Serverauswahl im Stil des Navigators“. Schnellbeitritt und Serverauswahl von Tamias lesen nur die Dienste des Tasks `Tamias`, nicht die Dienste von `Tamias-Staging`.

#### Scenario: Tamias mit Recht

- **WHEN** ein Spieler mit `titan.navigator.tamias` den Navigator öffnet und auf Platz 14 links klickt
- **THEN** zeigt Platz 14 den Eintrag Tamias, und der Spieler wird dem besten bereiten Dienst des Tasks `Tamias` zugeordnet

#### Scenario: Tamias ohne Recht

- **WHEN** ein Spieler ohne `titan.navigator.tamias` den Navigator öffnet
- **THEN** liegt auf Platz 14 eine graue Glasscheibe, und ein Klick darauf löst nichts aus

#### Scenario: Staging-Dienst erscheint nicht im Tamias-Schnellbeitritt

- **WHEN** `Tamias-Staging-1` als einziger Dienst freie Plätze hat und ein Spieler mit `titan.navigator.tamias` auf Tamias links klickt
- **THEN** wird niemand an `Tamias-Staging-1` übergeben

### Requirement: Staging-Einträge liegen im Untermenü „Staging“

Der Navigator MUSS auf Platz 16 den Eintrag „Staging“ zeigen, und zwar nur Spielern mit mindestens einem der Rechte `cygnus.staging.join`, `bounce.staging.join`, `elytrarace.staging.join` oder `tamias.staging.join`. Ohne eines dieser Rechte MUSS auf Platz 16 eine graue Glasscheibe liegen. Ein Klick auf „Staging“ MUSS das Untermenü öffnen. Das Untermenü MUSS genau neun Plätze haben: Slender (Staging) auf Platz 0, Bounce (Staging) auf Platz 1, ElytraRace (Staging) auf Platz 2, Tamias (Staging) auf Platz 3 und auf Platz 8 eine Zurück-Taste, die in den Navigator führt. Die übrigen Plätze MÜSSEN graue Glasscheiben sein. Ein Staging-Eintrag, für den das Recht fehlt, MUSS auf seinem Platz eine graue Glasscheibe zeigen. Jeder Staging-Eintrag MUSS die Klickzeile „▸ Linksklick  Starten und verbinden“ und die Zeile „Startet bei Bedarf“ zeigen. Er hat keinen Schnellbeitritt und keine Serverauswahl. Jeder Klick auf einen Staging-Eintrag MUSS das Recht des Eintrags erneut prüfen.

#### Scenario: Untermenü mit einem Staging-Recht

- **WHEN** ein Spieler nur `bounce.staging.join` hat und auf Platz 16 klickt
- **THEN** öffnet sich das Untermenü, Bounce (Staging) liegt auf Platz 1, die übrigen Staging-Plätze sind graue Glasscheiben, und auf Platz 8 liegt die Zurück-Taste

#### Scenario: Zurück in den Navigator

- **WHEN** ein Spieler im Untermenü auf die Zurück-Taste klickt
- **THEN** öffnet sich der Navigator mit den Zielen, die zu seinen Rechten passen

#### Scenario: Recht während des Untermenüs entzogen

- **WHEN** ein Spieler das Recht `bounce.staging.join` verliert, während das Untermenü offen ist, und danach auf Platz 1 klickt
- **THEN** wird niemand gestartet oder verbunden, und der Spieler erhält keine Chat-Nachricht

### Requirement: Staging-Ziele starten bei Bedarf

Klickt ein Spieler mit dem Recht eines Staging-Eintrags auf diesen Eintrag, MUSS Titan folgendermaßen vorgehen. Ist ein Dienst des Tasks bereit, MUSS Titan den Spieler sofort über den Task verbinden. Ein Dienst ist bereit, wenn er läuft und ein Spielerlimit angekündigt hat. Gibt es keinen Dienst im Zustand „startet“ oder „bereit“, MUSS Titan über die CloudNet-API genau einen Dienst des Tasks anlegen oder starten. Dem Spieler MUSS Titan dabei die Aktionsleiste „Die Umgebung „{Name}“ startet – du wirst verbunden, sobald sie bereit ist.“ zeigen, auf Englisch „Starting „{Name}“ – you will be connected once it is ready.“ je nach Sprache des Spielers. Sind die Dienste des Tasks nicht lesbar, MUSS die Ablehnung aus „Unlesbare Dienste werden im Chat abgelehnt“ gelten, und es DARF KEIN Start stattfinden.

#### Scenario: Erster Klick startet den Dienst

- **WHEN** ein Spieler mit `elytrarace.staging.join` auf ElytraRace (Staging) klickt, und `ElytraRace-Staging` hat keinen Dienst
- **THEN** wird genau ein Dienst von `ElytraRace-Staging` angelegt oder gestartet, der Spieler sieht die Aktionsleiste „startet“, und er wird nicht sofort verbunden

#### Scenario: Bereiter Dienst wird sofort genutzt

- **WHEN** ein Spieler mit `tamias.staging.join` auf Tamias (Staging) klickt, und `Tamias-Staging-1` läuft und hat ein Spielerlimit angekündigt
- **THEN** wird der Spieler über den Task `Tamias-Staging` verbunden, und es wird kein Dienst gestartet

#### Scenario: Englische Aktionsleiste

- **WHEN** ein Spieler mit englischer Sprache auf einen Staging-Eintrag klickt, der einen Start auslöst
- **THEN** zeigt die Aktionsleiste „Starting „Bounce (Staging)“ – you will be connected once it is ready.“

#### Scenario: Unlesbare Dienste verhindern den Start

- **WHEN** ein Spieler auf Bounce (Staging) klickt und die Dienste des Tasks nicht gelesen werden können
- **THEN** wird kein Dienst angelegt oder gestartet, und der Spieler erhält die Ablehnung im Chat

### Requirement: Staging-Start wartet auf Bereitschaft und endet nach 90 Sekunden

Titan MUSS die Bereitschaft eines gestarteten Dienstes beim nächsten Aktualisierungszyklus des Navigators prüfen, nicht in einem eigenen Takt. Wird der Dienst bereit, MUSS Titan jeden wartenden Spieler verbinden, der noch online ist und das Recht noch hat. Wer das Recht verloren hat, wird ohne Chat-Nachricht aus der Warteliste gestrichen. Wird der Dienst nicht innerhalb von 90 Sekunden seit dem Start bereit, MUSS Titan jedem Wartenden die Ablehnung „Servers » Die Umgebung „{Name}“ ist nicht rechtzeitig bereit. Versuch es später noch einmal.“ senden, die Warteliste leeren und den Dienst NICHT stoppen. Verliert ein wartender Spieler die Verbindung, MUSS Titan ihn aus der Warteliste entfernen; der Dienst läuft weiter. Ein erneuter Klick desselben Spielers DARF KEINEN zweiten Start auslösen; er erhält den Wartehinweis erneut.

#### Scenario: Dienst wird während des Wartens bereit

- **WHEN** zwei Spieler auf `Slender-Staging` warten und der Dienst beim nächsten Aktualisierungszyklus bereit ist
- **THEN** werden beide über den Task `Slender-Staging` verbunden

#### Scenario: Zeitlimit überschritten

- **WHEN** ein Spieler wartet und 91 Sekunden nach dem Start der Dienst noch nicht bereit ist
- **THEN** erhält der Spieler die Ablehnung „nicht rechtzeitig bereit“, und der Dienst wird nicht gestoppt

#### Scenario: Spieler verlässt die Lobby während des Wartens

- **WHEN** ein wartender Spieler die Verbindung verliert, bevor der Dienst bereit ist
- **THEN** wird er aus der Warteliste entfernt, der Dienst läuft weiter, und bei Bereitschaft wird niemand für ihn verbunden

#### Scenario: Zweiter Klick während des Wartens

- **WHEN** ein wartender Spieler erneut auf den Staging-Eintrag klickt
- **THEN** wird kein zweiter Start ausgelöst, und er erhält den Wartehinweis erneut

#### Scenario: Recht während des Wartens entzogen

- **WHEN** ein wartender Spieler das Recht verliert, bevor der Dienst bereit ist
- **THEN** wird er bei Bereitschaft nicht verbunden, und er erhält keine Chat-Nachricht

### Requirement: Staging-Starts laufen nicht doppelt

Ein Staging-Task gilt als „startet“, wenn einer seiner Dienste im CloudNet-Zustand `PREPARED` steht oder ein Zustand, den Titan nicht kennt. Solange ein Dienst startet oder läuft, DARF Titan für denselben Task keinen weiteren Dienst anlegen. Nach einem eigenen Start DARF Titan für 30 Sekunden keinen neuen Start für denselben Task auslösen, auch wenn CloudNet den neuen Dienst noch nicht zeigt. Zwischen Lobbys besteht keine Sperre: Zwei Klicks auf denselben Task in zwei Lobbys im selben Lesefenster DÜRFEN zu zwei Diensten führen. Der Leerlauf-Stopp MUSS den überzähligen Dienst danach beenden.

#### Scenario: Dienst startet bereits

- **WHEN** ein Spieler auf `Bounce-Staging` klickt und `Bounce-Staging-1` im Zustand `PREPARED` steht
- **THEN** wird kein Dienst angelegt, und der Spieler wartet auf `Bounce-Staging-1`

#### Scenario: Cooldown nach eigenem Start

- **WHEN** ein Spieler auf `Tamias-Staging` klickt, Titan einen Dienst gestartet hat und 10 Sekunden später ein zweiter Spieler klickt, während CloudNet den Dienst noch nicht zeigt
- **THEN** wird kein zweiter Dienst angelegt, und der zweite Spieler wartet

#### Scenario: Unbekannter Zustand zählt als startend

- **WHEN** ein Dienst im Zustand steht, den Titan nicht kennt
- **THEN** wird kein Start ausgelöst

#### Scenario: Zwei Lobbys im selben Lesefenster

- **WHEN** Spieler auf zwei Lobbys im selben Lesefenster auf `ElytraRace-Staging` klicken
- **THEN** dürfen zwei Dienste entstehen, und der Leerlauf-Stopp beendet den überzähligen Dienst, sobald er leer ist

### Requirement: Staging-Dienste stoppen nach Leerlauf

Titan MUSS einen Staging-Dienst stoppen, der die Dauer `navigator.stagingIdleSeconds` ohne Spieler gelaufen ist. Die Dauer MUSS aus dem injizierten Uhr-Dienst kommen. Sie beginnt, wenn der Dienst erstmals ohne Spieler gelesen wird, und wird zurückgesetzt, sobald ein Spieler auf dem Dienst ist. Vor dem Stopp MUSS Titan den Dienst frisch lesen und nur stoppen, wenn er dann noch leer und laufend ist. Ein Lesefehler DARF NICHT als leer zählen; er stoppt nichts und lässt die Dauer nicht weiterlaufen. Der Stopp MUSS idempotent sein: Ein zweiter Stopp desselben Dienstes, etwa von der zweiten Lobby, ist ein No-op ohne Fehler. Ein Dienst, der nicht läuft, DARF Titan über den Leerlauf nicht stoppen.

#### Scenario: Leerlauf erreicht

- **WHEN** `Bounce-Staging-1` 600 Sekunden lang ohne Spieler läuft
- **THEN** stoppt Titan den Dienst, und der Stopp wird einmal geloggt

#### Scenario: Leerlauf noch nicht erreicht

- **WHEN** `Bounce-Staging-1` 599 Sekunden lang ohne Spieler läuft
- **THEN** stoppt Titan den Dienst nicht

#### Scenario: Spieler setzt die Dauer zurück

- **WHEN** `Bounce-Staging-1` 500 Sekunden leer war und dann ein Spieler darauf verbunden wird, der 10 Minuten später wieder geht
- **THEN** beginnt die Dauer erst nach dem Weggang des Spielers neu

#### Scenario: Lesefehler stoppt nichts

- **WHEN** die Dienste eines Staging-Tasks für eine Weile nicht lesbar sind
- **THEN** stoppt Titan nichts, und die Dauer läuft in dieser Zeit nicht weiter

#### Scenario: Zwei Lobbys stoppen denselben Dienst

- **WHEN** zwei Lobbys denselben leeren Dienst nacheinander stoppen wollen
- **THEN** wird der Dienst einmal gestoppt, der zweite Stopp ist ohne Fehler und ohne Log-Zeile

### Requirement: Nur als Staging markierte Tasks werden gestoppt

Titan DARF ausschließlich Dienste der Tasks `Slender-Staging`, `Bounce-Staging`, `ElytraRace-Staging` und `Tamias-Staging` stoppen. Diese Markierung MUSS im Code stehen. Werte unter `navigator.*` aus Dateien, Profilen, Env-Variablen oder System-Properties DÜRFEN die Markierung NICHT ändern. Die Live-Tasks `Slender`, `Bounce`, `ElytraRace`, `Tamias`, `Survival`, `Creative` und `Build` DÜRFEN NIE gestoppt werden, auch nicht bei Leerlauf.

#### Scenario: Live-Task ist leer

- **WHEN** `Tamias` zwei Stunden lang ohne Spieler läuft
- **THEN** stoppt Titan den Dienst nicht

#### Scenario: Konfiguration will einen Live-Task markieren

- **WHEN** die `application.yaml` des Betreibers einen Eintrag setzt, der `Survival` als Staging markiert
- **THEN** startet die Lobby ohne Fehler, und `Survival` wird nie gestoppt

### Requirement: Leerlaufzeit ist konfigurierbar

Der Wert `navigator.stagingIdleSeconds` MUSS aus der Standarddatei des Navigators gelesen werden, mit dem Standard 600 Sekunden und einem zulässigen Bereich von 60 bis 86400 ganzen Sekunden. Ein Wert außerhalb des Bereichs oder ein Text MUSS den Start des Navigators abbrechen, wie es `lobbyswitcher.refreshSeconds` tut. Der Wert ändert nur die Leerlaufzeit; Titel, Ziele und Plätze bleiben fest.

#### Scenario: Standardwert

- **WHEN** der Betreiber `navigator.stagingIdleSeconds` nicht setzt
- **THEN** beträgt die Leerlaufzeit 600 Sekunden

#### Scenario: Wert außerhalb des Bereichs

- **WHEN** `navigator.stagingIdleSeconds` auf `59` gesetzt ist und die Lobby startet
- **THEN** bricht der Start des Navigators mit einem Fehler ab

#### Scenario: Text statt Zahl

- **WHEN** `navigator.stagingIdleSeconds` auf `zehn` gesetzt ist und die Lobby startet
- **THEN** bricht der Start des Navigators mit einem Fehler ab

### Requirement: Start- und Stopp-Anfragen sind telemetriert

Der Navigator MUSS den Zähler `titan.navigator.staging.starts` (Einheit `{start}`), den Zähler `titan.navigator.staging.stops` (Einheit `{stop}`) und das Histogramm `titan.navigator.staging.wait` (Einheit `s`) führen. Alle tragen das Attribut `destination` mit dem Namen des Staging-Eintrags. Die Zähler für Starts und Stopps tragen das Attribut `result`, das Histogramm trägt `result`, und die Werte sind fest vorgegeben: `requested`, `skipped_starting`, `cooldown`, `failed` für Starts; `requested`, `failed` für Stopps; `ready`, `timeout`, `left` für die Wartezeit. Jede angenommene Start- und Stopp-Anfrage MUSS einmal auf INFO mit Task- und Dienstnamen geloggt werden, ohne Spielernamen. Ein Start, der das Zeitlimit überschreitet, und ein fehlgeschlagener Stopp MÜSSEN auf WARN geloggt werden. Spielernamen, UUIDs und freier Text DÜRFEN NICHT als Attribut erscheinen.

#### Scenario: Angenommener Start

- **WHEN** ein Spieler einen Start auslöst und Titan die Anfrage annimmt
- **THEN** erhöht sich `titan.navigator.staging.starts` mit `result=requested` um eins, und die INFO-Zeile nennt den Task und den Dienst

#### Scenario: Zeitlimit überschritten

- **WHEN** ein Start 90 Sekunden lang nicht bereit wird
- **THEN** nimmt das Histogramm `titan.navigator.staging.wait` einen Wert mit `result=timeout` auf, und eine WARN-Zeile wird geschrieben

#### Scenario: Stopp durch Leerlauf

- **WHEN** Titan einen leeren Dienst stoppt
- **THEN** erhöht sich `titan.navigator.staging.stops` mit `result=requested` um eins, und die INFO-Zeile nennt den Task und den Dienst
