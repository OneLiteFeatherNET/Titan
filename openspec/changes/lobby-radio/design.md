# Design

## Kontext

Eine Column ist ein `@Singleton` unter `features/<name>`, das nur von `core` abhängt, und hängt seinen `FeatureNode` an den `titan`-Knoten. Belegte Prioritäten: 100, 200, 300, 400, 450, 500, 550 (Einstellungen), 600, 620 (Fangen), 650, 700, 750 (Wurf), 800, 900, 920, 930, 1000. Hotbar-Plätze: 0 (Sprungtest), 4 (Navigator), 7 (Einstellungen), 8 (Lobbywechsel).

Das Radio spielt zwei Arten von Musik, die sich nur in der Wiedergabe unterscheiden: Eine Vanilla-Schallplatte lässt der Client selbst spielen (ein benannter Ton, der irgendwann endet), ein MIDI-Stück schickt der Server Note für Note. Beide brauchen denselben Lebenszyklus: starten, stoppen aus fünf Gründen, Telemetrie, Sichtbarkeit für genau einen Spieler. Deshalb gibt es ein gemeinsames Songmodell und eine gemeinsame Wiedergabe mit zwei Inhalten.

Der Takt ist der Server-Tick, 20 pro Sekunde (50 ms). Töne gehen als Sound-Pakete an einen Spieler, nicht über Notenblöcke in der Welt.

## Ziele / Nicht-Ziele

**Ziele**

- Ein Item öffnet das Radio. Start, Weiter und Stopp, sonst nichts.
- Vanilla-Schallplatten sind immer verfügbar, auch ohne MIDI-Dateien.
- Ton nur für den Spieler, der das Radio gestartet hat.
- MIDI-Dateien im Template, beim Start geladen, fehlerhafte Dateien übersprungen.
- Alles, was reine Logik ist (Parsen, Abbilden, Takt, Budget, Plattentabelle, Wiedergabe), ohne Server testbar.

**Nicht-Ziele**

- Einstellung, Befehl, Otis-Anbindung (D1).
- Sub-Tick-Genauigkeit, Note-Off, Pedal, Pitch-Bend (D5).
- Audioausgabe auf dem Server.
- Exakte Plattenlängen aus den Client-Dateien (D11).

## Entscheidungen

### D1 Kein Einstellungsschalter, Start und Stopp im Menü

Das Radio hat keinen Eintrag in den Spielereinstellungen und keinen Befehl. Der Grund:

- Es passiert nichts ohne Klick. Der Spieler hat sich damit ausdrücklich dafür entschieden, und das ist das Opt-in.
- Der Zustand gilt nur für die Sitzung. Abmelden, Serverwechsel und Sprungtest stoppen ihn, also gibt es nichts, was man speichern müsste. Eine Einstellung würde einen zweiten Weg schaffen, der beim nächsten Login sofort wieder stoppen müsste.
- Die Einstellungen (`lobby-settings`) würden eine Otis-Abhängigkeit für einen Schalter einführen, den niemand braucht.

Sollte später ein Start beim Beitritt gewünscht sein, ist das ein eigener Change.

### D2 Bausteine im Modul `features/radio`

- **`RadioSong`** (sealed interface, `permits VanillaDisc, MidiSong`): gemeinsame Felder `title()`, `artist()`, `disc()` (`Material`) und `kind()` (Enum `RadioKind`: `VANILLA`, `MIDI`). `VanillaDisc` ist ein Record mit Plattenname, Plattenton (`SoundEvent`) und Länge in Ticks. `MidiSong` ist ein Record mit Titel, Künstler, Schallplatte und der Notenliste (D3, D4).
- **`RadioPlayback`** (rein, ohne Minestom): ein Objekt je Spieler mit einem Song, einem Stand und einem Grund zum Beenden. `start`, `tick`, `stop(reason)` sind für beide Arten gleich. Nur der Inhalt unterscheidet sich: Eine MIDI-Wiedergabe spielt fällige Noten (D5), eine Vanilla-Wiedergabe startet einmal den Plattenton und endet nach der Länge aus der Tabelle (D11). Die Töne gehen über das Interface **`RadioSink`** (`note`, `playRecord`, `stopRecord`). Die Produktion setzt es mit dem Spieler um, die Tests mit einer Aufzeichnung.
- **`RadioModule`** (`@Singleton`): `@PostConstruct` lädt die MIDI-Songs, baut die Menüs, hängt den `FeatureNode` mit Priorität 680 an und startet einen Takt-Task (`TaskSchedule.tick(1)`), der alle aktiven Wiedergaben ruft. `@PreDestroy` stoppt alle mit `SHUTDOWN`, hebt den Takt auf, schließt den Knoten und entfernt die Menüs.
- **`RadioItems`** (`@Factory`, `Optional<LobbyItem>`): `titan:radio`, Material `JUKEBOX`, Hotbar-Platz 5. Immer vorhanden, weil die Vanilla-Platten immer da sind.
- **`RadioMenuBuilder`**: der einzige Builder des Moduls. Er baut zwei Aves-Inventare, eines für Vanilla-Platten, eines für eigene Songs (D12). Die Inventare sind geteilt und ändern sich nicht je Spieler.
- **`RadioPlaybacks`**: eine `ConcurrentHashMap<UUID, RadioPlayback>`, Start und Stopp je Spieler. Start und Stopp kommen von Klicks und Ereignissen, der Takt läuft auf dem Scheduler-Thread. Deshalb ist die Map nebenläufig, und eine Wiedergabe hat ein `closed`-Flag.

### D3 MIDI lesen

- Die Datei wird vor dem Lesen auf die Größe geprüft (D8). Danach liest `MidiSystem.getSequence(InputStream)` aus `javax.sound.midi` die Sequenz. Das braucht keinen Synthesizer, keinen Sequencer und keine Audio-Hardware.
- Aus jeder Spur werden Note-On-Ereignisse mit Anschlagstärke über 0 übernommen. Note-Off wird ignoriert, weil Notenblock-Töne feste Länge haben.
- Programmwechsel wird je Kanal mitgeführt. Ein Ton bekommt das Programm, das auf seinem Kanal zu seinem Zeitpunkt gilt.
- Ticks werden mit `MidiUtils.tick2microsecond` in Mikrosekunden umgerechnet. Das berücksichtigt Tempowechsel automatisch.
- Jeder Fehler ist eine `SongFormatException` mit Grund (zu groß, kein MIDI, zu viele Noten, zu lang). Nichts davon ist fatal.

### D4 Instrument, Tonhöhe, Lautstärke (nur MIDI)

**Instrument je Kanal und Programm.** Schlagzeug liegt auf MIDI-Kanal 10 (Index 9). Melodische Kanäle folgen dem GM-Programm:

| GM-Programme | Notenblock-Instrument |
| --- | --- |
| 0–7 Klavier | `harp` |
| 8–11 Chromatisches Schlagwerk (Celesta, Glockenspiel, Spieluhr, Vibrafon) | `chime` |
| 12–13 Marimba, Xylophon | `xylophone` |
| 14 Tubular Bells | `bell` |
| 15 Dulcimer | `iron_xylophone` |
| 16–23 Orgel | `flute` |
| 24–31 Gitarre | `guitar` |
| 32–39 Bass | `bass` |
| 40–55 Streicher und Ensemble | `harp` |
| 56–63 Blech | `didgeridoo` |
| 64–79 Holzbläser und Flöten | `flute` |
| 80–87 Synth-Lead | `bit` |
| 88–103 Synth-Pad und Effekte | `pling` |
| 104–111 Volksmusik (Sitar, Banjo) | `banjo` |
| 112–119 Schlagwerk (Tuned Percussion) | `cow_bell` |
| 120–127 Geräusche | `hat` |

Schlagzeug (Kanal 10) nach Taste: Kick (35, 36) `basedrum`; Snare und Clap (37–40) `snare`; geschlossenes und offenes Hi-Hat (42, 44, 46) `hat`; Cowbell (56) `cow_bell`; alles andere `hat`. Die Tabelle ist eine erste Fassung und wird nach Gehör angepasst, ohne den Code umzubauen.

**Tonhöhe.** Ein Notenblock spielt Noten 0 bis 24, also F#3 bis F#5 (MIDI 54 bis 78). Die Note `n` wird über `n = midi − 54` bestimmt, und die Tonhöhe des Sounds ist

    pitch(n) = 2^((n − 12) / 12)

Das gibt 0,5 für n = 0, 1,0 für n = 12 (F#4, MIDI 66) und 2,0 für n = 24.

Der Bereich wird in zwei Schritten erreicht:

1. **Transponieren pro Song:** Der Median der melodischen Noten (ohne Schlagzeug) wird auf die Mitte des Bereichs gebracht. Das Ergebnis ist ein Vielfaches von 12. Bei Gleichstand wird nach unten gerundet.
2. **Falten je Note:** Liegt eine Note danach noch außerhalb, wird sie um Oktaven verschoben, bis sie passt. Liegt sie zu tief, wird sie erhöht, liegt sie zu hoch, gesenkt. Die Tonhöhenklasse bleibt erhalten.

**Lautstärke.** Die Lautstärke ist `velocity / 127`, also 0,008 bis 1,0. Anschlagstärke 0 ist Note-Off und wird nicht gespielt.

### D5 Takt: 50 ms, ohne Drift (nur MIDI)

- Jede Note bekommt eine Zeit in Mikrosekunden aus der Sequenz. Daraus folgt der Spiel-Tick mit `tick = round(µs / 50 000)`, also `(µs + 25 000) / 50 000` in ganzer Arithmetik. Die Rechnung nimmt die absolute Zeit, nicht die Summe der Abstände, damit sich kein Fehler aufschaukelt.
- Eine Wiedergabe hält ein sortiertes Array von Noten und einen Cursor. Ein Takt läuft den Cursor nur vorwärts ab, ohne die ganze Liste zu durchsuchen.
- Je Tick spielt die Wiedergabe alle fälligen Noten. Sie sind nach Anschlagstärke absteigend sortiert, und die ersten 16 gehen raus (D8). Alles darüber fällt weg, und `titan.radio.notes.dropped` zählt es.
- Eine Note, die mehr als einen Tick zu spät ist, wird verworfen und nicht nachgeholt. Sonst gäbe es nach einem Hänger einen Schwall von Tönen.

**Akzeptierte Einschränkung:** Die Töne liegen auf dem 50-ms-Raster, mit bis zu ±25 ms Abweichung. Schnelle Figuren (zum Beispiel Zweiunddreißigstel bei 120 BPM, 62,5 ms) fallen teils zusammen. Das ist für Hintergrundmusik vertretbar, und Notenblöcke in der Welt haben dieselbe Grenze.

**Verworfene Alternativen:**

- Sub-Tick-Timing: Sound-Pakete können nicht verzögert werden. Ein Paket wirkt, sobald es ankommt.
- Eigene Sounds über ein Resource Pack: verboten, Vanilla only.
- Echte Notenblöcke in der Welt: nicht privat, alle hören mit, und sie belasten die Welt.

### D6 Stoppgründe und Wechselwirkungen (beide Arten)

| Grund (Telemetrie) | Auslöser |
| --- | --- |
| `user` | Spieler klickt „Stopp“. |
| `next` | Spieler klickt „Weiter“ oder eine andere Schallplatte. Der neue Titel startet sofort. |
| `end` | Der Titel ist zu Ende: bei MIDI die letzte Note, bei Vanilla die Länge aus D11 plus Puffer. Keine Schleife, kein automatischer Nachfolger. |
| `logout` | `PlayerDisconnectEvent`. Der Client ist weg, also wird nichts mehr gesendet. |
| `busy` | `LobbyPlayerBusyEvent` mit `busy = true`, gesendet vom Sprungtest beim Start eines Laufs und vom Portal bei Beginn einer Übergabe. Der Serverwechsel ist damit abgedeckt. |
| `shutdown` | `@PreDestroy` des Moduls. |

**Was der Stopp am Client tut:**

- **MIDI:** Stopp sendet nichts. Die laufenden Notenblock-Töne klingen nach, ein bis zwei Sekunden.
- **Vanilla:** Stopp sendet einen benannten Stopp (`SoundStop.named(Key)`) für genau diesen Plattenton. Ohne ihn würde der Titel über den Stopp hinaus weiterlaufen, weil er eine lange Datei ist. Der benannte Stopp schneidet nur diesen Ton ab, nicht alle Töne der Kategorie. Liegt dieselbe Platte zufällig in einem Plattenspieler der Welt, hört der Spieler sie in diesem Moment auch nicht.

**Weitere Regeln:**

- **Kein Wiedereinstieg nach `busy`.** Nach einem Sprungtest oder einer Übergabe startet der Spieler das Radio neu.
- **Fangen, Tanz, Sitzen, Elytra, Spawn und Respawn** haben keine Wirkung. Fangen sendet kein `LobbyPlayerBusyEvent`, und der Ton ist privat. Das Radio spielt also während einer Jagd weiter (offene Frage 5).
- Ein `LobbyPlayerBusyEvent` mit `busy = false` tut nichts. Ein Listener darf nie werfen, weil das Ereignis synchron ausgelöst wird.

### D7 Songs laden, Konfiguration, Modul immer an

Im Arbeitsverzeichnis der Lobby liegt optional ein Verzeichnis `radio/` mit `songs.yaml` und den MIDI-Dateien. Die Datei ist eine Liste:

```yaml
songs:
  - id: sunrise
    title: Sunrise
    artist: Beispiel
    disc: MUSIC_DISC_CAT
    file: sunrise.mid
```

- Die Datei wird einmal beim Start gelesen. Der Takt liest nie von der Platte.
- Das YAML liest **SnakeYAML**. Die Version steht schon im Katalog und wird heute nur in Tests genutzt. Sie wird Laufzeitabhängigkeit von `features/radio`. Ein Parser aus `avaje-config` wäre die Alternative; geprüft wird das vor der Umsetzung.
- Regeln beim Laden: fehlt die Datei oder ist sie nicht lesbar, wird der Song übersprungen (WARN mit Dateiname). Die Schallplatte muss ein `MUSIC_DISC_*` sein. Eine Schallplatte, die schon eine Vanilla-Platte ist oder bereits einem MIDI-Stück gehört, wird übersprungen, damit jede Platte im Menü einmal vorkommt. `id` und `disc` müssen je Liste eindeutig sein; der zweite Treffer wird übersprungen. Ein Parserfehler überspringt nur den Song. Überschuss über 18 Songs wird übersprungen (WARN).
- **Das Modul ist immer an.** Es hängt sich immer an, auch ohne `radio/`. Ohne MIDI-Datei fehlt nur der Umschalter „Eigene Songs“ (D12); die Vanilla-Platten bleiben. Es gibt keinen Schalter, der das Modul aus schaltet, weil keine Konfiguration nötig ist, um es zu nutzen.
- Der Hinweis „Keine eigenen Songs geladen“ erscheint als WARN einmal beim Start, nicht als Fehler.

### D8 Grenzen

| Grenze | Wert | Wirkung bei Überschreitung |
| --- | --- | --- |
| Dateigröße (MIDI) | 512 KiB | Song wird übersprungen, WARN. |
| Noten je MIDI-Song | 20 000 | Song wird übersprungen, WARN. |
| Länge je MIDI-Song | 15 Minuten (18 000 Ticks) | Song wird übersprungen, WARN. |
| MIDI-Songs je Lobby | 18 (zwei Reihen im Menü) | Überschuss wird übersprungen, WARN. |
| Vanilla-Platten | 22, fest im Code | Keine Grenze nötig, die Liste ist vollständig. |
| Noten je Tick und Spieler (MIDI) | 16 | Die leisesten fallen weg, `titan.radio.notes.dropped` zählt sie. |
| Gleichzeitige Wiedergaben | eine je Spieler | Ein neuer Start ersetzt die alte Wiedergabe. |

Die Werte stehen als Konstanten im Code, wie bei den anderen Columns. Die Speichergrenze liegt bei etwa 3 MB für 18 volle MIDI-Songs.

Die Packet-Grenze von 16 Noten je Tick ergibt höchstens 320 Sound-Pakete je Sekunde und Spieler. Ein typischer Song liegt darunter. Eine Vanilla-Platte sendet ein Paket beim Start und eins beim Stopp.

### D9 Telemetrie und Logs

- `titan.radio.plays{kind}` (Zähler): jeder Start, auch der Neustart durch eine andere Schallplatte. `kind` ist `vanilla` oder `midi`.
- `titan.radio.stops{reason, kind}` (Zähler): je Grund aus D6 und je Art. Keine Spieler-Id.
- `titan.radio.notes.dropped` (Zähler): Noten, die das Budget oder die Verspätung verworfen hat. Nur MIDI.
- `titan.radio.songs{result}` (Zähler): `loaded` und `rejected` beim Start, je MIDI-Datei. Vanilla-Platten zählen nicht.
- Kein Span, weil Starts und Stopps zu häufig sind, um sie einzeln zu verfolgen. Dasselbe gilt für Noten.
- Logs: WARN je abgelehnter Datei mit Dateiname und Grund. INFO einmal beim Start mit der Zahl geladener MIDI-Songs. Keine Spielernamen in Logs.

### D10 Meldungen

Dateien `titan/radio/messages_de.properties` und `messages_en.properties`, mit denselben Schlüsseln. Benötigt werden: Itemname und Hinweis, Menütitel für beide Menüs, Umschalter „Eigene Songs“ und „Vanilla-Platten“, Lore der Schallplatte (Titel, Künstler, Art), Stopp, Weiter, Aktionsleiste „Läuft: {0} von {1}“, „Radio aus“ bei Stopp und Abbruch durch `busy`. Ein Test prüft, dass beide Sprachen dieselben Schlüssel haben und alle rendern.

### D11 Vanilla-Schallplatten

**Quelle der Liste.** Die 22 Plattenarten sind gegen `Material.MUSIC_DISC_*` und `SoundEvents.MUSIC_DISC_*` der Minestom-Version `2026.10.07-26.2` geprüft. Die Liste des Owners nennt 21 Platten. Minestom kennt zusätzlich `MUSIC_DISC_BOUNCE`; sie ist aufgenommen (offene Frage 6).

**Tabelle im Code.** Eine kleine feste Tabelle in `VanillaDiscs` (ein Enum oder eine unveränderliche Liste) mit Schallplatte, Plattenton und Länge. Seltene Daten liegen im Code, nicht in der Konfiguration. Die Plattenton-Id ist `SoundEvents.MUSIC_DISC_<NAME>` aus Minestom, der Titel steht in den Meldungen.

**Längen.** Die Längen sind Näherungen in Sekunden, nicht aus den Client-Dateien gemessen. Die Umsetzung misst sie gegen die Plattendateien des Clients (Aufgabe 3.1). Bis dahin gilt der Wert der Tabelle, und ein falscher Wert verschiebt nur den Zeitpunkt von `end`:

| Platte | Plattenton | Länge (ca.) |
| --- | --- | --- |
| 13 | `music_disc.13` | 2:58 |
| cat | `music_disc.cat` | 3:05 |
| blocks | `music_disc.blocks` | 5:45 |
| chirp | `music_disc.chirp` | 3:05 |
| far | `music_disc.far` | 2:54 |
| mall | `music_disc.mall` | 3:17 |
| mellohi | `music_disc.mellohi` | 1:36 |
| stal | `music_disc.stal` | 2:30 |
| strad | `music_disc.strad` | 3:08 |
| ward | `music_disc.ward` | 4:11 |
| 11 | `music_disc.11` | 1:11 |
| wait | `music_disc.wait` | 3:58 |
| otherside | `music_disc.otherside` | 3:15 |
| 5 | `music_disc.5` | 2:58 |
| pigstep | `music_disc.pigstep` | 2:28 |
| relic | `music_disc.relic` | 3:17 |
| creator | `music_disc.creator` | 3:00 |
| creator_music_box | `music_disc.creator_music_box` | 1:13 |
| precipice | `music_disc.precipice` | 5:59 |
| tears | `music_disc.tears` | Platzhalter 3:00, prüfen |
| lava_chicken | `music_disc.lava_chicken` | Platzhalter 3:00, prüfen |
| bounce | `music_disc.bounce` | Platzhalter 3:00, prüfen |

Die Längen der drei Platzhalter sind nicht bekannt und müssen in Aufgabe 3.1 gemessen werden.

**Ablauf.**

- **Start:** Die Wiedergabe sendet den Plattenton an den Spieler, Quelle `Sound.Source.RECORD`, Emitter `Sound.Emitter.self()`, Lautstärke 1,0, Tonhöhe 1,0. Der Emitter ist der Spieler selbst, der Ton ist nicht räumlich und folgt dem Spieler beim Bewegen. Er ist in voller Lautstärke und nur bei ihm hörbar.
- **Lauf:** Der Client spielt die Datei selbst. Der Server zählt nur die Länge aus der Tabelle mit, plus einen Puffer von 5 Sekunden.
- **Ende:** Nach Länge plus Puffer sendet der Server den benannten Stopp, damit ein Überhang abgeschnitten wird, und meldet `end`. Ist der Tabellenwert zu lang, endet der Ton beim Client von selbst, und `end` kommt später als nötig. Ist er zu kurz, schneidet der Server den Titel ab, höchstens nach dem Puffer.
- **Stopp, Weiter, Busy, Shutdown:** benannter Stopp, wie in D6 beschrieben.
- **Keine Resource Pack, keine Notenblöcke:** Die Platte ist ein Vanilla-Sound, den jeder Client schon hat.

### D12 Menü: zwei feste Menüs statt Seiten

Die Zahl passt nicht in eine Kiste: 22 Vanilla-Platten und bis zu 18 eigene Songs ergeben 40 Symbole, und eine Kiste hat 27 Plätze (drei Reihen).

**Entscheidung: zwei feste Menüs mit Umschalter, keine Seiten.**

- Seiten bräuchten einen Zustand pro Spieler (auf welcher Seite er steht). Dann wären die Inventare nicht mehr geteilt, und pro Spieler würde je Öffnen gebaut. Zwei feste Menüs bleiben geteilt, wie das Navigator-Untermenü.
- Ein Umschalter mit zwei Zielen ist mit Aves so einfach wie ein Klick-Handler, der das andere Inventar öffnet.
- Die Trennung ist für den Spieler klar: „Vanilla-Platten“ und „Eigene Songs“ sind verschiedene Dinge.

**Layout je Menü (3 Reihen, 27 Plätze):**

- **Vanilla-Platten:** 22 Platten auf den Plätzen 0 bis 21. Stopp auf Platz 22, Weiter auf Platz 23, Umschalter „Eigene Songs“ auf Platz 26. Der Umschalter fehlt, wenn es keine MIDI-Songs gibt.
- **Eigene Songs:** bis zu 18 Platten auf den Plätzen 0 bis 17. Stopp auf 22, Weiter auf 23, Umschalter „Vanilla-Platten“ auf 26.

Der Hotbar-Eintrag öffnet immer das Menü „Vanilla-Platten“. Ein Klick auf eine Platte startet genau den Song dieser Platte, Art egal.

## Risiken / Abwägungen

- **Qualität der MIDI-Stücke:** 50 ms Raster, keine Note-Off-Länge, keine Pedale und keine Lautstärkekurven. Für Hintergrundmusik ausreichend, für Präzisionsstücke nicht.
- **Längen der Vanilla-Platten:** Näherungen. Die Folge ist nur der Zeitpunkt von `end` und ein Abschnitt beim Abschneiden. Die Messung ist Aufgabe 3.1.
- **Rechte der MIDI-Musik:** Die Dateien kommen vom Betreiber und liegen nicht im Repository. Die Rechte der Stücke trägt der Betreiber. Die Vanilla-Platten sind Teil des Spiels.
- **`java.desktop` im Image:** `javax.sound.midi` liegt im Modul `java.desktop`. Ist das Laufzeitimage verkleinert (jlink), muss das Modul drin sein. Vor dem Rollout prüfen.
- **Hotbar-Platz 5:** Ein Item mehr auf der Leiste. Der Konflikt-Test der Hotbar prüft die Belegung.
- **Last:** Viele Spieler mit vollen MIDI-Songs erzeugen viele Pakete. Die Grenze von 16 je Tick begrenzt das pro Spieler. Ein Lasttest mit 50 Spielern gehört in den Abnahmeschritt (Aufgabe 7.3).
- **Benannter Stopp:** Er wirkt auf den Ton des Clients, nicht auf die Welt. Der Nebeneffekt bei gleicher Platte im Plattenspieler ist in D6 beschrieben.

## Test Strategy

Alle Tests folgen F.I.R.S.T.: keine Sleeps, keine Systemzeit, frische Fixtures je Test, Takt explizit.

- **Unit, rein (Kern der Änderung):**
  - `MidiSongParserTest`: Sequenzen werden im Speicher gebaut (`new Sequence(Sequence.PPQ, 24)`, `Track.add(new MidiEvent(...))`), mit `MidiSystem.write` in ein `ByteArrayOutputStream` geschrieben und wieder gelesen. Fälle: Tempo 120 BPM, Tempowechsel, Note-Off ignoriert, Anschlagstärke 0 ignoriert, kaputte Bytes, zu viele Noten, zu lang.
  - `InstrumentMapperTest`: je Programmgruppe ein Instrument, Schlagzeug nach Taste.
  - `PitchTest`: `pitch(0) = 0,5`, `pitch(12) = 1,0`, `pitch(24) = 2,0`; Transponieren (Median), Falten (Oktaven hoch und runter), Gleichstand.
  - `RadioPlaybackTest` mit `RecordingRadioSink` und explizitem `tick()`: MIDI-Quantisierung, MIDI-Budget 16 nach Lautstärke, verspätete Noten verfallen, Ende setzt `END`, Stopp ist idempotent, nach `closed` kommen keine Töne mehr.
  - `RadioSelectionTest`: „Weiter“ wechselt auf den nächsten Song und wrapt von der letzten Platte des Menüs auf die erste; Vanilla und MIDI werden in der gleichen Reihenfolge abgelaufen, die das Menü zeigt.
- **Unit, Vanilla-Tabelle:**
  - `VanillaDiscsTest`: Die Tabelle ist vollständig. Sie enthält genau die Materialien `MUSIC_DISC_*` aus `Material.values()`, je einen Plattenton aus `SoundEvents`, keine Duplikate, und jede Länge liegt über null und unter 15 Minuten. Kommt in einer künftigen Minestom-Version eine Platte hinzu, schlägt der Test fehl.
  - `VanillaDiscPlaybackTest` mit `RecordingRadioSink`: Start sendet genau einen Plattenton `RECORD` mit Emitter `self`, Lautstärke 1,0, Tonhöhe 1,0; Stopp sendet genau einen benannten Stopp mit demselben Schlüssel; Länge plus Puffer löst `END` und einen benannten Stopp aus; „Weiter“ während Vanilla sendet erst den Stopp, dann den Start des nächsten Titels.
- **Unit, Laden (`@TempDir`):** `SongCatalogTest`: fehlende Datei, falscher Schallplattentyp, Platte schon vergeben (Vanilla oder MIDI), doppelte `id` oder `disc`, zu viele Songs. Ohne `radio/` bleibt die Liste leer und die Vanilla-Platten sind trotzdem da.
- **Unit, Telemetrie und Meldungen:** `RadioTelemetryTest` mit `TestTelemetry` (Attribute `kind` und `reason`, keine Spieler-Id); `RadioMessagesTest` (gleiche Schlüssel, alle rendern).
- **Env (Minestom, `env.tick()`, Testverbindung):** `RadioModuleIntegrationTest`:
  - Item auf Platz 5; Klick startet, Töne gehen nur an den Startenden (zwei Spieler, jeweils eigene Verbindung, die Pakete werden erfasst).
  - Vanilla-Platte: Start, Stopp und Ende werden auf der Verbindung des Spielers erfasst (Plattenton-Paket mit Quelle RECORD, benannter Stopp mit demselben Schlüssel), die Verbindung des anderen Spielers bleibt leer.
  - Abmelden stoppt; `busy` stoppt; Ende stoppt; `close()` lässt keinen Listener zurück (Muster `SpawnModuleTest#stopLeavesNoListenerBehind`).
- **Architektur:** `ColumnArchitectureTest` wie bei den übrigen Columns; `features/radio` hängt nur von `core` ab.

## Offene Fragen

Die Fragen aus `proposal.md` sind mit einem Standard beantwortet. Zusätzlich offen, ohne Einfluss auf Spec und Aufgaben:

1. Ob `avaje-config` ein YAML-Listenformat liest. Dann entfällt SnakeYAML als Laufzeitabhängigkeit (D7). Prüfung in Aufgabe 1.3.
2. Ob die Lautstärke der Schallplatten-Kategorie oder der Musik-Kategorie folgt. Standard: Schallplatten (`RECORD`), weil Spieler dort die Plattenmusik regeln. Gilt für MIDI und Vanilla gleich.
