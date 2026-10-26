# Spec Delta

## ADDED Requirements

### Requirement: Das Radio öffnet sich über ein Hotbar-Item
_The radio is one hotbar item that opens a shared menu, and only exists when songs are loaded._

Die Lobby MUSS das Item `titan:radio` (Material `JUKEBOX`) auf Hotbar-Platz 5 anbieten, sobald mindestens ein gültiger Song geladen ist. Ein Klick auf das Item MUSS das Radio-Menü öffnen. Das Menü MUSS je Song eine Schallplatte zeigen sowie die Steuerelemente Stopp und Weiter.

#### Scenario: Menü öffnen

- **WHEN** ein Spieler das Radio-Item benutzt
- **THEN** sieht er das Menü mit einer Schallplatte je geladenem Song

#### Scenario: Ohne Songs kein Item

- **WHEN** im Verzeichnis `radio/` kein gültiger Song liegt
- **THEN** gibt es kein Radio-Item und kein Menü, und das Modul sendet nichts

### Requirement: Töne hört nur der Spieler, der das Radio gestartet hat
_Sound is sent per player as a non-positional packet; nobody else hears it._

Die Lobby MUSS die Töne eines Songs als Sound-Paket an genau den Spieler schicken, der das Radio gestartet hat. Die Töne MÜSSEN nicht räumlich sein. Die Lobby DARF in der Welt keinen Ton erzeugen.

#### Scenario: Zwei Spieler

- **WHEN** Spieler A ein Lied startet und Spieler B in derselben Lobby ist
- **THEN** erhält nur A Töne, und B erhält keine Töne des Radios

#### Scenario: Keine Töne in der Welt

- **WHEN** ein Lied läuft
- **THEN** entsteht in der Welt kein Notenblock und kein Ton

### Requirement: Das Radio hat keinen Einstellungsschalter und keinen Befehl
_Start and stop happen in the menu only; the radio is not stored as a player setting._

Die Lobby MUSS das Radio nicht über die Spielereinstellungen steuern und MUSS keinen Befehl dafür anbieten. Start und Stopp MÜSSEN über das Menü geschehen.

#### Scenario: Keine Einstellung

- **WHEN** ein Spieler das Einstellungsmenü öffnet
- **THEN** gibt es darin keinen Eintrag für das Radio

### Requirement: Songs werden beim Start geladen, fehlerhafte Dateien werden übersprungen
_A broken file is logged and skipped; it never stops the lobby._

Die Lobby MUSS die Songs aus `radio/songs.yaml` und den zugehörigen MIDI-Dateien einmal beim Start laden. Eine Datei, die fehlt, zu groß ist, nicht als MIDI gelesen werden kann oder eine ungültige Schallplatte hat, MUSS übersprungen und mit Dateiname und Grund protokolliert werden. Sie DARF den Start der Lobby NICHT abbrechen.

#### Scenario: Kaputte Datei

- **WHEN** eine von drei MIDI-Dateien keine gültige MIDI-Datei ist
- **THEN** wird dieser Song übersprungen, die beiden anderen stehen im Menü, und die Lobby startet

#### Scenario: Doppelte Schallplatte

- **WHEN** zwei Einträge in `songs.yaml` dieselbe Schallplatte nennen
- **THEN** steht nur der erste im Menü, und der zweite wird übersprungen

### Requirement: MIDI-Noten werden auf Notenblock-Instrumente und Tonhöhen abgebildet
_Channels and programs choose the instrument, pitches are folded into the two-octave range, velocity sets the volume._

Die Lobby MUSS jede Note einem der 16 Notenblock-Instrumente zuordnen, Schlagzeug auf Kanal 10 nach Taste. Die Tonhöhe MUSS in den Bereich der Notenblöcke (Noten 0 bis 24) gebracht werden, und die Tonhöhe des Sounds MUSS `2^((n − 12) / 12)` sein. Die Lautstärke MUSS aus der Anschlagstärke `velocity / 127` folgen. Notenereignisse mit Anschlagstärke 0 MÜSSEN ignoriert werden.

#### Scenario: Schlagzeug

- **WHEN** eine Note mit Taste 36 auf Kanal 10 gespielt wird
- **THEN** wird das Instrument `basedrum` gewählt

#### Scenario: Tonhöhe

- **WHEN** die Note 12 gespielt wird
- **THEN** ist die Tonhöhe des Sounds 1,0

#### Scenario: Zu hohe Note

- **WHEN** eine Melodie-Note außerhalb des Bereichs liegt
- **THEN** wird sie um Oktaven verschoben, bis sie im Bereich liegt, und ihre Tonhöhenklasse bleibt erhalten

### Requirement: Der Takt ist der Server-Tick, Noten werden je Tick gruppiert
_Timing is quantised to 50 ms; the quantisation is an accepted limit, not a bug._

Die Lobby MUSS die Noten eines Songs auf den Server-Tick (50 ms) umrechnen und je Tick abspielen. Die Umrechnung MUSS die absolute Zeit verwenden, damit sich kein Fehler aufsummiert. Eine Note, die mehr als einen Tick zu spät ist, MUSS verworfen werden und DARF NICHT nachgeholt werden.

#### Scenario: Keine Drift

- **WHEN** ein Song mit 1000 Noten auf dem Raster gespielt wird
- **THEN** liegt die letzte Note nicht mehr als einen halben Tick neben ihrer Zeit

#### Scenario: Hänger im Tick

- **WHEN** der Takt zwei Ticks lang ausbleibt
- **THEN** werden die Noten aus diesem Zeitraum nicht als Schwall nachgespielt

### Requirement: Stoppgründe beenden die Wiedergabe sofort
_The radio stops on logout, busy events, end of song, skip and stop; it never resumes on its own._

Die Lobby MUSS die Wiedergabe beenden, wenn der Spieler sich abmeldet, wenn ein `LobbyPlayerBusyEvent` mit `busy = true` für ihn kommt, wenn der Song endet, und wenn der Spieler „Stopp“ oder „Weiter“ wählt. Die Lobby DARF die Wiedergabe NICHT von selbst wieder aufnehmen.

#### Scenario: Abmelden

- **WHEN** ein Spieler das Radio hört und sich abmeldet
- **THEN** endet die Wiedergabe mit dem Grund `logout`

#### Scenario: Sprungtest und Serverwechsel

- **WHEN** ein Spieler einen Sprungtest startet oder eine Übergabe zu einem anderen Server beginnt
- **THEN** endet die Wiedergabe mit dem Grund `busy`, und sie startet danach nicht von selbst

#### Scenario: Weiter

- **WHEN** ein Spieler „Weiter“ wählt
- **THEN** endet der laufende Song mit dem Grund `next`, und der nächste Song in der Reihenfolge startet

#### Scenario: Song-Ende

- **WHEN** der letzte Ton eines Songs gespielt ist
- **THEN** endet die Wiedergabe mit dem Grund `end`, und es folgt kein weiterer Song

#### Scenario: Fangen beendet das Radio nicht

- **WHEN** ein Spieler beim Fangen als Fänger oder Gejagter mitspielt
- **THEN** läuft das Radio weiter, weil Fangen kein `LobbyPlayerBusyEvent` sendet

### Requirement: Grenzen begrenzen Paketzahl und Speicher
_Packet budget per tick and per player, file size and note count limits._

Die Lobby MUSS folgende Grenzen einhalten: eine Datei höchstens 512 KiB, ein Song höchstens 20 000 Noten und 15 Minuten, höchstens 18 Songs, und höchstens 16 Noten je Tick und Spieler. Überschreitet ein Song die Grenzen für Datei, Noten oder Länge, MUSS er übersprungen werden. Überschreitet eine Note das Budget je Tick, MUSS die leiseste Note verworfen werden.

#### Scenario: Zu viele Noten je Tick

- **WHEN** 20 Noten im selben Tick fällig sind
- **THEN** werden die 16 lautesten gespielt und vier verworfen

#### Scenario: Zu große Datei

- **WHEN** eine MIDI-Datei größer als 512 KiB ist
- **THEN** wird der Song übersprungen und protokolliert

### Requirement: Telemetrie und Meldungen

_Counters by reason, no player identifiers; texts in German and English._

Die Lobby MUSS die Zähler `titan.radio.plays`, `titan.radio.stops{reason}`, `titan.radio.notes.dropped` und `titan.radio.songs{result}` führen. Attribute MÜSSEN ohne Spieler-Id auskommen. Alle Nutzertexte MÜSSEN in Deutsch und Englisch mit denselben Schlüsseln vorliegen.

#### Scenario: Stopp mit Grund

- **WHEN** ein Spieler „Stopp“ wählt
- **THEN** wird `titan.radio.stops` mit `reason=user` um eins erhöht, ohne Spieler-Id

#### Scenario: Meldungen in beiden Sprachen

- **WHEN** die Aktionsleiste für einen Spieler mit deutscher oder englischer Sprache gerendert wird
- **THEN** ist der Text in der Sprache des Spielers, und jeder Schlüssel existiert in beiden Sprachen
