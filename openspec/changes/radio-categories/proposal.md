# Proposal

## Why

Das Radio der Lobby (`lobby-radio`, in 4.9.0 ausgeliefert) hat zwei feste Menüs: „Vanilla-Platten“ mit 22 Platten und „Eigene Songs“ mit höchstens 18 MIDI-Stücken hinter einem Umschalter. Die Stücke lassen sich nicht ordnen, und es gibt keine Seiten und keine Lautstärke. Wer ein Stück aus einer Stimmung sucht, findet es nicht, und wer leiser stellen will, hat keinen Knopf dafür. Die Steuerzeile hat dafür keinen Platz, und das geteilte Inventar je Menü und Modus kann keinen Spielerstand tragen.

## What Changes

- **Kategorien:** Jeder Eintrag in `radio/songs.yaml` bekommt ein Feld `category`. Die Kategorien stehen oben in derselben Datei (Id, Anzeigename Deutsch und Englisch oder ein Name, Symbol als Vanilla-Item, Reihenfolge). Die 22 Vanilla-Platten bilden die eingebaute Kategorie „Minecraft“. Ein fehlendes oder unbekanntes `category` landet in „Sonstige“ und wird protokolliert (WARN), nie fatal (R2, R3).
- **Übersicht:** Das Hotbar-Item öffnet immer die Kategorieübersicht. Ein Klick auf eine Kategorie öffnet ihre Titelliste mit Zurück-Knopf. Der Umschalter „Eigene Songs“ entfällt.
- **Kategorie-Bereich:** Zufall und „Weiter“ bleiben in der Kategorie des laufenden Titels (R4).
- **Keine Grenzen, verzögertes Laden:** Es gibt keine Obergrenzen für Titel, Dateigröße, Noten oder Länge. Beim Start liest die Lobby nur die Metadaten aus `songs.yaml` und prüft je Datei den MIDI-Kopf. Die Noten werden erst beim Start eines Titels geladen, außerhalb des Takts, und in einem kleinen Cache gehalten (R5).
- **Seiten:** Titellisten sind seitenweise, 45 Titel je Seite in einer 6-Reihen-Kiste. Die Steuerzeile in der untersten Reihe hat neun feste Plätze. Die Seite gehört dem Spieler (Sitzung) (R7).
- **Aufbau des Menüs:** Statt sechs geteilter Inventare baut die Lobby beim Öffnen ein Inventar je Spieler über Aves und aktualisiert es an Ort und Stelle. Das ersetzt die geteilten Varianten aus `lobby-radio`, D14 (R6).
- **Lautstärke:** Ein Knopf in der Steuerzeile mit 25, 50, 75 und 100 Prozent, Standard 100. Linksklick erhöht, Rechtsklick senkt, am Rand bleibt der Wert stehen. Die Lautstärke wird **pro Spieler in Otis gespeichert**, also über den Logout hinaus. Dafür ist eine Voraussetzung nötig: die Spielereinstellungen in Titan kennen nur Schalter (R9).
- **Glanz:** Der Titel, der für diesen Spieler gerade läuft, leuchtet (Glint). Das gilt nur im Fenster dieses Spielers (R12).
- **Übersichtliche Steuerzeile und „Läuft jetzt“:** Die untere Reihe hat neun feste Plätze (Zurück, Seite zurück, Seitenanzeige, Stopp, Läuft jetzt, Weiter, Modus, Lautstärke, Seite weiter). Nicht benötigte Knöpfe sind Füllglas statt entfernt. „Läuft jetzt“ nennt Titel, Künstler, Kategorie, Modus und Lautstärke (R8).
- **Telemetrie:** `titan.radio.plays` bekommt das Attribut `category`, begrenzt durch die Obergrenze von 22 Kategorien (R10). `titan.radio.songs{result=rejected}` zählt zusätzlich die Titel, die beim Abspielen nicht lesbar sind.
- **Migration:** Eine `songs.yaml` ohne `category` und ohne Kategorienliste bleibt gültig. Alle Titel landen in „Sonstige“ (R11).

Unverändert bleiben: private Wiedergabe nur für den Startenden, Stoppgründe (`user`, `next`, `end`, `logout`, `busy`, `shutdown`), Modus als Sitzungseinstellung mit Standard Einmal, Telemetrie ohne Spieler-Id, Meldungen in Deutsch und Englisch, die 16-Noten-Grenze je Tick (Spielqualität, siehe R5).

Nicht Teil dieser Änderung:

- Keine Lautstärke im Einstellungsmenü der Lobby (R9).
- Kein Zufall über alle Kategorien (Entschieden 4).
- Keine Rechte je Kategorie und keine Kategorie-Auswahl durch Spieler.
- Keine Änderung an Telemetrie-Spans, an `LobbyPlayerBusyEvent` oder an der Otis-API.
- Kein Resource Pack und keine Icons außerhalb der Vanilla-Materialien.

## Type and Scope

Die Umsetzung wird als **`feat(radio)`** ausgeliefert, mit dem Pull-Request-Titel `feat(radio): add categories, paging and volume to the radio menu`. Die Änderung ist nicht brechend: Bestehende `songs.yaml` ohne Kategorie funktionieren, nur das Menü ändert sich. Der Hotbar-Eintrag öffnet danach die Übersicht statt der Vanilla-Platten.

Die Voraussetzung für die gespeicherte Lautstärke ändert die gemeinsame Spielereinstellung in `core`, `features/settings` und `platform/otis`. Sie ist im selben Pull Request als eigener Commit `feat(settings)` vorgesehen (`tasks.md`, Gruppe 2). Der Owner kann sie vorab als eigenen Change ausliefern lassen, siehe Offen.

Die Dokumentation in `docs/lobby-modules.md` folgt in einem eigenen Commit `docs(radio)` im selben Pull Request.

Die Vorschlags-Artefakte stehen im Commit `docs(openspec): propose radio-categories` (PR #511), die Entscheidungen des Owners im Commit `docs(openspec): record owner decisions for radio-categories`.

## Capabilities

### New Capabilities

Keine.

### Modified Capabilities

- `lobby-radio`: Menüstruktur, Umschalter, Modusknopf, Zufall und Weiter, Lautstärke, Grenzen (auf die Paketzahl reduziert), Ladeverhalten der MIDI-Dateien, Telemetrie. Die Änderung ist ein Delta gegen `openspec/changes/lobby-radio/specs/lobby-radio/spec.md`. Die Vorbedingung fürs Archivieren steht in `tasks.md`: `lobby-radio` wird zuerst archiviert.
- `lobby-settings` (über die Voraussetzung): Spielereinstellungen können einen Wert mit fester Stufenliste speichern, nicht nur einen Schalter. Dieses Delta wird mit dem Commit `feat(settings)` angelegt, siehe `tasks.md` Gruppe 2.

## Impact

- **Code:** `features/radio` (Kategorien, Seiten, Cache und Laden, Lautstärke über den Port, Glanz), `core/settings` (Stufen-Einstellung und Port), `features/settings` (Ladung und Schreiben für Stufen), `platform/otis` (Wert als JSON-Zahl). `features/radio` hängt weiter nur von `core` ab.
- **Otis:** keine Änderung nötig. Der Otis-Stand auf `origin/main` (Einstellungs-API, PR #181) nimmt beliebige JSON-Werte bis 64 KiB entgegen. Ob die produktive Otis-Instanz diesen Stand hat, prüft das Repository nicht; die Schalter der Lobby hängen bereits davon ab (R9).
- **Speicher:** Beim Start nur Metadaten. Zur Laufzeit höchstens 8 zuletzt gespielte Titel plus die laufenden Titel im Cache (R5).
- **Nutzertexte:** neue Schlüssel für Kategorien, Seitenanzeige, Lautstärke, „Läuft jetzt“, „Lädt …“ und „nicht abspielbar“ in `messages_de.properties` und `messages_en.properties`.
- **Dokumentation:** `docs/lobby-modules.md`, Abschnitt Radio.
- **Pull-Request-Vorlage:** `.github/pull_request_template.md` existiert nicht. Die Beschreibung folgt dem Schema Problem/Approach.

## Entschieden

Diese Entscheidungen sind vom Owner getroffen (Antwort auf den Vorschlag, PR #511). Begründung und Umsetzung in `design.md`.

1. **Keine Grenzen.** Die Titelgrenze (200), die Notengesamtgrenze (1 000 000) und die Grenzen aus `lobby-radio`, D8 (512 KiB, 20 000 Noten, 15 Minuten, 18 Titel) entfallen. Der Speicher wird durch verzögertes Laden und einen kleinen Cache gesichert (R5).
2. **Lautstärke in Otis gespeichert.** Pro Spieler über die Spielereinstellungen, Schlüssel `titan:radio.volume`, Wert als JSON-Zahl. Sie gilt über den Logout hinaus (R9).
3. **Stufen 25, 50, 75, 100 Prozent**, Standard 100, kein 0 (Stopp deckt das ab). Linksklick erhöht, Rechtsklick senkt, am Rand bleibt der Wert stehen (kein Umlauf) (R9).
4. **Kein Zufall über alle Kategorien.** Zufall und „Weiter“ bleiben in der Kategorie des laufenden Titels (R4).
5. **Glanz auf dem laufenden Titel** im Fenster des Spielers, über `DataComponents.ENCHANTMENT_GLINT_OVERRIDE`, vanilla und ohne Resource Pack (R12).
6. **Lautstärke nicht im Einstellungsmenü.** Sie steht nur im Radio, weil das Menü ein Schaltermenü mit Platz für eine Zeile ist und vier Stufen dort nicht passen (R9).
7. **Die 16-Noten-Grenze je Tick bleibt.** Sie ist Spielqualität: sie begrenzt Pakete je Spieler und Tick, nicht den Inhalt einer Datei.
8. **Verzögertes Laden mit Cache.** Beim Start nur Metadaten und ein Kopfcheck; Noten beim Start eines Titels, asynchron, mit einem LRU-Cache von 8 nicht laufenden Titeln (R5).
9. **Kategorien in `songs.yaml`**, nicht als Ordner (R2).
10. **Keine Konfigurationsoption für die Standardkategorie**; sie ist „Sonstige“ (R3).
11. **Ein Inventar je Spieler**, beim Öffnen gebaut und in Ort und Stelle aktualisiert. Die geteilten Varianten aus `lobby-radio` entfallen (R6).
12. **Seiten zu 45 Titeln** in einer 6-Reihen-Kiste; Steuerzeile mit neun festen Plätzen (R7).
13. **Attribut `category` nur bei `titan.radio.plays`** (R10).

## Offen

1. **Voraussetzung in diesem PR oder als eigener Change?** Die Änderung an der Spielereinstellung betrifft drei Module. Vorschlag: im selben PR als Commit `feat(settings)`, damit die Umsetzung in einem Stück läuft. Der Owner kann sie auch vorab als `lobby-settings-levels` ausliefern lassen.
2. **Cachegröße 8:** Eine Zahl, die auf dem Testserver mit der Speichermessung (Aufgabe 15.2) zu prüfen ist. Bei Speicherwarnungen wäre ein Cache nach Notenzahl die nächste Stufe.
3. **Otis-Stand produktiv:** Das Repository zeigt, dass die Einstellungs-API auf `origin/main` liegt, aber nicht, ob die produktive Otis-Instanz sie hat. Das prüft der Owner, bevor die Lautstärke ausgeliefert wird. Die Schalter der Lobby hängen bereits davon ab.
4. **Aves-Fenstertitel in place?** Ob Aves den Titel eines übersetzten Builders beim Seitenwechsel ändern kann, prüft Aufgabe 1.1. Wenn nicht, öffnet der Seitenwechsel das Fenster neu. Modus und Lautstärke bleiben in jedem Fall in place.
5. **Vorladen des nächsten Titels?** Ohne Vorladen wartet der Spieler bei einem Cache-Fehlgriff den Parse-Lauf ab (Millisekunden bis wenige Sekunden je nach Datei). Vorladen ist nicht Teil dieser Änderung.
