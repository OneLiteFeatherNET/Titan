# Tasks

Jeder Schritt: Test zuerst (rot), dann Code (grün). Commits je Typ: `fix(navigator)` für die Implementierung, `docs(openspec)` für den Vorschlag.

## 1. Übersetzungen

- [ ] 1.1 Test zuerst (Unit, `NavigatorMessagesBundleTest`): `messages_en.properties` hat genau die Schlüssel aus `NavigatorMessages.KEYS`; `messages_de.properties` hat dieselben Schlüssel. Rot, dann Bundles und `NavigatorMessages`.
- [ ] 1.2 Test zuerst (Unit, `NavigatorMessagesTest`): Die Hinweise sind je Klick eine Zeile, Deutsch und Englisch nicht leer, Deutsch unterscheidet sich von Englisch. Rot, dann grün.

## 2. Lore-Zeilen

- [ ] 2.1 Test zuerst (Unit, Minestom-Env, `DestinationClickHintLoreTest`): Links- und Rechtsklick stehen auf je einer Zeile nach der Spielerzahl. Rot, dann `Destination.item(OptionalInt, List<Component>)`.

## 3. Navigator

- [ ] 3.1 Test zuerst (Integration, `NavigatorClickHintTest`, `Env`): Mit Portal zwei Zeilen bei Survival, ohne Portal eine, deutscher Spieler bekommt deutsche Zeilen. Rot, dann `NavigatorModule.itemOf`.
- [ ] 3.2 Bestehende exakte Lore-Assertions auf die Zeilenform bringen (`NavigatorClickHintTest`, `DestinationPlayerLoreTest`, `NavigatorDestinationTest`); `NavigatorPlayerCountTest` prüft nur Modi ohne Portal und bleibt. Grün.

## 4. Abnahme

- [ ] 4.1 `./gradlew spotlessApply` und `./gradlew build` grün.
- [ ] 4.2 `openspec validate navigator-click-hints-two-lines --strict` ohne Fehler.

## 5. Pull Request

- [ ] 5.1 Pull Request mit dem Titel `fix(navigator): show left and right click hints on separate lines` gegen `main` öffnen, englischer Text, CI abwarten.
