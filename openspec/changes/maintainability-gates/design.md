# Design

## Context

Anforderungen stehen in `specs/maintainability/spec.md`; warum, in `proposal.md`. Dieser Abschnitt hält den Ist-Stand fest, auf dem die Schwellen und Baselines beruhen. Gemessen am Stand von `origin/main` (Commit `03c09ae`), per `grep` und einem kleinen Skript; die Werte werden in Task 1.x mit den Gradle-Reports nachgeprüft.

| Prüfpunkt | Ist-Stand | Erzwungen? |
| --- | --- | --- |
| Column → Column-Import | 0 Treffer über `features/*/src/main` | Nein. Im Build wäre `implementation(project(":features:x"))` möglich |
| Column-Build-Abhängigkeiten | nur `core`, außer `features/jumprun` → `persistence` (8 Dateien importieren es) | Nein. `titan.column` schreibt `core` vor, verbietet aber nichts |
| Plattform/common/runtime/core → Feature | 0 Treffer | Nein |
| Logger | 42 Produktionsdateien mit `LoggerFactory`, als `static final` deklariert. Zu prüfen: `bridge/.../ServiceReadings.java:36` (Instanzfeld), `bridge/.../TitanBridgePermissionExtension.java:103` (`System.getLogger`) | Nein, nur Konvention |
| `System.out`/`System.err` in `src/main` | 0 | Nein |
| `printStackTrace()` im Code | 0 (ein Treffer nur im Javadoc von `TitanObservability`) | Nein |
| Reflexion (`setAccessible`) | 2 Stellen, beide in `core/src/testFixtures/.../EventListenerCounter.java` | Nein |
| Systemuhr in `src/main` (`Instant.now()`, `currentTimeMillis()`, `LocalDate(Time).now()`) | 0; 53 Dateien nutzen `Clock` | Nein, nur Konvention |
| `Thread.sleep` | 1 in `src/main` (`runtime/.../TitanApplication.java:81`, AOT-Trainingslauf), 0 in Tests | Nein |
| Architekturtests | 16 `ColumnArchitectureTest` (je 3 der 6 Regeln aus `ColumnArchitectureRules`), je 1 Telemetrie-Test in `core` und `apps/cloudnet`. Module wie `setup`, `bridge`, `common`, `runtime`, `persistence`, `platform/*` haben keinen | Nur für Columns, ohne Guard |
| Formatierung, Lizenzkopf, ungenutzte Imports | Spotless in allen Subprojekten; `spotlessCheck` hängt an `check` (Standardverhalten des Plugins, in Task 1.1 bestätigen) | Ja, bleibt unverändert |
| Javadoc | keine Prüfung. `core`: 50 öffentliche Top-Level-Typen, 3 ohne Javadoc (`DeliverComponent`, `DeliverType`, `Cancelable`); Methoden nicht gemessen | Nein |
| Compiler-Warnungen | `-Xlint:deprecation,unchecked`, nicht `-Werror` | Nein |
| Größe | 412 Produktionsdateien; größte: `JumprunModule` 606 Zeilen, `NavigatorModule` 522, `CourseGenerator` 402, `PortalEditor` 401 | Nein |
| Paketzyklen | nicht gemessen (Task 1.2) | Nein |
| JaCoCo | Bericht, keine Schwelle | Nein (Non-Goal) |
| CI | `build-pr.yml` ruft den Reusable-Workflow `gradle-build-pr.yml@v2.9.0` auf (Standard `build test` auf ubuntu, windows, macos). `build` hängt an `check`, die neuen Gates laufen also dort mit | – |
| Release | `release-type: simple`, keine `changelog-sections`; `build:` erscheint nicht im `CHANGELOG.md` | – |

Die bestehende Column-Regel "hängt nur von `core` ab" steht im Kommentar von `titan.column` und in `docs/lobby-modules.md`, aber nicht im Build. Die Anforderung "Module sind voneinander unabhängig" (`lobby-modules`) ist damit heute nur durch Disziplin und Kompilierfehler gedeckt, nicht durch eine Prüfung.

## Goals / Non-Goals

**Goals**
- Jede Regel aus `specs/maintainability` hat genau ein Werkzeug, das sie prüft, und einen Testfall, der beweist, dass die Prüfung anschlägt.
- Bestehender Code blockiert die Einführung nicht; neue Verstöße schon.
- Das Werkzeug ist bestehend oder eingebaut, wo es das gibt. Eigener Code bleibt auf die Stellen beschränkt, für die es kein Werkzeug gibt.

**Non-Goals**
- Testpyramide, Testauswahl in CI, Modul-Tests statt E2E: eigener Change ("module-based tests", parallel).
- Coverage-Schwellen (JaCoCo bleibt beim Bericht). Eine Prozentzahl belohnt Tests um der Zahl willen; das ist eine eigene Entscheidung.
- Error Prone, NullAway, Checkstyle, SonarQube (siehe D1).
- `-Werror` für `-Xlint`: die Warnungen sind nicht gezählt; wird nach Messung entschieden, nicht in diesem Change.
- Behebung der Bestandsverstöße. Sie stehen in der Baseline; das Verkleinern ist Folgearbeit.

## Werkzeug je Regel

| Regel (Spec-Requirement) | Werkzeug | Eingebaut oder eigener Code |
| --- | --- | --- |
| Erlaubnisliste für Projekt-Abhängigkeiten | Gradle, Prüfung im Convention-Plugin | eigene Prüffunktion, ~30 Zeilen, Tests in `buildSrc` |
| Paketzyklen | ArchUnit `slices()` mit `beFreeOfCycles()` | eingebaut; Baseline über ArchUnit `FreezingArchRule` (eingebaut) |
| Pflicht-Architekturtest je Modul | Guard im Convention-Plugin (Datei `*ArchitectureTest*` unter `src/test` muss existieren) | ~10 Zeilen |
| Logger `private static final` | PMD `ProperLogger` (errorprone) | eingebaut |
| Keine Konsolenausgabe, kein `printStackTrace` | PMD `SystemPrintln`, `AvoidPrintStackTrace` | eingebaut |
| Keine Reflexion (`setAccessible`) | ArchUnit `callMethod(AccessibleObject.class, "setAccessible", boolean.class)` | eingebaut; Ausnahmenliste in der Regelklasse |
| Keine Systemuhr | ArchUnit `callMethod(Instant.class, "now")` u. a., `System.currentTimeMillis` | eingebaut |
| Kein `Thread.sleep` in Tests | ArchUnit `callMethod(Thread.class, "sleep", long.class)` über Testklassen | eingebaut |
| Ungenutzter Code, leere `catch`-Blöcke | PMD `UnusedPrivateMethod`, `UnusedPrivateField`, `UnusedLocalVariable`, `EmptyCatchBlock` | eingebaut |
| Komplexität, Größe | PMD `CyclomaticComplexity`, `NcssCount` (Methode und Klasse) | eingebaut |
| Baseline für PMD | Ratchet-Prüfung im Gradle-Build, liest die PMD-XML-Berichte und vergleicht je Datei und Regel mit `config/quality/pmd-baseline.properties` | eigener Code, ~40 Zeilen, Tests mit XML-Fixtures |
| Javadoc der `core`-API | `javadoc` mit doclint (`missing`) nur für `:core`, Warnungen brechen den Task ab | eingebaut |
| Javadoc einer Column | Prüfung im `titan.column`-Plugin (`package-info.java` enthält einen Javadoc-Block vor der Paketdeklaration) | ~10 Zeilen, pure Funktion mit Tests |
| Formatierung | Spotless (bleibt) | bestehend |

## Decisions

### D1 Werkzeugwahl für statische Analyse: PMD

PMD läuft über das in Gradle eingebaute `pmd`-Plugin. Es bringt keine neue Plugin-Abhängigkeit, liefert Komplexitäts- und Größenmetriken (`CyclomaticComplexity`, `NcssCount`), die Error Prone nicht hat, und kennt `@SuppressWarnings("PMD.<Regel>")` als begründbare Ausnahme direkt am Code.

Verworfen:
- **Error Prone** (+ NullAway): findet Fehlermuster zur Compile-Zeit, misst aber keine Komplexität und Größe. Einbindung über ein Compiler-Plugin, das bei jeder JDK-25-Änderung mitzieht. Für Wartbarkeit nicht der richtige Hebel; kann später ergänzt werden.
- **Checkstyle**: überlappt mit Spotless (Stil), Ausnahmen laufen über eine XML-Suppression-Datei statt am Code.
- **SonarQube/SonarCloud**: externer Dienst und Server; passt nicht zu KISS und zu einem Build, der offline läuft.

Test: Task 1.4 (PMD-Version mit Java-25-Unterstützung prüfen; ohne sie greift die Regelmenge nicht). Testfall je Regel mit einer Fixture-Klasse, die verletzt (rot), dann Regel grün. SOLID: Das Regelset ist Konfiguration, kein Code; neue Regeln ändern keine Klassen (OCP).

### D2 Modulgrenzen: Erlaubnisliste im Convention-Plugin

Die Liste steht in einer Tabelle an einer Stelle (`buildSrc`), nicht in jedem `build.gradle.kts`:

| Modulart | Erlaubte Projekt-Abhängigkeiten |
| --- | --- |
| `core` | keine |
| `common` | `core` |
| `persistence` | keine |
| `features/*` (Column) | `core`, `persistence` |
| `platform/*` | `core`, `common`, `platform/extensions` |
| `runtime`, `setup`, `bridge` | `core`, `common` (`runtime` zusätzlich `persistence`) |
| `apps/*` | alles (Composition Root) |

Die Prüfung läuft auf allen Konfigurationen (`implementation`, `api`, `compileOnly`, `testImplementation`, `testFixtures(...)`) und beim Konfigurieren, nicht erst beim Kompilieren, damit der Fehler mit Modul und Ziel erscheint. `testFixtures(project(":core"))` zählt als `core`.

Entschieden (Open Question 1): Columns dürfen `persistence` deklarieren. `features/jumprun` bleibt, wie es ist; die Persistenz-Logik wird nicht verschoben.

Ausnahme für `persistence`: Es hat keine Projekt-Abhängigkeit, braucht aber den Architekturtest mit den gemeinsamen Regeln aus `core`-Test-Fixtures. Die Tabelle erlaubt deshalb `core` für `persistence`. Dessen Produktionscode darf `core` trotzdem nicht importieren; das prüft der Architekturtest des Moduls (`mainCodeDoesNotDependOnCore`).

Test: Die Prüffunktion ist rein (`violation(modulePath, targetPath) -> Optional<String>`) und wird in `buildSrc/src/test` mit fester Tabelle geprüft (Modul/Ziel-Paare). Ein Gradle-Lauf mit einer absichtlich falschen Abhängigkeit ist der Negativtest (Task 2.3, nicht committen). SOLID: DIP, die Regel hängt von einer Tabelle ab, nicht von Gradle-Typen in der Entscheidung.

### D3 Architekturregeln: gemeinsame Klasse, je Modul ein Testaufruf

Die bestehenden Regeln in `ColumnArchitectureRules` bleiben. Neue Projektregeln (Logger, Konsole, Reflexion, Systemuhr, `Thread.sleep`) liegen in einer zweiten Klasse `MaintainabilityRules` in `core/src/testFixtures`, daneben. Jedes Modul mit Produktionscode bekommt eine kurze Testklasse mit `@AnalyzeClasses(packages = ...)`, wie die Columns heute. Der Guard aus der Werkzeug-Tabelle stellt sicher, dass sie existiert.

Alternative: Ein Convention-Plugin erzeugt die Testklasse. Verworfen: Der Test ist dann unsichtbar und lässt sich nicht pro Modul mit Ausnahmen versehen. Die Kosten sind ~25 Klassen mit je 10 Zeilen.

Entschieden (Open Question 3): Jede `MaintainabilityArchitectureTest` wird pro Modul explizit geschrieben, nicht erzeugt. Der Guard aus D2 prüft, dass sie existiert.

`Thread.sleep` in Tests braucht Testklassen auf dem Analysepfad. Die Klassen einer Column sind nicht auf dem Classpath von `apps/cloudnet`, deshalb gilt die Regel pro Modul und nicht zentral.

Test: Jede Regel bekommt eine Fixture-Klasse, die sie verletzt, und ein Testfall prüft, dass der Check fehlschlägt (wie `TelemetryArchitectureRulesTest` heute). Ein Testfall ohne Fehlschlag ist wertlos, deshalb gilt die Regel als erst grün, wenn die Verletzung gefunden wird. SOLID: ISP, jede Regel ist ein eigenes Feld, eine Regel pro Verhalten.

### D4 PMD-Regelmenge, Größen und Baseline

Regelmenge (versioniert in `config/quality/pmd-ruleset.xml`): die Regeln aus der Werkzeug-Tabelle, sonst nichts. Kein Stil, keine Formatierung (Spotless), keine Namensregeln. PMD läuft nur auf Produktionscode (`src/main`); Testcode bleibt außen vor (Open Question 5). Die Schwellen 15 (CC), 60 (Methode NCSS), 500 (Klasse NCSS) sind Startwerte; Task 1.1 misst den Bestand, und die Schwellen werden angepasst, bevor der Build sie erzwingt. Gemessener Ausreißer nach Zeilen: `JumprunModule` mit 606 Zeilen. NCSS ist nicht gleich Zeilen; Task 1.1 zählt, ob die Klasse über 500 NCSS liegt, und trägt sie sonst als Bestandsverstoß in die Baseline ein.

Baseline: PMD hat keine eingebaute Baseline, und Gradle auch nicht. Eigener Code ist dafür nötig (`PmdRatchet`): Er liest die PMD-XML-Berichte, zählt Verstöße je Datei und Regel und vergleicht mit `config/quality/pmd-baseline.properties`. Mehr als erlaubt: Fehler. Weniger: Hinweis, Baseline kann verkleinert werden. Nicht in der Baseline: Fehler.

Verworfen: `@SuppressWarnings` an jedem Bestandsverstoß. Das wären vermutlich über 100 Annotationen ohne Begründung im Code, und die Begründung ist für Bestandsverstöße nicht dieselbe wie für eine bewusste Ausnahme.

Test: `PmdRatchet` bekommt XML-Fixtures (ein Bericht mit 2 Verstößen, Baseline mit 1, usw.) in `buildSrc/src/test`. Keine echte PMD-Ausführung im Unit-Test (F.I.R.S.T.: keine Abhängigkeit vom Bestand). SOLID: SRP, Parsen und Vergleichen sind getrennt.

### D5 Ausnahmen und Freeze

- **PMD**: `@SuppressWarnings("PMD.<Regel>")` am kleinsten sinnvollen Element, mit Kommentar. Review prüft die Begründung.
- **ArchUnit**: benannte Ausnahme als Liste in der Regelklasse, mit `because`. Bestandsverstöße (Paketzyklen) laufen über `FreezingArchRule` (eingebaut; Speicher und Aktualisierung in Task 1.2 prüfen).
- **Baseline-Dateien** (`config/quality/pmd-baseline.properties`, `archunit_store/`): unter CODEOWNERS (`@OneLiteFeatherNET/titan-maintainers`). Eine Erweiterung ist im PR zu begründen, eine Freigabe der Maintainer ist Pflicht. Das ist ein Review-Schritt, kein Werkzeug: Ein Build kann nicht prüfen, ob eine Baseline "neu gewollt" war.

### D6 Javadoc der `core`-API

Geändert gegenüber der ersten Fassung (Messung in Task 1.3): doclint-Gruppe `missing` fordert zusätzlich `@param` und `@return` auf jeder Methode, also 230 Warnungen in 46 Dateien statt der Presence-Lücke aus der Anforderung. Deshalb:

- **Präsenz** (Anforderung): PMD `CommentRequired` im Modul `core`, eigene Regeldatei `config/quality/pmd-core-api-ruleset.xml`. Bestand: 30 Lücken, in `docs(core)` geschlossen, Baseline leer.
- **`:core:javadoc`**: doclint mit allen Gruppen außer `missing`, `-Werror`. Bestand: 0 Warnungen, 2 Referenzfehler (`ItemConflicts` liegt in `features/hotbar`) in `docs(core)` behoben. `:core:check` hängt an `:core:javadoc`.

Warum nicht Checkstyle: siehe D1. Warum nicht ein Javadoc-Linter: doclint ist im JDK.

Test: Negativtests manuell (Task 5.2, nicht committen, im PR belegt): eine `{@link}` auf einen fehlenden Typ lässt `:core:javadoc` fehlschlagen; eine öffentliche Methode ohne Kommentar lässt `:core:pmdRatchet` fehlschlagen.

### D7 Ungenutzte Abhängigkeiten (SOLLTE): Folgearbeit

Das Dependency-Analysis-Plugin (`com.autonomousapps.dependency-analysis`) ist das verbreitete Werkzeug für ungenutzte und zu lose deklarierte Abhängigkeiten in Gradle. Es ist nicht Teil dieser Änderung (Open Question 4, Entscheidung des Nutzers): Zuerst folgt ein Dry-Run auf `main`, danach ein eigener Change, der das SOLLTE-Requirement als Gate einführt. Gründe für den Aufschub: Der Bestand ist unbekannt (Annotation-Prozessoren, BOM-Abhängigkeiten, Laufzeit-Bibliotheken würden Fehlalarme erzeugen).

### D8 Column-Beschreibung

Jede Column hat schon `package-info.java` (für `@InjectModule`). Der Build prüft, dass dort ein Javadoc-Block steht. Ein README je Modul wird nicht eingeführt: Es würde neben `docs/lobby-modules.md` dupliziert und veralten. Die Prüfung ist bewusst grob (Vorhandensein, kein Inhalt); der Inhalt bleibt Review-Sache.

Test: Die Prüffunktion wird mit fester Eingabe getestet (mit Javadoc, ohne Javadoc, nur Kommentar `//`).

Hinweis zu "Zero changed lines outside the new module" (Requirement "Ein neues Feature ist nur ein neues Modul"): Die Column-Beschreibung liegt im neuen Modul. Ein Eintrag in `docs/lobby-modules.md` wäre eine Änderung außerhalb und wird deshalb nicht verlangt.

## Migration Plan

1. **Messen (Welle 1)**: PMD, ArchUnit, doclint, Dry-Run des Dependency-Analysis-Plugins auf `main`. Ergebnisse in diesem Dokument (Tabelle unten, fortschreiben).
2. **Gates einschalten, Baseline gefüllt**: Jedes Gate ist einzeln ein Commit im PR. `./gradlew build` grün, Baseline entspricht dem Messstand.
3. **Verkleinern** ist kein Teil dieses Changes. Folge-Changes verkleinern die Baselines, ein Modul nach dem anderen.

Rollback: Revert des PR-Commits. Keine Laufzeitwirkung, keine Datenmigration.

### Messergebnisse (Tasks 1.1 bis 1.3)

Gemessen auf dem Stand von `origin/main` (Commit `03c09ae`), PMD 7.28.0 und Gradle 9.8.2, Java 25.

**PMD (Task 1.1), Produktionscode aller Module**

| Regel | Verstöße | Bemerkung |
| --- | --- | --- |
| `SystemPrintln`, `AvoidPrintStackTrace` | 0 | |
| `UnusedPrivateMethod`, `EmptyCatchBlock` | 0 | |
| `UnusedPrivateField` | 2 | `DanceMenu.messages`, `PortalCommand.sources`: Baseline |
| `UnusedLocalVariable` | 1 | `SchemaMigrations.lock`: Baseline |
| `CyclomaticComplexity` (Methode, > 15) | 0 | Höchster Wert 15 (`JumprunModule.end`). Bei Grenze 10: 4 Methoden (11, 12, 15, 11) |
| `NcssCount` (Methode, > 60) | 0 | Höchster Wert 40 (Konstruktor `PortalCommand`) |
| `NcssCount` (Klasse, > 500) | 0 | Höchster Wert 269 (`JumprunModule`) |
| `ProperLogger` (nur Vergleich, nicht übernommen) | 41 | Erwartet `LOG` als Name; der Bestand nutzt `LOGGER` |
| `CommentRequired` (nur Vergleich) | 197 | 30 davon in `core`; Rest in anderen Modulen, außerhalb des Scopes |

PMD meldet einen Wert, sobald er die Report-Level erreicht. Die Grenzen 15, 60 und 500 bleiben deshalb als Report-Level 16, 61 und 501 eingetragen (bestätigt an der 15er-Methode und dem 40er-Konstruktor). Die Schwellen blockieren den Bestand nicht; sie bleiben unverändert.

Ausführungszeit: `pmdMain` über alle Module etwa 33 s (warmer Daemon, parallel). Ganzer `./gradlew build` mit allen Gates: 7 min 45 s. Eine Laufzeit je Modul wurde nicht einzeln gemessen. PMD 7.28.0 parst alle Module mit Java 25 ohne Parse-Fehler.

**ArchUnit (Task 1.2)**

| Prüfpunkt | Ergebnis |
| --- | --- |
| Paketzyklen im Produktionscode (`slices`, ohne Testcode) | 0 in allen Modulen; Baseline leer |
| Paketzyklen mit Testcode | 1 in `core` (Telemetrie-Tests und `testfixtures` hängen gegenseitig voneinander ab); deshalb ist Testcode ausgenommen |
| Reflexion (`setAccessible`) in `src/main` und `src/testFixtures` | 2, beide in `EventListenerCounter` (benannte Ausnahme) |
| Systemuhr in `src/main` | 0 |
| `Thread.sleep` in Tests | 0 |
| `Thread.sleep` in `src/main` | 1 (`TitanApplication`, AOT-Trainingslauf); keine Regel, weil Produktionscode |
| Logger nicht `private static final` | 1 Instanzfeld (`ServiceReadings`) |
| `System.getLogger` in `src/main` | 1 (`TitanBridgePermissionExtension`) |
| `FreezingArchRule` | Speicher unter `src/test/resources/archunit_store`; Anlage und Aktualisierung in den Tests aus |

Bridge (Entscheidung 6): Minestom deklariert `slf4j-api` mit `runtime`-Scope (POM `minestom-2026.09.12-26.2`, `slf4j-api` 2.0.18). Die Extension bekommt SLF4J deshalb vom Server-Classpath. Nicht geprüft: der Classloader eines laufenden CloudNet-Dienstes.

**Javadoc (Task 1.3)**

- `:core:javadoc` mit doclint `missing`: 230 Warnungen in 46 Dateien (44 ohne Kommentar, 120 ohne `@param`, 53 ohne `@return`, 9 ohne Hauptbeschreibung) und 2 Referenzfehler.
- Nach dem Lauf der Gates: 30 Präsenz-Lücken dokumentiert (`docs(core)`), 0 Warnungen unter doclint ohne `missing` mit `-Werror`.
- Compiler-Warnungen (`-Xlint:deprecation,unchecked`): keine im Lauf des vollen Builds. Die 19 Meldungen `Sharing is only supported ...` kommen von der JVM der Testprozesse, nicht von javac.
- Dependency-Analysis: nicht gemessen; nach Entscheidung 4 ist sie nicht Teil dieser Umsetzung.

**Abweichungen von der ersten Fassung, begründet**

- Logger-Regeln als ArchUnit statt PMD `ProperLogger`: `ProperLogger` verlangt den Namen `LOG` und prüft nur einen Logger-Typ. Der Bestand nutzt `LOGGER` und mischt SLF4J mit `System.Logger`.
- Persistence darf die Test-Fixtures von `core` nutzen (Ausnahme in der Modulgrenze), damit der Architekturtest des Moduls läuft. Der Architekturtest verbietet `core`-Imports im Produktionscode von `persistence`.
- Paketzyklen laufen auf Produktionscode; Testcode ist ausgenommen (siehe oben).
- Javadoc: doclint `missing` nicht als Gate, Präsenz über PMD (siehe D6).
- Die Stores des Freeze-Mechanismus zählen Zeilen (Abhängigkeiten), keine Anzahl je Datei. Die Baseline der Paketzyklen ist ohnehin leer.

## Risks / Trade-offs

- **[Zu viele Architekturtests]** 25 kurze Klassen → Guard in D2, kurzer Aufruf, Option eines Convention-Plugins offen.
- **[PMD-Rauschen]** Eine zu große Regelmenge blockiert den Alltag → kleine, vorher gemessene Liste (D4). Jede weitere Regel braucht einen Messwert.
- **[Baseline wächst unbemerkt]** → CODEOWNERS und Review-Regel (D5). Kein Werkzeug fängt eine bewusste Erweiterung ab.
- **[PMD unterstützt Java 25 nicht vollständig]** → Task 1.4 prüft die Version; ohne Unterstützung bleibt PMD auf dem letzten passenden Release, bis PMD nachzieht. Kein Downgrade des Toolchains.
- **[Plugin-Abhängigkeit an Gradle-Version]** → Versionen im Versionskatalog gepinnt, Renovate hebt sie an.
- **[Build-Zeit]** PMD läuft pro Modul über `check`, Ratchet und Guard sind billig. Messung in Task 1.1 (Sekunden je Modul). Keine Docker-Abhängigkeit, Windows und macOS sind nicht betroffen.
- **[Falsch-positive Fehler im Bestand]** Messung vor Aktivierung; ein Verstoß, der nur falsch ist, wird mit `@SuppressWarnings` und Begründung behandelt, nicht mit Regeländerung für alle.

## Open Questions

Alle acht Fragen sind mit dem Nutzer entschieden (Stand: Umsetzung). Die Antworten stehen im Text oben und in den Abschnitten D2 bis D7; hier die Zusammenfassung:

1. **`persistence` für Columns**: Erlaubt. `features/jumprun` bleibt unverändert (D2).
2. **Ein Change**: Ein Change `build(quality)` einschließlich ArchUnit-Anteil. Kein `test(architecture)`-Change. Ein Commit-Typ je Commit, ein Titel je PR.
3. **Architekturtests pro Modul**: explizit geschrieben, mit Guard im Convention-Plugin (D3).
4. **Dependency-Analysis-Plugin**: Später, als SOLLTE, nach einem Dry-Run. Nicht in dieser Umsetzung (D7).
5. **PMD nur auf Produktionscode**, Schwellen 15 (CC), 60 (Methode NCSS), 500 (Klasse NCSS). Die Schwellen werden nur angepasst, wenn die Messung zeigt, dass sie blockieren; die Messung steht im Migration Plan.
6. **Bridge-Abweichungen werden behoben, nicht ausgenommen**: `ServiceReadings` bekommt ein `private static final`-Logger-Feld. `TitanBridgePermissionExtension` wechselt von `System.getLogger` auf SLF4J, sofern der Bridge-Classloader SLF4J bereitstellt. Sonst bleibt `System.getLogger`, mit dokumentiertem Grund und einer benannten Ausnahme (Task 3.5).
7. **Agenten für Titan sind Haiku**. `tasks.md` nennt keine Modelle; die Aufgaben beschreiben nur Umfang, Dateien und Nachweis.
8. **Englische RFC-2119-Schlüsselwörter bleiben in Klammern**, z. B. `MUSS (MUST)`. `openspec validate --strict` läuft damit durch; das Muster gilt für die Specs dieses Change.
