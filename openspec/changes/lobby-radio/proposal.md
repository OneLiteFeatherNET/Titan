# Proposal

## Why

Die Lobby hat Spiele, Tänze und Sprungläufe, aber keine Musik. Ein Radio im Spiel soll Musik abspielen, und zwar nur für den Spieler, der es gestartet hat. Zur Auswahl stehen zwei Quellen: die Vanilla-Schallplatten, die jeder Spieler ohnehin kennt, und eigene MIDI-Stücke, die auf Notenblöcken klingen. Wer nicht zuhören will, wird nicht gestört, und es braucht kein Resource Pack.

## What Changes

- **Hotbar-Item:** `titan:radio` (Material `JUKEBOX`) auf Hotbar-Platz 5. Platz 5 ist frei; belegt sind 0 (Sprungtest), 4 (Navigator), 7 (Einstellungen) und 8 (Lobbywechsel). Das Item ist immer da und öffnet das Radio-Menü.
- **Zwei Quellen, ein Modell:** Ein Song ist entweder eine **Vanilla-Schallplatte** oder ein **MIDI-Stück**. Beide haben Titel, Künstler, Schallplatte (`Material.MUSIC_DISC_*`) und eine Art (`kind`: `VANILLA` oder `MIDI`). Start, Stopp, Weiter, Busy, Abmelden und Telemetrie laufen für beide gleich.
- **Vanilla-Schallplatten:** Alle 22 Plattenarten, die diese Minestom-Version kennt (Liste in `design.md`, D11). Sie spielen den eingebauten Plattenton des Clients, nur für den Spieler, aus dem Spieler heraus (er wandert mit ihm), in voller Lautstärke. Kein Resource Pack, keine Notenblöcke.
- **Eigene Schallplatten:** Vanilla-Items `Material.MUSIC_DISC_*` mit eigenem Namen und eigener Lore (Titel, Künstler). Bei MIDI-Stücken ist die Platte nur ein Symbol. Die Originalmusik der Platte wird nie abgespielt.
- **MIDI-Stücke:** MIDI-Dateien (`.mid`) und eine `songs.yaml` im Verzeichnis `radio/` der Lobby. Die Stücke spielen auf Notenblöcken, wieder nur für den Spieler, nicht räumlich. Fehlerhafte Dateien werden protokolliert und übersprungen, nie fatal. Ohne Datei gibt es keine MIDI-Stücke, aber die Vanilla-Platten bleiben.
- **Menü (Aves):** Zwei geteilte Menüs, Platten und Eigene Songs, mit einem Umschalter zwischen ihnen. Begründung in `design.md`, D12. Der Umschalter erscheint nur, wenn es MIDI-Stücke gibt.
- **Stopp-Gründe:** Stopp durch den Spieler, Weiter, Ende des Songs, Abmelden, Serverwechsel über den Portal (über das bestehende `LobbyPlayerBusyEvent`), Sprungtest (ebenfalls `LobbyPlayerBusyEvent`) und Herunterfahren.
- **Grenzen:** Dateigröße, Anzahl Noten, Länge, Anzahl MIDI-Stücke und Noten je Tick und Spieler. Werte in `design.md`, D8.
- **Telemetrie:** Zähler `titan.radio.plays{kind}`, `titan.radio.stops{reason, kind}`, `titan.radio.notes.dropped` und `titan.radio.songs{result}` (`loaded`, `rejected`, nur MIDI). Keine Spieler-Id.
- **Meldungen:** Deutsch und Englisch in `titan/radio/messages_de.properties` und `messages_en.properties`.

Nicht Teil dieser Änderung:

- Keine Einstellung in den Spielereinstellungen und kein Befehl. Start und Stopp laufen über das Menü (Begründung in `design.md`, D1).
- Kein Zufalls- oder Radiomodus, keine Schleife, kein automatisches Weiterspielen (siehe offene Fragen).
- Kein Upload von Musik durch Spieler.
- Keine MIDI-Details über Noten hinaus: Pedal, Pitch-Bend, Lautstärke-Controller, Panorama.
- Keine Audioausgabe auf dem Server. Es läuft kein Synthesizer und kein Sequencer.
- Die MIDI-Dateien liegen nicht im Repository.
- Keine eigenen Plattenlängen aus Dateien: die Längen der Vanilla-Platten stehen im Code (D11), nur als Näherung.

## Type and Scope

Die Implementierung wird als **`feat(radio)`** ausgeliefert, mit dem Pull-Request-Titel `feat(radio): add a note block radio with personal discs`. Die Änderung ist nicht brechend. Alle Teile sind `feat`; es gibt keine zweite Änderung für Fehlerbehebungen oder Refactorings.

## Capabilities

### New Capabilities

- `lobby-radio`: Hotbar-Item und Menüs, Vanilla-Schallplatten, MIDI-Stücke aus Dateien, Notenabbildung, private Wiedergabe, Stoppgründe, Grenzen, Telemetrie und Meldungen.

### Modified Capabilities

Keine. `LobbyPlayerBusyEvent` aus `core` wird unverändert genutzt, und `jumprun` und `portal` senden es bereits.

## Impact

- **Code:** neues Modul `features/radio` (Column `radioColumn`, Paket `net.onelitefeather.titan.feature.radio`), nur abhängig von `core`. Keine Änderung in `core`, `jumprun` oder `portal`.
- **Abhängigkeiten:**
  - `javax.sound.midi` aus dem JDK, nur zum Lesen der Sequenzen. Keine Audio-API, keine Hardware.
  - `SoundEvents` und `Material` aus Minestom (`2026.10.07-26.2`), bereits vorhanden. Die Plattenliste in D11 ist gegen genau diese Version geprüft.
  - `snakeyaml` (Version steht schon im Katalog, bisher nur für Tests) wird Laufzeitabhängigkeit von `features/radio`. Die Entscheidung ist in `design.md` (D7) begründet.
  - Das Laufzeitimage muss das Modul `java.desktop` enthalten. Das wird vor dem Rollout geprüft.
- **Betrieb:** Ohne Verzeichnis `radio/` gibt es nur die Vanilla-Platten. Mit `radio/songs.yaml` und den MIDI-Dateien kommen die eigenen Songs dazu. Ein Konfigurationsschalter ist nicht nötig.
- **Rechte:** Die MIDI-Musik stammt vom Betreiber. Der Betreiber ist für die Rechte der Stücke verantwortlich.
- **Nutzertexte:** `titan/radio/messages_de.properties`, `messages_en.properties`.

## Offene Fragen

Die Fragen sind mit einem Standard beantwortet, der in der Umsetzung gilt, bis der Owner anders entscheidet:

1. **Schleife:** Soll ein Song nach dem Ende von vorn anfangen? Standard: nein. Der Song endet und stoppt (Grund `end`).
2. **Radiomodus:** Soll nach dem Ende automatisch der nächste Song kommen, oder eine Zufallsreihenfolge gelten? Standard: nein. Nur „Weiter“ wechselt den Song.
3. **Schallplatten je Recht:** Sollen manche Songs nur mit einem Recht sichtbar sein? Standard: alle Songs sind für alle sichtbar.
4. **Sprungtest und Serverwechsel:** Soll das Radio nach einem Sprungtest oder einer Übergabe von selbst weiterspielen? Standard: nein, der Spieler startet es neu.
5. **Tag:** Soll das Radio während des Fangens weiterlaufen? Standard: ja. Fangen sendet kein `LobbyPlayerBusyEvent`, und der Ton ist privat.
6. **Platte `bounce`:** Die Liste des Owners nennt 21 Platten. Minestom kennt `MUSIC_DISC_BOUNCE` zusätzlich. Standard: aufnehmen, damit das Menü vollständig ist, mit 22 Platten. Ausschließen ist eine Zeile in der Tabelle.
7. **Länge der Vanilla-Platten:** Die Längen sind Näherungen (D11). Eine falsche Länge verschiebt nur den Zeitpunkt von `end`. Messung gegen die Client-Assets ist Aufgabe 3.1.

## Delivery

Der Pull-Request-Titel und der Squash-Commit der Umsetzung sind `feat(radio): add a note block radio with personal discs`. Die OpenSpec-Artefakte stehen im Commit `docs(openspec): propose lobby-radio` und die Erweiterung um Vanilla-Platten im Commit `docs(openspec): add vanilla discs to lobby-radio`. Der Code folgt in `feat(radio)`.
