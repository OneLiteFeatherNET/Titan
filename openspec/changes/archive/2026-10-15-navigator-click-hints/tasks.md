# Tasks

Gestapelt auf `feat/navigator-server-picker-2` (#424), inzwischen in `main` (v3.2.0). Jeder Schritt: Test zuerst (rot), dann Code (grün).

## 1. Übersetzungen

- [x] 1.1 Test zuerst (Unit, `NavigatorMessagesBundleTest`): `messages_en.properties` definiert alle Schlüssel aus `NavigatorMessages.KEYS`; `messages_de.properties` hat dieselben Schlüssel. Rot, dann Bundles und `NavigatorMessages`.
- [x] 1.2 Test zuerst (Unit, `NavigatorMessagesTest`): Für jede der vier Kombinationen (Serverauswahl ja/nein × Portal ja/nein) ist der gerenderte Text in Deutsch und Englisch nicht leer, und Deutsch unterscheidet sich von Englisch. Rot, dann grün.

## 2. Lore-Zeile

- [x] 2.1 Test zuerst (Unit, Minestom-Env, `DestinationClickHintLoreTest`): Ein Hinweis steht nach der Spielerzahl; ohne Spielerzahl steht nur der Hinweis. Rot, dann `Destination.item(OptionalInt, Component)`.

## 3. Navigator

- [x] 3.1 Test zuerst (Integration, `NavigatorClickHintTest`, `Env`): Mit Portal zeigt Survival beide Angaben, ohne Portal nur den Linksklick, Spawn keine Zeile, ein deutscher Spieler bekommt deutschen Text. Rot, dann je Sprache eine Inventory in `NavigatorModule` und `SharedNavigator`.
- [x] 3.2 Bestehende Tests anpassen: Die exakten Lore-Assertions in `NavigatorPlayerCountTest` erwarten jetzt zusätzlich den Hinweis; die Spielerzahl-Zeile bleibt gleich. `DestinationPlayerLoreTest` übergibt den Hinweis als Argument und prüft die Spielerzahl-Zeile unverändert. Grün.

## 4. Abnahme

- [x] 4.1 `./gradlew spotlessApply` und `./gradlew build` grün.
- [x] 4.2 `openspec validate navigator-click-hints --strict` ohne Fehler (nur die RFC-2119-Hinweise, die die bestehenden deutschen Specs auch haben). `docs/lobby-modules.md` beschreibt keine Navigator-Lore und bleibt unverändert.

## 5. Pull Request

- [x] 5.1 Pull Request #433 mit dem Titel `feat(navigator): explain click actions in the entry lore` ist umgesetzt und steht als 996d70f auf `main`.

## 6. Nachtrag: Klickhinweis trifft den tatsächlichen Klick

Der Hinweis „Server wählen“ galt für alle Ziele aus `ServerPicker.DESTINATIONS`, die Auswahl öffnet sich aber nur bei mehr als einem laufenden Dienst. Außerdem wurde der Portal-Hinweis nur beim Aktualisierungszyklus berechnet, nicht beim Öffnen.

- [x] 6.1 Test zuerst (Integration, `NavigatorClickHintTest`): ElytraRace mit mehreren Diensten zeigt „Server wählen“; mit einem oder keinem Dienst „verbinden“. Rot bei einem und keinem Dienst, dann Code.
- [x] 6.2 Test zuerst (Integration, `NavigatorClickHintTest`): Wird das Menü direkt nach einer Portal-Änderung geöffnet, zeigt es den Rechtsklick-Hinweis schon. Rot, dann Code: das Menü wird beim Öffnen neu befüllt.
- [x] 6.3 Die Spielerzahl wird aus derselben Dienste-Lesung summiert wie der Hinweis, so liest jeder Zyklus die Dienste einmal je Ziel. `Destination.item` verlangt den Hinweis; die Überladungen ohne ihn sind entfernt.
- [x] 6.4 Tests für deutsche Menüs (Spielerzahl-Zyklus, Team mit Deutsch) und Listener-Lebenszyklus je Sprache (Leak-Test).
- [x] 6.5 Pull Request #438 mit dem Titel `fix(navigator): keep click hints in line with the actual click` nach `main` geöffnet, CI grün, squash-gemergt als 972c932.
