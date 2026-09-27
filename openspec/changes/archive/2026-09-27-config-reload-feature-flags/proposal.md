# Proposal

## Why

Nach `avaje-config-facade` liest die Lobby ihre Konfiguration nur beim Start. Jede Änderung, z. B. ein neues Navigator-Ziel oder ein anderer Sitz-Versatz, braucht einen Neustart und wirft damit alle Spieler aus der Lobby. Daneben laufen die Feature-Flags als zweites System über Togglz und `flags.properties`: eine eigene Datei, ein eigenes Format, eigene Ladelogik (`SingletonFeatureManagerProvider`, `ThreadHelper`) und keine Profile oder Env-Overrides.

Die Lobby soll Konfigurationsänderungen im laufenden Betrieb übernehmen und die Feature-Flags als ganz normale Konfigurationswerte führen. Dann gibt es eine Quelle, ein Format und einen Weg für Overrides, und Flags lassen sich je Profil oder Umgebung setzen.

**Auslieferung:** `feat(config)!: reload configuration at runtime and read feature flags from it`. Der PR trägt diesen Footer:

`BREAKING CHANGE: feature flags move from flags.properties to the features section of application.yaml (e.g. features.NAVIGATOR_SLENDER: true, env FEATURES_NAVIGATOR_SLENDER); flags.properties and Togglz are no longer read.`

**Voraussetzung:** `avaje-config-facade` ist gemergt. Diese Change zweigt danach von `main` ab.

## What Changes

- **Konfiguration wird live gelesen:**
  - Jede Stelle, die einen Konfigurationswert braucht, liest ihn bei Gebrauch über die statische `Config`-Fassade (`Config.getInt`/`getLong`/`getBool`/`get`/`getAs`/`list()`…), statt ihn beim Start einmal in ein Feld zu übernehmen. Lesepunkte: tickle liest `cooldownMillis` je Angriff; sit liest `offset.*`/`allowedBlocks` je Blockinteraktion; elytra liest `burnDurationTicks`/`cooldownTicks` je Boost; spawn liest `minHeight`/`maxHeight` je Höhenprüfung und `simulationDistance` je Beitritt; der Navigator liest `title`/`entries` je Öffnen.
  - Die eingebaute Dateiüberwachung von avaje-config (`config.watch.enabled`, standardmäßig aus, `config.watch.delay`/`config.watch.period` je 10 Sekunden) hält die Fassade aktuell. Überwacht werden nur Dateien, die beim Start existierten. Eine Änderung wirkt beim nächsten Lesen — ohne Neustart, ohne dass ein Modul seinen Zustand verliert (ein sitzender Spieler bleibt sitzen, ein laufender Elytra-Flug läuft weiter).
  - Konfigurationswerte werden ausschließlich beim Start geprüft (unverändertes Verhalten aus `avaje-config-facade`): Ein ungültiger Wert bricht den Start weiterhin ab. Zur Laufzeit liest jede Stelle ihren Wert direkt über `Config.<methode>` an der Nutzungsstelle — ohne erneute Prüfung und ohne Rückfall auf einen anderen Wert. Ein zur Laufzeit ungültiger, aber parsbarer Wert wirkt so, wie die einfache Verwendung dieses Werts es ergibt (z. B. `tickle.cooldownMillis: -5` wie kein Cooldown). Ein Wert, der sich nicht parsen lässt, oder ein kaputter Navigator-Eintrag, lässt genau diese Aktion mit einer Exception fehlschlagen, die Minestoms bestehender Exception-Handler protokolliert — die Lobby läuft weiter.
  - **BREAKING, entfällt:** Der Befehl `/titanreload` fällt weg. Mit ihm entfallen der erste `TranslationStore` der Lobby, die Sprachbündel und Minestoms automatische Komponentenübersetzung — sie wurden nur vom Befehl gebraucht.
  - **Grenzen** (akzeptierte Einschränkungen der eingebauten Dateiüberwachung):
    - Ein Env- oder System-Property-Override für einen Schlüssel, dessen Datei sich ändert, wird bis zum nächsten Neustart der Lobby vom Dateiwert verdrängt.
    - Aus einer geänderten Datei gelöschte Schlüssel bleiben bis zum nächsten Neustart aktiv.
    - Eine erst nach dem Start angelegte Datei wird erst nach einem Neustart gelesen.
    - Es gibt keinen manuellen Auslöser: Eine Änderung wirkt spätestens nach `config.watch.delay` (erste Prüfung nach dem Start), danach nach jedem weiteren `config.watch.period`.
    - Eine syntaktisch kaputte Datei protokolliert avaje-config selbst über JUL (`java.util.logging`), nicht über SLF4J; ohne Brücke zum Log-Backend der Lobby erscheint diese Zeile nicht in denselben Logs wie der Rest der Lobby.
    - Es gibt keine Laufzeitvalidierung: Zur Laufzeit gelesene Werte werden weder geprüft noch fällt eine Einstellung auf einen mitgelieferten Standardwert zurück; es gibt weder eine WARN-Zeile noch eine Dedupe-Logik dafür. Der Betreiber ist für gültige Werte verantwortlich; die README beschreibt, eine Änderung vor dem Speichern in der Produktion zu prüfen (z. B. Neustart in einer Testumgebung).
- **Feature-Flags aus der Konfiguration:**
  - Flags stehen als Booleans im Abschnitt `features` (`features.NAVIGATOR_SLENDER: true`) und lassen sich wie jeder Wert per Profil, externer Datei, Env-Variable (`FEATURES_NAVIGATOR_SLENDER`) oder System-Property setzen.
  - Die mitgelieferte `application.yaml` im Classpath führt alle bekannten Flags mit dem Standardwert `false`. Eine Flag, die dort nicht steht, ist unbekannt: Ein Navigator-Eintrag, der sie nutzt, bricht den Start ab (wie heute). Taucht eine unbekannte Flag oder ein ungültiger Eintrag erst zur Laufzeit auf, wird das nicht mehr geprüft.
  - Eine geänderte Flag wirkt ohne Modulneustart beim nächsten Auswerten, beim Navigator also beim nächsten Öffnen.
- **BREAKING, entfällt:** Togglz (`TitanFeatures`, `TogglzFeatureFlags`, `SingletonFeatureManagerProvider`, `ThreadHelper`, soweit nur für Togglz genutzt), `flags.properties` und die ungenutzten `param.stage`-Einträge. Die Schnittstelle `FeatureFlags` bleibt und bekommt eine Implementierung über die Konfiguration.

## Capabilities

### New Capabilities
<!-- keine -->

### Modified Capabilities
- `lobby-module-config`: Neue Anforderungen dazu, wie geänderte Konfigurationswerte zur Laufzeit wirken (Auslöser, live gelesen bei Gebrauch statt Modulneustart, Validierung nur beim Start und zur Laufzeit unvalidierte Werte). Feature-Flags werden Teil der Konfiguration, samt Rangfolge der Overrides.
- `lobby-navigator`: Titel und Ziele werden bei jedem Öffnen neu gelesen. Die Anforderung zu Feature-Flags nennt `features.*` in der Konfiguration statt `flags.properties`; eine unbekannte Flag ist eine, die nicht in den mitgelieferten Standardwerten steht. Eine unbekannte Flag oder ein ungültiger Eintrag wird nur beim Start geprüft; tritt das erst zur Laufzeit auf, wird das nicht geprüft.

## Impact

- **Code:**
  - `common/feature`: `ConfigFeatureFlags` ersetzt `TogglzFeatureFlags`, `common/utils`: Togglz-Klassen entfallen.
  - `app/bootstrap/reload` entfällt vollständig (kein `ConfigChangeHandler`, keine Modul-Zuordnung, kein Revert). Stattdessen liest jedes betroffene Modul (tickle, sit, elytra, spawn, navigator) seine Werte bei Gebrauch direkt über die Fassade (`Config.<methode>`), ohne Validierung oder Rückfall. Es gibt keinen gemeinsamen Laufzeit-Helfer: `RuntimeConfigFallback` entfällt.
  - `app`: `ModuleRegistry#restart`, `RestartOutcome` und `moduleIds()` entfallen mangels Aufrufer, ebenso `ModuleRestartTest`/`ModuleRestartFailureTest`; die `ItemRegistry#unregister`-Korrektur für `keyClaims` und ihr Test bleiben erhalten. `NavigatorModule` bzw. `NavigatorEntries` lesen Titel und Einträge bei jedem Öffnen aus der Konfiguration.
  - Classpath-`application.yaml`: neuer Abschnitt `features` mit allen Flags und `config.watch.*`-Einstellungen.
- **Abhängigkeiten:** `org.togglz:*` entfällt, es kommen keine neuen dazu.
- **Texte für Nutzer:** entfallen. Ohne Befehl gibt es keine neue Ausgabe an Spieler; das bestehende i18n-System der Lobby bleibt unangetastet.
- **Betrieb (BREAKING):**
  - CloudNet-Templates und Deployments ersetzen `flags.properties` durch `features.*` in `application.yaml` bzw. durch Env-Variablen.
  - Keine LuckPerms-Berechtigung nötig: Ohne Befehl entfällt auch die bisher vorgesehene Reload-Berechtigung.
  - PR #304 (bisheriger Ansatz mit eigenem Loader/Watcher und Befehl) wird auf diesen Ansatz umgebaut, statt neu eröffnet zu werden.
- **Doku:** README und `docs/lobby-modules.md` beschreiben das Neuladen (Auslöser, Intervall, Grenzen) und die Flags.
