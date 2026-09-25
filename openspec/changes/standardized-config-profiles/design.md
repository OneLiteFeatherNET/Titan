# Design

## Context

Den Anlass beschreibt `proposal.md` (Why), die Anforderungen stehen in `specs/lobby-module-config/spec.md` und `specs/lobby-navigator/spec.md`. Diese Change baut auf `avaje-dependency-injection` auf (Avaje Inject und `PlatformBeans`).

Der Stand heute:
- **`common/.../config/ConfigStore`** hat mehrere Aufgaben:
  - `open(Path)` liest `app.json` und migriert v1 → v2 mit `.bak`.
  - `section(id, type, defaults)` führt den Abschnitt tief mit den Standardwerten zusammen, deserialisiert ihn per Gson in den Record, packt eine `ConfigException` aus dem Compact Constructor aus, meldet falsche Typen mit Feld und warnt bei unbekannten Schlüsseln.
  - `set`/`setSection`/`save` schreiben atomar zurück.
- **Nutzer:**
  - `ModuleContext.config(...)`, gebaut von `ModuleRegistry`/`Titan`,
  - fünf Module über `ctx.config(XConfig.class, XConfig.DEFAULTS)`,
  - der Setup-Server über `SetupConfigEditor`, `AppCommand` und `*SectionConfig`.
- **`NavigatorConfig(String title, List<Entry> entries)`**: Die Einträge sind eine Liste ohne Namen.
- **Feature-Flags** laufen getrennt über Togglz (`flags.properties`) und sind hier nicht betroffen.

## Goals / Non-Goals

**Goals:**
- Konfiguration aus Standardquellen: YAML, Profile, Env-Variablen und System-Properties, mit der Rangfolge aus der Spec.
- Die API für Module (`ctx.config(Typ, DEFAULTS)`) bleibt unverändert, Features merken vom Wechsel nichts.
- Der getestete Binding-Kern bleibt erhalten (Standardwerte, Validierung, Meldung falscher Typen, Warnung bei unbekannten Schlüsseln).

**Non-Goals:**
- Feature-Flags nach avaje-config holen (späterer Schritt).
- Config-Records per Konstruktor injizieren (Option B, späterer Schritt).
- Neuladen zur Laufzeit per `onChange`. Die Module lesen ihre Config in `enable()`. Neu laden würde bedeuten, sie neu zu starten, das ist eine eigene Change.
- Die Map-Daten des Setup-Servers (`map.json`) umstellen.

## Decisions

### 1. avaje-config als Quelle, als `Configuration`-Instanz statt statischem `Config`

**Entscheidung:** `io.avaje:avaje-config` (Version aus dem Spike, Task 1.1) in `common`. `PlatformBeans` stellt einen `io.avaje.config.Configuration`-Bean bereit, der aus avaje-config gebaut ist (Arbeitsverzeichnis, Profile über `AVAJE_PROFILES`/`avaje.profiles`, `PROPS_FILE`, Env-Variablen und System-Properties). Plattform-Code nutzt diese Instanz, nie die statische `Config`-Fassade. So bleibt die Config testbar (in Tests wird eine `Configuration` aus einer Map gebaut) und frei von globalem Zustand (F.I.R.S.T.).

**Built-in first:** Die eingebaute Alternative wäre `java.util.Properties` plus eigene Logik für Profile und Overrides. Verworfen, weil das genau die Infrastruktur ist, die avaje-config fertig und getestet liefert (Profile, Rangfolge der Quellen, Abbildung auf Env-Variablen). SmallRye und Configurate sind in `proposal.md` begründet verworfen.

**Test:** Unit-Tests mit einer `Configuration` aus einer Map. Ein Integrationstest mit echten Dateien und Profilen in `@TempDir` sichert die Rangfolge aus der Spec ab.

**SOLID:** DIP, weil der Binder von der `Configuration`-Abstraktion abhängt und nicht von Dateien.

### 2. `ConfigSections`: Den Binding-Kern aus `ConfigStore` weiterverwenden

**Entscheidung:** Der neue `common/.../config/ConfigSections` bekommt eine `Configuration` und bietet `<R extends Record> R section(String id, Class<R> type, R defaults)`. Intern passiert Folgendes:
1. Die flachen Schlüssel unter `id.` werden aus `Configuration.forPath(id)` bzw. aus der Liste der Schlüssel gelesen.
2. Daraus wird ein `JsonObject`-Baum gebaut: `offset.x` → `{offset:{x:…}}`, und Listen werden anhand des Typs der Record-Komponente gespalten.
3. Dieser Baum geht **durch den bestehenden, getesteten Kern** aus `ConfigStore.section`: Zusammenführen mit den Standardwerten, Gson, Auspacken der `ConfigException`, Meldung falscher Typen, Warnung bei unbekannten Schlüsseln.

Der Kern wird dafür aus `ConfigStore` in eine package-private Klasse `SectionBinder` herausgezogen. `ConfigStore` selbst (Datei, Speichern, `set`) entfällt.

`ModuleContext.config(...)` delegiert an `ConfigSections`. Die API für Module bleibt gleich.

**Built-in first:** avaje-config hat kein Binding an Records (laut Faktenprüfung nur einzelne Getter und `forPath`). Das eigene Binding existiert schon und ist getestet, deshalb wird es weiterverwendet (DRY) und nicht neu geschrieben.

**Test:** Unit-Tests zuerst, mit einer `Configuration` aus einer Map:
- fehlender Abschnitt oder Einzelwert ergibt den Standardwert,
- verschachtelter Record (`offset`),
- Liste von Strings (`allowedBlocks`),
- Map von Records (Navigator-Einträge),
- falscher Typ aus einem Override meldet Modul und Feld,
- Warnung bei unbekannten Schlüsseln, einmal pro Abschnitt (per abgefangenem Appender).

Die bestehenden Tests für den Binding-Kern ziehen mit um.

**SOLID:** SRP. Die Quelle (avaje-config) und das Binding (`SectionBinder`) sind getrennt.

### 3. Navigator-Einträge als Map mit Namen

**Entscheidung:** `NavigatorConfig(String title, Map<String, Entry> entries)`. Der Schlüssel ist der Name des Ziels (`elytrarace`, `survival`, `slender`, `creative`). Damit kann ein Profil bzw. eine Env-Variable genau einen Eintrag ändern (`navigator.entries.survival.destination`), wie es die Spec verlangt. Die Reihenfolge ergibt sich aus `slot`, also zählt die Reihenfolge der Map nicht. Die Standardwerte bleiben inhaltlich gleich. Fehlermeldungen nennen weiter `navigator.entries`, bei Bedarf mit dem Namen.

**Built-in first:** YAML-Maps werden in avaje-config zu Schlüsseln mit Punkten. Eine Liste von Objekten ließe sich über flache Schlüssel nicht einzeln überschreiben.

**Test:** Unit-Test auf `NavigatorConfig` (Standardwerte, Validierung), und ein Integrationstest „Profil ändert ein einzelnes Ziel“.

**SOLID:** OCP. Neue Ziele kommen über die Config dazu.

### 4. Einmalige Umstellung von `app.json` beim Start

**Entscheidung:** `common/.../config/AppJsonMigration` läuft in der Bootstrap-Phase, **bevor** die `Configuration` gebaut wird:
- **`app.json` vorhanden, keine `application.yaml`:**
  1. Einlesen (v1 flach über die bestehende `LegacyConfigMigration`-Abbildung nach v2, v2 direkt).
  2. Verworfene Schlüssel im Log nennen (WARN).
  3. `application.yaml` schreiben.
  4. `app.json` in `app.json.migrated` umbenennen.
  5. Eine WARN-Zeile zur Umstellung ausgeben.
- **Beide vorhanden:** `app.json` bleibt unangetastet, eine WARN-Zeile meldet „wird ignoriert“.
- **Keine von beiden:** Es passiert nichts.
- **Syntaktisch kaputte `app.json`:** Der Start bricht mit Datei und Stelle des Fehlers ab, nichts wird umbenannt.

Die YAML-Ausgabe übernimmt ein kleines, getestetes Schreibwerkzeug für den JSON-Baum (Maps, Listen, Skalare, Strings in Anführungszeichen). Hat der Spike gezeigt, dass avaje-config für YAML SnakeYAML ohnehin im Klassenpfad hat, nutzen wir dessen `Dump` (Task 1.2).

**Built-in first:** Java bringt `java.util.Properties#store` mit, das würde aber `application.properties` statt YAML ergeben. Die Spec verlangt YAML, und Betreiber sollen ein einziges Format sehen. Eine neue Abhängigkeit (SnakeYAML) nur für eine einmalige Umstellung wird nur akzeptiert, wenn sie ohnehin da ist.

**Test:** Unit-Tests mit `@TempDir`: v1 (echte alte `app.json` als Fixture), v2, beide Dateien vorhanden, keine Datei, kaputtes JSON. Außerdem ein Rundlauf: die geschriebene YAML durch `ConfigSections` lesen und dieselben Records erhalten.

**SOLID:** SRP. Die Umstellung ist von Laden und Binden getrennt und läuft genau einmal.

### 5. Setup-Server nur noch für Map-Daten

**Entscheidung:** `AppCommand`, `SetupConfigEditor` und `*SectionConfig` samt Tests werden entfernt, der `app`-Zweig von `SetupCommand` ebenso. Der Setup-Server liest nur noch `spawn.simulationDistance` für seinen `PlayerSpawnListener`, und zwar über dieselbe `Configuration`/`ConfigSections` (Standardwert 2). Er schreibt keine Config mehr.

**Built-in first:** Nicht anwendbar, Code wird entfernt.

**Test:** Die bestehenden Setup-Tests bleiben für die Map-Befehle erhalten. Ein Unit-Test prüft, dass die Simulationsdistanz aus der Config gelesen wird.

**SOLID:** SRP. Der Setup-Server ist nur für Map-Daten zuständig.

### 6. Schlüssel, Env-Variablen und Beispiel-Datei

**Entscheidung:** Die Schlüssel in YAML heißen wie die Felder der Records (`cooldownMillis`, `minHeight`), also genau wie heute in `app.json`. Das hält die Umstellung verlustfrei.

Die Abbildung auf Env-Variablen folgt avaje-config (Großbuchstaben, `.` wird zu `_`, z.B. `TICKLE_COOLDOWNMILLIS`). Der Spike (Task 1.3) bestätigt die genaue Regel und die Rangfolge. Weicht avaje-config von der Rangfolge aus der Spec ab, gleicht eine dünne Schicht mit Overrides in `PlatformBeans` das aus (System-Properties und Env-Variablen explizit zuletzt anwenden).

Mit der Distribution (`app/src/dist` bzw. README) wird eine kommentierte `application.example.yaml` mitgeliefert. Das Log listet beim Start die aktiven Profile auf INFO: `Active configuration profiles: {}`.

**Test:** Ein Integrationstest der Rangfolge mit `@TempDir`, Profil-Datei, gesetzter System-Property und einer Env-Variable, die über einen injizierten Env-Provider simuliert wird statt über echte Prozess-Env (F.I.R.S.T. Repeatable). Ob avaje-config einen solchen Provider anbietet, klärt Task 1.3. Sonst wird die Env-Rangfolge im E2E-Smoke-Test (Task 5.1) mit echter Env geprüft. Die Log-Zeile wird per abgefangenem Appender geprüft.

**SOLID:** Nicht zutreffend (Konvention).

## Risks / Trade-offs

- **[Risiko] Die Rangfolge von avaje-config weicht von der Spec ab**, zum Beispiel wenn Env-Variablen nur greifen, wenn ein Schlüssel fehlt. → Der Spike (Task 1.3) prüft das zuerst. Die Ausgleichsschicht ist in Entscheidung 6 vorgesehen.
- **[Risiko] Listen und Maps gehen beim Abflachen verloren**, etwa bei Komma-Listen oder Strings mit Kommas. → Der Spike prüft Listen und Maps, und die Unit-Tests des Binders decken sie ab. Bei Bedarf wird die YAML-Listensyntax direkt gelesen.
- **[Risiko] Umbenannte Env-Variablen**, weil camelCase-Schlüssel wie `TICKLE_COOLDOWNMILLIS` schwer lesbar sind. → Das ist bewusst hingenommen, zugunsten verlustfreier Umstellung und gleicher Namen in Code und Datei. Die README enthält eine Tabelle aller Env-Namen.
- **[Risiko] Rollback nach der Umstellung:** Ein altes Jar findet nur `app.json.migrated`. → Die Migration-Hinweise sagen: vor dem Rollback `app.json.migrated` zurück nach `app.json` umbenennen. Die Umstellung ist verlustfrei und umkehrbar.
- **[Trade-off] Kein Neuladen zur Laufzeit.** Änderungen wirken erst nach einem Neustart, wie heute.
- **[Trade-off] Neue Abhängigkeit** `avaje-config`: ohne weitere Abhängigkeiten, aus der Avaje-Familie. Ob sie SnakeYAML für YAML braucht, klärt der Spike.

## Migration Plan

1. Das neue Jar ausrollen. Beim ersten Start wird `app.json` zu `application.yaml` plus `app.json.migrated`, das Log meldet es.
2. CloudNet-Template bzw. Deployment umstellen: `application.yaml` (bzw. per `PROPS_FILE`) ausliefern und `AVAJE_PROFILES` setzen. Ist das erledigt, kann `app.json` im Template entfallen.
3. Den Setup-Server gemeinsam mit der Lobby ausrollen, weil die `/setup app`-Befehle wegfallen.
4. **Rollback:** Das alte Jar zurück und `app.json.migrated` → `app.json` umbenennen.

## Open Questions

- Die exakte avaje-config-Version und ob YAML SnakeYAML braucht: Das legt Task 1 fest und trägt es hier nach. Ansatz und Tasks ändern sich dadurch nicht, weil beide Varianten in Entscheidung 4 vorgesehen sind.
