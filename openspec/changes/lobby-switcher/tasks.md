# Tasks

## Execution Plan

Basis: `origin/main`. Jede Welle startet in einem eigenen Worktree vom aktuellen Stand des Integrationszweigs (`git reset --hard <Commit-ID>`, nie `HEAD` eines anderen Checkouts). Implementierung, Tests und Commits gehen an Sonnet-Agents, Lesen und Prüfen an Haiku-Agents. Jede Welle endet mit grünem `./gradlew build` und einem Review des Diffs durch die Hauptsitzung.

| Welle | Abschnitte | Schreibt | Darf nicht anfassen |
| --- | --- | --- | --- |
| 0 | 1 (Spike) | nur Notizen in `design.md` (Q2, Q7); Spike-Code wird verworfen | alles übrige |
| 1 | 2 (optionaler Refactor-PR) | `common/**` (`OffTickRefresh`), `features/portal/**` | `features/lobbyswitcher/**` |
| 2 | 3, 4 | `core/**`, `common/**`, `bridge/**` | `features/**` |
| 3 | 5 bis 9 | `features/lobbyswitcher/**`, `runtime/**` (`features.yaml`), `features/navigator/**` (nur Abschnitt `features` entfernen) | `core/**` außer Ergänzungen |
| 4 | 10 | Tests in `apps/**` | Produktionscode |
| 5 | 11 | `README.md`, `docs/lobby-modules.md` | Code |
| 6 | 12 | Nachweise, Smoke-Test, Archiv, PR | Code |

Regeln für alle Agenten:

- **Built-in first:** Minestom `Command`/`Scheduler`/`EventDispatcher`, Aves-Builder und -Hooks, Adventure `MiniMessageTranslationStore`/`GlobalTranslator`, Avaje `@Profile`/`@Factory`.
- **Java 25 ohne Preview:** Records, sealed, `switch`-Muster.
- **F.I.R.S.T.:** frische `Env` je Test, `env.tick()` statt Warten; keine `Thread.sleep`, keine Systemzeit (`Clock` injizieren); keine globale `Config`-Mutation; Assertions mit Meldung; Logs nur über einen Captured Appender prüfen; Telemetrie über `TestTelemetry`.
- **Test zuerst:** rot, dann grün, dann aufräumen. Ein Fehler wird zuerst als fehlschlagender Test reproduziert.
- **Kommentare:** nur das Warum, 1 bis 3 Zeilen.
- **Commits:** Conventional Commits, je Typ ein Commit. Der Feature-Commit ist `feat(lobbyswitcher): …`. Jeder Commit endet mit `Claude-Session: https://claude.ai/code/session_01GXose9rakuRNZoM3TP3xj1`.

## 1. Spike: offene Fragen klären (vor Welle 1)

- [ ] 1.1 Spike, Ergebnis in `design.md` festhalten (Q2, Q7), Spike-Code verwerfen:
  - Im Bridge-Extension-Kontext (`InjectionLayer.ext()`) prüfen, ob die eigene Dienst-Info erreichbar ist (`WrapperConfiguration.serviceInfoSnapshot()` bzw. `ServiceInfoHolder.serviceInfo()` aus `wrapper-jvm-api` 4.0.0-RC16). Liefert `serviceId().taskName()` den Task und `name()` den Dienstnamen? Falls nicht: Konfigurationsrückfall aus D3 festlegen.
  - In einer Aves-Testumgebung prüfen, ob `GlobalTranslatedInventoryBuilder.getInventory(Locale)` übersetzte Itemnamen je Locale rendert, und ob `setDataLayoutFunction` mit `invalidateDataLayout()` den Inhalt eines geöffneten Inventars aktualisiert. Falls nicht: D4 anpassen.

## 2. Gemeinsamer Refresh-Baustein (optional, eigener PR vorab)

- [ ] 2.1 Test zuerst (Unit, `common`): `OffTickRefresh` überspringt eine Periode, solange die vorige Lesung läuft; die Anwendung läuft auf dem nächsten Tick; nach `stop()` wird nichts mehr angewendet. Rot. Dann `OffTickRefresh` aus `LabelRefresh` herausziehen; `LabelRefresh` nutzt es. Grün, `LabelRefreshTest` unverändert grün.
- [ ] 2.2 Nachweis: `./gradlew build` grün; PR `refactor(common): share the off-tick refresh of portal labels` vor dem Feature-PR mergen. Entfällt, wenn Q3 mit „Kopie“ entschieden wird.

## 3. Schnittstellen in `core` und `common`

- [ ] 3.1 Test zuerst (Unit, `core`): `PlayerCounts.running(TASK, name)` hat eine leere Default-Implementierung; `ServiceCount` ist ein Record mit `name`, `online`, `max`. Rot. Dann Default-Methode und Record in `net.onelitefeather.titan.core.portal`. Grün.
- [ ] 3.2 Test zuerst (Unit, `core`): `LobbyIdentities.self()` liefert `Optional.empty()` ohne Bridge; `LobbyIdentity` lehnt leeren Task oder Dienstnamen ab. Rot. Dann `LobbyIdentity` und `LobbyIdentities` in `net.onelitefeather.titan.core.lobby`. Grün.
- [ ] 3.3 Test zuerst (Unit, `common`, `HolderPlayerCountsTest` erweitern): `HolderPlayerCounts.running` reicht den Lookup durch und liefert nur laufende Dienste; ohne Lookup eine leere Liste. Rot. Dann `TitanPlayerCountLookup` und `PlayerCountLookup` um die Auflistung erweitern (Default-Methode). Grün.
- [ ] 3.4 Test zuerst (Unit, `common`): `TitanLobbyIdentity`-Holder, statisch, volatile, wie `TitanPlayerCountLookup`; ohne Setzen `Optional.empty()`. Rot, dann grün.
- [ ] 3.5 Nachweis: `./gradlew :core:build :common:build` grün.

## 4. Bridge (CloudNet-Seite)

- [ ] 4.1 Test zuerst (Unit, `bridge`, ohne CloudNet-Typen): Die Zuordnung `ServiceReading -> ServiceCount` nimmt nur laufende Dienste und behält Dienstname, `online` und `max`. Rot. Dann Hilfsklasse neben `ServiceTotals` (`ServiceListing`). Grün.
- [ ] 4.2 Implementierung in `TitanBridgePermissionExtension`: Auflistung über dieselben `ServiceReadings`-Quellen wie `lookup`; Identität über den Pfad aus 1.1 setzen. CloudNet-Teil ist nicht unit-testbar; er wird über den Smoke-Test in 12.1 abgenommen.
- [ ] 4.3 Nachweis: `./gradlew :bridge:build` grün; der Startlog-Eintrag „Player count lookup installed“ bleibt unverändert.

## 5. Feature-Column: Einstellungen, Flag, Skelett

- [ ] 5.1 Test zuerst (Unit, `LobbySwitcherSettingsTest`): `lobbyswitcher.refreshSeconds` akzeptiert 1 und 3600, lehnt 0, 3601 und `fünf` ab; die Meldung nennt den Schlüssel. Rot. Dann `LobbySwitcherSettings` nach Vorbild `PortalSettings`. Grün.
- [ ] 5.2 Test zuerst (Unit, `runtime`): `ConfigFeatureFlags` kennt `LOBBYSWITCHER` mit Standard `false`, `NAVIGATOR_*` bleibt unverändert. Rot. Dann Abschnitt `features` aus `features/navigator/src/main/resources/titan/defaults/navigator.yaml` nach `runtime/src/main/resources/titan/defaults/features.yaml` verschieben und `LOBBYSWITCHER: false` ergänzen. `DefaultsMerger`-Test grün (kein doppelter Top-Level-Abschnitt). Grün.
- [ ] 5.3 Defaults der Column: `features/lobbyswitcher/src/main/resources/titan/defaults/lobbyswitcher.yaml` mit `lobbyswitcher.refreshSeconds: 5`, kommentiert. Test: der zusammengeführte Standardwert liest sich als 5.
- [ ] 5.4 Skelett: `features/lobbyswitcher/build.gradle.kts` mit `titan.column`, Paket `net.onelitefeather.titan.feature.lobbyswitcher`, `package-info.java`. Kein Verhalten. Nachweis: `./gradlew :features:lobbyswitcher:build` grün.

## 6. Zustände, Layout, Aktualisierung, Fehler (reine Logik)

- [ ] 6.1 Test zuerst (Unit, `SwitcherEntryTest`, jede Zeile der Tabelle D6): eigene Lobby → `CURRENT` (auch wenn voll); `max > 0` und `online >= max` → `FULL`; `max == 0` → `NOT_READY`; sonst `JOINABLE`. Sortierung nach Dienstname aufsteigend. Rot. Dann reine Funktion ohne Minestom. Grün.
- [ ] 6.2 Test zuerst (Unit, `SwitcherLayoutTest`): Zeilenzahl je Anzahl Einträge (1 bis 9 eine Zeile, bis höchstens sechs Zeilen); mehr Einträge werden abgeschnitten und mit einer Logzeile gemeldet (Captured Appender). Rot. Dann Layoutberechnung. Grün.
- [ ] 6.3 Test zuerst (Unit, `ViewerCounterTest`, Fake-Scheduler): erster Viewer startet die Periode mit `refreshSeconds * ServerFlag.SERVER_TICKS_PER_SECOND` Ticks; ein zweiter startet keine zweite; der letzte stoppt sie; danach keine Lesung. Rot. Dann Viewer-Zähler und Steuerung der Periode. Grün.
- [ ] 6.4 Test zuerst (Unit, `SwitcherReadingTest`, Fake-`PlayerCounts`): Provider wirft → letzter guter Stand bleibt, Warnung genau einmal (Captured Appender), jeder weitere Fehler nur `DEBUG`; ohne Stand die Meldung `unavailable`. Rot. Dann Fehlerbehandlung D8. Grün.

## 7. Inventar, Hotbar-Item, Klick, Aktualisierung

- [ ] 7.1 Test zuerst (Integration, Cyano-`Env`, `env.tick()`, Fake-`PlayerCounts` mit drei Diensten): Mit Flag und CloudNet-Profil erhält ein Spieler beim Beitritt das Item auf Slot 8; ohne Flag nicht; ohne Identität nicht. Rot. Dann `LobbySwitcherItems` (`@Factory`, `@Profile(CLOUDNET)`) und Modul (`@Singleton`, `@Profile(CLOUDNET)`, `FeatureNode.attach` mit gewählter Priorität). Grün.
- [ ] 7.2 Test zuerst (Integration): Klick auf das Item öffnet das Inventar mit den drei Diensten in Namensreihenfolge, die eigene Lobby ist markiert, die Zahlen stimmen mit dem Fake überein. Rot. Dann `SwitcherInventory` (`GlobalTranslatedInventoryBuilder`, `register()` in `@PostConstruct`, `unregister()` in `@PreDestroy`, Layout nach D4). Grün.
- [ ] 7.3 Test zuerst (Integration): Klick auf eine beitretbare Lobby ruft den Fake-`Deliver` genau einmal mit dem Dienstnamen (`serverBuilder`) auf und schließt das Inventar. Klick auf `CURRENT`, `FULL` oder `NOT_READY` ruft ihn nicht auf und sendet die passende Meldung. Rot. Dann Klick-Behandlung (D6). Grün.
- [ ] 7.4 Test zuerst (Integration): Nach `@PreDestroy` ist das Inventar nicht mehr registriert, das Item ist nicht mehr auf der Hotbar, keine Periode läuft. Rot. Dann Aufräumen. Grün.
- [ ] 7.5 Test zuerst (Integration, `env.tick()`): Ein geöffnetes Inventar wird nach `refreshSeconds * 20` Ticks mit neuen Zahlen aktualisiert; ohne Viewer findet nach dem Schließen keine Abfrage mehr statt (Zähler im Fake). Rot. Dann Aktualisierung über `OffTickRefresh` (oder Kopie, je Q3). Grün.

## 8. Telemetrie

- [ ] 8.1 Test zuerst (Integration, `TestTelemetry`): Öffnen erzeugt den Span `lobbyswitcher.open` mit `user.id` und `lobbyswitcher.entries`; die Aktualisierung erzeugt keinen Span. Rot. Dann `LobbySwitcherTelemetry`. Grün.
- [ ] 8.2 Test zuerst (Integration, `TestTelemetry`): Klick erzeugt den Span `lobbyswitcher.select` mit `user.id`, `lobbyswitcher.target` und `lobbyswitcher.result` (`sent`, `current`, `full`, `not_ready`). Der Zähler `titan.lobbyswitcher.selections` trägt nur `result`, keine Dienstnamen und keine UUID. Rot. Dann Zählerfunktion. Grün.

## 9. Texte

- [ ] 9.1 Test zuerst (Unit, `LobbySwitcherMessagesBundleTest`): `messages_en.properties` und `messages_de.properties` haben dieselben Schlüssel; jeder Schlüssel in `KEYS` steht im englischen Bundle. Rot. Dann `titan/lobbyswitcher/messages_{en,de}.properties` und `LobbySwitcherMessages` (Store registrieren/entfernen, explizit rendern). Grün.
- [ ] 9.2 Test zuerst (Integration, Cyano-`Env`): `de_DE` sieht die Meldung „volle Lobby“ auf Deutsch, `ja_JP` auf Englisch. Rot, dann grün (Ergebnis aus 1.1 beachten).

## 10. Verdrahtung in den Varianten

- [ ] 10.1 Test zuerst (Wiring, `apps/cloudnet`): Die Column ist im Container; Item und Inventar sind mit Flag vorhanden; `WiringTest` kennt das Modul. Rot, dann grün.
- [ ] 10.2 Test zuerst (Wiring, `apps/local`, `VariantStartTest`): Ohne CloudNet startet die lokale Variante; `lobbyswitcher`-Beans fehlen; `expectedModules` schlägt nicht fehl. Rot. Dann Anpassung der Modulliste, falls nötig, sonst Begründung im Test. Grün.
- [ ] 10.3 Nachweis: `./gradlew build` grün; `ColumnArchitectureRules` grün (die Column hängt nur an `core`).

## 11. Dokumentation

- [ ] 11.1 `README.md`: Abschnitt „Feature flags“ um `LOBBYSWITCHER` (Standard `false`) ergänzen; den Absatz „kein Neustart“ um die Ausnahme ergänzen (Item-Präsenz beim Start, Q1); Tabelle der Umgebungsvariablen um `lobbyswitcher.refreshSeconds` (`LOBBYSWITCHER_REFRESHSECONDS`); kurzer Eintrag zu Column und Hotbar-Slot 8.
- [ ] 11.2 `docs/lobby-modules.md`: Column-Liste im Kapitel „Module“ um `lobbyswitcher`; Abschnitt „Traces und Metriken“ um Spans und Zähler; Tabelle „Was Admin, Hotbar und Rechteprüfung liefern“ um `lobbyswitcher` ergänzen.
- [ ] 11.3 Nachweis: Dokumentation und Code stimmen überein (Flagname, Schlüssel, Span- und Zählernamen, Slot 8). Mechanisches Review durch einen Haiku-Agenten.

## 12. Smoke-Test und Abschluss

- [ ] 12.1 Smoke-Test auf CloudNet mit zwei Lobbys desselben Tasks (manuell): `FEATURES_LOBBYSWITCHER=true` setzen; beide Lobbys starten; Item auf Slot 8; die Liste zeigt beide mit Zahlen; Zahlen ändern sich, während das Inventar offen ist; Schließen stoppt die Aktualisierung (Zähler oder DEBUG-Log); Klick auf die andere Lobby wechselt; die eigene Lobby ist nicht klickbar; eine volle Lobby (Max-Wert der Lobby begrenzen) ist nicht beitretbar; Loki zeigt `lobbyswitcher.select` mit `result=sent`.
- [ ] 12.2 Offene Fragen Q4, Q5, Q6, Q9 entscheiden und in `design.md` festhalten, bevor der PR entsteht.
- [ ] 12.3 Archiv nach Umsetzung: `openspec/changes/lobby-switcher` nach `openspec/changes/archive/2026-10-10-lobby-switcher` verschieben, Delta nach `openspec/specs/lobby-switcher/spec.md` übernehmen. Commit `docs(openspec): archive lobby-switcher`.
- [ ] 12.4 Nachweis: `openspec validate lobby-switcher --strict` grün (vor dem Archiv); `./gradlew build` grün; das Startlog der cloudnet- und der local-Variante hat keine neuen Warnungen.
- [ ] 12.5 PR öffnen: Titel `feat(lobbyswitcher): show other lobbies with player counts and switch between them`; Beschreibung auf Englisch mit Zusammenfassung, Smoke-Test-Ergebnis und Verweis auf `openspec/changes/lobby-switcher`; abschließend `https://claude.ai/code/session_01GXose9rakuRNZoM3TP3xj1`. Danach nur auf grüne CI warten; Merge und Release macht der Nutzer.
