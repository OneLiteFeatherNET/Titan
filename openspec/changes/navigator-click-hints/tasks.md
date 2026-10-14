# Tasks

Gestapelt auf `feat/navigator-server-picker-2` (#424). Jeder Schritt: Test zuerst (rot), dann Code (grün).

## 1. Übersetzungen

- [x] 1.1 Test zuerst (Unit, `NavigatorMessagesBundleTest`): `messages_en.properties` definiert alle Schlüssel aus `NavigatorMessages.KEYS`; `messages_de.properties` hat dieselben Schlüssel. Rot, dann Bundles und `NavigatorMessages`.
- [x] 1.2 Test zuerst (Unit, `NavigatorMessagesTest`): Für jede der vier Kombinationen (Serverauswahl ja/nein × Portal ja/nein) ist der gerenderte Text in Deutsch und Englisch nicht leer, und Deutsch unterscheidet sich von Englisch. Rot, dann grün.

## 2. Lore-Zeile

- [x] 2.1 Test zuerst (Unit, Minestom-Env, `DestinationClickHintLoreTest`): Ein Hinweis steht nach der Spielerzahl; ohne Spielerzahl steht nur der Hinweis; ohne Hinweis bleibt die Lore wie bisher. Rot, dann `Destination.item(OptionalInt, Component)`.

## 3. Navigator

- [x] 3.1 Test zuerst (Integration, `NavigatorClickHintTest`, `Env`): Mit Portal zeigt Survival beide Angaben, ohne Portal nur den Linksklick, Spawn keine Zeile, ein deutscher Spieler bekommt deutschen Text. Rot, dann je Sprache eine Inventory in `NavigatorModule` und `SharedNavigator`.
- [x] 3.2 Bestehende Tests anpassen: Die exakten Lore-Assertions in `NavigatorPlayerCountTest` und `DestinationPlayerLoreTest` erwarten jetzt zusätzlich den Hinweis; die Spielerzahl-Zeile bleibt gleich. Grün.

## 4. Abnahme

- [x] 4.1 `./gradlew spotlessApply` und `./gradlew build` grün.
- [x] 4.2 `openspec validate navigator-click-hints --strict` ohne Fehler (nur die RFC-2119-Hinweise, die die bestehenden deutschen Specs auch haben). `docs/lobby-modules.md` beschreibt keine Navigator-Lore und bleibt unverändert.

## 5. Pull Request

- [ ] 5.1 Pull Request vom Zweig `feat/navigator-click-hints` auf `feat/navigator-server-picker-2` mit dem Titel `feat(navigator): explain click actions in the entry lore` öffnen (Titel und Beschreibung Englisch, Hinweis auf die Stapelung auf #424, Testplan). Nachweis: PR-URL, CI grün.
