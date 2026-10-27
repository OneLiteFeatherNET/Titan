# Spec Delta

## RENAMED Requirements

- FROM: `### Requirement: Das Radio öffnet sich über ein Hotbar-Item mit zwei Menüs`
- TO: `### Requirement: Das Radio öffnet sich über ein Hotbar-Item mit der Kategorieübersicht`

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

Die Lobby MUSS für jede der 22 Plattenarten aus der festen Tabelle den eingebauten Plattenton des Clients (`SoundEvents.MUSIC_DISC_*`) als Sound-Paket an den Spieler senden, der die Platte gewählt hat. Quelle MUSS `Sound.Source.RECORD` sein, Emitter `Sound.Emitter.self()`, Tonhöhe 1,0. Die Lautstärke MUSS die Lautstärke des Spielers im Startmoment sein (siehe „Die Lautstärke ist eine Sitzungseinstellung von 10 bis 100 Prozent“). Die Lobby DARF dafür keine Resource Pack und keine Notenblöcke benutzen.

#### Scenario: Platte starten

- **WHEN** ein Spieler mit Lautstärke 100 Prozent im Menü die Platte „13“ anklickt
- **THEN** erhält er genau einen Plattenton `music_disc.13` mit Quelle RECORD, Emitter `self`, Lautstärke 1,0 und Tonhöhe 1,0

#### Scenario: Platte mit Lautstärke 70 Prozent

- **WHEN** ein Spieler mit Lautstärke 70 Prozent die Platte „13“ anklickt
- **THEN** erhält er den Plattenton mit Lautstärke 0,7

#### Scenario: Keine Platte für andere

- **WHEN** Spieler A die Platte „cat“ startet und Spieler B in derselben Lobby ist
- **THEN** erhält nur A den Plattenton, und B erhält kein Paket

### Requirement: MIDI-Noten werden auf Notenblock-Instrumente und Tonhöhen abgebildet
_Channels and programs choose the instrument, pitches are folded into the two-octave range, velocity and the player's volume set the loudness._

Die Lobby MUSS jede Note eines MIDI-Songs einem der 16 Notenblock-Instrumente zuordnen, Schlagzeug auf Kanal 10 nach Taste. Die Tonhöhe MUSS in den Bereich der Notenblöcke (Noten 0 bis 24) gebracht werden, und die Tonhöhe des Sounds MUSS `2^((n − 12) / 12)` sein. Die Lautstärke jeder Note MUSS `velocity / 127` mal der Lautstärke des Spielers sein. Notenereignisse mit Anschlagstärke 0 MÜSSEN ignoriert werden. Ändert der Spieler die Lautstärke, MUSS die nächste Note mit der neuen Lautstärke klingen.

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

### Requirement: MIDI-Songs werden beim Start mit Kategorie geladen, fehlerhafte Dateien übersprungen
_A broken MIDI file is logged and skipped; every song gets a category, or Sonstige; nothing here stops the lobby._

Die Lobby MUSS die MIDI-Songs aus `radio/songs.yaml` und den zugehörigen Dateien einmal beim Start laden. Eine Datei, die fehlt, zu groß ist, nicht als MIDI gelesen werden kann oder eine ungültige Schallplatte hat, MUSS übersprungen und mit Dateiname und Grund protokolliert werden. Eine Schallplatte DARF in `songs.yaml` nur einmal vorkommen; eine doppelte `id` ebenfalls nicht. Eine MIDI-Platte DARF eine Vanilla-Platte sein, denn die Menüs sind getrennt (so umgesetzt, siehe `lobby-radio`, Umsetzungsnotizen). Die Lobby DARF den Start NICHT abbrechen. Fehlt das Verzeichnis `radio/` oder ist keine Datei gültig, MUSS die Lobby trotzdem alle Vanilla-Platten anbieten.

#### Scenario: Kaputte Datei

- **WHEN** eine von drei MIDI-Dateien keine gültige MIDI-Datei ist
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

### Requirement: Grenzen begrenzen Paketzahl und Speicher
_Packet budget per tick and per player, file size, note and song count limits, and a total note budget for the lobby's memory._

Die Lobby MUSS folgende Grenzen einhalten: eine MIDI-Datei höchstens 512 KiB, ein MIDI-Song höchstens 20 000 Noten und 15 Minuten, höchstens 200 MIDI-Songs, insgesamt höchstens 1 000 000 Noten über alle geladenen MIDI-Songs, und höchstens 16 Noten je Tick und Spieler. Überschreitet ein MIDI-Song die Grenzen für Datei, Noten oder Länge, MUSS er übersprungen werden. Überschreitet ein Song die Titelzahl oder das Notenbudget, MUSS er übersprungen und protokolliert werden. Überschreitet eine Note das Budget je Tick, MUSS die leiseste Note verworfen werden. Vanilla-Platten zählen nicht zu den 200 Songs.

#### Scenario: Zu viele Noten je Tick

- **WHEN** 20 Noten im selben Tick fällig sind
- **THEN** werden die 16 lautesten gespielt und vier verworfen

#### Scenario: Zu große Datei

- **WHEN** eine MIDI-Datei größer als 512 KiB ist
- **THEN** wird der Song übersprungen und protokolliert

#### Scenario: Der 201. Song

- **WHEN** `songs.yaml` 201 gültige Einträge hat
- **THEN** wird der letzte Eintrag in Konfigurationsreihenfolge übersprungen und protokolliert, und die anderen 200 stehen im Menü

#### Scenario: Notenbudget überschritten

- **WHEN** ein Song die Gesamtgrenze von 1 000 000 Noten überschreitet
- **THEN** wird er übersprungen und protokolliert, und die Songs davor bleiben geladen

### Requirement: Telemetrie und Meldungen
_Counters by kind, mode, trigger and reason, no player identifiers; plays carry the category id; texts in German and English._

Die Lobby MUSS die Zähler `titan.radio.plays{kind, mode, trigger, category}`, `titan.radio.stops{reason, kind}`, `titan.radio.notes.dropped` und `titan.radio.songs{result}` führen. `kind` ist `vanilla` oder `midi`, `mode` ist `once`, `loop` oder `shuffle`, `trigger` ist `user` oder `auto`, `category` ist die Id der Kategorie, bei Vanilla `minecraft`, bei Sonstige `other`. Stopps tragen keine Kategorie. Attribute MÜSSEN ohne Spieler-Id auskommen. Alle Nutzertexte MÜSSEN in Deutsch und Englisch mit denselben Schlüsseln vorliegen, auch die Kategorienamen, die Seitenanzeige, die Lautstärke und „Läuft jetzt“.

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
- **THEN** ist die Ansicht entfernt, und seine Sitzung mit Modus, Lautstärke und Kategorie bleibt bestehen

### Requirement: Die Lautstärke ist eine Sitzungseinstellung von 10 bis 100 Prozent
_Ten steps of ten percent, default 100, one button with two click directions, session only, forgotten on logout._

Die Lobby MUSS je Spieler eine Lautstärke in Zehnerschritten von 10 bis 100 Prozent halten, Standard 100 Prozent. Ein Linksklick auf den Lautstärkeknopf MUSS sie um 10 Prozent erhöhen, ein Rechtsklick um 10 Prozent senken, jeweils bis zur Grenze. Die Lautstärke MUSS nur für die Sitzung des Spielers gelten, und die Lobby DARF sie NICHT über `PlayerSettings` oder den Otis-Adapter speichern. Beim Abmelden MUSS die Lautstärke vergessen werden.

#### Scenario: Standard

- **WHEN** ein Spieler das Radio zum ersten Mal in der Sitzung öffnet
- **THEN** zeigt der Knopf „Lautstärke: 100 %“

#### Scenario: Lauter und leiser

- **WHEN** ein Spieler mit 100 Prozent einmal rechts und danach einmal links klickt
- **THEN** ist die Lautstärke 90 Prozent und danach wieder 100 Prozent

#### Scenario: Grenze

- **WHEN** ein Spieler mit 10 Prozent weiter leiser klickt
- **THEN** bleibt die Lautstärke bei 10 Prozent

#### Scenario: Vergessen beim Abmelden

- **WHEN** ein Spieler auf 40 Prozent steht, sich abmeldet und wieder anmeldet
- **THEN** ist seine Lautstärke wieder 100 Prozent

### Requirement: Eine laufende Schallplatte behält ihre Lautstärke bis zum nächsten Titel
_A running disc cannot change its volume; the next start uses the new one. MIDI follows at once._

Die Lobby MUSS bei einer laufenden Vanilla-Platte beim Ändern der Lautstärke NICHTS senden: die Platte läuft mit ihrer Lautstärke weiter, und die neue gilt ab dem nächsten Start. Ein Neustart der Platte DARF NICHT stattfinden. Die Anzeige „Läuft jetzt“ MUSS in diesem Fall den Hinweis „Lautstärke wirkt ab dem nächsten Titel“ zeigen. Bei MIDI-Songs MUSS die neue Lautstärke ab der nächsten Note gelten.

#### Scenario: Lautstärke während einer Schallplatte

- **WHEN** ein Spieler die Platte „13“ mit 100 Prozent hört und auf 70 Prozent stellt
- **THEN** sendet die Lobby kein Paket, „13“ läuft mit 100 Prozent weiter, und „Läuft jetzt“ zeigt den Hinweis

#### Scenario: Nächste Schallplatte

- **WHEN** danach ein Spieler die Platte „cat“ anklickt
- **THEN** erhält er den Plattenton von „cat“ mit Lautstärke 0,7

#### Scenario: MIDI folgt sofort

- **WHEN** ein Spieler einen MIDI-Song hört und auf 50 Prozent stellt
- **THEN** klingt die nächste Note mit der halben Lautstärke

### Requirement: „Läuft jetzt“ nennt Titel, Künstler, Kategorie, Modus und Lautstärke
_The now-playing item shows what plays, where it is from, how it repeats and how loud it is._

Der Knopf „Läuft jetzt“ MUSS den Titel als Name zeigen und in der Lore Künstler, Kategorie, Modus und Lautstärke. Läuft kein Titel, MUSS er „Kein Titel läuft“ zeigen, mit Modus und Lautstärke. Der Knopf MUSS nach jeder Änderung des laufenden Titels, des Modus oder der Lautstärke im offenen Fenster aktualisiert werden. Ein Klick auf ihn DARF keinen Titel starten oder stoppen.

#### Scenario: Titel läuft

- **WHEN** der Titel „Sunrise“ von „Beispiel“ aus der Kategorie „Spiele“ im Modus Schleife mit 70 Prozent läuft
- **THEN** zeigt „Läuft jetzt“ den Namen „Sunrise“ und in der Lore „Künstler: Beispiel“, „Kategorie: Spiele“, „Modus: Schleife“ und „Lautstärke: 70 %“

#### Scenario: Ende ohne Nachfolger

- **WHEN** der Titel im Modus Einmal endet
- **THEN** zeigt „Läuft jetzt“ „Kein Titel läuft“
