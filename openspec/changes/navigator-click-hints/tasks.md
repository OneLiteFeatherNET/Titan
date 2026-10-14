# Tasks

Gestapelt auf `feat/navigator-server-picker-2` (#424). Jeder Schritt: Test zuerst (rot), dann Code (grün).

## 1. Übersetzungen

- [ ] 1.1 Test zuerst (Unit, `NavigatorMessagesBundleTest`): `messages_en.properties` definiert alle Schlüssel aus `NavigatorMessages.KEYS`; `messages_de.properties` hat dieselben Schlüssel. Rot, dann Bundles und `NavigatorMessages`.
- [ ] 1.2 Test zuerst (Unit, `NavigatorMessagesTest`): Für jede der vier Kombinationen (Serverauswahl ja/nein × Portal ja/nein) ist der gerenderte Text in Deutsch und Englisch nicht leer, und Deutsch unterscheidet sich von Englisch. Rot, dann grün.

## 2. Lore-Zeile

- [ ] 2.1 Test zuerst (Unit, Minestom-Env, `DestinationClickHintLoreTest`): Ein Hinweis steht nach der Spielerzahl; ohne Spielerzahl steht nur der Hinweis; ohne Hinweis bleibt die Lore wie bisher. Rot, dann `Destination.item(OptionalInt, Component)`.

## 3. Navigator

- [ ] 3.1 Test zuerst (Integration, `NavigatorClickHintTest`, `Env`): Mit Portal zeigt Survival beide Angaben, ohne Portal nur den Linksklick, Spawn keine Zeile, ein deutscher Spieler bekommt deutschen Text. Rot, dann je Sprache eine Inventory in `NavigatorModule` und `SharedNavigator`.
- [ ] 3.2 Bestehende Tests anpassen: Die exakten Lore-Assertions in `NavigatorPlayerCountTest` und `DestinationPlayerLoreTest` erwarten jetzt zusätzlich den Hinweis; die Spielerzahl-Zeile bleibt gleich. Grün.

## 4. Abnahme

- [ ] 4.1 `./gradlew spotlessApply` und `./gradlew build` grün.
- [ ] 4.2 `openspec validate navigator-click-hints --strict` grün; `docs/lobby-modules.md` nennt nichts Geändertes (Prüfung).

## 5. Pull Request

- [ ] 5.1 Pull Request vom Zweig `feat/navigator-click-hints` auf `feat/navigator-server-picker-2` mit dem Titel `feat(navigator): explain click actions in the entry lore` öffnen (Titel und Beschreibung Englisch, Hinweis auf die Stapelung auf #424, Testplan). Nachweis: PR-URL, CI grün.
