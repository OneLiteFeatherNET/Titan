# Tasks

Reihenfolge: zuerst die Schnittstelle und das Menü mit dem In-Memory-Speicher, dann der Otis-Adapter. Jede Aufgabe mit Test beginnt rot, dann grün, dann aufräumen. Unit-Tests ohne Server, Netz, Zeit und Dateisystem; `Clock` wird injiziert; kein `Thread.sleep`. Integrationstests mit Cyano-`Env` und `env.tick()`; der Otis-Adapter wird gegen einen JDK-HTTP-Server auf Port 0 getestet.

## 1. Vorlauf

- [ ] 1.1 Aves-Spike: prüfen, ob ein Inventar je Betrachter unterschiedliche Items zeigen kann (Ergebnis in `design.md` D6 festhalten).
- [ ] 1.2 Otis-Voraussetzungen prüfen, nur lesend: Dienst-Authentifizierung vorhanden? Produktionsmigration V2 (`player_setting`) angewendet? Velocity legt Spieler beim Beitritt in Otis an? Ergebnis in `proposal.md` und `design.md` festhalten. Keine schreibenden Aufrufe gegen Produktion.
- [ ] 1.3 Client-Abhängigkeit: Version von `net.onelitefeather.otis:otis-client` mit Settings-Pfaden bestätigen, oder eigene JSON-Abbildung festlegen (`design.md`, Frage 3).
- [ ] 1.4 Hotbar-Slot 7 gegen `features/hotbar` prüfen (Konfliktprüfung) und in `design.md` Frage 5 beantworten.

## 2. Schnittstelle in `core` (`net.onelitefeather.titan.core.settings`)

- [ ] 2.1 Test zuerst (Unit, `SettingKeyTest`): `titan:tag.enabled` und `titan:throw.target` gültig; Großbuchstaben, Leerzeichen, fehlender Namespace und leerer Wert ungültig. Verifikation: rot, dann grün.
- [ ] 2.2 `SettingKey` mit Prüfung nach den Otis-Zeichenregeln (Namespace `[a-z0-9_.-]`, Wert `[a-z0-9_./-]`).
- [ ] 2.3 Test zuerst (Unit, `BooleanSettingTest`): `OPT_IN` liefert Standard `false`, `OPT_OUT` liefert `true`; Schlüssel und Standard sind unveränderlich. Verifikation: rot, dann grün.
- [ ] 2.4 `BooleanSetting` und `SettingDefault` (`OPT_IN`, `OPT_OUT`).
- [ ] 2.5 Test zuerst (Unit, `SettingsRegistryTest`): Registrierung in Reihenfolge; doppelter Schlüssel wirft mit beiden Registrierungen in der Meldung; unbekannter Schlüssel liefert leer. Verifikation: rot, dann grün.
- [ ] 2.6 `SettingsRegistry`.
- [ ] 2.7 `PlayerSettings`-Port, `SettingChangeListener`, `SettingsStore`-Port (`load`, `save`).

## 3. Speicher im Prozess und Dienst (`features/settings`)

- [ ] 3.1 Test zuerst (Unit, `PlayerSettingsServiceTest`, Fake-Store): ohne Laden liefert `isEnabled` den Standard; `set` wirkt sofort; Listener wird mit altem und neuem Wert benachrichtigt; Option eines anderen Spielers bleibt unverändert. Verifikation: rot, dann grün.
- [ ] 3.2 `PlayerSettingsService` mit `ConcurrentHashMap`, Eintrag je Schlüssel (`value`, `version`, `dirty`).
- [ ] 3.3 Test zuerst (Unit, `LoadMergeTest`): geladener Wert übernimmt, wenn der Schlüssel nicht `dirty` ist; lokal geänderter Schlüssel bleibt; Ergebnis nach Verlassen des Spielers wird verworfen. Verifikation: rot, dann grün.
- [ ] 3.4 `applyLoaded` und Verwerfen nach Verlassen.
- [ ] 3.5 Test zuerst (Unit, `WriteBehindTest`, injizierter Scheduler-Fake, kein Warten): drei Umschaltungen in zwei Sekunden ergeben einen Speicherauftrag mit dem letzten Wert; Verlassen flusht sofort; Fehlerwiederholung bis `writeRetries`; 400 wird nicht wiederholt. Verifikation: rot, dann grün.
- [ ] 3.6 `WriteBehind` mit Debounce, Flush und Wiederholung.
- [ ] 3.7 `InMemorySettingsStore`.

## 4. Menü, Befehl, Hotbar-Item (`features/settings`)

- [ ] 4.1 Test zuerst (Unit, `SettingsMenuTest`): ein Item je Option; Lime bei an, Grau bei aus; Klick schaltet um und zeichnet nur das betroffene Item neu; Texte de und en. Verifikation: rot, dann grün.
- [ ] 4.2 `SettingsMenu` (Aves, ein Builder im Modul, Inventar beim Öffnen gebaut und beim Schließen entfernt, je Ergebnis aus 1.1).
- [ ] 4.3 `messages_de.properties` und `messages_en.properties` mit gleichen Schlüsseln; Bundle-Test prüft, dass alle Schlüssel in beiden Dateien stehen und gerendert werden.
- [ ] 4.4 Test zuerst (Env, `SettingsCommandIntegrationTest`): `/settings` öffnet das Menü; `/einstellungen` ist unbekannt; Konsole erhält eine Meldung und nichts öffnet sich. Verifikation: rot, dann grün.
- [ ] 4.5 `SettingsCommand` (`settings`, ohne Alias) mit Registrierung und Abmeldung im Lebenszyklus.
- [ ] 4.6 Test zuerst (Env, `SettingsHotbarIntegrationTest`): Hotbar-Item in Slot 7 ist vorhanden, nach Respawn wieder da; Klick öffnet das Menü. Verifikation: rot, dann grün.
- [ ] 4.7 Hotbar-Item in Slot 7 über die Item-Schnittstelle von `lobby-hotbar`.

## 5. Otis-Adapter (`platform/otis`)

- [ ] 5.1 Test zuerst (Unit, `OtisJsonTest`): Antwort mit `key`, `value`, `version`, `updatedAt` lesen; unbekannte Felder ignorieren; Schreibanfrage als `{"value": …}`. Verifikation: rot, dann grün.
- [ ] 5.2 `OtisSettingsStore` mit `java.net.http.HttpClient`, Timeouts aus Konfiguration.
- [ ] 5.3 Test zuerst (Unit, `CircuitBreakerTest` mit `AdjustableClock`): nach drei Fehlern offen; nach 30 Sekunden halb offen mit genau einer Probe; Erfolg schließt; Fehler öffnet erneut. Verifikation: rot, dann grün.
- [ ] 5.4 `CircuitBreaker`, selbst gebaut, Zeit über `Clock`.
- [ ] 5.5 Integrationstest (`integrationTest`, `OtisSettingsStoreIntegrationTest`, JDK-`HttpServer` auf Port 0, je ein Verhalten pro Test): Laden liefert Werte; 404 wird als „leer“ gewertet; 5xx wird wiederholt; Timeout führt zum Standard; Token wird im Header `Authorization` gesendet, nie in Logs. Verifikation: rot, dann grün.
- [ ] 5.6 Test zuerst (Unit, `SettingsLogRedactionTest` mit captured appender): Token und Wert erscheinen in keiner Log-Zeile bei Fehlern. Verifikation: rot, dann grün.

## 6. Konfiguration und Verdrahtung

- [ ] 6.1 Standardwerte in `features/settings/src/main/resources/titan/defaults/settings.yaml`: `settings.enabled: false`, `otis.*` mit den Werten aus `design.md` D8.
- [ ] 6.2 Test zuerst (Unit, `SettingsConfigTest`): Standardwerte; leere `otis.url` wählt In-Memory; gesetzte URL wählt Otis; Token wird maskiert angezeigt. Verifikation: rot, dann grün.
- [ ] 6.3 Startwarnung bei leerer `otis.url` bei aktivierter Funktion.
- [ ] 6.4 `package-info.java` mit `@InjectModule(name = "settingsColumn", ...)`; `ColumnArchitectureTest` (wie bei den übrigen Columns, Abhängigkeit nur auf `core`).

## 7. Lebenszyklus und Beitritt

- [ ] 7.1 Test zuerst (Env, `SettingsJoinIntegrationTest`, je ein Verhalten pro Test): Beitritt läuft ohne Antwort von Otis durch (Stub, das nie abschließt); Übernahme geladener Werte erscheint nach `env.tick()`; Verlassen während des Ladens übernimmt nichts. Verifikation: rot, dann grün.
- [ ] 7.2 `SettingsModule`: `@PostConstruct` startet Listener für `AsyncPlayerConfigurationEvent`, Verlassen und Herunterfahren; `@PreDestroy` flusht mit Limit 5 Sekunden und hängt den `FeatureNode` ab.
- [ ] 7.3 Test zuerst (Env, `SettingsModuleLifecycleTest` wie `SpawnModuleTest#stopLeavesNoListenerBehind`): nach `stop()` löst kein Event mehr Code des Moduls aus.

## 8. Telemetrie

- [ ] 8.1 Test zuerst (Unit, `SettingsTelemetryTest` mit Fake-`Telemetry`): Ladedauer mit `outcome`; Schreibergebnis; Umschaltung ohne UUID; Breaker-Zustandswechsel. Verifikation: rot, dann grün.
- [ ] 8.2 `SettingsTelemetry` mit den Namen aus `design.md` D9.

## 9. Dokumentation

- [ ] 9.1 `docs/lobby-modules.md`: Abschnitt „Einstellungen (`settings`)“ mit Menü, Befehl, Opt-in und Opt-out, Otis-Anbindung und Offline-Variante.
- [ ] 9.2 Konfigurationsschlüssel und Secret `TITAN_OTIS_TOKEN` in der Betriebsdokumentation nennen.
- [ ] 9.3 Spec-Delta mit der Implementierung abgleichen (Szenario-Titel, Schlüssel, Slot, Standardwerte).

## 10. Prüfung

- [ ] 10.1 `./gradlew spotlessApply` und `./gradlew build` grün.
- [ ] 10.2 `openspec validate --strict lobby-settings` grün.
- [ ] 10.3 Sichtprüfung auf einem Testserver mit Otis-Testinstanz (nicht Produktion): Menü, Umschalten, Neustart des Servers, Wiederherstellung nach Otis-Ausfall. Nicht ausgeführt, falls kein Testserver bereitsteht; im PR als offen vermerken.

## 11. Abhängige Changes (nach Merge)

- [ ] 11.1 `lobby-tag` (Fangen) wird auf `BooleanSetting` `titan:tag.enabled` (`OPT_IN`) umgestellt; das Tag-Schalter-Verhalten kommt aus `PlayerSettings`. Eigener Change, nicht Teil dieses PRs.
- [ ] 11.2 `lobby-throw` (Schleudern-Ziel sein) registriert `titan:throw.target` (`OPT_OUT`). Eigener Change, nicht Teil dieses PRs.

## 12. Commits und Pull Request

- [ ] 12.1 Commit `docs(openspec): propose lobby-settings` (Artefakte und Aufgaben).
- [ ] 12.2 Commit `feat(settings): ...` für Code und Tests, Commit `docs(settings): ...` für `docs/`; abgehakte Aufgaben im selben Commit wie der zugehörige Code.
- [ ] 12.3 Pull Request öffnen mit dem Titel `feat(settings): add a lobby settings menu backed by otis player settings`. Beschreibung auf Englisch: Schnittstelle, Menü, Otis-Anbindung und Fallback, Voraussetzungen in Otis, offene Fragen, Testnachweis, offene Sichtprüfung. Abschließend `https://claude.ai/code/session_01AM6yGQ33vvJqWg39vCAcYt`. Danach nur auf grüne CI warten; Merge und Release macht der Nutzer.
