# Tasks

Jeder Schritt: Test zuerst (rot), dann Code (grün). Commits je Typ: `fix(navigator)` für die Implementierung, `docs(openspec)` für den Vorschlag.

## 1. Übersetzungen

- [x] 1.1 Test zuerst (Unit, `NavigatorMessagesBundleTest`): `messages_en.properties` hat genau die Schlüssel, die `NavigatorMessages.keysFor` über alle Kombinationen liefert; `messages_de.properties` hat dieselben Schlüssel. Rot, dann Bundles und `NavigatorMessages`.
- [x] 1.2 Test zuerst (Unit, `NavigatorMessagesTest`): Die Hinweise sind je Klick eine Zeile, Deutsch und Englisch nicht leer, Deutsch unterscheidet sich von Englisch. Rot, dann grün.

## 2. Lore-Zeilen

- [x] 2.1 Test zuerst (Unit, Minestom-Env, `DestinationClickHintLoreTest`): Links- und Rechtsklick stehen auf je einer Zeile nach der Spielerzahl. Rot, dann `Destination.item(OptionalInt, List<Component>)`.

## 3. Navigator

- [x] 3.1 Test zuerst (Integration, `NavigatorClickHintTest`, `Env`): Mit Portal zwei Zeilen bei Survival, ohne Portal eine, deutscher Spieler bekommt deutsche Zeilen. Rot, dann `NavigatorModule.itemOf`.
- [x] 3.2 Bestehende exakte Lore-Assertions auf die Zeilenform bringen (`NavigatorClickHintTest`, `DestinationPlayerLoreTest`, `NavigatorDestinationTest`); `NavigatorPlayerCountTest` prüft nur Modi ohne Portal und bleibt. Grün.

## 4. Abnahme

- [x] 4.1 `./gradlew spotlessApply` und `./gradlew build` grün.
- [x] 4.2 `openspec validate navigator-click-hints-two-lines` ohne Fehler. Nicht `--strict`: die bestehenden deutschen Specs melden dieselben RFC-2119-Warnungen (SHOULD/MUST-Wortwahl), die dieser Change nicht verändert.

## 5. Pull Request

- [x] 5.1 Pull Request mit dem Titel `fix(navigator): show left and right click hints on separate lines` gegen `main` geöffnet (#444), englischer Text, CI grün, squash-gemergt.
