# Proposal

## Why

Die Lobby hat Spiele, Tänze und Sprungläufe, aber keine Musik. Ein Radio im Spiel soll Musik abspielen, und zwar nur für den Spieler, der es gestartet hat. Zur Auswahl stehen zwei Quellen: die Vanilla-Schallplatten, die jeder Spieler ohnehin kennt, und eigene MIDI-Stücke, die auf Notenblöcken klingen. Wer nicht zuhören will, wird nicht gestört, und es braucht kein Resource Pack.

## What Changes

- **Hotbar-Item:** `titan:radio` (Material `JUKEBOX`) auf Hotbar-Platz 5. Platz 5 ist frei; belegt sind 0 (Sprungtest), 4 (Navigator), 7 (Einstellungen) und 8 (Lobbywechsel). Das Item ist immer da und öffnet das Radio-Menü.
- **Zwei Quellen, ein Modell:** Ein Song ist entweder eine **Vanilla-Schallplatte** oder ein **MIDI-Stück**. Beide haben Titel, Künstler, Schallplatte (`Material.MUSIC_DISC_*`) und eine Art (`kind`: `VANILLA` oder `MIDI`). Start, Stopp, Weiter, Busy, Abmelden und Telemetrie laufen für beide gleich.
- **Vanilla-Schallplatten:** Alle 22 Plattenarten, die diese Minestom-Version kennt (Liste in `design.md`, D11), einschließlich `bounce`. Sie spielen den eingebauten Plattenton des Clients, nur für den Spieler, aus dem Spieler heraus (er wandert mit ihm), in voller Lautstärke. Kein Resource Pack, keine Notenblöcke.
- **Eigene Schallplatten:** Vanilla-Items `Material.MUSIC_DISC_*` mit eigenem Namen und eigener Lore (Titel, Künstler). Bei MIDI-Stücken ist die Platte nur ein Symbol. Die Originalmusik der Platte wird nie abgespielt.
- **MIDI-Stücke:** MIDI-Dateien (`.mid`) und eine `songs.yaml` im Verzeichnis `radio/` der Lobby. Die Stücke spielen auf Notenblöcken, wieder nur für den Spieler, nicht räumlich. Fehlerhafte Dateien werden protokolliert und übersprungen, nie fatal. Ohne Datei gibt es keine MIDI-Stücke, aber die Vanilla-Platten bleiben.
- **Menü (Aves):** Zwei geteilte Menüs, Vanilla-Platten und Eigene Songs, mit einem Umschalter zwischen ihnen. Der Umschalter erscheint nur, wenn es MIDI-Stücke gibt. Begründung in `design.md`, D12.
- **Abspielmodus:** Ein Knopf im Menü schaltet pro Spieler den Modus um: **Einmal** (Standard), **Schleife** (der aktuelle Titel wiederholt sich) und **Zufall** (nach einem Titel kommt ein zufälliger anderer Titel aus allen verfügbaren Titeln, Vanilla und MIDI). „Weiter“ folgt dem Modus. Begründung und Darstellung in `design.md`, D13 und D14.
- **Sitzungszustand:** Der Modus gilt nur für die Sitzung des Spielers und wird beim Abmelden vergessen. Er ist nicht gespeichert.
- **Stopp-Gründe:** Stopp durch den Spieler, Weiter, Ende des Titels, Abmelden, Serverwechsel über den Portal (über das bestehende `LobbyPlayerBusyEvent`), Sprungtest (ebenfalls `LobbyPlayerBusyEvent`) und Herunterfahren. Ein Titelende, dem ein automatischer Nachfolger folgt, meldet `end` für den alten Titel und einen Start mit `trigger=auto` für den neuen.
- **Grenzen:** Dateigröße, Anzahl Noten, Länge, Anzahl MIDI-Stücke und Noten je Tick und Spieler. Werte in `design.md`, D8.
- **Telemetrie:** Zähler `titan.radio.plays{kind, mode, trigger}`, `titan.radio.stops{reason, kind}`, `titan.radio.notes.dropped` und `titan.radio.songs{result}` (`loaded`, `rejected`, nur MIDI). Keine Spieler-Id. Details in `design.md`, D9.
- **Meldungen:** Deutsch und Englisch in `titan/radio/messages_de.properties` und `messages_en.properties`.

Nicht Teil dieser Änderung:

- Keine Einstellung in den Spielereinstellungen und kein Befehl. Start, Stopp und Modus laufen über das Menü (Begründung in `design.md`, D1 und D13).
- Keine Persistenz des Abspielmodus über die Sitzung hinaus.
- Keine Berechtigungen je Song. Alle Titel sind für alle sichtbar.
- Kein Weiterspielen nach einem Sprungtest oder einem Serverwechsel. Der Spieler startet das Radio neu.
- Kein Upload von Musik durch Spieler.
- Keine MIDI-Details über Noten hinaus: Pedal, Pitch-Bend, Lautstärke-Controller, Panorama.
- Keine Audioausgabe auf dem Server. Es läuft kein Synthesizer und kein Sequencer.
- Die MIDI-Dateien liegen nicht im Repository.
- Keine eigenen Plattenlängen aus Dateien: die Längen der Vanilla-Platten stehen im Code (D11), nur als Näherung.

## Type and Scope

Die Implementierung wird als **`feat(radio)`** ausgeliefert, mit dem Pull-Request-Titel `feat(radio): add a note block radio with personal discs`. Die Änderung ist nicht brechend. Alle Teile sind `feat`; es gibt keine zweite Änderung für Fehlerbehebungen oder Refactorings.

## Capabilities

### New Capabilities

- `lobby-radio`: Hotbar-Item und Menüs, Vanilla-Schallplatten, MIDI-Stücke aus Dateien, Abspielmodi, Notenabbildung, private Wiedergabe, Stoppgründe, Grenzen, Telemetrie und Meldungen.

### Modified Capabilities

Keine. `LobbyPlayerBusyEvent` aus `core` wird unverändert genutzt, und `jumprun` und `portal` senden es bereits.

## Impact

- **Code:** neues Modul `features/radio` (Column `radioColumn`, Paket `net.onelitefeather.titan.feature.radio`), nur abhängig von `core`. Keine Änderung in `core`, `jumprun` oder `portal`.
- **Abhängigkeiten:**
  - `javax.sound.midi` aus dem JDK, nur zum Lesen der Sequenzen. Keine Audio-API, keine Hardware.
  - `SoundEvents`, `Material` und `SoundStop` (Adventure) aus Minestom (`2026.10.07-26.2`), bereits vorhanden. Die Plattenliste in D11 ist gegen genau diese Version geprüft.
  - `java.util.random.RandomGenerator` aus dem JDK für den Zufallsmodus. Im Test mit festem Seed.
  - `snakeyaml` (Version steht schon im Katalog, bisher nur für Tests) wird Laufzeitabhängigkeit von `features/radio`. Die Entscheidung ist in `design.md` (D7) begründet.
  - Das Laufzeitimage muss das Modul `java.desktop` enthalten. Das wird vor dem Rollout geprüft.
- **Betrieb:** Ohne Verzeichnis `radio/` gibt es nur die Vanilla-Platten. Mit `radio/songs.yaml` und den MIDI-Dateien kommen die eigenen Songs dazu. Ein Konfigurationsschalter ist nicht nötig.
- **Rechte:** Die MIDI-Musik stammt vom Betreiber. Der Betreiber ist für die Rechte der Stücke verantwortlich.
- **Nutzertexte:** `titan/radio/messages_de.properties`, `messages_en.properties`.

## Entschieden

Der Owner hat die offenen Fragen aus der ersten Fassung entschieden:

1. **Schleife erlaubt.** Der Modus „Schleife“ wiederholt den aktuellen Titel (D13).
2. **Zufalls- oder Radiomodus erlaubt.** Der Modus „Zufall“ wählt nach jedem Titel automatisch einen anderen zufälligen Titel (D13).
3. **Keine Berechtigungen je Song.** Alle Titel sind für alle sichtbar.
4. **Kein Weiterspielen nach Sprungtest oder Serverwechsel.** Der Spieler startet das Radio neu.
5. **Fangen: Radio läuft weiter.** Fangen sendet kein `LobbyPlayerBusyEvent`, und der Ton ist privat.
6. **Platte `bounce` aufnehmen.** Das Menü hat 22 Vanilla-Platten.

Dazu gilt der Standard für den Abspielmodus: Der Modus startet bei jedem Spieler auf **Einmal** (D13).

## Offen

1. **Länge der Vanilla-Platten:** Die Längen sind Näherungen (D11). Eine falsche Länge verschiebt nur den Zeitpunkt von `end` und damit den Auto-Nachfolger. Messung gegen die Client-Assets ist Aufgabe 3.1.

## Delivery

Der Pull-Request-Titel und der Squash-Commit der Umsetzung sind `feat(radio): add a note block radio with personal discs`. Die OpenSpec-Artefakte stehen in den Commits `docs(openspec): propose lobby-radio`, `docs(openspec): add vanilla discs to lobby-radio` und `docs(openspec): add loop and shuffle to lobby-radio`. Der Code folgt in `feat(radio)`.
