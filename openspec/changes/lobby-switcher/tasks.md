# Tasks

## Execution Plan

Basis: `origin/main`. Implementierung, Tests und Commits in einer Arbeitskopie, Abschnitt für Abschnitt. Jeder Abschnitt endet mit grünem Build der betroffenen Module.

| Abschnitt | Schreibt | Darf nicht anfassen |
| --- | --- | --- |
| 1 (Spike) | nur Notizen in `design.md` (Q2, Q7); Spike-Code wird verworfen | alles übrige |
| 3, 4 | `core/**`, `common/**`, `bridge/**` | `features/**` |
| 5 | `features/lobbyswitcher/**`, `runtime/**` (`features.yaml`), `features/navigator/**` (nur Abschnitt `features` entfernen) | `core/**` außer Ergänzungen |
| 6 bis 9 | `features/lobbyswitcher/**` | `core/**`, `common/**` |
| 10 | Tests in `apps/**` | Produktionscode |
| 11 | `README.md`, `docs/lobby-modules.md` | Code |
| 12 | Nachweise, Smoke-Test, Archiv, PR | Code |

Regeln:

- **Built-in first:** Minestom `Command`/`Scheduler`/`EventDispatcher`, Aves-Builder und -Hooks, Adventure `MiniMessageTranslationStore`/`GlobalTranslator`, Avaje `@Profile`/`@Factory`.
- **Java 25 ohne Preview:** Records, sealed, `switch`-Muster.
- **F.I.R.S.T.:** frische `Env` je Test, `env.tick()` statt Warten; keine `Thread.sleep`, keine Systemzeit (`Clock` injizieren); keine globale `Config`-Mutation; Assertions mit Meldung; Logs nur über einen Captured Appender prüfen; Telemetrie über `TestTelemetry`.
- **Test zuerst:** rot, dann grün, dann aufräumen. Ein Fehler wird zuerst als fehlschlagender Test reproduziert.
- **Kommentare:** nur das Warum, 1 bis 3 Zeilen.
- **Commits:** Conventional Commits, je Typ ein Commit. Feature-Commit `feat(lobbyswitcher): …`; das Verschieben des Abschnitts `features` (Task 5.2) ist ein eigener `refactor(runtime): …`-Commit; Doku und OpenSpec-Änderungen haben ihre eigenen Commits (`docs(…)`). Jeder Commit endet mit `Claude-Session: https://claude.ai/code/session_01GXose9rakuRNZoM3TP3xj1`.

## 1. Spike: offene Fragen klären (vor Abschnitt 3)

- [x] 1.1 Spike, Ergebnis in `design.md` unter „Spike-Ergebnis“ festhalten (Q2, Q7), Spike-Code verwerfen (erledigt: Identität über `InjectionLayer.ext()` erreichbar, Quellenlage, Smoke-Test bestätigt; Aves übersetzt je Viewer):
  - Im Bridge-Extension-Kontext (`InjectionLayer.ext()`) prüfen, ob die eigene Dienst-Info erreichbar ist (`WrapperConfiguration.serviceInfoSnapshot()` bzw. `ServiceInfoHolder.serviceInfo()` aus `wrapper-jvm-api` 4.0.0-RC16). Liefert `serviceId().taskName()` den Task und `name()` den Dienstnamen? Falls nicht: Konfigurationsrückfall aus D3 festlegen.
  - In einer Aves-Testumgebung prüfen, ob `GlobalTranslatedInventoryBuilder.getInventory(Locale)` übersetzte Itemnamen je Locale rendert, und ob `setDataLayoutFunction` mit `invalidateDataLayout()` den Inhalt eines geöffneten Inventars aktualisiert. Falls nicht: D4 anpassen.

## 2. Entfällt

Kein Refactor von `LabelRefresh` (Q3, design.md D5b). Der Switcher bekommt eine eigene kleine Aktualisierung im Modul (Abschnitt 7.5).

## 3. Schnittstellen in `core` und `common`

- [x] 3.1 Test zuerst (Unit, `core`): `PlayerCounts.running(TASK, name)` hat eine leere Default-Implementierung; `ServiceCount` ist ein Record mit `name`, `online`, `max`. Rot. Dann Default-Methode und Record in `net.onelitefeather.titan.core.portal`. Grün.
- [x] 3.2 Test zuerst (Unit, `core`): `LobbyIdentities.self()` liefert `Optional.empty()` ohne Bridge; `LobbyIdentity` lehnt leeren Task oder Dienstnamen ab. Rot. Dann `LobbyIdentity` und `LobbyIdentities` in `net.onelitefeather.titan.core.lobby`. Grün.
- [x] 3.3 Test zuerst (Unit, `common`, `HolderPlayerCountsTest` erweitern): `HolderPlayerCounts.running` reicht den Lookup durch und liefert nur laufende Dienste; ohne Lookup eine leere Liste. Rot. Dann `TitanPlayerCountLookup` und `PlayerCountLookup` um die Auflistung erweitern (Default-Methode). Grün.
- [x] 3.4 Test zuerst (Unit, `common`): `TitanLobbyIdentity`-Holder, statisch, volatile, wie `TitanPlayerCountLookup`; ohne Setzen `Optional.empty()`. Rot, dann grün.
- [x] 3.5 Nachweis: `./gradlew :core:build :common:build` grün.

## 4. Bridge (CloudNet-Seite)

- [x] 4.1 Test zuerst (Unit, `bridge`, ohne CloudNet-Typen): Die Zuordnung `ServiceReading -> ServiceCount` nimmt nur laufende Dienste und behält Dienstname, `online` und `max`. Rot. Dann Hilfsklasse neben `ServiceTotals` (`ServiceListing`). Grün.
- [x] 4.2 Implementierung in `TitanBridgePermissionExtension`: Auflistung über dieselben `ServiceReadings`-Quellen wie `lookup`; Identität über den Pfad aus 1.1 setzen. CloudNet-Teil ist nicht unit-testbar; er wird über den Smoke-Test in 12.1 abgenommen.
- [x] 4.3 Nachweis: `./gradlew :bridge:build` grün; der Startlog-Eintrag „Player count lookup installed“ bleibt unverändert.

## 5. Feature-Column: Einstellungen, Flag, Skelett

- [x] 5.1 Test zuerst (Unit, `LobbySwitcherSettingsTest`): `lobbyswitcher.refreshSeconds` akzeptiert 1 und 3600, lehnt 0, 3601 und `fünf` ab; die Meldung nennt den Schlüssel. Rot. Dann `LobbySwitcherSettings` nach Vorbild `PortalSettings`. Grün.
- [x] 5.2 Test zuerst (Unit, `runtime`): `ConfigFeatureFlags` kennt `LOBBYSWITCHER` mit Standard `true`, `NAVIGATOR_*` bleibt `false`. Rot. Dann Abschnitt `features` aus `features/navigator/src/main/resources/titan/defaults/navigator.yaml` nach `runtime/src/main/resources/titan/defaults/features.yaml` verschieben und `LOBBYSWITCHER: true` ergänzen. `DefaultsMerger`-Test grün (kein doppelter Top-Level-Abschnitt). Grün. Eigener Commit `refactor(runtime): move feature flags into their own defaults file`, vor dem Feature-Commit.
- [x] 5.3 Defaults der Column: `features/lobbyswitcher/src/main/resources/titan/defaults/lobbyswitcher.yaml` mit `lobbyswitcher.refreshSeconds: 5`, kommentiert. Test: der zusammengeführte Standardwert liest sich als 5.
- [x] 5.4 Skelett: `features/lobbyswitcher/build.gradle.kts` mit `titan.column`, Paket `net.onelitefeather.titan.feature.lobbyswitcher`, `package-info.java`. Kein Verhalten. Nachweis: `./gradlew :features:lobbyswitcher:build` grün.

## 6. Zustände, Layout, Aktualisierung, Fehler (reine Logik)

- [x] 6.1 Test zuerst (Unit, `SwitcherEntryTest`, jede Zeile der Tabelle D6): eigene Lobby → `CURRENT` (auch wenn voll); `max > 0` und `online >= max` → `FULL`; `max == 0` → `NOT_READY`; sonst `JOINABLE`. Sortierung nach Dienstname aufsteigend. Rot. Dann reine Funktion ohne Minestom. Grün.
- [x] 6.2 Test zuerst (Unit, `SwitcherLayoutTest`): Zeilenzahl je Anzahl Einträge (1 bis 9 eine Zeile, bis höchstens sechs Zeilen); mehr Einträge werden abgeschnitten und mit einer Logzeile gemeldet (Captured Appender). Rot. Dann Layoutberechnung. Grün.
- [x] 6.3 Test zuerst (Unit, `ViewerCounterTest`, Fake-Scheduler): erster Viewer startet die Periode mit `refreshSeconds * ServerFlag.SERVER_TICKS_PER_SECOND` Ticks; ein zweiter startet keine zweite; der letzte stoppt sie; danach keine Lesung. Rot. Dann Viewer-Zähler und Steuerung der Periode. Grün.
- [x] 6.4 Test zuerst (Unit, `SwitcherReadingTest`, Fake-`PlayerCounts`): Provider wirft → letzter guter Stand bleibt, Warnung genau einmal (Captured Appender), jeder weitere Fehler nur `DEBUG`; ohne Stand die Meldung `unavailable`. Rot. Dann Fehlerbehandlung D8. Grün.
- [x] 6.5 Test zuerst (Unit, `SwitcherClickDecisionTest`): Die Klick-Entscheidung aus frischem Stand (D6): `JOINABLE` → `SEND`; `FULL` → `full`; `NOT_READY` → `not_ready`; nicht mehr gelistet → `gone`; eigene Lobby → `current`; Prüfung geworfen → `error`. Rot. Dann reine Funktion. Grün.

## 7. Inventar, Hotbar-Item, Klick, Aktualisierung

- [x] 7.1 Test zuerst (Integration, Cyano-`Env`, `env.tick()`, Fake-`PlayerCounts` mit drei Diensten): Mit Flag und CloudNet-Profil erhält ein Spieler beim Beitritt das Item (`CLOCK`) auf Slot 8; ohne Flag nicht; ohne Identität nicht. Rot. Dann `LobbySwitcherItems` (`@Factory`, `@Profile(CLOUDNET)`) und Modul (`@Singleton`, `@Profile(CLOUDNET)`, `FeatureNode.attach` mit gewählter Priorität). Grün.
- [x] 7.2 Test zuerst (Integration): Klick auf das Item öffnet das Inventar mit den drei Diensten in Namensreihenfolge, die eigene Lobby ist markiert, die Zahlen stimmen mit dem Fake überein. Rot. Dann `SwitcherInventory` (`GlobalTranslatedInventoryBuilder`, `register()` in `@PostConstruct`, `unregister()` in `@PreDestroy`, Layout nach D4). Grün.
- [x] 7.3 Test zuerst (Integration): Klick auf eine beitretbare Lobby ruft den Fake-`Deliver` genau einmal mit dem Dienstnamen (`serverBuilder`) auf und schließt das Inventar. Klick auf `CURRENT`, `FULL` oder `NOT_READY` ruft ihn nicht auf und sendet die passende Meldung. Rot. Dann Klick-Behandlung (D6). Grün.
- [x] 7.4 Test zuerst (Integration): Lobby wird zwischen Anzeige und Klick voll: der Fake liefert bei der Klick-Prüfung `online == max`; `Deliver` wird nicht aufgerufen, die Meldung `full` erscheint, das Inventar zeigt den neuen Stand. Dienst beim Klick nicht mehr gelistet → `gone`, ebenso ohne Aufruf. Prüfung wirft → `error`, ohne Aufruf. Rot, dann grün.
- [x] 7.5 Test zuerst (Integration, `env.tick()`): Ein geöffnetes Inventar wird nach `refreshSeconds * 20` Ticks mit neuen Zahlen aktualisiert; ohne Viewer findet nach dem Schließen keine Abfrage mehr statt (Zähler im Fake). Rot. Dann Aktualisierung im Modul (eigener Refresh nach Muster `LabelRefresh`, Q3). Grün.
- [x] 7.6 Test zuerst (Integration): Nach `@PreDestroy` ist das Inventar nicht mehr registriert, das Item ist nicht mehr auf der Hotbar, keine Periode läuft. Rot. Dann Aufräumen. Grün.

## 8. Telemetrie

- [x] 8.1 Test zuerst (Integration, `TestTelemetry`): Öffnen erzeugt den Span `lobbyswitcher.open` mit `user.id` und `lobbyswitcher.entries`; die Aktualisierung erzeugt keinen Span. Rot. Dann `LobbySwitcherTelemetry`. Grün.
- [x] 8.2 Test zuerst (Integration, `TestTelemetry`): Klick erzeugt den Span `lobbyswitcher.select` mit `user.id`, `lobbyswitcher.target` und `lobbyswitcher.result` (`sent`, `current`, `full`, `not_ready`, `gone`, `error`); bei `error` Span-Status `ERROR`. Der Zähler `titan.lobbyswitcher.selections` trägt nur `result`, keine Dienstnamen und keine UUID. Rot. Dann Zählerfunktion. Grün.

## 9. Texte

- [x] 9.1 Test zuerst (Unit, `LobbySwitcherMessagesBundleTest`): `messages_en.properties` und `messages_de.properties` haben dieselben Schlüssel; jeder Schlüssel in `KEYS` steht im englischen Bundle. Rot. Dann `titan/lobbyswitcher/messages_{en,de}.properties` und `LobbySwitcherMessages` (Store registrieren/entfernen, explizit rendern). Grün.
- [x] 9.2 Test zuerst (Integration, Cyano-`Env`): `de_DE` sieht die Meldung „volle Lobby“ auf Deutsch, `ja_JP` auf Englisch. Rot, dann grün (Ergebnis aus 1.1 beachten).

## 10. Verdrahtung in den Varianten

- [x] 10.1 Test zuerst (Wiring, `apps/cloudnet`): Die Column ist im Container; Item und Inventar sind mit Flag vorhanden; `WiringTest` kennt das Modul. Rot, dann grün.
- [x] 10.2 Test zuerst (Wiring, `apps/local`, `VariantStartTest`): Ohne CloudNet startet die lokale Variante; `lobbyswitcher`-Beans fehlen; `expectedModules` schlägt nicht fehl. Rot. Dann Anpassung der Modulliste, falls nötig, sonst Begründung im Test. Grün.
- [x] 10.3 Nachweis: `./gradlew build` grün; `ColumnArchitectureRules` grün (die Column hängt nur an `core`).

## 11. Dokumentation

- [x] 11.1 `README.md`: Abschnitt „Feature flags“ um `LOBBYSWITCHER` (Standard `true`, nur CloudNet-Variante wirksam) ergänzen; den Absatz „kein Neustart“ um die Ausnahme ergänzen (Item-Präsenz wird beim Start festgelegt, Q1); Tabelle der Umgebungsvariablen um `lobbyswitcher.refreshSeconds`; kurzer Eintrag zu Column und Hotbar-Slot 8.
- [x] 11.2 `docs/lobby-modules.md`: Column-Liste im Kapitel „Module“ um `lobbyswitcher`; Abschnitt „Traces und Metriken“ um Spans und Zähler; Tabelle „Was Admin, Hotbar und Rechteprüfung liefern“ um `lobbyswitcher` ergänzen.
- [x] 11.3 Nachweis: Dokumentation und Code stimmen überein (Flagname, Schlüssel, Span- und Zählernamen, Slot 8). Mechanisches Review durch einen Haiku-Agenten.

## 12. Smoke-Test und Abschluss

- [ ] 12.1 Smoke-Test auf CloudNet mit zwei Lobbys desselben Tasks (manuell): Standard (Flag an) prüfen; beide Lobbys starten; Item (Uhr) auf Slot 8; die Liste zeigt beide mit Zahlen; Zahlen ändern sich, während das Inventar offen ist; Schließen stoppt die Aktualisierung (Zähler oder DEBUG-Log); Klick auf die andere Lobby wechselt; die eigene Lobby ist nicht klickbar; eine volle Lobby (Max-Wert der Lobby begrenzen) ist nicht beitretbar; Flag aus und Neustart entfernt das Item; Loki zeigt `lobbyswitcher.select` mit `result=sent`.
- [x] 12.2 Offene Fragen Q1 bis Q10 in `design.md` festhalten (Abschnitt „Entscheidungen“): Q1, Q3 bis Q6, Q8 bis Q10 sind entschieden; Q2 und Q7 schließt der Spike (Task 1.1).
- [ ] 12.3 Archiv nach Umsetzung: `openspec/changes/lobby-switcher` nach `openspec/changes/archive/2026-10-10-lobby-switcher` verschieben, Delta nach `openspec/specs/lobby-switcher/spec.md` übernehmen. Commit `docs(openspec): archive lobby-switcher`. Durch den Nutzer ausstehend.
- [ ] 12.4 Nachweis: `openspec validate lobby-switcher --strict` grün (vor dem Archiv); `./gradlew build` grün; das Startlog der cloudnet- und der local-Variante hat keine neuen Warnungen.
- [ ] 12.5 PR öffnen: Titel `feat(lobbyswitcher): show other lobbies with player counts and switch between them`; Beschreibung auf Englisch mit Zusammenfassung, Smoke-Test-Ergebnis und Verweis auf `openspec/changes/lobby-switcher`; abschließend `https://claude.ai/code/session_01GXose9rakuRNZoM3TP3xj1`. Danach nur auf grüne CI warten; Merge und Release macht der Nutzer.
