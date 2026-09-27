# Tasks

## Execution Plan

Voraussetzung: `avaje-config-facade` ist gemergt. Integrations-Branch: `feat/config-reload-feature-flags`, abgezweigt vom aktuellen `origin/main`. Jeder Agent arbeitet in einem eigenen Worktree und beginnt mit `git reset --hard feat/config-reload-feature-flags`. Nach jeder Welle wird zusammengeführt und `./gradlew build` ausgeführt. Danach prüfen ein Sonnet-Review und ein Haiku-Check (F.I.R.S.T.: keine Sleeps, kein Lesen der Systemzeit, keine `Config`-Mutatoren in Tests, keine Abhängigkeit von der Reihenfolge), und Befunde behebt ein Sonnet-Agent.

Jeder Agent-Prompt wiederholt die Regeln, die für ihn gelten:
- Built-in first: `config.watch.enabled` statt eigenem Loader/Watcher, Begründung in design.md Entscheidung 1. Kein `Config.onChange`, kein Modulneustart, kein Revert.
- Java-25-Features bleiben, wo sie schon stehen (Records, Pattern Matching).
- SLF4J mit Parametern, Stufen wie in design.md.
- Keine Metriken und Spans (Entscheidung 4).
- Kein Zugriff auf die `Config`-Fassade blockiert den Tick-Thread; Lesepunkte sind einfache, günstige Abfragen (siehe design.md Entscheidung 2, Performance).
- Keine `Config`-Mutatoren (`setProperty`/`putAll`/`clearProperty`/`eventBuilder`) in Tests.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| F | restart-removal | 2.1 | sonnet | `app/src/main/.../module/ModuleRegistry.java` (nur `restart`/`moduleIds`/`RestartOutcome` entfernen), `app/src/test/.../ModuleRestartTest.java` (löschen), `app/src/test/.../ModuleRestartFailureTest.java` (löschen) | `module/**`, `common/**`, `feature/**`, `ItemRegistry`-Korrektur und ihr Test |
| F | live-read | 3.1–3.7, 3a.1–3a.5 | sonnet | `common/config/**` (Entfernen von `RuntimeConfigFallback`), `app/src/{main,test}/.../feature/tickle/**`, `app/src/{main,test}/.../feature/sit/**`, `app/src/{main,test}/.../feature/elytra/**`, `app/src/{main,test}/.../feature/spawn/**`, `app/src/{main,test}/.../feature/navigator/**` (jeweils nur die Konfigurationszugriffe), `app/src/{main,test}/.../bootstrap/reload/**` (nur Löschen), `app/src/main/resources/application.yaml` | `feature/**` außer Testhelfern, `ModuleRegistry.java` |
| G | integration | 4.1 | sonnet | `app/src/test/.../ModuleWiringTest.java` | `module/**` außer bestehenden Testmodulen, `common/**` |
| G | docs, Hauptkontext | 6.1–6.4, 7.1 | sonnet / haiku / – | `README.md`, `docs/**` | Code |

## 1. Feature-Flags aus der Konfiguration (Welle A, test-first)

- [x] 1.1 **Unit zuerst:** `ConfigFeatureFlags` mit Konstruktor `(Set<String> known, Predicate<String> active)` bzw. einer Abfragefunktion und einer statischen Fabrik für die Fassade. Testfälle: bekannt und an, bekannt und aus, unbekannt (`exists == false`), fehlender Wert ergibt aus. Verifikation: Die Tests sind zuerst rot, dann grün. Kein Test ruft `Config` auf.
- [x] 1.2 Abschnitt `features` mit den fünf Navigator-Flags (`false`) in der Classpath-`application.yaml` ergänzen. Die bekannten Flags werden in der Fabrik aus der Classpath-Ressource als eigene Instanz geladen. **Unit-Test:** Jede Flag, die ein Standard-Navigator-Eintrag nutzt, steht in der Datei. Verifikation: Der Test ist grün.
- [x] 1.3 In `PlatformBeans` `TogglzFeatureFlags` durch `ConfigFeatureFlags` ersetzen und Togglz entfernen: `TitanFeatures`, `TogglzFeatureFlags`, `SingletonFeatureManagerProvider`, `ThreadHelper`, die SPI-Datei, die Togglz-Einträge in Versionskatalog und Build und `flags.properties` im Repo. Verifikation: `./gradlew build` ist grün, und `grep -rniE "togglz|TitanFeatures|ThreadHelper" --include=*.java --include=*.kts --include=*.toml app common setup gradle` findet nichts.

## 2. Restart-API entfernen (Welle F)

- [x] 2.1 **Zuerst entfernen:** `ModuleRegistry#restart(String)`, `RestartOutcome`, `ModuleRegistry#moduleIds()`, die Prüfung, dass `restart` auf dem Tick-Thread läuft, sowie `ModuleRestartTest` und `ModuleRestartFailureTest` — der Maintainer verwirft den Modulneustart-Ansatz (design.md Entscheidung 1). Die `ItemRegistry#unregister`-Korrektur für `keyClaims` und ihr Test bleiben unverändert erhalten, sie hängen nicht an `restart`. Verifikation: `./gradlew build` ist grün, `grep -rniE "RestartOutcome|\.restart\(|moduleIds\(\)" --include=*.java app common module` findet nichts mehr, und die verbliebenen `ItemRegistry`- und Registry-Tests sind grün.

## 3. Live lesen statt Modulneustart (Welle F, test-first)

> **Nachtrag (Maintainer-Entscheidung, siehe design.md Entscheidung 2):** Der in 3.1 beschriebene gemeinsame Helfer (`RuntimeConfigFallback`) samt Rückfall auf den Classpath-Standardwert und WARN-Dedupe ist überholt und entfällt (Abschnitt 3a). Die Lesepunkte aus 3.2–3.6 bleiben gültig, aber ohne Rückfall und ohne WARN.

- [x] 3.1 **Unit zuerst:** Gemeinsamer Helfer für die Laufzeitvalidierung (design.md Entscheidung 2): lädt die Classpath-Standardwerte einmalig als eigene `Configuration`-Instanz (wie `ConfigFeatureFlags`) und dedupliziert die WARN-Zeile je Schlüssel und Wert über eine kleine, thread-sichere Zuordnung. Testfälle: ein neuer ungültiger Wert warnt einmal, derselbe ungültige Wert erneut warnt nicht wieder, ein anderer ungültiger Wert für denselben Schlüssel warnt wieder, die Standardwerte werden nur einmal geladen. Verifikation: Die Tests sind zuerst rot, dann grün. Kein Test ruft `Config`-Mutatoren auf.
- Für 3.2–3.6 gilt: Der Produktivcode ruft `Config.<methode>` an der Nutzungsstelle auf (keine injizierte Wertquelle). Unit-Tests prüfen die reinen Prüf- und Rückfallfunktionen mit einfachen Werten; das Live-Verhalten deckt der Kind-JVM-Test aus 3.7 ab. Kein Test ruft `Config`-Mutatoren auf.
- [x] 3.2 **Unit zuerst:** tickle liest `cooldownMillis` bei jedem Angriff direkt über die statische `Config`-Fassade; ein Wert < 0 ergibt den Classpath-Standardwert (4000) über den Helfer aus 3.1 plus WARN mit Schlüssel und Grund. Verifikation: Die Tests sind zuerst rot, dann grün.
- [x] 3.3 **Unit zuerst:** sit liest `offset.*`/`allowedBlocks` bei jeder Blockinteraktion direkt über die statische `Config`-Fassade; ein bereits sitzender Spieler bleibt sitzen, wenn sich der Versatz während des Sitzens ändert — der neue Wert gilt erst bei der nächsten Interaktion. Verifikation: Die Tests sind zuerst rot, dann grün.
- [x] 3.4 **Unit zuerst:** elytra liest `burnDurationTicks`/`cooldownTicks` bei jedem Boost direkt über die statische `Config`-Fassade; `cooldownTicks <= burnDurationTicks` ergibt die Classpath-Standardwerte für beide Schlüssel über den Helfer aus 3.1 plus WARN. Verifikation: Die Tests sind zuerst rot, dann grün.
- [x] 3.5 **Unit zuerst:** spawn liest `minHeight`/`maxHeight` bei jeder Höhenprüfung und `simulationDistance` bei jedem Beitritt direkt über die statische `Config`-Fassade; `minHeight >= maxHeight` ergibt die Classpath-Standardwerte für beide Schlüssel über den Helfer aus 3.1 plus WARN. Verifikation: Die Tests sind zuerst rot, dann grün.
- [x] 3.6 **Unit zuerst:** Der Navigator liest `title`/`entries` bei jedem Öffnen direkt über die statische `Config`-Fassade; ein unbekannter Flag-Name oder ein ungültiger Eintrag ergibt über den Helfer aus 3.1 die mitgelieferten Standard-Einträge plus WARN, statt die bisherigen Ziele zu behalten. Verifikation: Die Tests sind zuerst rot, dann grün; `NavigatorModuleLeakTest` bleibt grün.
- [x] 3.7 **Zuerst entfernen:** Das Paket `app/bootstrap/reload` vollständig (`ConfigChangeHandler`, `ConfigChangeBootstrap`, `ModuleKeys`, `Causes`, `ConfigRevertWriter`, `FlatConfigValues`) und seine Tests. Ein Kind-JVM-Integrationstest bleibt erhalten, angepasst aus `ConfigChangeFileWatchIntegrationTest`: Mit `config.watch.delay: 1`/`config.watch.period: 1` ist eine Dateiänderung beim nächsten Lesen über die Fassade sichtbar (Bedingungs-Wartung mit Timeout, kein `Thread.sleep`). Verifikation: `./gradlew build` ist grün, und `grep -rniE "ConfigChangeHandler|ConfigChangeBootstrap|ModuleKeys|ConfigRevertWriter|FlatConfigValues" --include=*.java app common` findet nichts mehr.

## 3a. Laufzeitprüfung entfernen (Welle F, Nachtrag zu Maintainer-Entscheidung)

- [x] 3a.1 `RuntimeConfigFallback` (`common/config`) und `RuntimeConfigFallbackTest` entfernen. `ClasspathConfiguration` ist bereits entfernt. Verifikation: `grep -rniE "RuntimeConfigFallback|ClasspathConfiguration" --include=*.java common app` findet nichts mehr.
- [x] 3a.2 `TickleSettings#current()`, `SpawnSettings#currentHeightBounds()`/`#currentSimulationDistance()`, `SitSettings#currentOffset()`/`#currentAllowedBlocks()` und `ElytraSettings#current()` lesen ihren Wert direkt über `Config.<methode>` an der Nutzungsstelle, ohne `RuntimeConfigFallback`, ohne erneute Prüfung und ohne Rückfall auf den Classpath-Standardwert. Die Startvalidierung in `enable()` bleibt unverändert. Ein zur Laufzeit ungültiger, aber parsbarer Wert wirkt unvalidiert (z. B. `tickle.cooldownMillis: -5` wie kein Cooldown); ein nicht parsbarer Wert lässt die Aktion mit einer Exception fehlschlagen, die Minestoms Exception-Handler protokolliert. Verifikation: `./gradlew build` ist grün.
- [x] 3a.3 `NavigatorModule#currentTitle()`/`#resolveEntries(...)` liest Titel und Einträge bei jedem Öffnen direkt über die Fassade, ohne `RuntimeConfigFallback` und ohne Rückfall auf die mitgelieferten Standard-Einträge. Eine unbekannte Flag oder ein ungültiger Eintrag, die erst zur Laufzeit auftreten, werden nicht mehr geprüft (siehe `specs/lobby-navigator/spec.md`); die Startvalidierung (unbekannte Flag bricht den Start ab) bleibt unverändert. Verifikation: `NavigatorModuleLeakTest` bleibt grün.
- [x] 3a.4 Fallback-bezogene Tests entfernen bzw. anpassen: `RuntimeConfigFallbackTest` löschen; die WARN-/Dedupe-Testfälle in `TickleSettingsTest`, `SpawnSettingsTest`, `SitSettingsTest`, `ElytraSettingsTest` und `NavigatorModuleResolveEntriesTest` entfernen und durch einen Test ersetzen, der belegt, dass ein ungültiger, aber parsbarer Wert unvalidiert durchgereicht wird bzw. dass ein nicht parsbarer Wert die erwartete Exception auslöst. Kein Test ruft `RuntimeConfigFallback` mehr auf.
- [x] 3a.5 **Verifikation der Welle:** `grep -rniE "RuntimeConfigFallback|ClasspathConfiguration" --include=*.java common app` findet nichts mehr, und `./gradlew build` ist grün.

## 4. Integration (Welle G)

- [x] 4.1 `ModuleWiringTest` und die bestehenden Registry- und Modul-Tests sind grün, ohne `ModuleRegistry#restart` und ohne `ConfigChangeHandler`. Verifikation: `./gradlew build` ist grün.

## 5. Befehl und Übersetzung entfernen (Welle D)

- [x] 5.1 `ReloadCommand`, `ReloadResultMessages`, `i18n/TitanTranslations`, `lang/titan_*.properties`, `ComponentTranslationBootstrap` und dessen Aufruf in `TitanApplication.main` entfernen, dazu die zugehörigen Tests. Verifikation: `./gradlew build` ist grün, und `grep -rniE "titanreload|TitanTranslations|automatic-component-translation" --include=*.java --include=*.kts --include=*.yaml app common setup` findet nichts.

## 6. Abnahme und Doku (Welle G)

- [x] 6.1 **F.I.R.S.T.-Check (Haiku):** `grep -rnE "Config\.(setProperty|putAll|clearProperty|eventBuilder|onChange)|Thread\.sleep|System\.currentTimeMillis|Instant\.now" --include=*.java */src/test` findet nichts (der Grep verbietet in Tests jetzt auch `Config.onChange`/`Config.eventBuilder`, die es im Produktivcode nicht mehr gibt). Der Agent prüft die neuen Tests zusätzlich auf Abhängigkeit von der Reihenfolge und auf gemeinsamen Zustand. Verifikation: Die Ausgabe steht im PR.
- [x] 6.2 **E2E-Smoke-Test** mit dem Shaded-Jar in einem Scratch-Verzeichnis, mit kurzem `config.watch.delay`/`config.watch.period`:
  - (a) `tickle.cooldownMillis` in `application.yaml` ändern: Der nächste Tickle-Angriff nach höchstens einem Intervall nutzt den neuen Wert, ohne dass im Log ein Neustart erscheint.
  - (b) `tickle.cooldownMillis: -5` setzen: tickle nutzt beim nächsten Angriff den Wert -5 unvalidiert (wirkt wie kein Cooldown), ohne WARN-Zeile und ohne Rückfall auf einen Standardwert; die Lobby läuft ohne Neustart weiter.
  - (c) Die Datei so ändern, dass sie kein gültiges YAML mehr ist: avaje-config nennt Datei und Stelle des Fehlers auf ERROR (über JUL), aus dieser Datei wird kein Wert übernommen.
  - (d) `features.NAVIGATOR_SLENDER: true` setzen: Die Flag ist beim nächsten Öffnen des Navigators an.
  - (e) Die Lobby ohne `config.watch.enabled: true` starten: Beim Start erscheint kein ERROR wegen fehlender überwachter Dateien.

  Verifikation: Die Log-Zeilen stehen im PR.
- [x] 6.3 README und `docs/lobby-modules.md` anpassen:
  - Neuladen: die Dateiüberwachung von avaje-config, standardmäßig aus und per `config.watch.enabled: true` in der Betreiber-Datei einzuschalten, `config.watch.delay`/`config.watch.period`, dass geänderte Werte beim nächsten Gebrauch gelten statt bei einem Modul- oder Lobby-Neustart; dass Konfigurationswerte ausschließlich beim Start geprüft werden und zur Laufzeit unvalidiert gelesen werden — ein ungültiger, aber parsbarer Wert wirkt wie seine einfache Verwendung (Beispiel `tickle.cooldownMillis: -5` wie kein Cooldown), ein nicht parsbarer Wert oder ein kaputter Navigator-Eintrag lässt die betroffene Aktion mit einer von Minestoms Exception-Handler protokollierten Exception fehlschlagen, ohne WARN-Zeile und ohne Rückfall; der Hinweis, eine Änderung vor dem Speichern in der Produktion zu prüfen (z. B. Neustart in einer Testumgebung); die Liste der Grenzen (verdrängte Overrides, gelöschte Schlüssel, neue Dateien, kein manueller Auslöser, avaje-Fehler laufen über JUL statt SLF4J).
  - Flags: `features.*`, Env-Namen, eine Umstellungstabelle von `flags.properties` und eine Vorlage `application-local.yaml` mit allen Flags auf `true` für lokale Tests.
  - Für Modul-Autoren: Wie ein Lesepunkt aussieht (Beispiel aus `common/config`), und dass eine Konfigurationsänderung keinen Modulzustand mehr kostet (kein Neustart, kein verlorener Zustand) — sowie dass dabei nicht mehr validiert wird.

  Verifikation: Ein Review-Agent prüft Schlüssel, Env-Namen und die Intervall-Angaben gegen den Code.
- [x] 6.4 Lokale Abnahme im Client durch den Maintainer:
  - Ein geänderter `sit.offset.y` wirkt beim nächsten Sitzversuch ohne Neustart der Lobby, und ein bereits sitzender Spieler bleibt währenddessen unverändert sitzen.
  - Slender erscheint nach Umschalten von `features.NAVIGATOR_SLENDER` in der Datei beim nächsten Öffnen des Navigators.
  - Eine geänderte, gültige Einstellung (z. B. `elytra.burnDurationTicks`) wirkt beim nächsten Gebrauch, ohne dass die Lobby oder ein Modul neu startet.

  Verifikation: Die Checkliste im PR ist abgehakt.

## 7. Pull Request

- [x] 7.1 Den bestehenden Pull Request `feat(config)!: reload configuration at runtime and read feature flags from it` (Branch `feat/config-reload-feature-flags` → `main`, PR #304) überarbeiten. Der Titel bleibt, die Beschreibung auf Englisch wird auf den neuen Ansatz umgeschrieben (avaje-Dateiüberwachung + Live-Lesen über die Fassade bei Gebrauch, kein Modulneustart, kein `ConfigChangeHandler`, Validierung nur beim Start und zur Laufzeit unvalidierte Werte — kein Rückfall, keine WARN-Zeile) und enthält:
  - den `BREAKING CHANGE`-Footer aus proposal.md,
  - die Grenzen aus design.md (Kontext/Risks),
  - die Umstellungsschritte und den Rollback aus design.md,
  - die Ausgaben aus 6.1 und 6.2,
  - die Abnahme-Checkliste.

  Verifikation: Der PR ist aktualisiert, und die CI ist grün.
