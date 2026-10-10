# Proposal

## Why

Die Lobby-Welten enthalten gebaute Deko-Entities: Rüstungsständer, Itemrahmen, Gemälde und Display-Entities. Titan lädt die Welt mit Minestoms `AnvilLoader`, und der liest das Verzeichnis `entities/` nie. In der Lobby fehlt deshalb alles, was die Bauer als Entity gesetzt haben. Falco, unsere eigene Anvil-Engine, ist schneller und bekommt gerade Entity-Loading (eigenes Falco-Design). Mit dem Umstieg kommen die Deko-Entities in die Lobby.

## What Changes

- Titan lädt die Lobby-Welt (und Saisonwelten) mit dem `FalcoAnvilLoader` statt mit Minestoms `AnvilLoader`. Blöcke, Block-Entities und Licht verhalten sich für Spieler wie bisher.
- Deko-Entities aus `entities/` erscheinen beim Laden ihres Chunks an ihrer gespeicherten Position und mit ihren gespeicherten Daten:
  - Rüstungsständer: Pose, Ausrüstung, Flags
  - Itemrahmen und leuchtende Itemrahmen: Item, Drehung
  - Gemälde: Motiv
  - Block-, Item- und Text-Displays: Transformation, Billboard, Text
- Die Entities sind reine Deko: Spieler können sie nicht zerstören, Items nicht herausnehmen und nichts drehen.
- Die Lobby schreibt nie Entity-Daten zurück in die Welt (read-only).
- Andere Entity-Typen in der Welt (z. B. Mobs) werden übersprungen. Das Laden der Welt bricht dadurch nicht ab.
- Abhängigkeit: Ein Falco-Release mit Entity-Loading (Falco-Design `docs/superpowers/specs/2026-10-02-anvil-entity-loading-design.md`) muss vorher erscheinen.

## Capabilities

### New Capabilities
- `lobby-world-entities`: welche Entities aus den Lobby-Welten erscheinen, wie sie sich gegenüber Spielern verhalten und dass die Lobby sie nie speichert.

### Modified Capabilities
- keine (`lobby-seasons` wählt weiterhin nur die Welt, das Laden selbst ist Implementierung).

## Impact

- **Code:** `common/.../map/MapProvider` (Loader-Wechsel und Entity-Laden im read-only-Modus), gegebenenfalls `features/protection` für das Abwehren von Interaktionen mit Deko-Entities, `docs/world-conversion.md` (Entities werden jetzt genutzt, Hinweis auf geeignete Typen).
- **Abhängigkeiten:** neu `net.onelitefeather:falco-anvil` (und, falls das Design es verlangt, `falco-instance`) in der Version mit Entity-Loading, über den Versionskatalog in `settings.gradle.kts`. Falco nutzt wie Titan Minestom 26.2.
- **Nutzertexte:** keine.
- **Betrieb:** Bestehende Welten unter `worlds/` funktionieren ohne Umbau. Ihre `entities/*.mca` werden jetzt gelesen. Ein Welt-Update mit `--forceUpgrade` (siehe `docs/world-conversion.md`) muss `entities/` weiter mitkopieren, das tut das Skript heute schon.

## Delivery

Pull-Request-Titel und Squash-Commit: `feat(world): show decoration entities from lobby worlds via falco`
