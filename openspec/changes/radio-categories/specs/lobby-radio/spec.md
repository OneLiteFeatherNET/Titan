# Spec Delta

## RENAMED Requirements

- FROM: `### Requirement: Das Radio öffnet sich über ein Hotbar-Item mit zwei Menüs`
- TO: `### Requirement: Das Radio öffnet sich über ein Hotbar-Item mit der Kategorieübersicht`
- FROM: `### Requirement: Grenzen begrenzen Paketzahl und Speicher`
- TO: `### Requirement: Die Paketzahl je Spieler und Tick ist begrenzt`

## MODIFIED Requirements

### Requirement: Das Radio öffnet sich über ein Hotbar-Item mit der Kategorieübersicht
_The radio is one hotbar item, always there; it opens the category overview, and every category opens its own list._

Die Lobby MUSS das Item `titan:radio` (Material `JUKEBOX`) auf Hotbar-Platz 5 anbieten, immer, auch ohne `radio/`. Ein Klick auf das Item MUSS die Kategorieübersicht öffnen, und zwar immer auf der Übersicht, unabhängig davon, wo der Spieler beim letzten Mal war. Die Übersicht MUSS je Kategorie mit Titeln ein Symbol zeigen. Ein Klick auf eine Kategorie MUSS ihre Titelliste öffnen, mit einem Zurück-Knopf zur Übersicht. Der Umschalter „Eigene Songs“ entfällt; die Vanilla-Platten sind die Kategorie „Minecraft“.

#### Scenario: Öffnen ohne MIDI-Dateien

- **WHEN** ein Spieler das Radio-Item benutzt und im Verzeichnis `radio/` keine gültige Datei liegt
- **THEN** sieht er die Übersicht mit der Kategorie „Minecraft“ und keiner weiteren Kategorie außer „Sonstige“, falls sie Titel hat

#### Scenario: Kategorie öffnen und zurück

- **WHEN** ein Spieler in der Übersicht die Kategorie „Minecraft“ anklickt und danach den Zurück-Knopf
- **THEN** sieht er zuerst die 22 Vanilla-Platten in Menüreihenfolge, danach wieder die Übersicht

#### Scenario: Immer die Übersicht beim Öffnen

- **WHEN** ein Spieler auf der Seite 3 der Kategorie „Anime“ war, das Radio schließt und es über das Hotbar-Item wieder öffnet
- **THEN** sieht er die Übersicht und nicht die Seite 3

### Requirement: Der Modusknopf zeigt den Modus des Spielers und schaltet ihn um
_The mode button shows the player's own mode and changes it in place; the window stays open._

Der Modusknopf MUSS den aktuellen Modus des Spielers zeigen. Ein Klick MUSS den Modus des Spielers zyklisch weiterschalten (Einmal, Schleife, Zufall) und die Anzeige im offenen Fenster sofort aktualisieren. Das Fenster DARF bei einem Moduswechsel NICHT neu geöffnet werden.

#### Scenario: Modus umschalten

- **WHEN** ein Spieler im Modus Einmal den Modusknopf anklickt
- **THEN** ist sein Modus Schleife, das Fenster bleibt offen, und der Knopf zeigt „Modus: Schleife“

#### Scenario: Modus bleibt über Kategorien

- **WHEN** ein Spieler im Modus Zufall die Kategorie „Anime“ öffnet
- **THEN** zeigt das Fenster den Modus Zufall weiter

### Requirement: Titelende und Weiter richten sich nach dem Modus innerhalb der Kategorie
_Einmal stops, Schleife repeats the title, Zufall picks another title of the same category; Weiter follows the mode too._

Die Lobby MUSS beim Titelende nach dem Modus handeln. Im Modus Einmal MUSS kein Nachfolger starten. Im Modus Schleife MUSS derselbe Titel neu starten. Im Modus Zufall MUSS ein zufälliger anderer Titel aus der Kategorie des Titels starten, solange die Kategorie mindestens zwei Titel hat. Die Auswahl MUSS in der Kategorie des laufenden Titels bleiben und DARF NICHT in andere Kategorien wechseln. „Weiter“ MUSS den nächsten Titel der Kategorie wählen: in Einmal und Schleife in Reihenfolge, am Ende wieder vorn; in Zufall einen zufälligen anderen Titel. Läuft kein Titel, gilt die zuletzt geöffnete Kategorie. Eine leere Kategorie DARF keinen Titel liefern.

#### Scenario: Einmal endet

- **WHEN** der Titel „wait“ im Modus Einmal endet
- **THEN** endet die Wiedergabe mit dem Grund `end`, und es startet kein Nachfolger

#### Scenario: Schleife wiederholt den Titel

- **WHEN** der Titel „cat“ im Modus Schleife endet
- **THEN** endet „cat“ mit dem Grund `end`, und „cat“ startet mit `trigger=auto` neu

#### Scenario: Zufall bleibt in der Kategorie

- **WHEN** ein Titel der Kategorie „Anime“ im Modus Zufall endet, über viele Ziehungen mit festem Seed
- **THEN** ist der Nachfolger immer ein anderer Titel der Kategorie „Anime“, und nie der eben gespielte

#### Scenario: Weiter im Modus Einmal am Ende der Kategorie

- **WHEN** ein Spieler im Modus Einmal beim letzten Titel der Kategorie „Spiele“ „Weiter“ wählt
- **THEN** endet der Titel mit dem Grund `next`, und der erste Titel der Kategorie „Spiele“ startet

#### Scenario: Weiter ohne laufenden Titel

- **WHEN** kein Titel läuft, der Spieler zuletzt die Kategorie „Anime“ geöffnet hat und in der Übersicht „Weiter“ wählt
- **THEN** startet der erste Titel der Kategorie „Anime“

#### Scenario: Ein einziger Titel im Modus Zufall

- **WHEN** die Kategorie nur einen Titel hat und er im Modus Zufall endet
- **THEN** startet derselbe Titel mit `trigger=auto` neu

### Requirement: Vanilla-Schallplatten spielen den Plattenton des Clients nur für den Spieler
_A vanilla disc plays the client's own record sound, emitted by the player and heard by the player only, at the player's volume._

Die Lobby MUSS für jede der 22 Plattenarten aus der festen Tabelle den eingebauten Plattenton des Clients (`SoundEvents.MUSIC_DISC_*`) als Sound-Paket an den Spieler senden, der die Platte gewählt hat. Quelle MUSS `Sound.Source.RECORD` sein, Emitter `Sound.Emitter.self()`, Tonhöhe 1,0. Die Lautstärke MUSS die Lautstärke des Spielers im Startmoment sein (siehe „Die Lautstärke ist eine gespeicherte Spielereinstellung von 25 bis 100 Prozent“). Die Lobby DARF dafür keine Resource Pack und keine Notenblöcke benutzen.

#### Scenario: Platte starten

- **WHEN** ein Spieler mit Lautstärke 100 Prozent im Menü die Platte „13“ anklickt
- **THEN** erhält er genau einen Plattenton `music_disc.13` mit Quelle RECORD, Emitter `self`, Lautstärke 1,0 und Tonhöhe 1,0

#### Scenario: Platte mit Lautstärke 75 Prozent

- **WHEN** ein Spieler mit Lautstärke 75 Prozent die Platte „13“ anklickt
- **THEN** erhält er den Plattenton mit Lautstärke 0,75

#### Scenario: Keine Platte für andere

- **WHEN** Spieler A die Platte „cat“ startet und Spieler B in derselben Lobby ist
- **THEN** erhält nur A den Plattenton, und B erhält kein Paket

### Requirement: MIDI-Noten werden auf Notenblock-Instrumente und Tonhöhen abgebildet
_Channels and programs choose the instrument, pitches are folded into the two-octave range, velocity and the player's volume set the loudness._

Die Lobby MUSS jede Note eines MIDI-Songs einem der 16 Notenblock-Instrumente zuordnen, Schlagzeug auf Kanal 10 nach Taste. Die Tonhöhe MUSS in den Bereich der Notenblöcke (Noten 0 bis 24) gebracht werden, und die Tonhöhe des Sounds MUSS `2^((n − 12) / 12)` sein. Die Lautstärke jeder Note MUSS `velocity / 127` mal der Lautstärke des Spielers sein, wobei die Lautstärke in Prozent geteilt durch 100 gerechnet wird. Notenereignisse mit Anschlagstärke 0 MÜSSEN ignoriert werden. Ändert der Spieler die Lautstärke, MUSS die nächste Note mit der neuen Lautstärke klingen.

#### Scenario: Schlagzeug

- **WHEN** eine Note mit Taste 36 auf Kanal 10 gespielt wird
- **THEN** wird das Instrument `basedrum` gewählt

#### Scenario: Tonhöhe

- **WHEN** die Note 12 gespielt wird
- **THEN** ist die Tonhöhe des Sounds 1,0

#### Scenario: Lautstärke mit Anschlag und Spielerlautstärke

- **WHEN** eine Note mit Anschlag 127 gespielt wird und der Spieler 50 Prozent eingestellt hat
- **THEN** ist die Lautstärke des Sounds 0,5

#### Scenario: Zu hohe Note

- **WHEN** eine Melodie-Note außerhalb des Bereichs liegt
- **THEN** wird sie um Oktaven verschoben, bis sie im Bereich liegt, und ihre Tonhöhenklasse bleibt erhalten

### Requirement: MIDI-Songs werden beim Start nur geprüft, Noten erst beim Abspielen geladen
_At start only metadata and the MIDI header are checked; the notes are read when a title starts, never on the tick thread._

Die Lobby MUSS beim Start die Einträge aus `radio/songs.yaml` mit ihren Pflichtfeldern, der Platte, der Kategorie und der Eindeutigkeit der Id prüfen. Für jede Datei MUSS sie prüfen, dass sie eine reguläre Datei unter `radio/` ist und mit einem MIDI-Kopf beginnt, und dafür nur den Kopf lesen. Die Lobby DARF beim Start KEINE Noten einlesen. Eine Datei, die diese Prüfung nicht besteht, MUSS übersprungen und mit Dateiname und Grund protokolliert werden. Eine Schallplatte DARF in `songs.yaml` nur einmal vorkommen; eine doppelte `id` ebenfalls nicht. Eine MIDI-Platte DARF eine Vanilla-Platte sein, denn die Menüs sind getrennt. Die Lobby DARF den Start NICHT abbrechen. Fehlt das Verzeichnis `radio/` oder ist keine Datei gültig, MUSS die Lobby trotzdem alle Vanilla-Platten anbieten.

#### Scenario: Kaputter Kopf beim Start

- **WHEN** eine von drei Dateien nicht mit einem MIDI-Kopf beginnt
- **THEN** wird dieser Song übersprungen, die beiden anderen stehen in ihrer Kategorie, und die Lobby startet

#### Scenario: Ohne gültige Datei

- **WHEN** im Verzeichnis `radio/` keine gültige Datei liegt
- **THEN** startet die Lobby, das Radio-Item und die Vanilla-Platten sind da

#### Scenario: Doppelte Schallplatte in songs.yaml

- **WHEN** zwei Einträge in `songs.yaml` dieselbe Schallplatte nennen
- **THEN** steht nur der erste im Menü, und der zweite wird übersprungen

#### Scenario: MIDI-Platte ist eine Vanilla-Platte

- **WHEN** ein MIDI-Eintrag die Schallplatte `MUSIC_DISC_CAT` nennt, die auch Vanilla vorkommt
- **THEN** wird der Eintrag geladen, und beide stehen in ihrer Kategorie

#### Scenario: Start mit großen Dateien

- **WHEN** `songs.yaml` 200 Einträge mit je einer MIDI-Datei von 10 MB hat
- **THEN** startet die Lobby mit dem Speicher für die Metadaten, ohne eine der Dateien vollständig einzulesen

### Requirement: Die Paketzahl je Spieler und Tick ist begrenzt
_The packet budget is playback quality, not a content limit: no size, note, length or song-count limit exists._

Die Lobby MUSS je Spieler höchstens 16 Noten je Tick abspielen. Überschreitet eine Note das Budget je Tick, MUSS die leiseste Note verworfen werden. Die Lobby DARF KEINE Grenzen für Dateigröße, Noten, Länge oder Titelzahl einführen; die Grenze je Tick begrenzt Pakete je Spieler, nicht den Inhalt.

#### Scenario: Zu viele Noten je Tick

- **WHEN** 20 Noten im selben Tick fällig sind
- **THEN** werden die 16 lautesten gespielt und vier verworfen

#### Scenario: Keine Titelgrenze

- **WHEN** `songs.yaml` 500 gültige Einträge hat
- **THEN** stehen alle 500 im Menü, verteilt auf Seiten

### Requirement: Telemetrie und Meldungen
_Counters by kind, mode, trigger and reason, no player identifiers; plays carry the category id; texts in German and English._

Die Lobby MUSS die Zähler `titan.radio.plays{kind, mode, trigger, category}`, `titan.radio.stops{reason, kind}`, `titan.radio.notes.dropped` und `titan.radio.songs{result}` führen. `kind` ist `vanilla` oder `midi`, `mode` ist `once`, `loop` oder `shuffle`, `trigger` ist `user` oder `auto`, `category` ist die Id der Kategorie, bei Vanilla `minecraft`, bei Sonstige `other`. Stopps tragen keine Kategorie. `titan.radio.songs{result}` misst den Start-Check mit `loaded` und `rejected`, und ein Parse-Fehler beim Abspielen zählt zusätzlich `rejected`. Attribute MÜSSEN ohne Spieler-Id auskommen. Alle Nutzertexte MÜSSEN in Deutsch und Englisch mit denselben Schlüsseln vorliegen, auch die Kategorienamen, die Seitenanzeige, die Lautstärke, „Läuft jetzt“, „Lädt …“ und die Meldung für einen nicht lesbaren Titel.

#### Scenario: Start mit Kategorie

- **WHEN** ein Spieler im Modus Schleife den MIDI-Song „sunrise“ der Kategorie „games“ startet
- **THEN** wird `titan.radio.plays` mit `kind=midi`, `mode=loop`, `trigger=user` und `category=games` um eins erhöht, ohne Spieler-Id

#### Scenario: Stopp mit Grund und Art

- **WHEN** ein Spieler „Stopp“ wählt, während der MIDI-Song „sunrise“ läuft
- **THEN** wird `titan.radio.stops` mit `reason=user` und `kind=midi` um eins erhöht, ohne Spieler-Id und ohne Kategorie

#### Scenario: Meldungen in beiden Sprachen

- **WHEN** die Übersicht für einen Spieler mit deutscher oder englischer Sprache gerendert wird
- **THEN** ist der Text in der Sprache des Spielers, und jeder Schlüssel existiert in beiden Sprachen

## ADDED Requirements

### Requirement: Die Lobby lädt Kategorien aus songs.yaml mit festen Regeln
_Categories are declared in songs.yaml; Minecraft is built in; reserved and invalid entries are skipped with a warning._

Die Lobby MUSS Kategorien aus dem Abschnitt `categories` in `radio/songs.yaml` laden. Jede Kategorie MUSS eine Id, einen Anzeigenamen, ein Symbol als Vanilla-`Material` und eine Reihenfolge haben. Der Anzeigename MUSS entweder ein Text für beide Sprachen oder eine Zuordnung `{de, en}` sein. Die Kategorie „Minecraft“ ist eingebaut, steht vorn und enthält genau die Vanilla-Platten. Die Kategorie „Sonstige“ ist eingebaut, steht hinten und wird nur gezeigt, wenn sie Titel hat. Die Ids `minecraft` und `other` DÜRFEN nicht deklariert werden. Höchstens 20 deklarierte Kategorien werden geladen, der Überschuss wird übersprungen und protokolliert.

#### Scenario: Reihenfolge der Kategorien

- **WHEN** die Konfiguration die Kategorien „anime“ mit Reihenfolge 20, „games“ mit 10 und „classic“ mit 10 deklariert
- **THEN** stehen in der Übersicht „Minecraft“, „classic“, „games“, „anime“ und zuletzt „Sonstige“, falls sie Titel hat

#### Scenario: Reservierte Id

- **WHEN** die Konfiguration eine Kategorie mit der Id `minecraft` deklariert
- **THEN** wird sie übersprungen, mit einer Warnung, und die Lobby startet

#### Scenario: Name nur in einer Sprache

- **WHEN** eine Kategorie den Namen „Anime“ als Text hat
- **THEN** zeigt die Übersicht „Anime“ in beiden Sprachen

### Requirement: Eine unbekannte oder fehlende Kategorie landet in Sonstige
_A song without a known category goes to Sonstige and logs a warning; it is never fatal._

Ein MIDI-Song ohne Feld `category` oder mit einer nicht deklarierten Id MUSS in die Kategorie „Sonstige“. Die Lobby MUSS dafür je Song eine Warnung mit Dateiname und Grund protokollieren. Ein Song DARF wegen seiner Kategorie NICHT übersprungen werden. Ein Symbol, das kein Vanilla-Material ist, MUSS durch `PAPER` ersetzt und protokolliert werden.

#### Scenario: Alte songs.yaml ohne Kategorie

- **WHEN** `songs.yaml` nur die Einträge `id, title, artist, disc, file` hat
- **THEN** startet die Lobby, alle Songs stehen in „Sonstige“, und je Song gibt es eine Warnung

#### Scenario: Unbekannte Kategorie

- **WHEN** ein Song die Kategorie `winter` nennt, die nicht deklariert ist
- **THEN** steht er in „Sonstige“, und die Warnung nennt `winter`

### Requirement: Die Übersicht zeigt jede Kategorie mit Titeln, sonst nichts
_Categories without songs stay hidden; the overview has no pages._

Die Übersicht MUSS nur Kategorien mit mindestens einem Titel zeigen, außer „Minecraft“, die immer da ist. Sie MUSS ohne eigene Seiten auskommen, weil sie höchstens 22 Einträge hat. Eine leere deklarierte Kategorie DARF in der Übersicht NICHT erscheinen und wird nicht protokolliert.

#### Scenario: Leere Kategorie

- **WHEN** die Kategorie „halloween“ deklariert ist, aber keinen Titel hat
- **THEN** fehlt sie in der Übersicht

### Requirement: Die Kategorieseite zeigt 45 Titel je Seite an festen Plätzen
_Songs are paged 45 per page, slots are stable, each player has his own page._

Eine Kategorieseite MUSS höchstens 45 Titel an den Plätzen 0 bis 44 zeigen. Der Titel mit dem Index `i` in der Kategorie MUSS auf Seite `i / 45 + 1` am Platz `i % 45` stehen. Die Reihenfolge ist die Konfigurationsreihenfolge, und bei „Minecraft“ die Menüreihenfolge. Die Seite MUSS je Spieler in seiner Sitzung gespeichert sein. Blättern MUSS die Seite ändern und das Fenster offen lassen. Ein Wechsel der Kategorie oder ein erneutes Öffnen MUSS auf Seite 1 setzen. Eine Seite über der letzten MUSS auf die letzte geklemmt werden. Füllglas ohne Namen MUSS die übrigen Plätze des Inhalts füllen.

#### Scenario: Blättern

- **WHEN** ein Spieler in der Kategorie „games“ mit 60 Titeln auf Seite 1 die Seite weiter wählt
- **THEN** zeigt das Fenster Seite 2 mit den Titeln 46 bis 60 an den Plätzen 0 bis 14, der Rest ist Füllglas, und das Fenster bleibt offen

#### Scenario: Seite zurücksetzen

- **WHEN** ein Spieler auf Seite 2 der Kategorie „games“ den Zurück-Knopf zur Übersicht und danach „games“ erneut wählt
- **THEN** zeigt das Fenster Seite 1

### Requirement: Die Steuerzeile liegt unten und verschiebt sich nie
_Nine fixed controls in the bottom row, inactive ones are glass panes, not removed._

Die unterste Reihe der Kiste (Plätze 45 bis 53) MUSS die Steuerelemente Zurück, Seite zurück, Seitenanzeige, Stopp, Läuft jetzt, Weiter, Modus, Lautstärke und Seite weiter in dieser Reihenfolge zeigen. Ein nicht verfügbares Element MUSS an seinem Platz als Füllglas stehen und DARF NICHT verschoben oder entfernt werden. Stopp, Weiter, Modus und Lautstärke sind auf jeder Ansicht verfügbar, außer Weiter in der Übersicht, solange weder ein Titel läuft noch eine Kategorie geöffnet war.

#### Scenario: Seitenknopf auf der ersten Seite

- **WHEN** ein Spieler auf Seite 1 einer Kategorie mit zwei Seiten ist
- **THEN** steht auf Platz 46 Füllglas, und auf Platz 53 steht der Knopf „Seite weiter“

#### Scenario: Übersicht

- **WHEN** ein Spieler die Übersicht sieht
- **THEN** stehen auf den Plätzen 45, 46 und 53 Füllglas, und Stopp, Läuft jetzt, Modus und Lautstärke stehen an ihren Plätzen

### Requirement: Das Menü gehört dem Spieler und wird beim Öffnen gebaut
_One inventory per open window, refreshed in place; closing releases it; nothing is shared between players._

Die Lobby MUSS das Menü je Öffnen als Aves-Inventar für genau diesen Spieler bauen. Klicks MÜSSEN das Inventar an Ort und Stelle aktualisieren, ohne es neu zu öffnen, außer ein Seitenwechsel braucht laut Aves-Prüfung einen neuen Fenstertitel. Das Schließen eines Fensters MUSS seine Listener entfernen. Beim Herunterfahren MUSS die Lobby alle offenen Fenster schließen. Keine Ansicht DARF zwischen Spielern geteilt werden.

#### Scenario: Modusklick ohne Neuöffnen

- **WHEN** ein Spieler im Fenster den Modusknopf anklickt
- **THEN** ist es dasselbe Inventar, und sein Inhalt zeigt den neuen Modus

#### Scenario: Schließen gibt die Ansicht frei

- **WHEN** ein Spieler das Fenster schließt
- **THEN** ist die Ansicht entfernt, und seine Sitzung mit Modus, Kategorie und Seite bleibt bestehen

### Requirement: Die Lautstärke ist eine gespeicherte Spielereinstellung von 25 bis 100 Prozent
_Four steps, default 100, one button with two click directions, stored in Otis, kept across logouts, not in the settings menu._

Die Lobby MUSS je Spieler eine Lautstärke mit den Stufen 25, 50, 75 und 100 Prozent halten, Standard 100 Prozent. Ein Linksklick auf den Lautstärkeknopf MUSS auf die nächsthöhere Stufe gehen, ein Rechtsklick auf die nächsttiefere, und an den Rändern MUSS der Wert stehen bleiben. Die Lobby MUSS die Lautstärke über die Spielereinstellungen in Otis speichern, unter dem Schlüssel `titan:radio.volume` als JSON-Zahl. Die Lautstärke MUSS den Logout überdauern. Die Lautstärke DARF NICHT im Einstellungsmenü erscheinen. Ist Otis nicht erreichbar, MUSS die Lobby mit dem Standard 100 Prozent weiterspielen und die Änderung später speichern.

#### Scenario: Standard

- **WHEN** ein Spieler ohne gespeicherten Wert das Radio öffnet
- **THEN** zeigt der Knopf „Lautstärke: 100 %“

#### Scenario: Stufen

- **WHEN** ein Spieler mit 100 Prozent einmal rechts und danach einmal links klickt
- **THEN** ist die Lautstärke 75 Prozent und danach wieder 100 Prozent

#### Scenario: Ränder bleiben stehen

- **WHEN** ein Spieler mit 25 Prozent weiter leiser klickt
- **THEN** bleibt die Lautstärke bei 25 Prozent, und es gibt keinen Sprung auf 100 Prozent

#### Scenario: Speichern über den Logout

- **WHEN** ein Spieler auf 50 Prozent steht, sich abmeldet und wieder anmeldet
- **THEN** ist seine Lautstärke wieder 50 Prozent, weil der Wert in Otis gespeichert ist

#### Scenario: Otis nicht erreichbar

- **WHEN** ein Spieler beim Beitritt keine Antwort von Otis erhält
- **THEN** spielt das Radio mit 100 Prozent, und eine Änderung wird gespeichert, sobald Otis wieder antwortet

#### Scenario: Nicht im Einstellungsmenü

- **WHEN** ein Spieler das Einstellungsmenü öffnet
- **THEN** gibt es darin keinen Eintrag für die Lautstärke

### Requirement: Eine laufende Schallplatte behält ihre Lautstärke bis zum nächsten Titel
_A running disc cannot change its volume; the next start uses the new one. MIDI follows at once._

Die Lobby MUSS bei einer laufenden Vanilla-Platte beim Ändern der Lautstärke NICHTS senden: die Platte läuft mit ihrer Lautstärke weiter, und die neue gilt ab dem nächsten Start. Ein Neustart der Platte DARF NICHT stattfinden. Die Anzeige „Läuft jetzt“ MUSS in diesem Fall den Hinweis „Lautstärke wirkt ab dem nächsten Titel“ zeigen. Bei MIDI-Songs MUSS die neue Lautstärke ab der nächsten Note gelten.

#### Scenario: Lautstärke während einer Schallplatte

- **WHEN** ein Spieler die Platte „13“ mit 100 Prozent hört und auf 75 Prozent stellt
- **THEN** sendet die Lobby kein Paket, „13“ läuft mit 100 Prozent weiter, und „Läuft jetzt“ zeigt den Hinweis

#### Scenario: Nächste Schallplatte

- **WHEN** danach ein Spieler die Platte „cat“ anklickt
- **THEN** erhält er den Plattenton von „cat“ mit Lautstärke 0,75

#### Scenario: MIDI folgt sofort

- **WHEN** ein Spieler einen MIDI-Song hört und auf 50 Prozent stellt
- **THEN** klingt die nächste Note mit der halben Lautstärke

### Requirement: „Läuft jetzt“ nennt Titel, Künstler, Kategorie, Modus und Lautstärke
_The now-playing item shows what plays, where it is from, how it repeats and how loud it is._

Der Knopf „Läuft jetzt“ MUSS den Titel als Name zeigen und in der Lore Künstler, Kategorie, Modus und Lautstärke. Läuft kein Titel, MUSS er „Kein Titel läuft“ zeigen, mit Modus und Lautstärke. Der Knopf MUSS nach jeder Änderung des laufenden Titels, des Modus oder der Lautstärke im offenen Fenster aktualisiert werden. Ein Klick auf ihn DARF keinen Titel starten oder stoppen.

#### Scenario: Titel läuft

- **WHEN** der Titel „Sunrise“ von „Beispiel“ aus der Kategorie „Spiele“ im Modus Schleife mit 75 Prozent läuft
- **THEN** zeigt „Läuft jetzt“ den Namen „Sunrise“ und in der Lore „Künstler: Beispiel“, „Kategorie: Spiele“, „Modus: Schleife“ und „Lautstärke: 75 %“

#### Scenario: Ende ohne Nachfolger

- **WHEN** der Titel im Modus Einmal endet
- **THEN** zeigt „Läuft jetzt“ „Kein Titel läuft“

### Requirement: Noten werden beim Start eines Titels geladen, nicht auf dem Tick-Thread
_Parsing runs on a background executor; the title starts on the tick thread once the notes are ready._

Die Lobby MUSS die Noten eines MIDI-Songs beim Start des Titels laden, asynchron, und den Titel erst starten, wenn die Noten vorliegen. Das Laden MUSS außerhalb des Tick-Threads laufen. Der Spieler MUSS während des Ladens die Aktionsleiste „Lädt …“ sehen. Ein Stopp, ein Weiter oder ein Abmelden während des Ladens MUSS das Laden verwerfen, ohne dass der Titel später startet. Ein Vanilla-Titel lädt nichts.

#### Scenario: Laden beim Start

- **WHEN** ein Spieler einen MIDI-Song anklickt, dessen Noten nicht im Cache liegen
- **THEN** sieht er „Lädt …“, und der Titel startet, sobald die Noten bereit sind

#### Scenario: Stopp während des Ladens

- **WHEN** ein Spieler „Stopp“ wählt, während die Noten noch laden
- **THEN** startet der Titel nicht, und das Ergebnis des Ladens wird verworfen

### Requirement: Der Cache hält laufende und zuletzt gespielte Titel
_Playing titles are never evicted; the eight most recently released titles stay in memory, so a loop restarts without a reload._

Die Lobby MUSS die Noten laufender Titel halten, solange sie laufen, und höchstens acht zuletzt gespielte, nicht laufende Titel. Ein laufender Titel DARF NICHT aus dem Cache entfernt werden. Zwei Spieler, die denselben Titel gleichzeitig starten, MÜSSEN einen einzigen Ladevorgang auslösen. Ein Schleifen-Neustart MUSS ohne erneutes Laden starten, solange der Titel im Cache ist.

#### Scenario: Schleife ohne Neuladen

- **WHEN** der Titel „cat“ im Modus Schleife endet
- **THEN** startet „cat“ neu, ohne dass die Datei erneut gelesen wird

#### Scenario: Ein Ladevorgang für zwei Spieler

- **WHEN** Spieler A und Spieler B denselben nicht geladenen Titel gleichzeitig starten
- **THEN** wird die Datei einmal geparst, und beide starten, sobald die Noten bereit sind

#### Scenario: Neunter Titel

- **WHEN** neun verschiedene Titel gespielt und gestoppt wurden
- **THEN** ist der älteste von ihnen aus dem Cache entfernt, und die übrigen acht sind im Cache

### Requirement: Ein Titel, der beim Abspielen nicht lesbar ist, wird gemeldet
_A file that fails to parse at play time tells the player, logs a warning and counts as rejected; it never stops the lobby._

Schlägt der Parse einer MIDI-Datei beim Abspielen fehl, MUSS die Lobby dem Spieler die Meldung „Dieser Titel kann nicht abgespielt werden“ zeigen, eine Warnung mit Dateiname und Grund protokollieren und `titan.radio.songs{result=rejected}` um eins erhöhen. Der Fehler MUSS einmal je Ladevorgang gezählt werden. Der Titel startet nicht. Schlägt der Nachfolger eines Titelendes fehl, startet nichts, und die Lobby DARF keinen weiteren Nachfolger versuchen.

#### Scenario: Kaputte Datei beim Abspielen

- **WHEN** ein Spieler den Titel „sunrise“ anklickt, dessen Datei beim Parsen fehlschlägt
- **THEN** sieht er die Meldung „Dieser Titel kann nicht abgespielt werden“, `titan.radio.songs{result=rejected}` steigt um eins, und die Lobby protokolliert eine Warnung mit dem Dateinamen

#### Scenario: Fehlschlag als Nachfolger

- **WHEN** im Modus Zufall ein Titelende den Nachfolger „cat“ ergibt, dessen Datei nicht lesbar ist
- **THEN** startet nichts, der Spieler sieht die Meldung, und die Wiedergabe bleibt beendet

### Requirement: Das laufende Stück glänzt im Fenster des Spielers
_The title that plays for this player shows the enchantment glint in his own window; nothing glints for anyone else._

Im offenen Fenster eines Spielers MUSS das Item des Titels, der für genau diesen Spieler läuft, den Glanz der Verzauberung zeigen (`DataComponents.ENCHANTMENT_GLINT_OVERRIDE`, Vanilla, ohne Resource Pack). Ein Stopp, ein Titelende ohne Nachfolger oder ein Abmelden MUSS den Glanz entfernen. Der Glanz DARF NICHT in den Fenstern anderer Spieler erscheinen.

#### Scenario: Laufender Titel glänzt

- **WHEN** ein Spieler den Titel „sunrise“ in der Kategorie „Spiele“ startet
- **THEN** glänzt das Item von „sunrise“ in seinem Fenster, und das Item von „cat“ in der Liste nicht

#### Scenario: Glanz endet mit dem Stopp

- **WHEN** ein Spieler „Stopp“ wählt, während „sunrise“ läuft
- **THEN** glänzt in seinem Fenster kein Item mehr

#### Scenario: Glanz nur beim Spieler

- **WHEN** Spieler A „sunrise“ spielt und Spieler B dieselbe Kategorie offen hat
- **THEN** glänzt „sunrise“ nur im Fenster von A
