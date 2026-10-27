# Proposal

## Why

Das Radio der Lobby (`lobby-radio`, in 4.9.0 ausgeliefert) hat zwei feste Menüs: „Vanilla-Platten“ mit 22 Platten und „Eigene Songs“ mit höchstens 18 MIDI-Stücken. Mehr als 18 Stücke passen nicht, und die Stücke lassen sich nicht ordnen. Die Steuerzeile liegt ohne Lautstärke und ohne Seitenwahl in der untersten Reihe, und der Modusknopf zeigt den Modus nur über die Wahl des geteilten Inventars. Wer ein Stück aus einer bestimmten Stimmung sucht, findet es nicht, und wer leiser stellen will, hat keinen Knopf dafür.

## What Changes

- **Kategorien:** Jeder Eintrag in `radio/songs.yaml` bekommt ein Feld `category`. Die Kategorien selbst stehen oben in derselben Datei (Id, Anzeigename Deutsch und Englisch oder ein Name, Symbol als Vanilla-Item, Reihenfolge). Die 22 Vanilla-Platten bilden die eingebaute Kategorie „Minecraft“. Ein fehlendes oder unbekanntes `category` landet in „Sonstige“ und wird protokolliert (WARN), nie fatal. Begründung in `design.md`, R2 und R3.
- **Übersicht:** Das Hotbar-Item öffnet immer die Kategorieübersicht. Ein Klick auf eine Kategorie öffnet ihre Titelliste mit einem Zurück-Knopf. Der Umschalter „Eigene Songs“ entfällt, weil Vanilla eine Kategorie unter vielen ist.
- **Zufall im Kategorie-Scope:** Zufall und Weiter bleiben in der Kategorie des laufenden Titels. Begründung in R4.
- **Seiten:** Titellisten sind seitenweise, mit 45 Titeln je Seite in einer 6-Reihen-Kiste. Die Steuerzeile in der untersten Reihe hat feste Plätze. Die aktuelle Seite gehört dem Spieler (Sitzung). Die Obergrenze liegt bei 200 MIDI-Stücken und einer Gesamtgrenze von 1 000 000 Noten (R5).
- **Aufbau des Menüs:** Statt sechs geteilter Inventare baut die Lobby beim Öffnen ein Inventar je Spieler über Aves und aktualisiert es an Ort und Stelle. Das ersetzt die geteilten Varianten aus `lobby-radio`, D14. Begründung und Kosten in R6.
- **Lautstärke:** Ein Knopf in der Steuerzeile, 10 bis 100 Prozent in Zehnerschritten, Linksklick lauter, Rechtsklick leiser, Standard 100 Prozent. Sie gilt nur für die Sitzung. MIDI-Noten folgen sofort, eine laufende Schallplatte behält ihre Lautstärke bis zum nächsten Titel (R9).
- **Übersichtlichere Steuerzeile und „Läuft jetzt“:** Die untere Reihe hat neun feste Plätze (Zurück, Seite zurück, Seitenanzeige, Stopp, Läuft jetzt, Weiter, Modus, Lautstärke, Seite weiter). Nicht benötigte Knöpfe sind Füllglas statt entfernt. Der Knopf „Läuft jetzt“ nennt Titel, Künstler, Kategorie, Modus und Lautstärke. Nur Vanilla-Material, keine Resource Pack. Mock-ups in `design.md`, R8.
- **Telemetrie:** `titan.radio.plays` bekommt das Attribut `category`, weil die Kategorien durch die Obergrenze von 22 beschränkt sind (R10). Sonst bleibt die Telemetrie gleich.
- **Migration:** Eine `songs.yaml` ohne `category` und ohne Kategorienliste bleibt gültig. Alle Titel landen in „Sonstige“ (R11).

Unverändert bleiben: private Wiedergabe nur für den Startenden, Stoppgründe (`user`, `next`, `end`, `logout`, `busy`, `shutdown`), Sitzungsmodus mit Standard Einmal, Telemetrie ohne Spieler-Id, Meldungen in Deutsch und Englisch.

Nicht Teil dieser Änderung:

- Keine gespeicherte Lautstärke. Sie gilt für die Sitzung, bis über `PlayerSettings` nicht boolesche Werte möglich sind (R9).
- Kein globaler Zufall über alle Kategorien als fünfter Modus (offen, siehe unten).
- Keine Rechte je Kategorie und keine Kategorie-Auswahl durch Spieler.
- Keine Änderung an Telemetrie-Spans, an `LobbyPlayerBusyEvent` oder an `features/settings`.
- Kein Resource Pack und keine Icons außerhalb der Vanilla-Materialien.

## Type and Scope

Die Umsetzung wird als **`feat(radio)`** ausgeliefert, mit dem Pull-Request-Titel `feat(radio): add categories, paging and volume to the radio menu`. Die Änderung ist nicht brechend: Bestehende `songs.yaml` ohne Kategorie funktionieren, nur das Menü ändert sich. Der Hotbar-Eintrag öffnet danach die Übersicht statt der Vanilla-Platten. Das ist eine Änderung im Menü und wird im PR beschrieben, nicht als eigener Typ. Die Dokumentation in `docs/lobby-modules.md` gehört zur selben `feat`.

Die Vorschlags-Artefakte stehen im Commit `docs(openspec): propose radio-categories`. Der Code folgt in `feat(radio)`, die Dokumentation in einem eigenen Commit `docs(radio)` im selben Pull Request.

## Capabilities

### New Capabilities

Keine.

### Modified Capabilities

- `lobby-radio`: Menüstruktur, Umschalter, Modusknopf, Zufall und Weiter, Lautstärke, MIDI-Lautstärke, Grenzen, Telemetrie, Ladeverhalten der Kategorien. Die Änderung ist ein Delta gegen die Anforderungen in `openspec/changes/lobby-radio/specs/lobby-radio/spec.md`. Die Vorbedingung fürs Archivieren steht in `tasks.md`: `lobby-radio` wird zuerst archiviert, erst dann gibt es `openspec/specs/lobby-radio/spec.md`.

## Impact

- **Code:** nur `features/radio`. Neue Klassen für Kategorien, Seiten und Lautstärke; `RadioMenuBuilder` und `RadioMenuLayout` werden umgebaut; `RadioSelection`, `RadioSession`, `SongCatalog`, `RadioTelemetry` und die Meldungen werden erweitert. Keine Änderung in `core`, `features/settings` oder `features/navigator`.
- **Abhängigkeiten:** keine neue. Aves wird mit dem vorhandenen `PersonalTranslatedInventoryBuilder` genutzt; die Prüfung dazu ist Aufgabe 1.1.
- **Speicher:** bis zu 1 000 000 Noten (R5). Die Schätzung in `lobby-radio`, D8, ist zu niedrig; sie ist in `design.md` korrigiert.
- **Betrieb:** Die Lobby liest `radio/songs.yaml` weiterhin einmal beim Start. Ohne Datei bleibt das Radio mit den 22 Vanilla-Platten in der Kategorie „Minecraft“ betriebsbereit. Die Kategorie „Sonstige“ erscheint nur, wenn sie Titel hat.
- **Nutzertexte:** neue Schlüssel für Kategorienamen, Seitenanzeige, Lautstärke und „Läuft jetzt“ in `messages_de.properties` und `messages_en.properties`.
- **Dokumentation:** `docs/lobby-modules.md`, Abschnitt Radio, mit Beispiel für `songs.yaml` und Kategorien.
- **Pull-Request-Vorlage:** `.github/pull_request_template.md` existiert im Repository nicht. Die Beschreibung folgt dem Schema Problem/Approach der bisherigen Radio-PRs.

## Entschieden

Diese Entscheidungen gelten für den Vorschlag und sind vom Owner zu bestätigen. Begründung in `design.md`.

1. **Kategorien in `songs.yaml`**, nicht als Ordner (R2).
2. **Zufall und Weiter bleiben in der Kategorie des laufenden Titels** (R4).
3. **Obergrenze 200 MIDI-Stücke** und **1 000 000 Noten insgesamt** (R5).
4. **Ein Inventar je Spieler**, beim Öffnen gebaut und in Ort und Stelle aktualisiert. Die geteilten Varianten aus `lobby-radio` entfallen (R6).
5. **Seiten zu 45 Titeln** in einer 6-Reihen-Kiste; Steuerzeile mit neun festen Plätzen (R7).
6. **Lautstärke 10 bis 100 Prozent in Zehnerschritten**, ein Knopf mit Links- und Rechtsklick, Standard 100 Prozent (R9).
7. **Lautstärke nur für die Sitzung**, nicht in `PlayerSettings`, weil dieses nur Schalter kennt (R9).
8. **Eine laufende Schallplatte startet nicht neu**; eine Änderung der Lautstärke wirkt ab dem nächsten Titel. MIDI-Noten folgen sofort (R9).
9. **Attribut `category` nur bei `titan.radio.plays`**, nicht bei den Stopps (R10).
10. **Keine Konfigurationsoption für die Standardkategorie**; sie ist „Sonstige“ (R3).

## Offen

1. **Globaler Zufall als fünfter Modus?** Wer „Zufall über alles“ will, braucht einen Modus oder einen Schalter. Vorschlag: eigener Change, falls der Owner ihn wünscht. Dieser Vorschlag bleibt beim Kategorie-Scope.
2. **Lautstärke über den Logout hinaus speichern?** Dafür braucht `PlayerSettings` nicht boolesche Werte und der Otis-Adapter einen Wertetyp. Das ist ein eigener Change und nicht Teil dieser Umsetzung.
3. **Notenbudget 1 000 000:** ist die Obergrenze für den Heap der Lobby vertretbar? Die Zahl ist eine Schätzung (R5). Messung in Aufgabe 10.4.
4. **Schrittweite 10 Prozent oder 25 Prozent?** Vorschlag: 10 Prozent, also zehn Werte. Bei 25 Prozent gäbe es vier Werte, und die Lautstärke wäre grob.
5. **Titel der Seite in place?** Ob Aves den Fenstertitel eines übersetzten Builders beim Seitenwechsel ändern kann, prüft Aufgabe 1.1. Wenn nicht, öffnet der Seitenwechsel das Fenster neu. Mode und Lautstärke bleiben in jedem Fall in place.
6. **Glint für das laufende Stück?** Ein Leuchten auf dem Titel, der gerade läuft, hilft in langen Listen. Es ist Vanilla-Material und kostet nichts, ist aber nicht gefordert. Nicht enthalten.
