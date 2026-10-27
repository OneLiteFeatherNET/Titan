# Spec Delta

## ADDED Requirements

### Requirement: Der Song-Gegenstand zeigt nur Name und eigene Lore, ohne Plattenbeschreibung
_The song item is a music disc, but its tooltip shows only the radio's own name and lore, never the disc's vanilla description._

Der Gegenstand eines Titels im Menü und der Knopf „Läuft jetzt“ MÜSSEN ohne das Bauteil `jukebox_playable` gebaut werden. Der Tooltip DARF keine Beschreibung der Schallplatte aus dem Client zeigen, weder für Vanilla-Platten noch für MIDI-Songs. Der Name MUSS der Titel sein, danach folgen nur die Lore-Zeilen der Radio-Anzeige.

#### Scenario: MIDI-Song ohne Plattenbeschreibung

- **WHEN** ein Spieler das Menü einer Kategorie öffnet, in der der MIDI-Song „Sunrise“ mit der Platte `MUSIC_DISC_CAT` steht
- **THEN** hat der Gegenstand dieses Titels kein Bauteil `jukebox_playable`, und sein Tooltip beginnt mit dem Namen „Sunrise“ und enthält keine Zeile „C418 - cat“

#### Scenario: Vanilla-Platte ohne Plattenbeschreibung

- **WHEN** ein Spieler die Vanilla-Platte „cat“ im Menü sieht
- **THEN** hat ihr Gegenstand kein Bauteil `jukebox_playable`, und die einzige Herkunftszeile ist „C418 · Schallplatte“

### Requirement: Eine MIDI-Lore nennt Original und MIDI-Quelle in getrennten Zeilen
_An own song shows who made the original and, on its own line, who made the MIDI._

Die Lore eines MIDI-Songs MUSS die Zeile `Original: <artist>` zeigen, also den Original-Künstler aus dem Feld `artist`. Ist das Feld `midi` gesetzt, MUSS darunter eine eigene Zeile `MIDI: <midi>` stehen. Ist `midi` nicht gesetzt, DARF keine MIDI-Zeile erscheinen. Der Zusatz „Eigener Song“ bzw. „Own song“ DARF in der Lore eines MIDI-Songs NICHT mehr stehen. Die Lore einer Vanilla-Platte bleibt `<artist> · Schallplatte` bzw. `<artist> · Music disc` und hat keine MIDI-Zeile. Beide Zeilenarten MÜSSEN in Deutsch und Englisch über die Textdateien kommen.

#### Scenario: MIDI-Song mit Herkunft

- **WHEN** der Eintrag „Sunrise“ `artist: DJ Bjra ft. Julia` und `midi: BitMidi-Upload (unbekannt)` hat
- **THEN** zeigt die Lore die Zeilen „Original: DJ Bjra ft. Julia“ und „MIDI: BitMidi-Upload (unbekannt)“, in dieser Reihenfolge

#### Scenario: MIDI-Song ohne Herkunft

- **WHEN** der Eintrag „Sunrise“ kein Feld `midi` hat
- **THEN** zeigt die Lore nur „Original: DJ Bjra ft. Julia“, ohne MIDI-Zeile

#### Scenario: Vanilla-Platte hat keine MIDI-Zeile

- **WHEN** ein Spieler die Vanilla-Platte „13“ im Menü sieht
- **THEN** zeigt die Lore „C418 · Schallplatte“ und keine Zeile, die mit „MIDI:“ beginnt

### Requirement: songs.yaml nennt die MIDI-Quelle im optionalen Feld midi
_An entry names who made its MIDI, optionally; the artist stays the original artist._

Jeder Eintrag in `radio/songs.yaml` DARF ein optionales Feld `midi` tragen. Es nennt, wer das MIDI gemacht hat oder woher die Datei stammt, als Freitext. Das Feld `artist` bleibt der Original-Künstler und ist weiterhin Pflicht. Ein leerer oder fehlender Wert `midi` ist gültig und wird wie ein fehlendes Feld behandelt. Eine Datei mit `midi` wird nicht abgelehnt, und die Lobby protokolliert dazu keine Warnung.

#### Scenario: Bestehende songs.yaml ohne midi

- **WHEN** die Lobby eine `songs.yaml` lädt, deren Einträge kein Feld `midi` haben
- **THEN** lädt sie alle gültigen Einträge wie bisher, ohne Warnung, und jeder Song hat keine MIDI-Quelle

#### Scenario: Feld midi mit Leerzeichen und Klammern

- **WHEN** ein Eintrag `midi: BitMidi-Upload (unbekannt)` nennt
- **THEN** ist der Wert der Text `BitMidi-Upload (unbekannt)`, unverändert bis auf führende und folgende Leerzeichen
