# Proposal

## Why

Titans Konfiguration ist heute ein eigenes Format: eine `app.json` mit einem Abschnitt pro Modul, ein selbst gebauter `ConfigStore`, und der Setup-Server schreibt einzelne Werte zurück. Das funktioniert, ist aber kein Standard. Es kennt weder Umgebungen noch Profile und ist auf den heutigen Betrieb unter CloudNet zugeschnitten.

Ziel ist eine standardisierte Konfiguration wie in Spring Boot. Die Basis steht in `application.yaml`, Profile wie `dev` oder `prod` überschreiben sie, und einzelne Werte lassen sich per Env-Variable oder System-Property setzen. Die Lobby läuft damit unverändert unter CloudNet, Docker, Kubernetes oder lokal, und ein späterer Abschied von CloudNet hängt nicht an der Konfiguration.

Als Framework wird **avaje-config** eingesetzt. Es bringt Profile, mehrere geschichtete Quellen, Env- und `-D`-Overrides, externe Dateien und `onChange` mit, arbeitet ohne Reflection und ohne Abhängigkeiten (passt zum AOT-Cache) und lässt sich an Avaje Inject anbinden (`@Profile`, `@RequiresProperty`). SmallRye Config und Configurate wurden geprüft und verworfen:
- SmallRye erzeugt zur Laufzeit Klassen, bringt Hibernate Validator mit und folgt Quarkus-Konventionen.
- Configurate ist auf Minecraft zugeschnitten und kennt weder Profile noch Env-Overrides.

**Auslieferung:** `feat(config)!: load lobby configuration from application.yaml with profiles`. Der PR trägt diesen Footer:

`BREAKING CHANGE: app.json is replaced by application.yaml plus optional application-<profile>.yaml files; values can be overridden via environment variables and system properties; the setup server no longer edits configuration (/setup app commands removed).`

## What Changes

- **Quellen und Profile:**
  - `application.yaml` (Basis) und `application-<profil>.yaml`; das aktive Profil wird per `AVAJE_PROFILES` bzw. `-Davaje.profiles` gewählt.
  - Einzelne Werte werden per Env-Variable (`SIT_OFFSET_Y`) oder System-Property überschrieben.
  - Eine externe Datei lässt sich per `PROPS_FILE` einbinden, z.B. für eine Kubernetes-ConfigMap oder ein CloudNet-Template.
- **Pro Modul bleibt ein Abschnitt:** Jedes Modul liest weiterhin seinen Abschnitt (`sit`, `spawn`, …) über `ctx.config(Typ.class, DEFAULTS)` als Record. Eine dünne eigene Schicht bindet einen Abschnitt aus avaje-config an den Record, inklusive Standardwerten und Validierung im Compact Constructor wie heute. Ungültige Werte brechen den Start weiterhin mit Modul, Feld und Grund ab.
- **Nur noch lesen:** Die Lobby schreibt keine Konfiguration mehr. Standardwerte leben in den Config-Records bzw. einer mitgelieferten Beispiel-`application.yaml`, es wird nichts automatisch angelegt.
- **Setup-Server nur noch für Map-Daten:** Die Befehle `/setup app …` und der `SetupConfigEditor` entfallen. Konfiguration wird in YAML gepflegt und versioniert.
- **Umstieg von `app.json`:** Eine vorhandene `app.json`, ob im flachen Format (v1) oder mit Abschnitten (v2), wird einmalig in eine `application.yaml` übernommen. Ob das beim Start oder mit einem eigenen Werkzeug geschieht, entscheidet das Design.
- **Entfällt:**
  - `ConfigStore`, samt Speichern, `set`/`setSection` und atomarem Schreiben,
  - die JSON-Variante der Config,
  - die Warnlogik für unbekannte Schlüssel, soweit avaje-config sie ersetzt. Ob sie ersetzt wird, klärt das Design.
- **Später möglich, nicht Teil dieser Change:**
  - Feature-Flags wie `NAVIGATOR_SLENDER` aus `flags.properties` in die Profile holen (`features.*`),
  - Config-Records per Konstruktor injizieren (Micronaut-Stil) über eine generische `@Factory`.

## Capabilities

### New Capabilities
<!-- keine -->

### Modified Capabilities
- `lobby-module-config`: Quelle ist nicht mehr `app.json`, sondern `application.yaml` plus Profile und Overrides per Env und System-Property. Dadurch ändern sich drei Anforderungen:
  - „Fehlende Konfigurationsdatei wird angelegt“ weicht dem Satz „Standardwerte gelten ohne Datei“.
  - Die Migration des flachen Formats wird zur Umstellung von `app.json` auf YAML.
  - „Änderungen aus dem Setup-Server verlieren keine Werte“ entfällt ersatzlos.

  Dazu kommen neue Anforderungen für die Aktivierung von Profilen und die Rangfolge der Overrides.
- `lobby-navigator`: Navigator-Ziele bekommen einen eindeutigen Namen, über den ein Profil ein einzelnes Ziel ändern kann. Das Szenario zur unbekannten Flag verweist künftig auf `application.yaml` statt auf `app.json`.

## Impact

- **Code:**
  - `common/config`: `ConfigStore`, die Migration und die Tests werden ersetzt bzw. umgebaut, der Binder für Abschnitte kommt neu dazu.
  - `app`: Die Plattform (`ModuleContext.config`) bleibt als API gleich, nur die Implementierung dahinter ändert sich. `Titan`/Bootstrap lädt avaje-config.
  - `setup`: `AppCommand`, `SetupConfigEditor` und die `*SectionConfig`-Records werden entfernt.
- **Neue Abhängigkeiten:**
  - Laufzeit: `io.avaje:avaje-config` (5.x) und dessen YAML-Unterstützung (im Design klären, ob ein eigener YAML-Parser nötig ist).
  - Gson entfällt für die Config, wird aber anderswo weiter genutzt.
- **Betrieb (BREAKING):**
  - Aus `app.json` wird `application.yaml`.
  - Deployments (CloudNet-Template, später Docker/K8s) liefern die YAML-Datei bzw. `PROPS_FILE` aus und setzen `AVAJE_PROFILES`.
  - README und Rollout-Hinweise werden aktualisiert.
- **Texte für Nutzer:** Die Setup-Befehle `/setup app …` fallen weg. Die Hilfe- und Fehlermeldungen im Setup-Server passen sich entsprechend an.
- **Abhängigkeit zwischen Changes:** Baut auf `avaje-dependency-injection` auf. avaje-config wird dann über Avaje Inject bereitgestellt.
