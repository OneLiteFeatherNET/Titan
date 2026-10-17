# Tasks

Zweig: `feat/navigator-slender-permission` von `origin/main`. Vor dem Abhaken einer Aufgabe läuft `./gradlew build`.

## 1. Tests zuerst (rot)

- [x] 1.1 `NavigatorPermissionTest`: Recht erteilt zeigt Slender und der Klick verbindet mit „Slender“; `DENIED` und `NOT_SET` blenden Slender aus und weisen den Klick ab; Recht zwischen zwei Öffnungen; Menü je Rechte-Kombination geteilt. Nachweis: sieben Tests schlagen auf `main` fehl, die Tests mit erteilten Rechten und die Klicks.
- [x] 1.2 `NavigatorDestinationTest`: Slender trägt `titan.navigator.slender` und hat die Aufgabe `Slender`; nur Slender und Build sind an ein Recht gebunden. Nachweis: die Bindung schlägt auf `main` fehl.

## 2. Umsetzung

- [x] 2.1 `Destination`: `SLENDER` an `titan.navigator.slender`; `SLENDER_FLAG` und das Feld `feature` entfallen; `visible(Set<Destination>)` und `permissioned()`.
- [x] 2.2 `NavigatorModule` und `SharedNavigator`: je Teilmenge der gebundenen Ziele und Sprache ein geteiltes Inventar (acht), Layout einmal beim Start; `FeatureFlags` aus Konstruktor, `SharedNavigator` und `package-info` entfernt.
- [x] 2.3 `features.yaml` (runtime und Testkopie) ohne `NAVIGATOR_SLENDER`.
- [x] 2.4 Konfigurationstests (`runtime`, `apps/cloudnet`, `buildSrc`) verwenden `LOBBYSWITCHER` statt `NAVIGATOR_SLENDER` für die Flag-Mechanik.

## 3. Doku

- [x] 3.1 `README.md` und `docs/lobby-modules.md`: Slender und Build über Rechte, Betreiber-Schritte, lokale Variante.

## 4. Verifikation

- [x] 4.1 `./gradlew spotlessApply` und `./gradlew build` sind grün (CI auf PR #425 grün, gemergt).

## 5. Pull Request

- [x] 5.1 Pull Request #425 auf `main` mit dem Titel `feat(navigator)!: gate slender behind a permission` (Titel und Beschreibung Englisch). Die Beschreibung nennt den BREAKING-Hinweis und die Betreiber-Schritte, und die CI läuft grün.
