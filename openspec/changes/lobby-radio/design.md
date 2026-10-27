# Design

## Kontext

Eine Column ist ein `@Singleton` unter `features/<name>`, das nur von `core` abhängt, und hängt seinen `FeatureNode` an den `titan`-Knoten. Belegte Prioritäten: 100, 200, 300, 400, 450, 500, 550 (Einstellungen), 600, 620 (Fangen), 650, 700, 750 (Wurf), 800, 900, 920, 930, 1000. Hotbar-Plätze: 0 (Sprungtest), 4 (Navigator), 7 (Einstellungen), 8 (Lobbywechsel).

Das Radio spielt zwei Arten von Musik, die sich nur in der Wiedergabe unterscheiden: Eine Vanilla-Schallplatte lässt der Client selbst spielen (ein benannter Ton, der irgendwann endet), ein MIDI-Stück schickt der Server Note für Note. Beide brauchen denselben Lebenszyklus: starten, stoppen aus fünf Gründen, Telemetrie, Sichtbarkeit für genau einen Spieler. Deshalb gibt es ein gemeinsames Songmodell und eine gemeinsame Wiedergabe mit zwei Inhalten.

Der Takt ist der Server-Tick, 20 pro Sekunde (50 ms). Töne gehen als Sound-Pakete an einen Spieler, nicht über Notenblöcke in der Welt.

## Ziele / Nicht-Ziele

**Ziele**

- Ein Item öffnet das Radio. Start, Weiter, Stopp und Abspielmodus, sonst nichts.
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

Dasselbe gilt für den Abspielmodus (D13): Er ist Teil des Radiozustands der Sitzung und wird beim Abmelden vergessen.

Sollte später ein Start beim Beitritt oder ein gespeicherter Modus gewünscht sein, ist das ein eigener Change mit Otis-Anbindung.

### D2 Bausteine im Modul `features/radio`

- **`RadioSong`** (sealed interface, `permits VanillaDisc, MidiSong`): gemeinsame Felder `title()`, `artist()`, `disc()` (`Material`) und `kind()` (Enum `RadioKind`: `VANILLA`, `MIDI`). `VanillaDisc` ist ein Record mit Plattenname, Plattenton (`SoundEvent`) und Länge in Ticks. `MidiSong` ist ein Record mit Titel, Künstler, Schallplatte und der Notenliste (D3, D4).
- **`RadioPlayback`** (rein, ohne Minestom): ein Objekt je Spieler mit einem Song, einem Stand und einem Grund zum Beenden. `start`, `tick`, `stop(reason)` sind für beide Arten gleich. Nur der Inhalt unterscheidet sich: Eine MIDI-Wiedergabe spielt fällige Noten (D5), eine Vanilla-Wiedergabe startet einmal den Plattenton und endet nach der Länge aus der Tabelle (D11). Die Töne gehen über das Interface **`RadioSink`** (`note`, `playRecord`, `stopRecord`). Die Produktion setzt es mit dem Spieler um, die Tests mit einer Aufzeichnung.
- **`RadioModule`** (`@Singleton`): `@PostConstruct` lädt die MIDI-Songs, baut die Menüs, hängt den `FeatureNode` mit Priorität 680 an und startet einen Takt-Task (`TaskSchedule.tick(1)`), der alle aktiven Wiedergaben ruft. `@PreDestroy` stoppt alle mit `SHUTDOWN`, hebt den Takt auf, schließt den Knoten und entfernt die Menüs.
- **`RadioItems`** (`@Factory`, `Optional<LobbyItem>`): `titan:radio`, Material `JUKEBOX`, Hotbar-Platz 5. Immer vorhanden, weil die Vanilla-Platten immer da sind.
- **`RadioMenuBuilder`**: der einzige Builder des Moduls. Er baut die Aves-Inventare für zwei Menüs (Vanilla-Platten, Eigene Songs) und drei Modi, also sechs geteilte Inventare (D14).
- **`RadioSessions`**: eine `ConcurrentHashMap<UUID, RadioSession>`. Eine `RadioSession` hält je Spieler den Abspielmodus (`PlayMode`, Standard `ONCE`) und die laufende `RadioPlayback` oder keine. Start und Stopp kommen von Klicks und Ereignissen, der Takt läuft auf dem Scheduler-Thread. Deshalb ist die Map nebenläufig, und eine Wiedergabe hat ein `closed`-Flag. Beim Abmelden wird die Sitzung entfernt, also auch der Modus.
- **`RadioSelection`** (rein): berechnet den nächsten Titel aus Titel, Modus, Katalog und `RandomGenerator` (D13). Der Zufall kommt injiziert; die Produktion nutzt `RandomGenerator.getDefault()` über eine `@Factory`-Bean, die Tests einen festen Seed.

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
| `next` | Spieler klickt „Weiter“ oder eine andere Schallplatte. Der neue Titel startet sofort; bei „Weiter“ wählt der Modus den Titel (D13). |
| `end` | Der Titel ist zu Ende: bei MIDI die letzte Note, bei Vanilla die Länge aus D11 plus Puffer. Danach startet nach D13 je nach Modus ein Nachfolger (`plays` mit `trigger=auto`) oder nichts. |
| `logout` | `PlayerDisconnectEvent`. Der Client ist weg, also wird nichts mehr gesendet. |
| `busy` | `LobbyPlayerBusyEvent` mit `busy = true`, gesendet vom Sprungtest beim Start eines Laufs und vom Portal bei Beginn einer Übergabe. Der Serverwechsel ist damit abgedeckt. |
| `shutdown` | `@PreDestroy` des Moduls. |

**Was der Stopp am Client tut:**

- **MIDI:** Stopp sendet nichts. Die laufenden Notenblock-Töne klingen nach, ein bis zwei Sekunden.
- **Vanilla:** Stopp sendet einen benannten Stopp (`SoundStop.named(Key)`) für genau diesen Plattenton. Ohne ihn würde der Titel über den Stopp hinaus weiterlaufen, weil er eine lange Datei ist. Der benannte Stopp schneidet nur diesen Ton ab, nicht alle Töne der Kategorie. Liegt dieselbe Platte zufällig in einem Plattenspieler der Welt, hört der Spieler sie in diesem Moment auch nicht.

**Weitere Regeln:**

- **Kein Wiedereinstieg nach `busy`.** Nach einem Sprungtest oder einer Übergabe startet der Spieler das Radio neu.
- **Fangen, Tanz, Sitzen, Elytra, Spawn und Respawn** haben keine Wirkung. Fangen sendet kein `LobbyPlayerBusyEvent`, und der Ton ist privat. Das Radio spielt also während einer Jagd weiter (entschieden, siehe `proposal.md`).
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

- `titan.radio.plays{kind, mode, trigger}` (Zähler): jeder Start. `kind` ist `vanilla` oder `midi`, `mode` ist `once`, `loop` oder `shuffle` (der Modus zum Startzeitpunkt), `trigger` ist `user` (Klick auf eine Platte, Weiter) oder `auto` (Nachfolger nach `end`, also Schleife oder Zufall). Kardinalität: 2 × 3 × 2 = 12.
- `titan.radio.stops{reason, kind}` (Zähler): je Grund aus D6 und je Art. Keine Spieler-Id. Kardinalität: 6 × 2 = 12.
- **Ein Titelende mit Nachfolger** zählt also zweimal: einmal `stops{reason=end}` für den alten Titel und einmal `plays{trigger=auto}` für den neuen. Das ist gewollt, damit jede Wiedergabe einen Start und einen Grund zum Ende hat.
- **Modus-Wechsel** zählt nichts. Er ändert weder den laufenden Titel noch einen Start.
- `titan.radio.notes.dropped` (Zähler): Noten, die das Budget oder die Verspätung verworfen hat. Nur MIDI.
- `titan.radio.songs{result}` (Zähler): `loaded` und `rejected` beim Start, je MIDI-Datei. Vanilla-Platten zählen nicht.
- Kein Span, weil Starts und Stopps zu häufig sind, um sie einzeln zu verfolgen. Dasselbe gilt für Noten.
- Logs: WARN je abgelehnter Datei mit Dateiname und Grund. INFO einmal beim Start mit der Zahl geladener MIDI-Songs. Keine Spielernamen in Logs.

### D10 Meldungen

Dateien `titan/radio/messages_de.properties` und `messages_en.properties`, mit denselben Schlüsseln. Benötigt werden: Itemname und Hinweis, Menütitel für beide Menüs, Umschalter „Eigene Songs“ und „Vanilla-Platten“, Lore der Schallplatte (Titel, Künstler, Art), Stopp, Weiter, Modusknopf mit den Namen „Einmal“, „Schleife“, „Zufall“ und einer Zeile „Modus: …“, Aktionsleiste „Läuft: {0} von {1}“, „Radio aus“ bei Stopp und Abbruch durch `busy`. Ein Test prüft, dass beide Sprachen dieselben Schlüssel haben und alle rendern.

### D11 Vanilla-Schallplatten

**Quelle der Liste.** Die 22 Plattenarten sind gegen `Material.MUSIC_DISC_*` und `SoundEvents.MUSIC_DISC_*` der Minestom-Version `2026.10.07-26.2` geprüft. Die Liste des Owners nennt 21 Platten. Minestom kennt zusätzlich `MUSIC_DISC_BOUNCE`; sie ist aufgenommen (entschieden, siehe `proposal.md`).

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
| relic | `music_disc.relic` | 3:38 |
| creator | `music_disc.creator` | 2:56 |
| creator_music_box | `music_disc.creator_music_box` | 1:13 |
| precipice | `music_disc.precipice` | 4:59 |
| tears | `music_disc.tears` | 2:55 |
| lava_chicken | `music_disc.lava_chicken` | 2:15 |
| bounce | `music_disc.bounce` | Platzhalter 3:00, prüfen |

Die Länge von `bounce` ist nicht bekannt und muss in Aufgabe 3.1 gemessen werden. Die Längen von `relic`, `precipice`, `creator`, `tears` und `lava_chicken` stammen aus der Review-Nachbesserung (Wiki-Werte, nicht gemessen).

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

- **Vanilla-Platten:** 22 Platten auf den Plätzen 0 bis 21. Stopp auf Platz 22, Weiter auf Platz 23, Modusknopf auf Platz 24, Umschalter „Eigene Songs“ auf Platz 26. Der Umschalter fehlt, wenn es keine MIDI-Songs gibt.
- **Eigene Songs:** bis zu 18 Platten auf den Plätzen 0 bis 17. Stopp auf 22, Weiter auf 23, Modusknopf auf 24, Umschalter „Vanilla-Platten“ auf 26.

Der Hotbar-Eintrag öffnet immer das Menü „Vanilla-Platten“. Ein Klick auf eine Platte startet genau den Song dieser Platte, Art egal.

### D13 Abspielmodus: Einmal, Schleife, Zufall

**Modi.** `ONCE` (Einmal, Standard), `LOOP` (Schleife), `SHUFFLE` (Zufall). Ein Knopf im Menü schaltet den Modus des Spielers zyklisch um: Einmal → Schleife → Zufall → Einmal. Der Modus wirkt nur auf das Ende eines Titels und auf „Weiter“. Ein Klick auf eine Platte startet immer genau diesen Titel.

**Katalog.** Alle verfügbaren Titel sind die 22 Vanilla-Platten in Menüreihenfolge, danach die MIDI-Songs in Konfigurationsreihenfolge. „Weiter“ im Modus Einmal oder Schleife geht in dieser Reihenfolge weiter und wechselt am Ende wieder auf den ersten Titel.

| Modus | Titelende | Weiter |
| --- | --- | --- |
| Einmal | kein Nachfolger, Grund `end` | nächster Titel in der Reihenfolge |
| Schleife | derselbe Titel startet neu, `trigger=auto` | nächster Titel in der Reihenfolge; die Schleife gilt für ihn weiter |
| Zufall | ein zufälliger Titel aus dem ganzen Katalog, nie derselbe wie eben, `trigger=auto` | ein zufälliger Titel, nie derselbe wie der aktuelle |

**Zufall.** Die Auswahl ist gleichverteilt über alle Titel außer dem aktuellen. Sie nutzt einen injizierten `RandomGenerator`; damit ist sie in Tests mit festem Seed reproduzierbar. Ein Titel wird so nie zweimal hintereinander gespielt, solange der Katalog mindestens zwei Titel hat.

**Ein einziger Titel.** In der Produktion hat der Katalog immer 22 Titel, dieser Fall kann also nicht eintreten. In der reinen Auswahl gilt: Gibt es nur einen Titel, wiederholt Zufall ihn, weil es keinen anderen gibt. Die Regel „nie zweimal hintereinander“ ist dann nicht erfüllbar, und ein Stopp wäre für einen Modus, der weiterspielen soll, falsch. Ein leerer Katalog liefert keinen Titel und endet ohne Nachfolger.

**Standard Einmal.** Das Radio spielt nur, was der Spieler gewählt hat. Schleife und Zufall laufen weiter und brauchen deshalb eine bewusste Wahl.

**Sitzungszustand.** Der Modus gehört zur Sitzung und wird beim Abmelden mit der Sitzung entfernt. Er ist nicht gespeichert. Ein gespeicherter Modus bräuchte die Otis-Anbindung und einen zweiten Weg für den Zustand (D1). Das lohnt sich für einen Knopf nicht; wer ihn später will, bekommt einen eigenen Change.

**Stopp und Modus.** Ein Stopp setzt den Modus nicht zurück. Ein Modus-Wechsel während eines Titels wirkt erst auf das Ende oder auf „Weiter“.

### D14 Darstellung des Modusknopfs: ein Inventar je Modus und Menü

Der Modusknopf muss zeigen, welcher Modus gilt. Die Menüs sind geteilte Aves-Inventare, und ein Item in einem geteilten Inventar sieht für alle Spieler gleich aus. Drei Lösungen standen zur Wahl:

1. **Ein Inventar je Menü und Modus (2 × 3 = 6).** Der Modusknopf steht fest im Inventar mit dem Namen des Modus. Ein Klick schreibt den nächsten Modus in die Sitzung und öffnet dem Spieler das Inventar desselben Menüs für den neuen Modus. Der Zustand liegt in der Sitzung, das Inventar zeigt ihn nur an.
2. **Ein Item pro Spieler aktualisieren.** Verworfen. Ein geteiltes Inventar hat für alle Spieler dasselbe Item; ein Update wäre für alle sichtbar.
3. **Inventare pro Spieler bauen.** Verworfen. Der Aufbau kostet bei jedem Öffnen, und es gäbe so viele Inventare wie Spieler.

**Gewählt: Option 1.** Der Navigator macht es mit Varianten nach Berechtigung genauso: Die Varianten sind vorab gebaut, und der Spieler sieht die richtige. Hier sind es drei Modi statt Berechtigungen, und das Umschalten zwischen den Menüs nutzt dieselbe Logik. Der Umschalter öffnet das andere Menü im aktuellen Modus.

Die Kosten sind kurz: Ein Klick auf den Modusknopf öffnet das Fenster neu. Das ist für den Spieler ein kurzes Neuladen, und es ist akzeptiert.

**Sprache.** Die Namen der Modi, Knöpfe und Menütitel liegen als übersetzte Aves-Items vor, die der Client pro Betrachter auflöst (wie im Navigator). Dann bleiben es sechs Inventare. Löst Aves die Sprache nicht pro Betrachter auf, gibt es je Sprache eine Variante, also zwölf Inventare. Das wird in Aufgabe 6.4 geprüft. Der Speicher ist in beiden Fällen klein.

**Zustand.** Der Modus liegt nur in der Sitzung (D13). Die Inventare sind unveränderlich und tragen nichts von einem Spieler.

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
  - `RadioSelectionTest` mit `java.util.Random(seed)` als `RandomGenerator`: Einmal startet am Ende keinen Nachfolger; Schleife startet am Ende denselben Titel neu; Zufall wählt nach dem Ende nie den eben gespielten Titel, über viele Ziehungen mit festem Seed; mit nur einem Titel wiederholt Zufall ihn; ein leerer Katalog liefert nichts; „Weiter“ geht in jedem Modus den Katalog weiter (Einmal und Schleife in Reihenfolge, Zufall ohne Wiederholung des aktuellen Titels), und der Wrap von der letzten auf die erste Platte stimmt.
  - `PlayModeTest`: Standard ist Einmal; der Knopf zyklisch Einmal → Schleife → Zufall → Einmal; ein Modus-Wechsel während eines Titels ändert nichts am laufenden Titel.
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

Die Entscheidungen des Owners stehen in `proposal.md` („Entschieden“). Offen, ohne Einfluss auf Spec und Aufgaben:

1. Ob `avaje-config` ein YAML-Listenformat liest. Dann entfällt SnakeYAML als Laufzeitabhängigkeit (D7). Prüfung in Aufgabe 1.3.
2. Ob die Lautstärke der Schallplatten-Kategorie oder der Musik-Kategorie folgt. Standard: Schallplatten (`RECORD`), weil Spieler dort die Plattenmusik regeln. Gilt für MIDI und Vanilla gleich.
3. Ob Aves übersetzte Items pro Betrachter auflöst. Entscheidet über sechs oder zwölf Inventare in D14. Prüfung in Aufgabe 6.4.

## Umsetzungsnotizen

Festgehalten bei der Umsetzung (Aufgaben 1.2 bis 8.4). Wo die Umsetzung von D1 bis D14 abweicht, steht hier, warum.

- **Abweichung D7, Platte einer MIDI-Datei (Spec „Doppelte Schallplatte“).** Die 22 Vanilla-Platten decken alle `Material.MUSIC_DISC_*` dieser Minestom-Version ab. Die Regel „eine MIDI-Platte, die eine Vanilla-Platte ist, wird übersprungen“ würde also jeden eigenen Song verhindern. Die Menüs sind getrennt, eine Kollision gibt es nicht. Umgesetzt: Eine Platte darf von einem MIDI-Song und einer Vanilla-Platte genutzt werden; doppelt sein dürfen nur `id` und Platte innerhalb von `songs.yaml`. Der zweite Teil des Szenarios „Doppelte Schallplatte“ gilt damit nicht mehr.
- **Abweichung D5, Zeit des Takts.** Der Takt zählt die Läufe des Takt-Tasks, nicht die Wanduhr (kein Systemzeit-Zugriff). Ein Hänger des Takts verschiebt die Noten, er löst keinen Schwall aus. Die Regel „mehr als einen Tick zu spät verwerfen“ greift, wenn der Aufrufer einen Sprung meldet (in den Tests).
- **Entscheidung 1.3, YAML.** `avaje-config` (`Config`) liefert Einzelwerte und Listen von Zeichenketten, aber kein Einlesen einer Liste von Objekten aus einer Datei. SnakeYAML wird deshalb direkt als Abhängigkeit von `features/radio` deklariert (`libs.snakeyaml`, Version 2.7). Die Auslieferung enthält dieselbe Version bereits über `common`; es kommt kein neues Laufzeit-Artefakt dazu.
- **Entscheidung 6.4, Aves und Sprache, sechs Builder.** Aves rendert übersetzte Items pro Betrachter (`GlobalTranslatedInventoryBuilder`, Test `RadioMenuTest`). Es gibt also sechs Builder: zwei Menüs in drei Modi. Die Sprache wird wie im Navigator auf Deutsch oder Englisch reduziert; Aves hält dafür intern je Sprache ein Inventar, beide werden beim Start gebaut. Klicks behandelt `RadioModule` selbst (`InventoryPreClickEvent`), weil Aves' eigene Klick-Behandlung für übersetzte Builder nicht läuft (wie im Lobbyswitcher).
- **Aves füllt den Inhalt auf dem nächsten Tick.** Ein frisch gebautes Inventar ist vor dem ersten Tick leer. Deshalb baut der Start alle Inventare vorab, und die Tests lesen Namen erst nach einem Tick.
- **Aufgabe 1.2, `java.desktop`.** Das Repository erzeugt kein jlink-Image (keine Treffer für `jlink`, `add-modules` oder `java.desktop` in Build und Deployment). Produktiv läuft ein volles Temurin-JDK 25, `javax.sound.midi` ist dort enthalten. Hotbar-Platz 5 ist in der Lobby sonst frei (Suche über alle `ItemSlot.hotbar(...)`), Priorität 680 ist frei (`EVENT_PRIORITY` aller Columns). Ein eigener Konflikt-Test der Hotbar wurde nicht ergänzt; der Env-Test `RadioModuleIntegrationTest` prüft die Platzierung.
- **Aufgabe 3.1, Plattenlängen: offen.** Die Längen sind Wiki-Werte in Sekunden, nicht an den Client-Dateien gemessen. Nur `bounce` bleibt Platzhalter mit 3:00. Die Künstlerzeilen von `tears`, `lava_chicken` und `bounce` sind ebenfalls Platzhalter („Mojang“).
- **Aufgabe 8.3, Abnahme auf einem Testserver: nicht ausgeführt.** Offen, wie vom Auftrag vorgegeben.
- **Aufgabe 8.2, `openspec validate lobby-radio --strict`.** Ohne Fehler. Die Warnungen betreffen den deutschen Spec-Text: Anforderungstitel ohne SHALL/MUST und Anforderungen über 500 Zeichen. Der Text bleibt, wie der Owner ihn geschrieben hat.
- **Modusknopf.** Wie in D12 und D14: Der Knopf öffnet dem Spieler das Inventar desselben Menüs im nächsten Modus. Ein Klick auf eine Platte startet genau diesen Titel, unabhängig vom Modus.
