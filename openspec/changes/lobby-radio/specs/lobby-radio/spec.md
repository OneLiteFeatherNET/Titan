# Spec Delta

## ADDED Requirements

### Requirement: Das Radio öffnet sich über ein Hotbar-Item mit zwei Menüs
_The radio is one hotbar item, always there, that opens the vanilla discs; custom songs are one switch away._

Die Lobby MUSS das Item `titan:radio` (Material `JUKEBOX`) auf Hotbar-Platz 5 anbieten, immer, auch ohne MIDI-Dateien. Ein Klick auf das Item MUSS das Menü „Vanilla-Platten“ öffnen. Das Menü MUSS je Vanilla-Platte eine Schallplatte zeigen sowie die Steuerelemente Stopp, Weiter und den Modusknopf. Gibt es MIDI-Songs, MUSS das Menü einen Umschalter zu „Eigene Songs“ haben, und das zweite Menü MUSS je MIDI-Song eine Schallplatte zeigen, mit denselben Steuerelementen und einem Umschalter zurück.

#### Scenario: Menü öffnen ohne MIDI-Dateien

- **WHEN** ein Spieler das Radio-Item benutzt und im Verzeichnis `radio/` keine MIDI-Datei liegt
- **THEN** sieht er das Menü „Vanilla-Platten“ mit 22 Platten, Stopp, Weiter, Modusknopf und keinem Umschalter

#### Scenario: Umschalten zu den eigenen Songs

- **WHEN** ein Spieler mit mindestens einem geladenen MIDI-Song im Menü „Vanilla-Platten“ den Umschalter wählt
- **THEN** sieht er das Menü „Eigene Songs“ mit einer Schallplatte je MIDI-Song, dem Modusknopf im aktuellen Modus und einem Umschalter zurück

### Requirement: Der Modusknopf zeigt den Modus des Spielers und schaltet ihn um

_The mode button shows the player's mode through the inventory variant that is open; each variant is shared._

Die Lobby MUSS für jedes Menü und jeden Modus ein Inventar bereithalten, also sechs Inventare. Der Modusknopf MUSS den aktuellen Modus des Spielers zeigen, indem das für ihn geöffnete Inventar den Modus trägt. Ein Klick auf den Modusknopf MUSS den Modus des Spielers zyklisch weiterschalten (Einmal, Schleife, Zufall) und dem Spieler das Inventar desselben Menüs im neuen Modus öffnen.

#### Scenario: Modus umschalten

- **WHEN** ein Spieler im Modus Einmal den Modusknopf anklickt
- **THEN** ist sein Modus Schleife, und er sieht das Inventar „Vanilla-Platten“ im Modus Schleife

#### Scenario: Umschalter behält den Modus

- **WHEN** ein Spieler im Modus Zufall den Umschalter zu „Eigene Songs“ wählt
- **THEN** sieht er „Eigene Songs“ im Modus Zufall

### Requirement: Der Abspielmodus ist eine Sitzungseinstellung mit dem Standard Einmal

_Three modes, default Einmal; the mode lives in the session and is forgotten on logout._

Die Lobby MUSS drei Abspielmodi kennen: Einmal, Schleife und Zufall. Der Standard MUSS Einmal sein. Der Modus MUSS nur für die Sitzung des Spielers gelten, und die Lobby DARF ihn NICHT speichern. Beim Abmelden MUSS der Modus vergessen werden.

#### Scenario: Standard

- **WHEN** ein Spieler die Lobby betritt und das Radio öffnet
- **THEN** ist sein Modus Einmal

#### Scenario: Modus nach dem Abmelden

- **WHEN** ein Spieler im Modus Schleife ist, sich abmeldet und wieder anmeldet
- **THEN** ist sein Modus wieder Einmal

### Requirement: Titelende und Weiter richten sich nach dem Modus

_Einmal stops, Schleife repeats the same title, Zufall picks a random other title; Weiter follows the mode too._

Die Lobby MUSS beim Titelende nach dem Modus handeln. Im Modus Einmal MUSS kein Nachfolger starten. Im Modus Schleife MUSS derselbe Titel neu starten. Im Modus Zufall MUSS ein zufälliger Titel aus allen verfügbaren Titeln starten, nicht derselbe wie eben, solange der Katalog mindestens zwei Titel hat. „Weiter“ MUSS den nächsten Titel nach dem Modus wählen: in Einmal und Schleife den nächsten in der Reihenfolge (Vanilla in Menüreihenfolge, danach MIDI in Konfigurationsreihenfolge, am Ende wieder vorn), in Zufall einen zufälligen anderen Titel. Mit nur einem Titel MUSS Zufall diesen Titel wiederholen. Ein leerer Katalog DARF keinen Titel liefern.

#### Scenario: Einmal endet

- **WHEN** der Titel „wait“ im Modus Einmal endet
- **THEN** endet die Wiedergabe mit dem Grund `end`, und es startet kein Nachfolger

#### Scenario: Schleife wiederholt den Titel

- **WHEN** der Titel „cat“ im Modus Schleife endet
- **THEN** endet „cat“ mit dem Grund `end`, und „cat“ startet mit `trigger=auto` neu

#### Scenario: Zufall wählt einen anderen Titel

- **WHEN** der Titel „cat“ im Modus Zufall endet
- **THEN** endet „cat“ mit dem Grund `end`, und ein zufälliger anderer Titel startet mit `trigger=auto`

#### Scenario: Zufall wiederholt nie denselben Titel zweimal hintereinander

- **WHEN** viele Titelenden im Modus Zufall mit festem Seed ausgelost werden
- **THEN** ist kein Nachfolger gleich dem Titel, der eben endete

#### Scenario: Weiter im Modus Einmal

- **WHEN** ein Spieler im Modus Einmal bei „cat“ „Weiter“ wählt
- **THEN** endet „cat“ mit dem Grund `next`, und der nächste Titel in der Reihenfolge startet

#### Scenario: Weiter im Modus Schleife

- **WHEN** ein Spieler im Modus Schleife bei „cat“ „Weiter“ wählt
- **THEN** endet „cat“ mit dem Grund `next`, der nächste Titel in der Reihenfolge startet, und er wird im Modus Schleife wiederholt

#### Scenario: Weiter im Modus Zufall

- **WHEN** ein Spieler im Modus Zufall bei „cat“ „Weiter“ wählt
- **THEN** endet „cat“ mit dem Grund `next`, und ein zufälliger anderer Titel startet

#### Scenario: Ein einziger Titel im Modus Zufall

- **WHEN** der Katalog nur einen Titel hat und er im Modus Zufall endet
- **THEN** startet derselbe Titel mit `trigger=auto` neu

### Requirement: Vanilla-Schallplatten spielen den Plattenton des Clients nur für den Spieler

_A vanilla disc plays the client's own record sound, emitted by the player and heard by the player only._

Die Lobby MUSS für jede der 22 Plattenarten aus der festen Tabelle den eingebauten Plattenton des Clients (`SoundEvents.MUSIC_DISC_*`) als Sound-Paket an den Spieler senden, der die Platte gewählt hat. Quelle MUSS `Sound.Source.RECORD` sein, Emitter `Sound.Emitter.self()`, Lautstärke 1,0, Tonhöhe 1,0. Die Lobby DARF dafür keine Resource Pack und keine Notenblöcke benutzen.

#### Scenario: Platte starten

- **WHEN** ein Spieler im Menü „Vanilla-Platten“ die Platte „13“ anklickt
- **THEN** erhält er genau einen Plattenton `music_disc.13` mit Quelle RECORD, Emitter `self`, Lautstärke 1,0 und Tonhöhe 1,0

#### Scenario: Keine Platte für andere

- **WHEN** Spieler A die Platte „cat“ startet und Spieler B in derselben Lobby ist
- **THEN** erhält nur A den Plattenton, und B erhält kein Paket

### Requirement: Eine Vanilla-Platte endet nach Länge aus der Tabelle

_Lengths are approximate constants in code; the server ends the disc itself after the length plus a short margin._

Die Lobby MUSS für jede Vanilla-Platte eine Länge aus der festen Tabelle kennen. Nach Länge plus 5 Sekunden Puffer MUSS sie den Plattenton mit einem benannten Stopp beenden und den Grund `end` melden. Ob danach ein Titel startet, entscheidet der Modus (siehe „Titelende und Weiter richten sich nach dem Modus“).

#### Scenario: Ende nach der Länge

- **WHEN** die Platte „mellohi“ (Länge 1:36) gestartet wurde und 1:41 vergangen sind
- **THEN** hat die Lobby einen benannten Stopp für den Plattenton gesendet und meldet `end`

#### Scenario: Platte mit überlangem Ende

- **WHEN** die Länge aus der Tabelle größer als der tatsächliche Titel ist
- **THEN** endet der Ton beim Client von selbst, und die Lobby meldet `end` nach Ablauf der Länge plus Puffer

### Requirement: Stopp und Weiter beenden den Plattenton mit einem benannten Stopp

_Stopping a vanilla disc sends a named stop for that one sound; MIDI notes simply decay._

Die Lobby MUSS beim Stopp, bei „Weiter“ und beim Ende einer Vanilla-Platte einen benannten Stopp (`SoundStop.named(Key)`) für genau diesen Plattenton senden. Bei MIDI-Songs DARF die Lobby keinen Stopp-Befehl senden. Die Töne klingen dort nach.

#### Scenario: Stopp einer Vanilla-Platte

- **WHEN** ein Spieler „Stopp“ wählt, während die Platte „wait“ läuft
- **THEN** erhält er einen benannten Stopp für `music_disc.wait` und keinen weiteren Ton

#### Scenario: Weiter während einer Vanilla-Platte

- **WHEN** ein Spieler „Weiter“ wählt, während die Platte „13“ läuft, und der nächste Titel „cat“ ist
- **THEN** erhält er zuerst den benannten Stopp für `music_disc.13`, dann den Plattenton `music_disc.cat`

### Requirement: Töne hört nur der Spieler, der das Radio gestartet hat

_Sound is sent per player; nobody else hears either kind of song._

Die Lobby MUSS die Töne jedes Titels, Vanilla und MIDI, als Sound-Paket an genau den Spieler schicken, der das Radio gestartet hat. Die Lobby DARF in der Welt keinen Ton erzeugen.

#### Scenario: Zwei Spieler mit MIDI

- **WHEN** Spieler A ein MIDI-Stück startet und Spieler B in derselben Lobby ist
- **THEN** erhält nur A Töne, und B erhält keine Töne des Radios

#### Scenario: Keine Töne in der Welt

- **WHEN** ein Titel läuft
- **THEN** entsteht in der Welt kein Notenblock und kein Ton

### Requirement: Das Radio hat keinen Einstellungsschalter und keinen Befehl

_Start, stop and mode are in the menu only; the radio is not stored as a player setting._

Die Lobby MUSS das Radio nicht über die Spielereinstellungen steuern und MUSS keinen Befehl dafür anbieten. Start, Stopp und Modus MÜSSEN über das Menü geschehen.

#### Scenario: Keine Einstellung

- **WHEN** ein Spieler das Einstellungsmenü öffnet
- **THEN** gibt es darin keinen Eintrag für das Radio

### Requirement: MIDI-Songs werden beim Start geladen, fehlerhafte Dateien übersprungen

_A broken MIDI file is logged and skipped; it never stops the lobby, and the vanilla discs stay available._

Die Lobby MUSS die MIDI-Songs aus `radio/songs.yaml` und den zugehörigen Dateien einmal beim Start laden. Eine Datei, die fehlt, zu groß ist, nicht als MIDI gelesen werden kann oder eine ungültige Schallplatte hat, MUSS übersprungen und mit Dateiname und Grund protokolliert werden. Sie DARF den Start der Lobby NICHT abbrechen. Fehlt das Verzeichnis `radio/` oder ist keine Datei gültig, MUSS die Lobby trotzdem alle Vanilla-Platten anbieten.

#### Scenario: Kaputte Datei

- **WHEN** eine von drei MIDI-Dateien keine gültige MIDI-Datei ist
- **THEN** wird dieser Song übersprungen, die beiden anderen stehen im Menü „Eigene Songs“, und die Lobby startet

#### Scenario: Ohne MIDI-Dateien

- **WHEN** im Verzeichnis `radio/` keine gültige Datei liegt
- **THEN** startet die Lobby, das Radio-Item und die Vanilla-Platten sind da, und es fehlt nur der Umschalter „Eigene Songs“

#### Scenario: Doppelte Schallplatte

- **WHEN** zwei Einträge in `songs.yaml` dieselbe Schallplatte nennen, oder eine MIDI-Platte eine Vanilla-Platte ist
- **THEN** steht nur der erste im Menü, und der zweite wird übersprungen

### Requirement: MIDI-Noten werden auf Notenblock-Instrumente und Tonhöhen abgebildet

_Channels and programs choose the instrument, pitches are folded into the two-octave range, velocity sets the volume._

Die Lobby MUSS jede Note eines MIDI-Songs einem der 16 Notenblock-Instrumente zuordnen, Schlagzeug auf Kanal 10 nach Taste. Die Tonhöhe MUSS in den Bereich der Notenblöcke (Noten 0 bis 24) gebracht werden, und die Tonhöhe des Sounds MUSS `2^((n − 12) / 12)` sein. Die Lautstärke MUSS aus der Anschlagstärke `velocity / 127` folgen. Notenereignisse mit Anschlagstärke 0 MÜSSEN ignoriert werden.

#### Scenario: Schlagzeug

- **WHEN** eine Note mit Taste 36 auf Kanal 10 gespielt wird
- **THEN** wird das Instrument `basedrum` gewählt

#### Scenario: Tonhöhe

- **WHEN** die Note 12 gespielt wird
- **THEN** ist die Tonhöhe des Sounds 1,0

#### Scenario: Zu hohe Note

- **WHEN** eine Melodie-Note außerhalb des Bereichs liegt
- **THEN** wird sie um Oktaven verschoben, bis sie im Bereich liegt, und ihre Tonhöhenklasse bleibt erhalten

### Requirement: Der Takt ist der Server-Tick, MIDI-Noten werden je Tick gruppiert

_Timing is quantised to 50 ms; the quantisation is an accepted limit, not a bug._

Die Lobby MUSS die Noten eines MIDI-Songs auf den Server-Tick (50 ms) umrechnen und je Tick abspielen. Die Umrechnung MUSS die absolute Zeit verwenden, damit sich kein Fehler aufsummiert. Eine Note, die mehr als einen Tick zu spät ist, MUSS verworfen werden und DARF NICHT nachgeholt werden.

#### Scenario: Keine Drift

- **WHEN** ein Song mit 1000 Noten auf dem Raster gespielt wird
- **THEN** liegt die letzte Note nicht mehr als einen halben Tick neben ihrer Zeit

#### Scenario: Hänger im Tick

- **WHEN** der Takt zwei Ticks lang ausbleibt
- **THEN** werden die Noten aus diesem Zeitraum nicht als Schwall nachgespielt

### Requirement: Stoppgründe beenden die Wiedergabe sofort, für beide Arten

_The radio stops on logout, busy events, end of title, skip and stop; it never resumes on its own after busy or logout._

Die Lobby MUSS die Wiedergabe beenden, wenn der Spieler sich abmeldet, wenn ein `LobbyPlayerBusyEvent` mit `busy = true` für ihn kommt, wenn der Titel endet, und wenn der Spieler „Stopp“ oder „Weiter“ wählt. Das gilt für Vanilla-Platten und MIDI-Songs gleich. Die Lobby DARF die Wiedergabe nach `busy` oder nach einem Abmelden NICHT von selbst wieder aufnehmen. Nur das Titelende kann nach dem Modus einen Nachfolger starten.

#### Scenario: Abmelden

- **WHEN** ein Spieler das Radio hört und sich abmeldet
- **THEN** endet die Wiedergabe mit dem Grund `logout`, und der Modus wird vergessen

#### Scenario: Sprungtest und Serverwechsel

- **WHEN** ein Spieler einen Sprungtest startet oder eine Übergabe zu einem anderen Server beginnt
- **THEN** endet die Wiedergabe mit dem Grund `busy`, und sie startet danach nicht von selbst, auch nicht im Modus Schleife oder Zufall

#### Scenario: Fangen beendet das Radio nicht

- **WHEN** ein Spieler beim Fangen als Fänger oder Gejagter mitspielt
- **THEN** läuft das Radio weiter, weil Fangen kein `LobbyPlayerBusyEvent` sendet

### Requirement: Grenzen begrenzen Paketzahl und Speicher

_Packet budget per tick and per player, file size and note count limits for MIDI._

Die Lobby MUSS folgende Grenzen einhalten: eine MIDI-Datei höchstens 512 KiB, ein MIDI-Song höchstens 20 000 Noten und 15 Minuten, höchstens 18 MIDI-Songs, und höchstens 16 Noten je Tick und Spieler. Überschreitet ein MIDI-Song die Grenzen für Datei, Noten oder Länge, MUSS er übersprungen werden. Überschreitet eine Note das Budget je Tick, MUSS die leiseste Note verworfen werden.

#### Scenario: Zu viele Noten je Tick

- **WHEN** 20 Noten im selben Tick fällig sind
- **THEN** werden die 16 lautesten gespielt und vier verworfen

#### Scenario: Zu große Datei

- **WHEN** eine MIDI-Datei größer als 512 KiB ist
- **THEN** wird der Song übersprungen und protokolliert

### Requirement: Telemetrie und Meldungen

_Counters by kind, mode, trigger and reason, no player identifiers; texts in German and English._

Die Lobby MUSS die Zähler `titan.radio.plays{kind, mode, trigger}`, `titan.radio.stops{reason, kind}`, `titan.radio.notes.dropped` und `titan.radio.songs{result}` führen. `kind` ist `vanilla` oder `midi`, `mode` ist `once`, `loop` oder `shuffle`, `trigger` ist `user` oder `auto`. Attribute MÜSSEN ohne Spieler-Id auskommen. Ein Titelende mit Nachfolger MUSS als Stopp mit `reason=end` und als Start mit `trigger=auto` zählen. Alle Nutzertexte MÜSSEN in Deutsch und Englisch mit denselben Schlüsseln vorliegen, auch die Menütitel, der Umschalter und die Modusnamen.

#### Scenario: Stopp mit Grund und Art

- **WHEN** ein Spieler „Stopp“ wählt, während der MIDI-Song „sunrise“ läuft
- **THEN** wird `titan.radio.stops` mit `reason=user` und `kind=midi` um eins erhöht, ohne Spieler-Id

#### Scenario: Schleife zählt Ende und Neustart

- **WHEN** im Modus Schleife der Titel „cat“ endet und neu startet
- **THEN** wird `titan.radio.stops` mit `reason=end` und `kind=vanilla` um eins erhöht, und `titan.radio.plays` mit `kind=vanilla`, `mode=loop` und `trigger=auto` um eins

#### Scenario: Meldungen in beiden Sprachen

- **WHEN** die Aktionsleiste für einen Spieler mit deutscher oder englischer Sprache gerendert wird
- **THEN** ist der Text in der Sprache des Spielers, und jeder Schlüssel existiert in beiden Sprachen
