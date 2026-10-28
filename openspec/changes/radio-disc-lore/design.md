# Design

## Kontext

Ein Song-Gegenstand im Menü ist eine Vanilla-Schallplatte (`Material.MUSIC_DISC_*`). Der Client zeigt bei einer Schallplatte die Beschreibung aus dem Bauteil `jukebox_playable`, auch wenn der Gegenstand nie in einer Jukebox liegt. Die Lore ist eine Liste von Zeilen, die `RadioMenuBuilder` aus `RadioMessages` baut.

## Entscheidungen

### D1 Bauteil entfernen, nicht verstecken

`ItemStack.without(DataComponents.JUKEBOX_PLAYABLE)` entfernt das Bauteil explizit, auch wenn die Schallplatte es von Haus aus hat. Die Alternative, die Komponente über `TOOLTIP_DISPLAY` (`hidden_components`) auszublenden, behält das Bauteil am Gegenstand und hängt von der Tooltip-Logik des Clients ab. Gewählt wird das Entfernen. Der Test prüft `get(DataComponents.JUKEBOX_PLAYABLE)` auf dem gebauten Gegenstand und sieht damit beide Fälle nicht gemischt.

Der Schalter gilt für Song-Gegenstände und für „Läuft jetzt“, denn beide zeigen eine Schallplatte.

### D2 Feld `midi` ist optional und `null`

`MidiSong` bekommt das Feld `midi` als letzte Komponente, `null` wenn `songs.yaml` keines nennt. Ein `Optional` im Record wird vermieden. Der Leerwert wird wie bei den anderen Feldern in `SongCatalog.text(...)` zu `null`.

### D3 Lore-Zeilen

- MIDI-Song: `Original: <artist>`, danach `MIDI: <midi>`, wenn `midi` gesetzt ist.
- Vanilla-Platte: wie bisher `<artist> · Schallplatte` bzw. `<artist> · Music disc`.

Die Auswahl steht in einem `switch` über das sealed `RadioSong`, damit der Compiler einen neuen Typ bemerkt.

### D4 Texte

`titan.radio.disc.lore.midi` bekommt den neuen Text „MIDI: <arg:0>“ (de und en), der Schlüssel `titan.radio.disc.lore.original` neu „Original: <arg:0>“. Die Schlüsselliste `RadioMessages.KEYS` wird ergänzt, `RadioMessagesTest` prüft die Gleichheit der Bundles.

## Offen

- Der Zusatz „Eigener Song“ entfällt. Ein Song ohne `midi` ist im Tooltip nur an seiner Kategorie zu erkennen. Ob das reicht, entscheidet der Owner beim Review.
