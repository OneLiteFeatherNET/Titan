# Design

## Context

Zum Anlass siehe `proposal.md`. Die Anforderungen stehen in `specs/lobby-module-config` und `specs/lobby-navigator`. Diese Change setzt auf den Stand nach `avaje-config-facade` auf: Module lesen in `enable()` über die statische `Config`-Fassade, prüfen in reinen Funktionen, und die Standardwerte stehen in der Classpath-`application.yaml`.

Der Stand in Titan:
- **Module:** `ModuleRegistry` kennt nur `enableAll()` und `disableAll()`. Beim Abschalten eines Moduls wird sein Event-Node abgehängt, die Aufgaben werden abgebrochen, und die Aufräum-Hooks laufen in umgekehrter Reihenfolge (Befehle, Items, Navigator-Einträge), erst dann `LobbyModule#disable()`. Nach `enableAll()` prüft die Registry `ItemRegistry#validate()` und `NavigatorEntries#validate(FeatureFlags)`. `ItemRegistry#equip(Player)` legt einem Spieler die Items an. `NavigatorModuleLeakTest` belegt, dass beim Öffnen und Schließen nichts liegen bleibt. Es gibt keinen Neustart eines einzelnen Moduls und keinen Bedarf dafür: Diese Change ändert an `ModuleRegistry` nichts außer dem Entfernen ungenutzten Codes (siehe Migrations-Hinweis unten).
- **Einstellungen:** Nach `avaje-config-facade` prüfen die `*Settings`-Klassen jedes Moduls ihre Werte beim Start mit reinen Funktionen (z. B. Cooldown >= 0, `min < max`) und werfen bei einem ungültigen Wert eine `ConfigException`, die den Start abbricht. Diese Prüffunktionen bleiben ausschließlich für den Start; zur Laufzeit lesen die Module ihre Werte direkt über die Fassade, ohne diese Prüffunktionen erneut aufzurufen (Entscheidung 2).
- **Flags:** `FeatureFlags` (`exists`, `isActive`) wird von `TogglzFeatureFlags` über das Enum `TitanFeatures` und `SingletonFeatureManagerProvider` (`flags.properties`, Togglz-SPI unter `META-INF/services`) umgesetzt. `ThreadHelper` gibt es nur für Togglz. `NavigatorInventory` wertet die Sichtbarkeit bei jedem Öffnen neu aus. `RESOURCE_PACK_REQUIRED_KICK` und die `param.stage`-Einträge nutzt kein Code.

**Migrations-Hinweis:** Eine frühere Fassung dieser Change hatte `ModuleRegistry#restart(String)`, `RestartOutcome` und `moduleIds()` bereits umgesetzt, zusammen mit einem `ConfigChangeHandler` unter `app/bootstrap/reload`, der auf `Config.onChange` reagierte und das betroffene Modul neu startete. Der Maintainer hat diesen Ansatz verworfen (siehe Entscheidung 1): Beide werden vollständig entfernt, mangels Aufrufer bzw. weil kein Handler mehr existiert, der sie aufrufen würde.

**Faktenprüfung der Dateiüberwachung von avaje-config 5.2** (Quelltext `FileWatch`, `CoreEventBuilder`, `ModificationEvent`), als akzeptierte Grenzen dieser Change:
- `config.watch.enabled: true` fragt nur Dateien ab, die beim Start existierten (Dateisystem-Dateien: `application.yaml`/Profil-Dateien/`CONFIG_FILE`), alle `config.watch.period` Sekunden (Standard 10) nach `config.watch.delay` (Standard 60), auf avajes eigenem Daemon-Timer-Thread „ConfigTimer“.
- Bei einer Änderung liest sie die ganze Datei neu und macht `eventBuilder("reload").put(key, value)` für jeden Schlüssel; `put` merkt nur Schlüssel vor, deren Wert sich vom aktuellen unterscheidet. Es gibt kein `remove`: aus der Datei gelöschte Schlüssel bleiben bestehen. Env- und System-Property-Overrides werden nicht erneut angewendet: Für Schlüssel, die in einer geänderten Datei stehen, verdrängt der Dateiwert einen Override, bis zum nächsten Neustart. Eine kaputte Datei loggt avaje selbst auf ERROR; ihre Werte werden nicht übernommen, andere geänderte Dateien aber schon.
- Es gibt keine öffentliche API, um das Neuladen von Hand auszulösen.
- `Config.onChange(Consumer<ModificationEvent>)` meldet Änderungen; `ModificationEvent` liefert `name()`, `configuration()` und `modifiedKeys()` — keine alten Werte.

## Goals / Non-Goals

**Goals:**
- Neuladen über die eingebaute Dateiüberwachung von avaje-config, mit den im Kontext genannten, bewusst akzeptierten Grenzen.
- Konfigurationswerte werden bei Gebrauch live über die statische Fassade gelesen, ohne Modul- oder Lobby-Neustart und ohne Modulzustand zu verlieren.
- Konfigurationswerte werden nur beim Start geprüft; zur Laufzeit werden sie unvalidiert aus der Fassade gelesen (siehe proposal.md, Grenzen).
- Flags als normale Konfigurationswerte, Togglz entfällt.

**Non-Goals:**
- Ein Modul, das aktiv auf eine Änderung reagiert (Push über `Config.onChange`). Module lesen stattdessen passiv bei jedem Gebrauch (Pull).
- Ein Neustart-Mechanismus für einzelne Module. `ModuleRegistry` bleibt bei `enableAll()`/`disableAll()`.
- Rollout-Stufen oder Flags pro Spieler bzw. pro Berechtigung.
- Neuladen im Setup-Server. Dort ändert sich nur `spawn.simulationDistance`, ein Neustart genügt.
- Die Classpath-Standardwerte zur Laufzeit ändern (das geht nur mit einem neuen Jar).
- Ein manueller Auslöser (Befehl) für sofortiges Neuladen. Die eingebaute Dateiüberwachung ist der einzige Weg, eine Änderung zu übernehmen.

## Decisions

### 1. Eingebaute Dateiüberwachung von avaje-config, Werte live über die Fassade gelesen

**Entscheidung:** Die Lobby nutzt weiterhin avaje-configs eingebaute Dateiüberwachung. Die Classpath-`application.yaml` setzt `config.watch.enabled: false` sowie `config.watch.delay: 10` und `config.watch.period: 10` (Sekunden), kommentiert mit einem Verweis auf diese Entscheidung. Der Betreiber schaltet die Überwachung in seiner eigenen Datei mit `config.watch.enabled: true` ein. Dann ist garantiert, dass es eine Datei zum Überwachen gibt: Ohne Datei im Dateisystem bekäme avajes `FileWatch` eine leere Liste und loggte bei jedem Start `No files to watch?` auf ERROR (bei gesetztem `TITAN_SENTRY_DSN` auch in Sentry). Das ist beim erlaubten Betrieb nur mit Standardwerten der Fall.

Es gibt aber keinen Handler mehr, der auf `Config.onChange` reagiert. Jede Stelle, die eine Einstellung braucht, liest sie bei Gebrauch direkt über die statische `Config`-Fassade (Lesepunkte: siehe `proposal.md`; Entscheidung 2 dieses Dokuments dazu, dass dabei nicht mehr geprüft wird). Ändert avajes Dateiüberwachung nach einer Dateiänderung die Werte hinter der Fassade, sieht der nächste Lesevorgang automatisch den neuen Wert — ohne Modulneustart, ohne dass irgendein Modulzustand verloren geht.

**Built-in first:** Zwei verworfene Alternativen:
- **Erste Fassung dieser Change — ein eigener Loader:** bei jedem Neuladen eine frische `Configuration.builder().includeResourceLoading().build()`, ein eigener Diff, ein eigener Datei-Watcher und ein Befehl. Er hätte Overrides, gelöschte Schlüssel, neue Dateien und Alles-oder-nichts-Anwendung sauberer gelöst, aber etwa die Hälfte des Neulade-Codes zusätzlich gekostet, dazu einen Befehl samt i18n.
- **Zweite Fassung dieser Change — `Config.onChange` mit Modulneustart:** ein `ConfigChangeHandler`, der Schlüssel auf Module abbildete und das betroffene Modul über `ModuleRegistry#restart` neu startete, mit Schnappschuss und `eventBuilder("reload-revert")` als Rückfall bei einem fehlgeschlagenen Neustart. Verworfen, weil ein Neustart Modulzustand verliert (ein sitzender Spieler steht auf, ein laufender Elytra-Flug bricht ab) und zusätzlichen Code für Schnappschuss und Revert brauchte. Live-Lesen bei Gebrauch ist einfacher, verliert keinen Zustand und kommt ohne einen zweiten Mechanismus (Revert) aus.

Der Maintainer nimmt die im Kontext genannten Grenzen der eingebauten Dateiüberwachung bewusst in Kauf (siehe proposal.md, Grenzen). Für das Neuladen selbst bleibt damit kein eigener Code übrig: avaje-config liefert Dateiüberwachung und Fassade, der Rest ist gewöhnlicher, unvalidierter Lesezugriff (Entscheidung 2).

**Test:**
- Integration (Kind-JVM) mit `config.watch.delay: 1` und `config.watch.period: 1`: Eine überwachte Datei wird geändert, und der Test wartet mit Timeout auf den neuen Wert über die Fassade (Bedingungs-Wartung, für Integrationstests erlaubt; kein `Thread.sleep`; angepasst aus `ConfigChangeFileWatchIntegrationTest`).

**SOLID:** DIP (jede lesende Stelle hängt nur von der `Config`-Fassade ab, nicht von einem Reload-Mechanismus). KISS/YAGNI: kein Handler, keine Schlüssel-zu-Modul-Abbildung, kein Schnappschuss, kein Revert.

### 2. Prüfung nur beim Start

**Entscheidung:** Konfigurationswerte werden ausschließlich beim Start geprüft, in `enable()` der jeweiligen `*Settings`-Klassen (unverändertes Verhalten aus `avaje-config-facade`): Eine reine Prüffunktion (Cooldown >= 0, `min < max` usw.) wirft bei einem ungültigen Wert eine `ConfigException`, die den Start abbricht. Zur Laufzeit liest jede Stelle ihren Wert direkt über `Config.<methode>` an der Nutzungsstelle — ohne erneute Prüfung und ohne Rückfall. Ein Wert, der zur Laufzeit ungültig, aber parsbar wird (z. B. `tickle.cooldownMillis: -5`), wirkt dann so, wie die einfache Verwendung dieses Werts es ergibt (ein negativer Cooldown wirkt wie kein Cooldown); ein Wert, der sich nicht parsen lässt, oder ein kaputter Navigator-Eintrag, lässt genau diese Aktion mit einer Exception fehlschlagen, die Minestoms bestehender Exception-Handler protokolliert — die Lobby läuft weiter.

Der Maintainer entscheidet sich bewusst gegen jede Form von Laufzeitvalidierung: Live-Lesen über die Fassade ist der einzige Mechanismus, den diese Change einführt; ein zusätzlicher Prüf- und Rückfallpfad wäre zusätzlicher Code für ein Bedienfehler-Szenario, das der Betreiber selbst vermeiden kann. Der Betreiber ist für gültige Werte verantwortlich; die README beschreibt, eine Änderung vor dem Speichern in der Produktion zu prüfen (z. B. Neustart in einer Testumgebung).

**Verworfene Alternativen:**
- **Rückfall auf den Classpath-Standardwert über einen gemeinsamen Helfer (`RuntimeConfigFallback`):** hätte die Classpath-Standardwerte einmalig geladen und bei einem ungültigen Laufzeitwert dorthin zurückfallen lassen, mit einer je Schlüssel und Wert deduplizierten WARN-Zeile. Verworfen, weil er zusätzlichen, nur für dieses Szenario existierenden Code gebraucht hätte (Laden, Dedupe-Zustand, ein Zugriff je Modul) für ein Bedienfehler-Szenario, das die README stattdessen durch Vorsicht beim Speichern vermeidet.
- **Prüfen und bei einem ungültigen Wert überspringen, nur warnen:** hätte denselben Dedupe-Zustand gebraucht wie der Rückfall, ohne dessen Vorteil (ein nutzbarer Wert). Aus demselben Grund verworfen.
- **Feste Code-Konstanten als Rückfallwert statt der Classpath-Datei:** hätte den Standardwert an zwei Stellen gepflegt (Code und `application.yaml`); sobald der Rückfall selbst verworfen war, entfiel auch diese Variante.

**Performance:** Jeder Lesepunkt macht eine Kartenabfrage über `Config.get*` — für tickle (je Angriff), sit (je Blockinteraktion), elytra (je Boost) und spawn (je Höhenprüfung bzw. je Beitritt) unauffällig. Der Navigator parst `entries` bei jedem Öffnen neu, statt sie einmal beim Start zu parsen; das ist je Öffnen, nicht je Tick, und bleibt akzeptabel (siehe Risks).

**Built-in first:** Nicht zutreffend — der Verzicht auf Laufzeitvalidierung ist eine Entscheidung gegen zusätzlichen Code, nicht die Wahl eines Bausteins.

**Test:**
- Unit je Modul-Zugriff (tickle, sit, elytra, spawn, Navigator): ein gültiger Wert wird durchgereicht. Es gibt keinen Test für einen Rückfall, weil es keinen gibt; ein Test belegt stattdessen, dass ein ungültiger, aber parsbarer Wert unverändert (unvalidiert) durchgereicht wird bzw. dass ein nicht parsbarer Wert die erwartete Exception auslöst.

**SOLID:** SRP (Lesen bleibt eine einfache Fassadenabfrage, ohne Prüf- oder Rückfalllogik am Lesepunkt), YAGNI (kein Code für ein Szenario, das der Betreiber durch Vorsicht beim Speichern vermeidet).

### 3. Feature-Flags aus der Konfiguration

**Entscheidung:** `ConfigFeatureFlags implements FeatureFlags` ersetzt `TogglzFeatureFlags`.
- `isActive(name)` liefert `Config.getBool("features." + name, false)`. Das ist eine Map-Abfrage und für jedes Öffnen des Navigators billig genug.
- `exists(name)` prüft gegen die bekannten Flags. Das sind die Schlüssel unter `features.` in der **Classpath**-`application.yaml`, die beim Start einmal als eigene Instanz geladen wird. So macht ein Tippfehler in der Betreiber-Datei eine Flag nicht „bekannt“.
- Für Tests nimmt der Konstruktor die Menge der bekannten Flags und eine Abfragefunktion `String -> boolean`. Eine statische Fabrik verdrahtet beides produktiv mit der Fassade.

Die Flag-Namen bleiben in Großbuchstaben (`NAVIGATOR_SLENDER`), damit Navigator-Einträge (`feature: NAVIGATOR_SLENDER`) unverändert bleiben. Die Env-Variable folgt der Abbildung von avaje-config: `FEATURES_NAVIGATOR_SLENDER`.

Die Classpath-`application.yaml` bekommt den Abschnitt `features` mit `NAVIGATOR_CREATIVE`, `NAVIGATOR_SLENDER`, `NAVIGATOR_MANIS`, `NAVIGATOR_SURVIVAL` und `NAVIGATOR_ELYTRA`, alle `false`. `RESOURCE_PACK_REQUIRED_KICK` wird nicht übernommen, weil kein Code es nutzt.

`features.*` gehört zu keinem Modul. Ändert sich eine Flag, startet nichts neu. Der Navigator wertet sie beim nächsten Öffnen aus (bestehendes Verhalten von `NavigatorInventory`).

Entfernt werden `TitanFeatures`, `TogglzFeatureFlags`, `SingletonFeatureManagerProvider`, `ThreadHelper`, die SPI-Datei `META-INF/services/org.togglz.core.spi.FeatureManagerProvider`, die Togglz-Abhängigkeiten in Versionskatalog und Build und `flags.properties` im Repo. Die lokale `flags.properties` mit allem auf `true` wird zu einer `application-local.yaml`-Vorlage, die in der README beschrieben ist.

**Built-in first:** Togglz ist die heutige eingebaute Lösung und wird bewusst ersetzt. Es bringt eine zweite Konfigurationsquelle ohne Profile und Env mit, und der Classloader-Workaround (`ThreadHelper`) wird mit ihm überflüssig. Die gebrauchten Funktionen (Wahrheitswert pro Name, zur Laufzeit änderbar) deckt avaje-config mit `getBool` und der Dateiüberwachung ab. Strategien pro Spieler werden nicht genutzt.

**Test:**
- Unit auf `ConfigFeatureFlags` mit Fakes: bekannt und an, bekannt und aus, unbekannt (`exists` ist false), fehlender Wert ergibt aus.
- Unit: Die Classpath-Datei enthält alle Flags, die ein Standard-Navigator-Eintrag nutzt.
- Integration (Kind-JVM): `FEATURES_NAVIGATOR_SLENDER=true` als Env schaltet die Flag an, und eine übrig gebliebene `flags.properties` bleibt wirkungslos.
- Die bestehenden Navigator-Tests mit `FakeFeatureFlags` bleiben unverändert.

**SOLID:** DIP (Navigator und Registry hängen weiter nur von `FeatureFlags` ab), OCP (neue Flags sind reine Konfiguration plus ein Eintrag in der Classpath-Datei).

### 4. Keine Metriken und Spans

**Entscheidung:** Ein ungültiger Laufzeitwert ist ein seltenes Bedienfehler-Ereignis und kein Hot Path. Ohne Laufzeitvalidierung (Entscheidung 2) gibt es dafür keine eigene Log-Zeile mehr, die eine Metrik oder Span ergänzen könnte: Ein nicht parsbarer Wert oder ein kaputter Navigator-Eintrag zeigt sich als Exception an der Nutzungsstelle, die Minestoms bestehender Exception-Handler protokolliert. Eine zusätzliche Metrik würde nur dasselbe seltene Ereignis zählen, eine Span hätte keinen Aufrufer, der Kontext weiterträgt. Deshalb kommt keins von beiden dazu.

**Test:** Nicht zutreffend — es gibt keine eigene Log-Zeile, die geprüft werden müsste; die Exception aus einer fehlgeschlagenen Aktion deckt der bestehende Minestom-Exception-Handler ab.

**SOLID:** Nicht zutreffend.

## Risks / Trade-offs

- **[Risiko] Restfelder, die eine Einstellung beim `enable()` einmalig in ein Objekt kopieren, statt sie bei Gebrauch live zu lesen.** Ein Modul, das (versehentlich oder aus einer früheren Fassung) einen Wert nur beim Start in ein langlebiges Feld schreibt, ändert sich dann nicht mehr live. → Jeder Lesepunkt aus proposal.md ruft die Fassade bei Gebrauch auf; ein Review prüft neue und bestehende Modul-Zugriffe explizit darauf.
- **[Trade-off] Neuladen ist opt-in.** Ohne `config.watch.enabled: true` in der Betreiber-Datei braucht jede Änderung einen Neustart. → README und die Deployment-Vorlagen nennen den Schalter.
- **[Risiko] Ein ungültiger Laufzeitwert wirkt unvalidiert oder lässt eine Aktion fehlschlagen.** Ohne Laufzeitvalidierung (Entscheidung 2) wirkt ein ungültiger, aber parsbarer Wert so, wie die einfache Verwendung ihn ergibt (z. B. `tickle.cooldownMillis: -5` wie kein Cooldown); ein nicht parsbarer Wert oder ein kaputter Navigator-Eintrag lässt die betroffene Aktion mit einer Exception fehlschlagen, die Minestoms Exception-Handler protokolliert, während die Lobby weiterläuft. → Der Betreiber prüft eine Änderung vor dem Speichern in der Produktion (z. B. Neustart in einer Testumgebung); die README beschreibt das.
- **[Risiko] Overrides werden bei einer Dateiänderung verdrängt.** Ändert sich eine Datei, die einen per Env oder System-Property gesetzten Schlüssel enthält, gilt bis zum nächsten Neustart der Dateiwert (siehe Kontext). → Betreiber setzen Modul-Schlüssel nicht per Env in Umgebungen, in denen Dateien zur Laufzeit bearbeitet werden; ein Neustart der Lobby stellt den Override wieder her; die README dokumentiert das Verhalten.
- **[Trade-off] Keine sofortige Übernahme, kein Befehl.** Eine Änderung wirkt erst nach bis zu `config.watch.delay` plus `config.watch.period`. Es gibt keinen Weg, das von Hand zu beschleunigen.
- **[Trade-off] Der Navigator parst `entries` bei jedem Öffnen neu**, statt sie einmal beim Start zu parsen (siehe Entscheidung 2, Performance). → Das passiert je Öffnen, nicht je Tick, und bleibt bei der erwarteten Navigator-Größe unauffällig.
- **[Risiko, BREAKING] Flags aus `flags.properties` gehen verloren**, wenn das Deployment nicht umgestellt wird. Alle Flags sind dann aus, der Navigator zeigt also nur ungebundene Ziele. → `BREAKING CHANGE`-Footer, eine Umstellungstabelle in der README und eine Kind-JVM-Prüfung, dass `flags.properties` wirkungslos ist.

## Migration Plan

1. Voraussetzung: `avaje-config-facade` ist gemergt. Diese Change zweigt danach von `origin/main` ab.
2. Vor dem Update im CloudNet-Template bzw. im Deployment:
   - jede Zeile `NAME=true` aus `flags.properties` als `features.NAME: true` in `application.yaml` oder als Env-Variable `FEATURES_NAME=true` übertragen,
   - `flags.properties` entfernen.
3. Ausrollen. Die Log-Zeile beim Start zeigt die aktiven Profile. Eine Probe-Änderung an einer überwachten Datei (z. B. `tickle.cooldownMillis`) bestätigt spätestens nach `config.watch.delay`/`config.watch.period`, dass der neue Wert beim nächsten Lesepunkt gilt (z. B. beim nächsten Tickle-Angriff) — ohne dass im Log ein Neustart erscheint.
4. **Rollback:** das vorherige Jar zurück und `flags.properties` wiederherstellen. `features.*` in `application.yaml` stört das alte Jar nicht.
