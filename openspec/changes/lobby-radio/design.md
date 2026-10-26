# Design

## Kontext

Eine Column ist ein `@Singleton` unter `features/<name>`, das nur von `core` abhängt, und hängt seinen `FeatureNode` an den `titan`-Knoten. Belegte Prioritäten: 100, 200, 300, 400, 450, 500, 550 (Einstellungen), 600, 620 (Fangen), 650, 700, 750 (Wurf), 800, 900, 920, 930, 1000. Hotbar-Plätze: 0 (Sprungtest), 4 (Navigator), 7 (Einstellungen), 8 (Lobbywechsel).

Das Radio braucht drei Dinge, die die Lobby bisher nicht hat: einen Takt, der Töne über Zeit verteilt, einen Ton, der nur bei einem Spieler ankommt, und das Lesen von MIDI-Dateien. Der Takt ist der Minecraft-Server-Tick, 20 pro Sekunde (50 ms). Der Ton wird als Sound-Paket an einen Spieler geschickt, nicht über Notenblöcke in der Welt.

## Ziele / Nicht-Ziele

**Ziele**

- Ein Item öffnet ein Menü mit Songs als Schallplatten. Start, Weiter und Stopp, sonst nichts.
- Ton nur für den Spieler, der das Radio gestartet hat.
- Songs als Dateien im Template, beim Start geladen, fehlerhafte Dateien übersprungen.
- Alles, was reine Logik ist (Parsen, Abbilden, Takt, Budget), ohne Server testbar.

**Nicht-Ziele**

- Einstellung, Befehl, Otis-Anbindung (D1).
- Sub-Tick-Genauigkeit, Note-Off, Pedal, Pitch-Bend (D5).
- Audioausgabe auf dem Server.

## Entscheidungen

### D1 Kein Einstellungsschalter, Start und Stopp im Menü

Das Radio hat keinen Eintrag in den Spielereinstellungen und keinen Befehl. Der Grund:

- Es passiert nichts ohne Klick. Der Spieler hat sich damit ausdrücklich dafür entschieden, und das ist das Opt-in.
- Der Zustand gilt nur für die Sitzung. Abmelden, Serverwechsel und Sprungtest stoppen ihn, also gibt es nichts, was man speichern müsste. Eine Einstellung würde einen zweiten Weg schaffen, der beim nächsten Login sofort wieder stoppen müsste.
- Die Einstellungen (`lobby-settings`) würden eine Otis-Abhängigkeit für einen Schalter einführen, den niemand braucht.

Sollte später ein Start beim Beitritt gewünscht sein, ist das ein eigener Change.

### D2 Bausteine im Modul `features/radio`

- `RadioModule` (`@Singleton`): `@PostConstruct` lädt die Songs, registriert das Menü und hängt den `FeatureNode` mit Priorität 680 an. Ein Takt-Task (`TaskSchedule.tick(1)`) ruft alle aktiven Wiedergaben auf. `@PreDestroy` stoppt alle Wiedergaben mit Grund `shutdown`, hebt den Takt auf, schließt den Knoten und entfernt das Menü.
- `RadioItems` (`@Factory`, `Optional<LobbyItem>`): `titan:radio`, Material `JUKEBOX`, Hotbar-Platz 5. Leer, wenn keine Songs geladen sind. Wie `SettingsItems`.
- `RadioMenuBuilder`: der einzige Builder des Moduls. Er baut das Aves-Inventar mit einer Schallplatte je Song, Stopp und Weiter. Das Inventar ist geteilt und ändert sich nicht je Spieler. Deshalb gibt es keine Markierung „läuft“ im Menü; der laufende Song steht in der Aktionsleiste.
- `RadioPlaybacks`: eine `ConcurrentHashMap<UUID, RadioPlayback>`, Start und Stopp je Spieler. Start und Stopp kommen von Klicks und Ereignissen, der Takt läuft auf dem Scheduler-Thread. Deshalb ist die Map nebenläufig, und eine Wiedergabe hat ein `closed`-Flag.
- `RadioPlayback` (rein, ohne Minestom): hält den Song, den Cursor und den Stand. Die Töne gehen über das Interface `NoteSink`. Die Produktion implementiert es mit `player.playSound(...)`, die Tests mit einer Aufzeichnung.

### D3 MIDI lesen

- Die Datei wird vor dem Lesen auf die Größe geprüft (Grenzen, D8). Danach liest `MidiSystem.getSequence(InputStream)` aus `javax.sound.midi` die Sequenz. Das braucht keinen Synthesizer, keinen Sequencer und keine Audio-Hardware.
- Aus jeder Spur werden Note-On-Ereignisse mit Anschlagstärke über 0 übernommen. Note-Off wird ignoriert, weil Notenblock-Töne feste Länge haben.
- Programmwechsel wird je Kanal mitgeführt. Ein Ton bekommt das Programm, das auf seinem Kanal zu seinem Zeitpunkt gilt.
- Ticks werden mit `MidiUtils.tick2microsecond` in Mikrosekunden umgerechnet. Das berücksichtigt Tempowechsel automatisch.
- Jeder Fehler ist eine `SongFormatException` mit Grund (zu groß, kein MIDI, zu viele Noten, zu lang). Nichts davon ist fatal.

### D4 Instrument, Tonhöhe, Lautstärke

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

1. **Transponieren pro Song:** Der Median der melodischen Noten (ohne Schlagzeug) wird auf die Mitte des Bereichs gebracht. Das Ergebnis ist ein Vielfaches von 12. Ein Song mit einer tiefen Melodie klingt dadurch nicht komplett falsch, und die Kontur bleibt erhalten. Bei Gleichstand wird nach unten gerundet.
2. **Falten je Note:** Liegt eine Note danach noch außerhalb, wird sie um Oktaven verschoben, bis sie passt. Liegt sie zu tief, wird sie erhöht, liegt sie zu hoch, gesenkt. Die Tonhöhenklasse bleibt erhalten.

Die Verschiebung ist ein reiner Rechenschritt und wird mit festen Werten getestet.

**Lautstärke.** Die Lautstärke ist `velocity / 127`, also 0,008 bis 1,0. Anschlagstärke 0 ist Note-Off und wird nicht gespielt.

### D5 Takt: 50 ms, ohne Drift

- Jede Note bekommt eine Zeit in Mikrosekunden aus der Sequenz. Daraus folgt der Spiel-Tick mit `tick = round(µs / 50 000)`, also `(µs + 25 000) / 50 000` in ganzer Arithmetik. Die Rechnung nimmt die absolute Zeit, nicht die Summe der Abstände, damit sich kein Fehler aufschaukelt.
- Eine Wiedergabe hält ein sortiertes Array von Noten und einen Cursor. Ein Takt läuft den Cursor nur vorwärts ab, ohne die ganze Liste zu durchsuchen.
- Je Tick spielt die Wiedergabe alle fälligen Noten. Sie sind nach Anschlagstärke absteigend sortiert, und die ersten 16 gehen raus (D8). Alles darüber fällt weg, und `titan.radio.notes.dropped` zählt es.
- Eine Note, die mehr als einen Tick zu spät ist, wird verworfen und nicht nachgeholt. Sonst gäbe es nach einem Hänger einen Schwall von Tönen.

**Akzeptierte Einschränkung:** Die Töne liegen auf dem 50-ms-Raster, mit bis zu ±25 ms Abweichung. Schnelle Figuren (zum Beispiel Zweiunddreißigstel bei 120 BPM, 62,5 ms) fallen teils zusammen. Das ist für eine Lobby-Musik vertretbar, und die Notenblöcke in der Welt haben dieselbe Grenze.

**Verworfene Alternativen:**

- Sub-Tick-Timing: Sound-Pakete können nicht verzögert werden. Ein Paket wirkt, sobald es ankommt.
- Eigene Sounds über ein Resource Pack: verboten, Vanilla only.
- Echte Notenblöcke in der Welt: nicht privat, alle hören mit, und sie belasten die Welt.

### D6 Stoppgründe und Wechselwirkungen

| Grund (Telemetrie) | Auslöser |
| --- | --- |
| `user` | Spieler klickt „Stopp“. |
| `next` | Spieler klickt „Weiter“ oder eine andere Schallplatte. Der neue Song startet sofort. |
| `end` | Der Song ist zu Ende. Keine Schleife, kein automatischer Nachfolger (offene Frage 1 und 2). |
| `logout` | `PlayerDisconnectEvent`. |
| `busy` | `LobbyPlayerBusyEvent` mit `busy = true`, gesendet vom Sprungtest beim Start eines Laufs und vom Portal bei Beginn einer Übergabe. Der Serverwechsel ist damit abgedeckt. |
| `shutdown` | `@PreDestroy` des Moduls. |

- **Stopp schneidet keine Töne ab.** Ein Stoppbefehl an den Client würde auch Plattenmusik im Spiel abschneiden. Die laufenden Notenblock-Töne klingen nach, ein bis zwei Sekunden.
- **Kein Wiedereinstieg nach `busy`.** Nach einem Sprungtest oder einer Übergabe startet der Spieler das Radio neu.
- **Fangen, Tanz, Sitzen, Elytra, Spawn und Respawn** haben keine Wirkung. Fangen sendet kein `LobbyPlayerBusyEvent`, und der Ton ist privat. Das Radio spielt also während einer Jagd weiter (offene Frage 5).
- Ein `LobbyPlayerBusyEvent` mit `busy = false` tut nichts. Ein Listener darf nie werfen, weil das Ereignis synchron ausgelöst wird.

### D7 Songs laden und Konfiguration

Im Arbeitsverzeichnis der Lobby liegt ein Verzeichnis `radio/` mit `songs.yaml` und den MIDI-Dateien. Die Datei ist eine Liste:

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
- Regeln beim Laden: fehlt die Datei oder ist sie nicht lesbar, wird der Song übersprungen (WARN mit Dateiname). Die Schallplatte muss ein `MUSIC_DISC_*` sein, `id` und `disc` müssen je Liste eindeutig sein; der zweite Treffer wird übersprungen. Ein Parserfehler überspringt nur den Song. Die Zahl der Songs ist begrenzt, Überschuss wird übersprungen (WARN).
- Gibt es danach keinen gültigen Song, ist das Modul aus: kein Item, kein Menü, kein `FeatureNode`. So sendet es keinen Traffic.

### D8 Grenzen

| Grenze | Wert | Wirkung bei Überschreitung |
| --- | --- | --- |
| Dateigröße | 512 KiB | Song wird übersprungen, WARN. |
| Noten je Song | 20 000 | Song wird übersprungen, WARN. |
| Länge je Song | 15 Minuten (18 000 Ticks) | Song wird übersprungen, WARN. |
| Songs je Lobby | 18 (zwei Reihen im Menü) | Überschuss wird übersprungen, WARN. |
| Noten je Tick und Spieler | 16 | Die leisesten fallen weg, `titan.radio.notes.dropped` zählt sie. |
| Gleichzeitige Wiedergaben | eine je Spieler | Ein neuer Start ersetzt die alte Wiedergabe. |

Die Werte stehen als Konstanten im Code, wie bei den anderen Columns. Die Speichergrenze liegt bei etwa 3 MB für 18 volle Songs.

Die Packet-Grenze von 16 Noten je Tick ergibt höchstens 320 Sound-Pakete je Sekunde und Spieler. Ein typischer Song liegt darunter.

### D9 Telemetrie und Logs

- `titan.radio.plays` (Zähler, `{play}`): jeder Start, auch der Neustart durch eine andere Schallplatte.
- `titan.radio.stops{reason}` (Zähler): je Grund aus D6, mit dem Wert `reason`. Keine Spieler-Id.
- `titan.radio.notes.dropped` (Zähler, `{note}`): Noten, die das Budget oder die Verspätung verworfen hat.
- `titan.radio.songs{result}` (Zähler): `loaded` und `rejected` beim Start, je Song.
- Kein Span, weil Starts und Stopps zu häufig sind, um sie einzeln zu verfolgen. Dasselbe gilt für Noten.
- Logs: WARN je abgelehnter Datei mit Dateiname und Grund. INFO einmal beim Start mit der Zahl geladener Songs. Keine Spielernamen in Logs.

### D10 Meldungen

Dateien `titan/radio/messages_de.properties` und `messages_en.properties`, mit denselben Schlüsseln. Benötigt werden: Itemname und Hinweis, Menütitel, Lore der Schallplatte (Titel, Künstler), Stopp, Weiter, Aktionsleiste „Läuft: {0} von {1}“, „Radio aus“ bei Stopp und Abbruch durch `busy`. Ein Test prüft, dass beide Sprachen dieselben Schlüssel haben und alle rendern.

## Risiken / Abwägungen

- **Qualität:** 50 ms Raster, keine Note-Off-Länge, keine Pedale und keine Lautstärkekurven. Für Hintergrundmusik ausreichend, für Präzisionsstücke nicht. Das ist der Preis für Vanilla only.
- **Rechte der Musik:** Die Dateien kommen vom Betreiber und liegen nicht im Repository. Die Rechte der Stücke trägt der Betreiber.
- **`java.desktop` im Image:** `javax.sound.midi` liegt im Modul `java.desktop`. Ist das Laufzeitimage verkleinert (jlink), muss das Modul drin sein. Vor dem Rollout prüfen.
- **Hotbar-Platz 5:** Ein Item mehr auf der Leiste. Der Konflikt-Test der Hotbar prüft die Belegung.
- **Verstärkung durch Last:** Viele Spieler mit vollen Songs erzeugen viele Pakete. Die Grenze von 16 je Tick begrenzt das pro Spieler. Ein Lasttest mit 50 Spielern gehört in den Abnahmeschritt (tasks 7.2).

## Test Strategy

Alle Tests folgen F.I.R.S.T.: keine Sleeps, keine Systemzeit, frische Fixtures je Test, Takt explizit.

- **Unit, rein (Kern der Änderung):**
  - `MidiSongParserTest`: Sequenzen werden im Speicher gebaut (`new Sequence(Sequence.PPQ, 24)`, `Track.add(new MidiEvent(...))`), mit `MidiSystem.write` in ein `ByteArrayOutputStream` geschrieben und wieder gelesen. Fälle: Tempo 120 BPM, Tempowechsel, Note-Off ignoriert, Anschlagstärke 0 ignoriert, kaputte Bytes, zu viele Noten, zu lang.
  - `InstrumentMapperTest`: je Programmgruppe ein Instrument, Schlagzeug nach Taste.
  - `PitchTest`: `pitch(0) = 0,5`, `pitch(12) = 1,0`, `pitch(24) = 2,0`; Transponieren (Median), Falten (Oktaven hoch und runter), Gleichstand.
  - `RadioPlaybackTest` mit `RecordingNoteSink` und explizitem `tick()`: Quantisierung, Budget 16 nach Lautstärke, verspätete Noten verfallen, Song-Ende setzt `end`, Stopp ist idempotent, nach `closed` kommen keine Töne mehr.
  - `RadioSelectionTest`: Weiter wrapt von der letzten Schallplatte auf die erste.
- **Unit, Laden (`@TempDir`):** `SongCatalogTest`: fehlende Datei, falscher Schallplattentyp, doppelte `id` oder `disc`, zu viele Songs, kein gültiger Song macht das Modul aus.
- **Unit, Telemetrie und Meldungen:** `RadioTelemetryTest` mit `TestTelemetry` (Attribute, keine Spieler-Id); `RadioMessagesTest` (gleiche Schlüssel, alle rendern).
- **Env (Minestom, `env.tick()`):** `RadioModuleIntegrationTest`: Item auf Platz 5; Klick startet, Töne gehen nur an den Startenden (zwei Spieler, zwei Sinks); Abmelden stoppt; `busy` stoppt; Ende stoppt; `close()` lässt keinen Listener zurück (Muster `SpawnModuleTest#stopLeavesNoListenerBehind`).
- **Architektur:** `ColumnArchitectureTest` wie bei den übrigen Columns; `features/radio` hängt nur von `core` ab.

## Offene Fragen

Die Fragen aus `proposal.md` sind mit einem Standard beantwortet. Zusätzlich offen, ohne Einfluss auf Spec und Aufgaben:

1. Ob `avaje-config` ein YAML-Listenformat liest. Dann entfällt SnakeYAML als Laufzeitabhängigkeit (D7). Prüfung in Aufgabe 2.1.
2. Ob die Lautstärke der Schallplatten-Kategorie oder der Musik-Kategorie folgt. Standard: Schallplatten, weil Spieler dort die Plattenmusik regeln.
