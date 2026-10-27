# Design

## Kontext

Das Radio aus `lobby-radio` (4.9.0) besteht aus einem Hotbar-Item und zwei festen Menüs. Jedes Menü ist ein geteiltes Aves-Inventar je Modus, also sechs Inventare, die für alle Spieler gleich aussehen (`lobby-radio`, D12 und D14). Dieses Modell trägt keine Seiten, keine Kategorien und keine Lautstärke, denn jeder Spielerstand würde ein eigenes Inventar verlangen.

Geprüft in den Quellen (Aves 1.16.7, Sources; Titan bindet Aves über `aonyx-bom`, die genaue Version bestätigt Aufgabe 1.1):

- `PersonalTranslatedInventoryBuilder(type, player)` ist ein `GlobalTranslatedInventoryBuilder` für einen Spieler, geöffnet mit `open(locale)`. Die Datenebene kommt aus `setDataLayoutFunction`. `invalidateDataLayout()` ruft sie erneut auf und aktualisiert offene Inventare in place. `unregister()` entfernt die Listener vom Inventar-Knoten und schließt die Betrachter.
- `PageableInventory` ist `@ApiStatus.Experimental`, kennt nur Zurück und Weiter als Steuerelemente und baut mit dem nicht übersetzten `PersonalInventoryBuilder`. Die Steuerzeile dieses Menüs hat neun Funktionen, also reicht es nicht.
- Aves füllt Inhalte auf dem nächsten Tick, wie schon in `lobby-radio` festgehalten.

Geprüft in `openspec/changes/lobby-settings`: `PlayerSettings` kennt nur `BooleanSetting`, `SettingsStore.save(...)` nimmt einen Wahrheitswert, und der Otis-Adapter schickt einen Wahrheitswert. Nicht boolesche Optionen sind dort ausdrücklich Nicht-Ziel.

## Ziele / Nicht-Ziele

**Ziele**

- Kategorien aus `songs.yaml`, Vanilla als eingebaute Kategorie, Sonstige als Ziel für alles Unbekannte.
- Seiten mit festen Plätzen, Seite je Spieler, Steuerzeile ohne Verschiebung.
- Lautstärke je Spieler, für MIDI und Vanilla, mit ehrlicher Regel für laufende Schallplatten.
- Alles, was `lobby-radio` festlegt, bleibt: private Wiedergabe, Stoppgründe, Busy und Logout, Telemetrie ohne Spieler-Id.

**Nicht-Ziele**

- Gespeicherte Lautstärke, globaler Zufall als Modus, Rechte je Kategorie (siehe `proposal.md`, Offen).
- Neue Spans oder neue Stoppgründe.
- Ein Resource Pack.

## Entscheidungen

### R1 Basis: Delta zu lobby-radio, ersetzt die Menüstruktur

Diese Änderung ist ein Delta zu `openspec/changes/lobby-radio/specs/lobby-radio/spec.md`. Die Umsetzung von `lobby-radio` ist in 4.9.0 ausgeliefert, aber die Change-Ordner sind noch nicht archiviert. Deshalb gilt die Reihenfolge: `lobby-radio` zuerst archivieren, dann `radio-categories`. Vorher verweist das MODIFIED-Delta auf einen Hauptbestand, den es noch nicht gibt (wie bei `lobbyswitcher-back`).

Ersetzt werden:

- `lobby-radio`, D12 (zwei feste Menüs mit Umschalter) und die Umschalter-Szenarien.
- `lobby-radio`, D14 (sechs geteilte Inventare je Menü und Modus).

Bleiben gültig: D1 (kein Einstellungsschalter, Sitzung), D6 (Stoppgründe), D9 (Telemetrie, außer R10), D13 (Abspielmodus, Standard Einmal, Vergessen beim Logout).

### R2 Kategorien in songs.yaml

Die Kategorien stehen in derselben Datei wie die Songs. Eine Kategorie ist ein Eintrag mit Id, Anzeigename, Symbol und Reihenfolge.

```yaml
categories:
  - id: games
    name: { de: Spiele, en: Games }
    icon: EMERALD
    order: 10
  - id: anime
    name: Anime
    icon: FIREWORK_ROCKET
    order: 20
  - id: classic
    name: { de: Klassik, en: Classic }
    icon: CLOCK
    order: 30
  - id: halloween
    name: { de: Halloween, en: Halloween }
    icon: JACK_O_LANTERN
    order: 40
songs:
  - id: sunrise
    title: Sunrise
    artist: Beispiel
    disc: MUSIC_DISC_CAT
    file: sunrise.mid
    category: games
```

Regeln:

- **Id:** `[a-z0-9_-]{1,32}`. Die Ids `minecraft` und `other` sind reserviert; ein Eintrag mit einer dieser Ids wird übersprungen (WARN).
- **Name:** entweder ein Text für beide Sprachen oder eine Zuordnung `{ de, en }`. Fehlt eine Sprache, gilt die andere. Fehlt der Name ganz, zeigt die Übersicht die Id und protokolliert WARN.
- **Symbol:** ein Vanilla-`Material`. Unbekannt oder fehlend: WARN, Ersatz `PAPER`.
- **Reihenfolge:** Ganzzahl, aufsteigend. Gleiche Werte sortieren nach Id.
- **Obergrenze:** 20 deklarierte Kategorien. Der Überschuss wird übersprungen (WARN). Mit „Minecraft“ und „Sonstige“ sind es höchstens 22 Einträge, also passt die Übersicht ohne eigene Seiten in 45 Plätze.

Verworfene Alternative: eine Kategorie je Ordner unter `radio/`. Die Id käme dann aus dem Pfad, Namen und Reihenfolge hätten keinen Platz, und es gäbe zwei Stellen für die Konfiguration. Alle bisherigen Inhalte liegen in einer Datei, also bleibt es bei einer Datei.

Die Kategorie „Minecraft“ ist eingebaut, mit festem Namen „Minecraft“ in beiden Sprachen, Symbol `MUSIC_DISC_13` und fester Position vorn. Sie enthält genau die 22 Vanilla-Platten in Menüreihenfolge. Die Kategorie „Sonstige“ (Deutsch) bzw. „Other“ (Englisch), Symbol `PAPER`, steht hinten und erscheint nur, wenn sie Titel hat.

### R3 Unbekannte und fehlende Kategorie landet in Sonstige

- Ein Song ohne `category` gehört zu „Sonstige“. Die Lobby protokolliert WARN mit Dateiname und dem Hinweis „no category, using other“. Das ist je Song einmal beim Start.
- Ein Song mit einer nicht deklarierten Id gehört ebenfalls zu „Sonstige“, mit WARN und der Id.
- Eine Kategorie ohne Titel erscheint nicht in der Übersicht. Das ist kein Fehler und wird nicht protokolliert.
- Der Song wird nie übersprungen wegen seiner Kategorie. Nur Datei, Id, Platte, Titel und Künstler können ihn ablehnen wie bisher.

Die Standardkategorie ist fest „Sonstige“. Eine Konfigurationsoption dafür wird nicht gebaut: sie hätte keinen Nutzen, den „Sonstige“ nicht schon hat, und sie ließe sich nur über einen zweiten Ort in der Konfiguration erklären (siehe `proposal.md`, Entschieden 10).

### R4 Zufall und Weiter bleiben in der Kategorie des laufenden Titels

Der Zufallsmodus und „Weiter“ wählen aus der Kategorie des Titels, der läuft. Läuft keiner, gilt die zuletzt geöffnete Kategorie.

| Modus | Titelende | Weiter |
| --- | --- | --- |
| Einmal | kein Nachfolger, Grund `end` | nächster Titel der Kategorie in Reihenfolge, nach dem letzten der erste |
| Schleife | derselbe Titel, `trigger=auto` | nächster Titel der Kategorie; die Schleife gilt für ihn weiter |
| Zufall | gleichverteilt aus der Kategorie ohne den eben gespielten Titel, `trigger=auto` | gleichverteilt aus der Kategorie ohne den aktuellen Titel |

Eine Kategorie mit einem einzigen Titel wiederholt ihn im Zufallsmodus, wie bisher. Ohne laufenden Titel startet „Weiter“ den ersten Titel der zuletzt geöffneten Kategorie. Ist im Überblick keine Kategorie geöffnet gewesen, ist „Weiter“ ausgegraut.

Begründung:

1. **Die Stimmung bleibt.** Wer „Anime“ startet, hört danach Anime und nicht Halloween. Bei einer globalen Auswahl wäre die Kategorie nur ein Einstieg, der danach ignoriert wird.
2. **Das Verhältnis bleibt.** Die 22 Vanilla-Platten machen den größten Teil des Katalogs aus. Eine globale Gleichverteilung würde MIDI-Stücke fast nie spielen lassen, sobald Minecraft gefüllt ist.
3. **Das Blättern ändert nichts.** Wer eine andere Kategorie ansieht, ändert damit den Scope nicht. Nur ein Start ändert ihn.

Verworfene Alternativen:

- **Global über alle Titel.** Ist die ehrliche Lesart von „Zufall“, widerspricht aber 1 und 2. Der Owner kann einen fünften Modus „Alle“ später nachziehen (offen).
- **Kategorie beim Zufall wählen lassen.** Ein Klick mehr und ein Zustand mehr, den die Steuerzeile nicht hat.

### R5 Obergrenze: 200 MIDI-Stücke und 1 000 000 Noten

- **Titel:** höchstens 200 MIDI-Stücke. Die Vanilla-Platten zählen nicht mit. Der 201. Eintrag in Konfigurationsreihenfolge wird übersprungen (WARN), wie heute der Überschuss über 18.
- **Noten:** höchstens 1 000 000 Noten insgesamt über alle geladenen Stücke. Ein Stück, das die Summe sprengt, wird übersprungen (WARN) und zählt nicht. Die Auswahl folgt der Ladereihenfolge.
- **Je Stück** bleiben die Grenzen aus `lobby-radio`, D8: 512 KiB, 20 000 Noten, 15 Minuten.

Begründung für 200 Titel:

- Bei 5 bis 8 Kategorien sind das 25 bis 40 Titel je Kategorie, also knapp eine Seite zu 45 Plätzen. Mehr würde ein Betreiber kaum pflegen, und jedes Stück braucht eine Datei und einen Eintrag.
- Mit 200 Titeln reichen fünf Seiten in der größten denkbaren Kategorie. Die Übersicht bleibt bei 22 Einträgen, also ohne eigene Seiten.
- Das Laden bleibt beim Start, also ohne Auswirkung auf den Takt.

Begründung für 1 000 000 Noten:

- `MidiNote` ist ein Record aus `int`, einer Enum-Referenz, `int` und `int`. Auf der JVM ist das grob 32 Byte je Note mit Objekt-Header. Das ist eine Schätzung; die Messung ist Aufgabe 10.4.
- 1 000 000 Noten ergeben damit etwa 32 MB Heap. Ohne Gesamtgrenze wären 200 volle Stücke 4 000 000 Noten, also etwa 128 MB.
- Typische Hintergrundstücke liegen bei 2 000 bis 5 000 Noten (Schätzung). Dann passen 200 bis 500 Stücke ins Budget, die Titelgrenze ist also in der Praxis die engere.

Korrektur zu `lobby-radio`, D8: Die dort genannte Speichergrenze von etwa 3 MB für 18 volle Stücke ist zu niedrig. Mit etwa 32 Byte je Note sind es rund 11,5 MB. Die Zahl bleibt im Konstanten-Block, bis die Messung steht.

### R6 Aufbau des Menüs: ein Inventar je Spieler, in place aktualisiert

Das ist die Kernentscheidung der Änderung. Es gab vier Wege.

| Option | Inventare | Kosten | Modus- und Lautstärke-Klick | Entscheidung |
| --- | --- | --- | --- | --- |
| A. Geteilt für jeden Zustand | (1 + Seiten) × 3 Modi × 10 Lautstärken × 2 Sprachen, also rund 800 typisch und bis 1 380 an der Obergrenze, ungefähr 75 000 Stacks | Aufbau beim Start und bei jedem Neuladen | öffnet ein neues Fenster, bei jedem Lautstärkeschritt ein Flackern | verworfen |
| B. Geteilt, Zustand als Item-Daten | wie A, mit Seiten | wie A | nicht möglich: geteilte Items sehen für alle gleich aus (`lobby-radio`, D14) | verworfen |
| C. Ein Inventar je Spieler, beim Öffnen gebaut, in place aktualisiert | eines je offenem Fenster | je Öffnen ein Builder mit 54 Plätzen; je Klick eine Neuberechnung der Datenebene und ein Fensterupdate | kein Neuöffnen | **gewählt** |
| D. Aves `PageableInventory` | eines je Spieler | wie C | nur Zurück und Weiter, experimentell, nicht übersetzt | verworfen; die Seitenlogik entsteht als eigene reine Klasse (R7) |

**Kosten von C, ehrlich:**

- Je Öffnen entsteht ein Builder und ein Inventar mit 54 Plätzen. Das ist nach Schätzung ein Bruchteil eines Ticks (50 ms); die Messung ist Aufgabe 10.4.
- Je Klick berechnet Aves die Datenebene neu und Minestom aktualisiert das Fenster. Minestom schickt dabei das ganze Fenster mit 54 Plätzen. Bei menschlicher Klickrate ist das unkritisch; die Lobby hat bei 50 Spielern gleichzeitig höchstens einige Klicks je Sekunde.
- Der Speicher wächst nur mit offenen Fenstern, nicht mit der Zahl der Zustände.
- Der Aves-Lebenszyklus verlangt `register()` beim Öffnen und `unregister()` beim Schließen. Das Schließen löst ein Ereignis aus, das die Ansicht aufräumt; `unregister()` schließt die Betrachter, also muss die Ansicht vorher aus der Map sein, sonst gibt es eine Rekursion (Test).

**Abweichung von der Projektregel „ein Builder pro Modul“:** Ein Builder je Fenster ist die einzige Form, in der ein Spielerstand in einem Aves-Inventar liegen kann. Die Regel bleibt für die Definition: `RadioMenuBuilder` ist die einzige Klasse, die das Layout beschreibt, und sie erzeugt je Fenster einen Builder. `RadioViews` hält die offenen Fenster, damit `@PreDestroy` sie alle schließt.

**Öffnen, aktualisieren, schließen:**

1. **Öffnen** (Hotbar-Klick): Die Sitzung setzt Ansicht auf Übersicht und Seite auf 1. Modus und Lautstärke bleiben. `RadioViews.open` baut einen `PersonalTranslatedInventoryBuilder` mit einer Datenebenen-Funktion, die die Sitzung liest, ruft `register()` und `open(locale)` auf und merkt sich die Ansicht des Spielers. Die Sprache ist die des Clients (`RadioMenuBuilder.languageOf`).
2. **Klick im Fenster:** Der Modul-Listener auf `InventoryPreClickEvent` bricht das Ereignis ab und ordnet es über die Inventar-Identität der Ansicht zu. Die Steuerung ändert die Sitzung, dann ruft die Ansicht `invalidateDataLayout()`. Der Inhalt aktualisiert sich ohne Neuöffnen.
3. **Seitenwechsel und Kategorie:** dasselbe wie Klick. Der Fenstertitel („Radio · Spiele · 1/2“) soll mit wechseln. Ob Aves das für übersetzte Builder in place kann, prüft Aufgabe 1.1. Wenn nicht, öffnet genau dieser Wechsel das Fenster neu; Modus und Lautstärke bleiben in place.
4. **Tick:** Startet, endet oder wechselt ein Titel, und hat der Spieler ein Fenster offen, ruft der Tick für dieses Fenster `invalidateDataLayout()` auf. „Läuft jetzt“ bleibt so aktuell.
5. **Schließen** (`InventoryCloseEvent` oder Abmelden): Die Ansicht wird aus der Map entfernt, dann `unregister()`. Die Sitzung bleibt; beim Abmelden wird sie vergessen.
6. **Herunterfahren** (`@PreDestroy`): alle offenen Ansichten werden entfernt und unregistriert.

Aves füllt Inhalte auf dem nächsten Tick. Das Fenster ist also für einen Tick leer. Das war in `lobby-radio` schon so, und die Tests lesen Inhalte erst nach `env.tick()`.

### R7 Seiten: 45 Titel je Seite, feste Plätze, Seite je Spieler

- **Layout:** 6 Reihen, 54 Plätze. Plätze 0 bis 44 sind der Inhalt (45 je Seite). Plätze 45 bis 53 sind die Steuerzeile (R8). Die Steuerzeile ist die unterste Reihe und verschiebt sich nie.
- **Seitenzahl:** `max(1, ceil(n / 45))`. Ein Titel an Index `i` in der Kategorie steht auf Seite `i / 45 + 1` an Platz `i % 45`.
- **Reihenfolge:** Konfigurationsreihenfolge innerhalb der Kategorie. „Minecraft“ folgt der Menüreihenfolge. Dadurch ändert sich ein Platz nur, wenn vorher Titel in derselben Kategorie entfernt werden, und das passiert nur beim Neuladen beim Start.
- **Leere Plätze:** Füllglas (`GRAY_STAINED_GLASS_PANE`, ohne Name). Eine nicht volle Seite verrät so nicht, wie viele Titel fehlen.
- **Seite je Spieler:** Die Seite gehört zur Sitzung. Sie bleibt beim Blättern und bei Stopp, Weiter, Modus und Lautstärke. Sie geht auf 1 zurück, wenn der Spieler eine Kategorie öffnet oder das Radio neu öffnet. Das ist einfacher als eine Seiten-Merkliste je Kategorie, und der Owner hat nur „die aktuelle Seite“ gefordert.
- **Klammer:** Eine Seite über der letzten wird auf die letzte geklemmt, falls sich die Zahl der Titel ändert.
- **Übersicht:** nicht seitenweise, weil sie höchstens 22 Einträge hat.

Die Seitenlogik ist eine reine Klasse ohne Aves und ohne Minestom. Sie wird mit einer Reihe von Fällen getestet: 0, 1, 45, 46 und 200 Titel, Klammer, Überlauf und Steuerzeile.

### R8 Steuerzeile, Übersicht und Mock-ups

Die untere Reihe hat neun feste Plätze. Entfällt ein Knopf, bleibt sein Platz als Füllglas.

| Platz | Element | Material | Klick |
| --- | --- | --- | --- |
| 45 | Zurück | `OAK_DOOR` | Kategorieseite → Übersicht; in der Übersicht Füllglas |
| 46 | Seite zurück | `SPECTRAL_ARROW` | eine Seite zurück; auf Seite 1 Füllglas |
| 47 | Seitenanzeige | `BOOK` | keine; Name „Seite 1 von 2“, in der Übersicht „Kategorien: 5“ |
| 48 | Stopp | `BARRIER` | stoppt mit Grund `user` |
| 49 | Läuft jetzt | Schallplatte des laufenden Titels, sonst `JUKEBOX` | keine Aktion |
| 50 | Weiter | `ARROW` | R4; ohne laufenden Titel und ohne vorher geöffnete Kategorie Füllglas |
| 51 | Modus | `REPEATER` | zyklisch Einmal → Schleife → Zufall |
| 52 | Lautstärke | `NOTE_BLOCK` | Linksklick +10 %, Rechtsklick −10 %, Grenzen 10 und 100 |
| 53 | Seite weiter | `SPECTRAL_ARROW` | eine Seite vor; auf der letzten Seite Füllglas |

Inhalt auf Plätzen 0 bis 44: in der Übersicht ein Symbol je Kategorie, das die Kategorie öffnet. Auf einer Kategorieseite eine Schallplatte je Titel, deren Klick den Titel startet.

**Läuft jetzt** (Platz 49) zeigt in der Beschreibung den Titel, in der Lore Künstler, Kategorie, Modus und Lautstärke. Beispiel:

```
Läuft: Sunrise
  Künstler: Beispiel
  Kategorie: Spiele
  Modus: Schleife
  Lautstärke: 70 %
```

Ohne laufenden Titel: „Kein Titel läuft“, Modus und Lautstärke. Spielt eine Schallplatte mit einer anderen Lautstärke als der jetzt eingestellten, steht zusätzlich „Lautstärke wirkt ab dem nächsten Titel“ (R9).

**Mock-up der Übersicht (6 Reihen, Plätze der Kategorien ab Platz 10, sieben je Reihe).** Die Zahlen sind die Plätze. `.` ist Füllglas. Kürzel: `Mc` Minecraft, `Sp` Spiele, `An` Anime, `Kl` Klassik, `Ha` Halloween, `Kat` Kategorienzahl, `St` Stopp, `Ls` Läuft jetzt, `Wt` Weiter, `Mo` Modus, `Lv` Lautstärke.

```
+-----+-----+-----+-----+-----+-----+-----+-----+-----+
|  0  |  1  |  2  |  3  |  4  |  5  |  6  |  7  |  8  |
|  .  |  .  |  .  |  .  |  .  |  .  |  .  |  .  |  .  |
+-----+-----+-----+-----+-----+-----+-----+-----+-----+
|  9  | 10  | 11  | 12  | 13  | 14  | 15  | 16  | 17  |
|  .  | Mc  | Sp  | An  | Kl  | Ha  |  .  |  .  |  .  |
+-----+-----+-----+-----+-----+-----+-----+-----+-----+
| 18  | 19  | 20  | 21  | 22  | 23  | 24  | 25  | 26  |
|  .  |  .  |  .  |  .  |  .  |  .  |  .  |  .  |  .  |
+-----+-----+-----+-----+-----+-----+-----+-----+-----+
| 27  | 28  | 29  | 30  | 31  | 32  | 33  | 34  | 35  |
|  .  |  .  |  .  |  .  |  .  |  .  |  .  |  .  |  .  |
+-----+-----+-----+-----+-----+-----+-----+-----+-----+
| 36  | 37  | 38  | 39  | 40  | 41  | 42  | 43  | 44  |
|  .  |  .  |  .  |  .  |  .  |  .  |  .  |  .  |  .  |
+-----+-----+-----+-----+-----+-----+-----+-----+-----+
| 45  | 46  | 47  | 48  | 49  | 50  | 51  | 52  | 53  |
|  .  |  .  | Kat | St  | Ls  | Wt  | Mo  | Lv  |  .  |
+-----+-----+-----+-----+-----+-----+-----+-----+-----+
```

Die Übersicht zeigt fünf Kategorien (Minecraft, Spiele, Anime, Klassik, Halloween). Kommt „Sonstige“ dazu, steht sie auf Platz 15. Die Plätze 45 und 46 sind Füllglas, weil in der Übersicht kein Zurück und keine Seite gibt. Platz 50 („Weiter“) ist mit laufendem Titel gezeigt; ohne Titel und ohne vorher geöffnete Kategorie wird er Füllglas.

**Mock-up einer Kategorieseite (6 Reihen, „Spiele“, Seite 1 von 2, 14 Titel).** `S1` bis `S14` sind die Titel in Reihenfolge. Dieselben Kürzel wie oben.

```
+-----+-----+-----+-----+-----+-----+-----+-----+-----+
|  0  |  1  |  2  |  3  |  4  |  5  |  6  |  7  |  8  |
| S1  | S2  | S3  | S4  | S5  | S6  | S7  | S8  | S9  |
+-----+-----+-----+-----+-----+-----+-----+-----+-----+
|  9  | 10  | 11  | 12  | 13  | 14  | 15  | 16  | 17  |
| S10 | S11 | S12 | S13 | S14 |  .  |  .  |  .  |  .  |
+-----+-----+-----+-----+-----+-----+-----+-----+-----+
| 18  | 19  | 20  | 21  | 22  | 23  | 24  | 25  | 26  |
|  .  |  .  |  .  |  .  |  .  |  .  |  .  |  .  |  .  |
+-----+-----+-----+-----+-----+-----+-----+-----+-----+
| 27  | 28  | 29  | 30  | 31  | 32  | 33  | 34  | 35  |
|  .  |  .  |  .  |  .  |  .  |  .  |  .  |  .  |  .  |
+-----+-----+-----+-----+-----+-----+-----+-----+-----+
| 36  | 37  | 38  | 39  | 40  | 41  | 42  | 43  | 44  |
|  .  |  .  |  .  |  .  |  .  |  .  |  .  |  .  |  .  |
+-----+-----+-----+-----+-----+-----+-----+-----+-----+
| 45  | 46  | 47  | 48  | 49  | 50  | 51  | 52  | 53  |
| Zu  |  .  | 1/2 | St  | Ls  | Wt  | Mo  | Lv  |  >  |
+-----+-----+-----+-----+-----+-----+-----+-----+-----+
```

Auf Seite 2 wird Platz 46 zu `<` (aktiv) und Platz 53 zu Füllglas, sobald es keine weitere Seite gibt. Die Steuerzeile bleibt an denselben Plätzen.

### R9 Lautstärke: eine Sitzungseinstellung, Schritte von 10 Prozent

**Wert:** Ganzzahl in Prozent, 10 bis 100 in Zehnerschritten, Standard 100 (so klingt es wie bisher). Linksklick +10, Rechtsklick −10, an den Grenzen unverändert. Null gibt es nicht: Stopp ist dafür da, und ein stummes Radio, das weiter Pakete schickt, wäre verwirrend.

**Speicher:** `RadioSession`. Sie gilt für die Sitzung und wird beim Abmelden vergessen, wie der Modus. Sie ist nicht in `PlayerSettings`, aus drei Gründen:

1. `PlayerSettings` kennt nur `BooleanSetting`. Eine Zahl braucht einen neuen Werttyp in `core`, eine Erweiterung von `SettingsStore` und eine Änderung am Otis-Adapter. Das ist ein eigener Change.
2. `lobby-settings` schließt nicht boolesche Optionen ausdrücklich aus.
3. `lobby-radio`, D1: Der Radio-Zustand hat keinen zweiten Speicherweg, weil das Radio nur auf Klick läuft.

**MIDI:** Die Lautstärke jeder Note ist `velocity / 127 × Lautstärke`. Sie wirkt ab der nächsten Note, also sofort. Die Rangfolge für das Budget von 16 Noten je Tick (nach Anschlag) bleibt gleich, weil der Faktor für alle gleich ist.

**Vanilla:** Die Schallplatte wird mit der Lautstärke gestartet, die im Moment des Starts gilt (`Sound.sound(disc, RECORD, Lautstärke / 100, 1.0)`). Eine Änderung während des Laufs sendet nichts. Das Ende nach Länge plus Puffer ist unverändert.

Verworfen: Neustart der laufenden Schallplatte mit neuer Lautstärke. Der Client kennt keine Position im Titel und startet die Datei von vorn. Der Spieler hört den Titel neu, die Länge läuft ab Null, und das Titelende springt. Den Neustart könnte man nur mit zwei Paketen mitten im Titel machen.

Begründung für Vanilla gegen MIDI: Ein MIDI-Titel ist eine Folge von Noten, die die Lobby einzeln schickt. Eine Schallplatte ist ein einzelner Sound des Clients, dessen Lautstärke der Server nicht mehr ändern kann. Die Menüanzeige sagt das im Lore von „Läuft jetzt“.

**Knopf statt Paar:** Ein Knopf mit zwei Klickrichtungen. Ein Paar −/+ würde einen Platz der Steuerzeile kosten; die neun Funktionen aus dem Auftrag füllen die Zeile aber genau aus.

**Klangebene:** `Sound.Source.RECORD` folgt zusätzlich dem Platten-Regler des Spielers im Client. Die Lobby-Lautstärke multipliziert sich damit, sie ersetzt ihn nicht.

### R10 Telemetrie: category nur bei plays

- `titan.radio.plays{kind, mode, trigger, category}`. `category` ist die Id: `minecraft` für Vanilla, `other` für Sonstige, sonst die deklarierte Id. Die Kardinalität ist bis 2 × 3 × 2 × 22 = 264 Reihen, weil die Obergrenze von 20 Kategorien (R2) die Menge begrenzt.
- `titan.radio.stops{reason, kind}` bleibt unverändert. Eine Kategorie dort würde jeden Start doppelt zählen, und die Auswertung „welche Kategorie wird gehört“ kommt aus den Starts.
- Kein Attribut mit Spieler-Id. Keine Zähler für Seitenwechsel oder Lautstärkeschritte: das sind Klicks, und sie wären zu häufig für einen Zähler (wie bei den Stopps in `lobby-radio`, D9).
- Der Start protokolliert INFO mit Anzahl Kategorien und Titeln, wie heute mit Anzahl MIDI-Stücken.

### R11 Migration

- **`songs.yaml` ohne `category` und ohne `categories`:** gültig. Alle Titel landen in „Sonstige“. Je Titel ein WARN beim Start. Die Übersicht zeigt „Minecraft“ und „Sonstige“.
- **Einträge mit `id, title, artist, disc, file`:** bleiben gültig. Sie brauchen keine Änderung.
- **Mehr als 18 Titel:** bisher abgelehnt, jetzt bis 200 (R5).
- **Hotbar-Item:** öffnet die Übersicht statt der Vanilla-Liste. Das ist die sichtbare Änderung für alle Spieler und steht im PR und in `docs/lobby-modules.md`.
- **Zurückrollen:** Die alte Version liest nur `songs` und die bekannten Felder jedes Eintrags. Die neuen Felder `categories` und `category` ignoriert sie. Mehr als 18 Titel lehnt sie ab, sonst läuft sie. Eine Rückrollung ist also ohne Datenänderung möglich.
- **Keine Otis-, Schema- oder Konfigurationsänderung.**

## Test-Strategie

Alle Tests folgen F.I.R.S.T. (`openspec/config.yaml`, Titan-Regeln):

- **Rein (Unit, ohne Server):** `SongCatalogTest` mit `@TempDir` und Logback-`ListAppender` für WARN (Kategorien, Reservierung, Sonstige, Symbolfehler, Obergrenzen 20, 200 und 1 000 000 Noten). `RadioCategoriesTest` (Reihenfolge, leere Kategorie, Sonstige hinten). `PageLayoutTest` (Seiten, Plätze, Steuerzeile, Klammer). `RadioSelectionTest` mit `java.util.Random(seed)` (Scope je Modus, Wrap, eine Kategorie mit einem Titel, leere Kategorie). `VolumeTest` (Schritte, Grenzen, Standard). `NoteMapperTest` und `RadioPlaybackTest` mit der Lautstärke, `VanillaDiscPlaybackTest` mit der Frage „ändert eine Lautstärkeänderung den laufenden Titel?“ (nein). `RadioSessionTest` (Vergessen beim Logout, Seite bleibt beim Blättern, Seite springt zurück beim Kategoriewechsel).
- **Telemetrie und Meldungen:** `RadioTelemetryTest` prüft, dass `plays` ein `category`-Attribut hat, `stops` keines, und dass nie eine Spieler-Id vorkommt. `RadioMessagesTest` prüft die neuen Schlüssel in beiden Sprachen.
- **Env (`env.tick()`, Testverbindung):** `RadioMenuIntegrationTest`: Hotbar öffnet die Übersicht, Klick auf Kategorie öffnet ihre Seite, Blättern, Modus- und Lautstärkeklick aktualisieren dasselbe Inventar (gleiche Instanz, kein Neuöffnen), Zurück, Schließen entfernt die Ansicht ohne Rekursion, `@PreDestroy` schließt alle offenen Fenster.
- **Architektur:** `ColumnArchitectureTest` wie bisher. `features/radio` hängt weiter nur von `core` ab.
- **Keine** Sleeps, keine Systemzeit, keine geteilten statischen Zustände, Zufall nur mit festem Seed.

## Offene Fragen

Die Liste steht in `proposal.md`, Offen. Die Fragen betreffen Aufgabe 1.1 (Aves-Titel), 10.4 (Speichermessung) und den Owner (globaler Zufall, Speicherung der Lautstärke, Schrittweite).
