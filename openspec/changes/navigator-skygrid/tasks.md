# Tasks

## 1. Eintrag und Texte

- [ ] 1.1 Test zuerst (Unit, `NavigatorDestinationTest`): `SKYGRID` gehört zur Gruppe `SERVER`, Ziel `SkyGrid`, Recht `titan.navigator.skygrid`, Platz nach `BUILD` in der Aufzählung. Nachweis: Test rot.
- [ ] 1.2 `Destination.SKYGRID`, `NavigatorMessages.DESCRIPTION_SKYGRID` (in `DESCRIPTIONS` und `KEYS`), Schlüssel in `messages_de.properties` und `messages_en.properties`. Nachweis: 1.1 grün, `NavigatorMessagesBundleTest` grün.

## 2. Sichtbarkeit, Platz und Klick

- [ ] 2.1 Test zuerst (Unit, `NavigatorDestinationTest`): `visible()` ohne Recht enthält SkyGrid nicht, mit Recht enthält es SkyGrid. Nachweis: Test rot, dann grün nach 1.2.
- [ ] 2.2 Test zuerst (Unit, `NavigatorLayoutTest`): mit vier Servern liegen die Plätze 20 bis 23 (zentriert), SkyGrid auf 23. Nachweis: Test rot, dann grün.
- [ ] 2.3 Test zuerst (Muster `NavigatorBuildDestinationTest`): Klick mit Recht leitet an `SkyGrid` weiter und schließt; Klick nach Entzug leitet nichts weiter. Nachweis: Test rot, dann grün.
- [ ] 2.4 Zählungen anpassen: `NavigatorPermissionTest` erwartet 160 Inventare (128 Hauptmenüs, 32 Untermenüs) statt 96. Nachweis: Test grün.

## 3. Verifikation und Abnahme

- [ ] 3.1 `./gradlew :features:navigator:test` grün; F.I.R.S.T.-Check der neuen Tests (kein Sleep, keine Systemzeit, keine geteilten statischen Zustände).
- [ ] 3.2 Manuelle Abnahme (Lobby mit CloudNet): Spieler mit Recht sieht SkyGrid in der Serverreihe, ein Klick verbindet mit dem Task `SkyGrid`; ohne Recht fehlt der Eintrag ohne Lücke. Nachweis: Hinweis im PR.

## 4. Pull Request

- [ ] 4.1 Code-Pull-Request von `feat/navigator-skygrid` nach `main` unter dem Titel `feat(navigator): add a permission-gated skygrid entry` öffnen; englische Beschreibung aus Sicht der Spieler (wer SkyGrid sieht und was ein Klick bewirkt). Nachweis: PR-URL, CI grün.
