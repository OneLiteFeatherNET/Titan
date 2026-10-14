# Proposal

## Why

Der Navigator hat seit #424 zwei Klickarten für Modi: Linksklick auf ein Ziel, das gerade auf mehreren Servern läuft, öffnet die Serverauswahl; Rechtsklick läuft zum Portal des Ziels. Nichts im Menü verrät das.

## What Changes

- Jeder Modus-Eintrag im Navigator bekommt eine zusätzliche Lore-Zeile mit den Klicks:
  - Linksklick: „Server wählen“, solange ein Ziel mit Serverauswahl (ElytraRace, Slender) auf mehreren laufenden Servern läuft, weil der Klick dann die Auswahl öffnet; sonst „verbinden“.
  - Rechtsklick: „zum Portal laufen“, nur wenn für das Ziel ein Portal existiert.
- Die Zeile ist übersetzt (de/en) über die vorhandene Übersetzungsart: `MiniMessageTranslationStore` mit `GlobalTranslator`, Bundles `messages_de.properties` und `messages_en.properties`.
- Der Spawn-Eintrag bekommt keinen Hinweis, weil sein Klick nicht von der Maustaste abhängt.
- Die Zeile mit der Spielerzahl bleibt unverändert und steht vor dem Hinweis.

## Capabilities

### New Capabilities

### Modified Capabilities
- `lobby-navigator`: neue Anforderung für die Klickhinweise in der Lore der Modus-Einträge.

## Impact

- **Code:** `features/navigator` (`Destination`, `NavigatorModule`, `SharedNavigator`, neue `NavigatorMessages`, Bundles unter `features/navigator/src/main/resources/titan/navigator/`).
- **Nutzertexte:** neue übersetzte Lore-Zeilen (de/en). Titel und Ziel-Namen bleiben unverändert.
- **Abhängigkeiten:** keine neuen.
- **Basis:** `main` nach #424 (Serverauswahl), ausgeliefert als #433.

## Delivery

Pull-Request-Titel und Squash-Commit von #433: `feat(navigator): explain click actions in the entry lore`

Der Nachtrag, der den Hinweis an den tatsächlichen Klick angleicht, ist in `tasks.md` (Abschnitt 6) beschrieben und trägt den Titel `fix(navigator): keep click hints in line with the actual click`.
