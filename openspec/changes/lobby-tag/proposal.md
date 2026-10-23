# Proposal

## Why

Die Lobby hat Winken, Tänze und Sprungläufe, aber kein Spiel, das Spieler zusammenbringt. „Fangen“ ist ein einfaches Spiel ohne Ende: Ein Spieler ist Fänger, trifft er einen anderen aktiven Spieler, wird der andere Fänger. Es gibt keine Runden, keinen Timer und keinen Gewinner. Es soll einfach Spaß machen, und es braucht nichts außer Vanilla.

Die Teilnahme ist freiwillig. Wer nicht mitspielen will, wird nie betroffen, und sein Schutz bleibt wie bisher.

## What Changes

- **Teilnahme als Einstellung:** Fangen ist ein Opt-in mit Standard „aus“. Der Schalter ist die typisierte Einstellung `titan:tag.enabled` über die Settings-Schnittstelle aus `lobby-settings` (Otis-Spielerdaten). Das Menü dafür liefert `lobby-settings`; diese Änderung baut keinen eigenen Schalter und kein Hotbar-Item dafür.
- **Befehl:** `/tag` schaltet dieselbe Einstellung um. Er ist eine Abkürzung, keine zweite Quelle der Wahrheit. Konsole erhält eine Meldung, dass nur Spieler mitspielen können.
- **Aktive Spieler:** Ein Spieler ist aktiv, wenn er die Einstellung auf „an“ hat, in der Lobby-Instanz ist und gerade nicht in einem Sprungtest-Lauf steckt und nicht in der Übergabe zu einem anderen Server ist (Details in `design.md`).
- **Genau ein Fänger pro Instanz:** Ab zwei aktiven Spielern gibt es immer genau einen Fänger. Verlässt er die Gruppe, schaltet ab oder wird inaktiv, wird zufällig ein anderer aktiver Spieler Fänger. Bei weniger als zwei aktiven Spielern gibt es keinen Fänger, und die Actionbar sagt das.
- **Sichtbarkeit ohne Resource Pack:** Der Fänger leuchtet (Glowing) in Rot, sein Name bekommt ein rotes Team-Präfix, und er hat rote Partikel um sich. Alles über Vanilla-Mittel.
- **Treffer:** Ein Nahkampftreffer des Fängers auf einen anderen aktiven Spieler übergibt die Rolle. Der Treffer macht keinen Schaden und keinen Rückstoß, nur eine kurze Trefferanimation und einen Ton. Der frisch gefangene Spieler bekommt einen Titel, beide hören einen Ton. Zurück zum vorherigen Fänger geht es für 5 Sekunden nicht.
- **Andere Treffer:** Nahkampftreffer zwischen zwei aktiven Spielern, bei denen nicht der Fänger angreift, werden ebenfalls ohne Schaden abgebrochen. Für Spieler außerhalb des Spiels bleibt der Schutz unverändert.
- **Inaktivität des Fängers:** Hat der Fänger 120 Sekunden lang weder sich bewegt noch getroffen, wird die Rolle neu vergeben, damit das Spiel nicht an einem AFK-Fänger hängen bleibt.
- **Interaktionen:** Tanz, Sitzen, Elytra, Sprungtest und Serverwechsel sind in `design.md` entschieden.
- **Telemetrie:** Zähler `titan.tag.tags{result}` (`tagged`, `cooldown`) und `titan.tag.toggles{state}` (`on`, `off`), ohne Spieler-Id.
- **Meldungen:** Deutsch und Englisch in `titan/tag/messages_de.properties` und `messages_en.properties`.

Nicht Teil dieser Änderung:

- Das Settings-Menü, die Otis-Anbindung und die Typen der Einstellungen. Das liefert `lobby-settings`.
- Statistiken wie „Zeit als Fänger“, Bestenlisten oder Persistenz über Sitzungen. Nur ein Zähler für Treffer; Zeit als Fänger ist ein späteres Thema.
- Eigene Resource Packs, Modelle oder Sounds.
- Konfigurierbare Abklingzeiten, Inaktivitätsschwelle oder Farben. Die Werte stehen im Code, bis ein Betreiber sie braucht.
- Ein Rundenmodus oder ein Timer. Das Spiel läuft dauerhaft.
- Treffer mit Projektilen. Nur Nahkampf.

## Delivery

Der Pull-Request-Titel und der Squash-Commit der späteren Umsetzung sind `feat(tag): add an always-open tag game to the lobby`. Die Änderung ist nicht breaking. Die OpenSpec-Artefakte stehen in diesem Commit `docs(openspec): propose lobby-tag`, der Code folgt in `feat(tag)`.

## Abhängigkeiten

- **`lobby-settings` (Voraussetzung, wird zuerst umgesetzt):** `lobby-tag` baut auf `lobby-settings` auf. Die Einstellung `titan:tag.enabled`, die Settings-Schnittstelle in `core` und die Otis-Anbindung kommen aus `lobby-settings`. Mit der Umsetzung von `lobby-tag` wird erst begonnen, wenn `lobby-settings` gemerged ist. Die Reihenfolge steht in `tasks.md`, Abschnitt 0.
- **Zusätzlich berührt:** `jumprun` und `portal` senden ein neues Core-Ereignis `LobbyPlayerBusyEvent` (siehe `design.md` D4). Das ist eine kleine Änderung in diesen Columns, keine Abhängigkeit von Fangen auf sie.

## Capabilities

### New Capabilities

- `lobby-tag`: Opt-in, Fänger-Rolle je Instanz, Treffer, Sichtbarkeit, Interaktionen, Befehl und Telemetrie.

### Modified Capabilities

Keine. Die Einstellung und ihr Menü gehören zu `lobby-settings`.

## Impact

- **Code:** neues Modul `features/tag` (Column `tagColumn`, Paket `net.onelitefeather.titan.feature.tag`), nur abhängig von `core`. Dazu im Core das Ereignis `LobbyPlayerBusyEvent` und die Schnittstelle für Einstellungen aus `lobby-settings`. Kleine Ergänzungen in `jumprun` und `portal`, die das Ereignis senden.
- **Abhängigkeiten:** keine neuen Bibliotheken. Minestom bleibt bei `2026.10.07-26.2`.
- **Nutzertexte:** `titan/tag/messages_de.properties` und `messages_en.properties`.
- **Betrieb:** Keine Konfiguration und keine Welt. Ohne Opt-in ist das Modul für niemanden sichtbar.
- **Offene Punkte:** siehe `design.md`, Abschnitt „Offene Fragen“.
