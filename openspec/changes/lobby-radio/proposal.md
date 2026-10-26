# Proposal

## Why

Die Lobby hat Spiele, Tänze und Sprungläufe, aber keine Musik. Ein Radio im Spiel soll MIDI-Stücke auf Notenblöcken spielen, mit eigenen Schallplatten als Auswahl. Der Ton kommt nur bei dem Spieler an, der das Radio gestartet hat. Wer nicht zuhören will, wird nicht gestört, und es braucht kein Resource Pack.

## What Changes

- **Hotbar-Item:** `titan:radio` (Material `JUKEBOX`) auf Hotbar-Platz 5. Platz 5 ist frei; belegt sind 0 (Sprungtest), 4 (Navigator), 7 (Einstellungen) und 8 (Lobbywechsel). Das Item öffnet das Radio-Menü.
- **Radio-Menü (Aves):** Ein geteiltes Inventar mit einer Schallplatte je Song. Ein Klick auf eine Schallplatte startet diesen Song neu. Zwei Steuerelemente: **Stopp** und **Weiter**. Weiter spielt den nächsten Song in der Reihenfolge der Konfiguration, nach dem letzten wieder den ersten.
- **Eigene Schallplatten:** Vanilla-Items `Material.MUSIC_DISC_*` mit eigenem Namen und eigener Lore (Titel, Künstler). Die Platte ist nur ein Symbol. Ihre Original-Musik wird nie abgespielt. Kein Resource Pack.
- **Nur für den Spieler:** Die Töne sind Sound-Pakete an genau einen Spieler, nicht räumlich, in der Kategorie „Schallplatten“ der Client-Lautstärke. Andere Spieler hören nichts, und in der Welt entsteht kein Ton.
- **Songs als Dateien:** MIDI-Dateien (`.mid`) und eine `songs.yaml` im Verzeichnis `radio/` der Lobby (Template im CloudNet). Die Liste steht je Song mit Titel, Künstler, Schallplatte und Dateiname. Alles wird beim Start geladen. Fehlerhafte Dateien werden protokolliert und übersprungen, nie fatal.
- **Abspielen:** MIDI-Kanäle und Programme werden auf die 16 Notenblock-Instrumente abgebildet. Tonhöhen werden in den Zwei-Oktaven-Bereich der Notenblöcke gebracht, die Anschlagstärke wird zur Lautstärke. Details in `design.md`.
- **Stopp-Gründe:** Stopp durch den Spieler, Weiter, Ende des Songs, Abmelden, Serverwechsel über den Portal (über das bestehende `LobbyPlayerBusyEvent`), Sprungtest (ebenfalls `LobbyPlayerBusyEvent`) und Herunterfahren.
- **Grenzen:** Dateigröße, Anzahl Noten, Länge, Anzahl Songs und Noten je Tick und Spieler. Werte in `design.md`, D8.
- **Telemetrie:** Zähler `titan.radio.plays`, `titan.radio.stops{reason}`, `titan.radio.notes.dropped` und `titan.radio.songs{result}` (`loaded`, `rejected`). Keine Spieler-Id.
- **Meldungen:** Deutsch und Englisch in `titan/radio/messages_de.properties` und `messages_en.properties`.

Nicht Teil dieser Änderung:

- Keine Einstellung in den Spielereinstellungen und kein Befehl. Start und Stopp laufen über das Menü (Begründung in `design.md`, D1).
- Kein Zufalls- oder Radiomodus, keine Schleife, kein automatisches Weiterspielen (siehe offene Fragen).
- Kein Upload von Musik durch Spieler.
- Keine MIDI-Details über Noten hinaus: Pedal, Pitch-Bend, Lautstärke-Controller, Panorama.
- Keine Audioausgabe auf dem Server. Es läuft kein Synthesizer und kein Sequencer.
- Die MIDI-Dateien liegen nicht im Repository.

## Type and Scope

Die Implementierung wird als **`feat(radio)`** ausgeliefert, mit dem Pull-Request-Titel `feat(radio): add a note block radio with personal discs`. Die Änderung ist nicht brechend. Alle Teile sind `feat`; es gibt keine zweite Änderung für Fehlerbehebungen oder Refactorings.

## Capabilities

### New Capabilities

- `lobby-radio`: Hotbar-Item und Menü, Songs aus Dateien, Notenabbildung, private Wiedergabe, Stoppgründe, Grenzen, Telemetrie und Meldungen.

### Modified Capabilities

Keine. `LobbyPlayerBusyEvent` aus `core` wird unverändert genutzt, und `jumprun` und `portal` senden es bereits.

## Impact

- **Code:** neues Modul `features/radio` (Column `radioColumn`, Paket `net.onelitefeather.titan.feature.radio`), nur abhängig von `core`. Keine Änderung in `core`, `jumprun` oder `portal`.
- **Abhängigkeiten:**
  - `javax.sound.midi` aus dem JDK, nur zum Lesen der Sequenzen. Keine Audio-API, keine Hardware.
  - `snakeyaml` (Version steht schon im Katalog, bisher nur für Tests) wird Laufzeitabhängigkeit von `features/radio`. Die Entscheidung ist in `design.md` (D7) begründet.
  - Das Laufzeitimage muss das Modul `java.desktop` enthalten. Das wird vor dem Rollout geprüft.
- **Betrieb:** Verzeichnis `radio/` mit `songs.yaml` und den MIDI-Dateien im Template der Lobby. Ohne gültige Songs ist das Modul aus: kein Item, kein Menü.
- **Rechte:** Die Musik stammt vom Betreiber. Der Betreiber ist für die Rechte der Stücke verantwortlich.
- **Nutzertexte:** `titan/radio/messages_de.properties`, `messages_en.properties`.

## Offene Fragen

Die Fragen sind mit einem Standard beantwortet, der in der Umsetzung gilt, bis der Owner anders entscheidet:

1. **Schleife:** Soll ein Song nach dem Ende von vorn anfangen? Standard: nein. Der Song endet und stoppt (Grund `end`).
2. **Radiomodus:** Soll nach dem Ende automatisch der nächste Song kommen, oder eine Zufallsreihenfolge gelten? Standard: nein. Nur „Weiter“ wechselt den Song.
3. **Schallplatten je Recht:** Sollen manche Songs nur mit einem Recht sichtbar sein? Standard: alle Songs sind für alle sichtbar.
4. **Sprungtest und Serverwechsel:** Soll das Radio nach einem Sprungtest oder einer Übergabe von selbst weiterspielen? Standard: nein, der Spieler startet es neu.
5. **Tag:** Soll das Radio während des Fangens weiterlaufen? Standard: ja. Fangen sendet kein `LobbyPlayerBusyEvent`, und der Ton ist privat.
6. **Anzahl Songs:** Reichen 18 Songs (zwei Reihen im Menü)? Vanilla hat rund zwanzig Plattenarten, mehr geht nicht ohne neue Plattenarten.

## Delivery

Der Pull-Request-Titel und der Squash-Commit der Umsetzung sind `feat(radio): add a note block radio with personal discs`. Die OpenSpec-Artefakte stehen im Commit `docs(openspec): propose lobby-radio`, der Code folgt in `feat(radio)`.
