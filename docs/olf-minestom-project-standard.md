# OLF Minestom Project Standard

**Status:** Vorschlag zur Review
**Datum:** 2026-08-20
**Geltungsbereich:** Alle OneLiteFeather-Minestom-Projekte (Server, Extensions, Libraries)
**Erster Anwender:** Titan (Migrationsplan in Anhang B)

---

## 0. Warum es dieses Dokument gibt

OneLiteFeather betreibt inzwischen vier vergleichbare Minestom-Server-Projekte —
ManisGame, Cygnus, Tamias und Titan — und eine Reihe von Libraries (Aves, Xerus,
Coris, Guira, pica, Cyano). Die Libraries folgen einer konsistenten, über Jahre
gewachsenen Handschrift. Die Server-Projekte tun das unterschiedlich stark.

Der Unterschied ist messbar, nicht Geschmackssache:

| Kennzahl (nur `src/main`) | Titan | ManisGame | Cygnus | Tamias | Aves | pica | Xerus |
|---|---:|---:|---:|---:|---:|---:|---:|
| Java-Dateien | 74 | 359 | 239 | 181 | 99 | 38 | 34 |
| davon mit `@author` | **2** | 152 | 121 | 89 | 78 | 18 | 33 |
| davon mit `@since` | **3** | 157 | 122 | 89 | 81 | 18 | 33 |
| `package-info.java` | **0** | 77 | 54 | 35 | 14 | 11 | 0 |
| `@ApiStatus`-Nutzung | **0** | 9 | 0 | 3 | 8 | 5 | 1 |
| `sealed`-Nutzung | 3 | 10 | 5 | 10 | 14 | 7 | 0 |
| öffentliche `*Impl` im API-Modul | 0 | 0 | — | — | — | — | — |
| Convention-Plugin (`buildSrc`) | **nein** | ja (3) | ja (1) | ja (1) | — | — | — |

Titan ist damit nicht "schlechter geschrieben", sondern **ohne durchgesetzte
Konvention gewachsen**. Es ist das einzige der vier Server-Projekte ohne
`buildSrc`.

**Eine Warnung zu dieser Tabelle.** Zahlen zeigen, wo man hinschauen soll — sie
ersetzen das Hinschauen nicht. Eine frühere Fassung dieses Dokuments las Titans
vier `*Impl`-Dateien im `:api`-Modul als Verstoß gegen die API/Impl-Trennung. Ein
Blick in den Code widerlegte das: alle vier sind package-private und hinter
Factory-Methoden verborgen — es ist eines der saubersten APIs im Bestand (siehe
OLF-L2-02). Wer diesen Standard anwendet, prüft jeden Zählwert am Code, bevor er
daraus eine Aufgabe ableitet.

Dieses Dokument leitet die Konvention aus dem ab, was in den Referenzprojekten
nachweislich funktioniert, macht sie maschinell prüfbar und beschreibt, wie Titan
als erstes Projekt darauf migriert.

**Die Referenzprojekte und wofür sie stehen:**

| Projekt | Referenz für |
|---|---|
| **ManisGame** | Modultopologie (`shared:*` + Server-Varianten), Convention-Plugin-Hierarchie, API-Modul-Disziplin |
| **Cygnus** | Bootstrap-Extraktion, echte Minestom-Extension-Jars, `package-info`-Disziplin |
| **Tamias** | Schlanke Drei-Modul-Topologie (`common`/`game`/`setup`), feine Paketierung |
| **Aves / Xerus / pica / Coris** | Klassen-Patterns: Interface + Factory, Builder, `sealed`, Javadoc-Konvention |

### Die drei Leitentscheidungen

1. **Deployment:** Ein schlanker Host-Prozess lädt alle Fachlogik als Extensions.
   Butterfly und die Lobby-Logik werden Extension-Jars, kein Fat-Jar-Inhalt.
2. **Reichweite:** Org-weiter Standard. Titan ist der erste Anwender, nicht der
   einzige Adressat.
3. **Durchsetzung:** Was prüfbar ist, wird geprüft. Regeln landen als
   `buildSrc`-Convention (Checkstyle, ArchUnit, Jacoco), nicht als Wunsch im Wiki.

### Regel-IDs

Jede Regel hat eine ID der Form `OLF-L<Ebene>-<Nummer>`. ArchUnit-Testmethoden
und Review-Kommentare referenzieren diese ID, damit eine Beanstandung immer auf
eine nachlesbare Begründung zeigt statt auf persönlichen Geschmack.

---

## 1. Ebene L1 — Projekt

### OLF-L1-01: `buildSrc` ist Pflicht, Build-Logik wird nicht kopiert

**Regel.** Jedes Multi-Modul-Projekt hat ein `buildSrc` mit einer
Convention-Plugin-**Hierarchie**: ein Basis-Plugin mit Toolchain, Compiler-Flags
und Test-Konfiguration, darauf aufbauend je ein Plugin pro Modulrolle. Toolchain,
Test-Konfiguration, Qualitäts-Gates und der Publishing-Block stehen dort
**einmal**. Jedes Modul wendet genau ein Rollen-Plugin an.

**Begründung.** Titan hat heute keinen `buildSrc`. Der `publishing { ... }`-Block
mit POM-Metadaten, Lizenz, Developer und SCM ist in `app`, `setup` und `bridge`
**dreimal wortgleich** kopiert, die Repository-Definition mit
Credential-Handling ebenso. Die Java-Toolchain 25 ist **fünfmal** dupliziert, die
Test-Konfiguration `useJUnitPlatform()` + `-Dminestom.inside-test=true` **viermal**.
Jede Änderung an einer dieser Stellen ist heute eine Änderung an vier bis fünf
Dateien — mit der üblichen Folge, dass eine vergessen wird. Cygnus zeigt die
Gegenprobe: `cygnus.java-conventions.gradle.kts`, ein einziger Ort.

**Ziel-Layout.** ManisGame lebt die Hierarchie bereits vor und ist die Vorlage:

```
buildSrc/src/main/kotlin/
  titan.java-conventions.gradle.kts        Basis: Toolchain, Compiler, Test, Jacoco
  titan.library-conventions.gradle.kts     ← java-conventions + `java-library`
  titan.application-conventions.gradle.kts ← java-conventions + `application` + shadow
  titan.extension-conventions.gradle.kts   ← java-conventions + Processor, compileOnly
  titan.quality-conventions.gradle.kts     Checkstyle, ArchUnit, Nullability-Gate
  titan.publish-conventions.gradle.kts     POM, Lizenz, Developer, SCM, Repository
```

**Referenz** (ManisGame — Vererbung in Reinform, das Rollen-Plugin enthält nur
noch den Unterschied):

```kotlin
// manis.library-conventions.gradle.kts
plugins {
    id("manis.java-conventions")
    `java-library`
}

// manis.application-conventions.gradle.kts
plugins {
    id("manis.java-conventions")
    application
}
```

**Referenz** (Cygnus, gekürzt — die Basis, auf der `titan.java-conventions` aufsetzt):

```kotlin
plugins {
    java
    jacoco
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(25)) }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}

tasks.withType<Test>().configureEach {
    jvmArgs("-Dminestom.inside-test=true")
    finalizedBy(tasks.matching { it.name == "jacocoTestReport" })
    useJUnitPlatform()
    testLogging { events("passed", "skipped", "failed") }
}
```

**Durchsetzung.** Ein Modul-`build.gradle.kts`, das `java { toolchain { ... } }`
oder einen `publishing`-Block selbst deklariert, statt das Convention-Plugin
anzuwenden, wird im Review abgelehnt. Perspektivisch prüfbar über einen
`checkNoInlineToolchain`-Task, der die Buildfiles nach diesen Blöcken greppt.

**Warnendes Beispiel.** Tamias hat ein `buildSrc` mit
`tamias.java-conventions.gradle.kts` — wendet es in `game/build.gradle.kts` aber
nicht an; dort stehen nur `alias(libs.plugins.shadow)` und `application`. Das
Convention-Plugin allein genügt also nicht: ohne Prüfung driften Module wieder
auseinander, und der `buildSrc` wird zur Dekoration. Genau deshalb steht in
diesem Standard bei jeder Regel eine Durchsetzungsangabe.

---

### OLF-L1-02: Version-Catalog im `settings.gradle.kts`, Versionen über BOM

**Regel.** Abhängigkeiten werden im Inline-Version-Catalog des
`settings.gradle.kts` deklariert (nicht in `libs.versions.toml` — das ist die
OLF-Abweichung vom Gradle-Default). Konkrete Versionen kommen wo immer möglich
aus einer BOM (`mycelium-bom` für Libraries, `aonyx-bom` für Minestom-Anwendungen,
`manis-bom` für Spiele) über `.withoutVersion()`.

**Begründung.** `.withoutVersion()` bedeutet: die BOM entscheidet, ein
Renovate-Update an der BOM zieht das ganze Ökosystem nach. Eine hart notierte
Version im Catalog hebelt das aus und muss pro Projekt gepflegt werden. Titan hat
aktuell 12 harte Versionen im Catalog, von denen mindestens `guava` und
`kotlin-stdlib-jdk8` nur deshalb dort stehen, weil sie transitive Lücken stopfen.
Solche Einträge brauchen einen Kommentar, der die Lücke benennt — Titan macht das
bereits vorbildlich, das ist beizubehalten.

**Regel im Detail.** Eine harte Version im Catalog ist zulässig, wenn:
1. die BOM sie nicht führt, **und**
2. ein Kommentar direkt darüber erklärt, warum sie nötig ist und wann sie
   entfallen kann.

---

### OLF-L1-03: Ein Modul, ein Zweck, ein Artefakt

**Regel.** Jedes Gradle-Modul produziert genau ein Artefakt mit genau einer Rolle.
Die zulässigen Rollen sind: `bootstrap` (ausführbarer Host), `api` (Contracts),
`common` (geteilte Domäne), `extension` (Fachlogik als Extension-Jar).

**Begründung.** Titans `:api`-Modul verletzt das doppelt: es enthält sowohl
Contracts (`Deliver`, `DeliverComponent`, `DeliverType`) als auch vier
Implementierungen (`ServerBuilderImpl`, `ServerDeliverComponentImpl`,
`TaskBuilderImpl`, `TaskComponentImpl`). Siehe OLF-L2-02.

**Zwei erprobte Zuschnitte.** OLF hat bereits zwei funktionierende Topologien —
die Wahl richtet sich nach der Projektgröße, nicht nach Geschmack:

*Tamias (schlank, 181 Dateien)* — drei Module, für Projekte mit einer Server-Rolle:

```
:common    geteilte Domäne (area, config, event, explosion, map, ground, ...)
:game      Spiel-Logik, application
:setup     Setup-Modus, application
```

*ManisGame (ausgebaut, 359 Dateien)* — geteilte Basis fein geschnitten, darauf
mehrere Server-Varianten:

```
shared:api        reine Contracts, 0 Impl, compileOnly(minestom)
shared:common     geteilte Domäne
shared:database   Persistenz
shared:queue      Messaging
shared:cloud      CloudNet-Anbindung
shared:scare, shared:day   Fach-Features
extensions:lobby  Server-Variante (application + mainClass)
extensions:game   Server-Variante
extensions:setup  Server-Variante
```

**Empfehlung für Titan.** Der Tamias-Zuschnitt plus ein `:api`-Modul und die
Extension-Aufteilung aus Abschnitt 5 — Titan hat mit 74 Dateien nicht die Größe,
die ManisGames Sieben-Wege-Split rechtfertigt. Wächst Titan, ist der Weg von
Tamias- zu ManisGame-Topologie additiv: `:common` wird aufgeteilt, die Rollen
bleiben.

**Namenswarnung.** ManisGames Verzeichnis `extensions/` enthält **keine**
Minestom-Extensions — es sind eigenständige Server-Anwendungen mit `mainClass`
und `shadowJar`; im gesamten Repository existiert keine `extension.json`. Für
echte Minestom-Extension-Jars sind Cygnus' und Titans `:bridge`-Module die
Referenz (Abschnitt 5). Wer ManisGame als Vorlage nimmt, darf die beiden
Bedeutungen von "Extension" nicht vermischen.

---

### OLF-L1-04: Release Please, Renovate-Preset, OLF-Workflows

**Regel.** Jedes Repository nutzt Release Please für Versionierung und Changelog,
das zentrale OneLiteFeather-Renovate-Preset und die reusable Workflows aus
`OneLiteFeatherNET/workflows`.

**Begründung.** Titan und Cygnus erfüllen das bereits. Butterfly nutzt noch
`.releaserc.json` (semantic-release) — das ist die einzige verbleibende Abweichung
im betrachteten Umfeld und im Zuge der Extension-Umstellung mitzuziehen.

---

## 2. Ebene L2 — Modul

### OLF-L2-01: Eine Abhängigkeitsrichtung, keine Zyklen

**Regel.** Abhängigkeiten fließen ausschließlich in eine Richtung:

```
extension ──▶ common ──▶ api
     │                     ▲
     └─────────────────────┘
bootstrap ──▶ api        (nur Contracts, nie common, nie extension)
```

Verboten: `common → extension`, `api → common`, `extension → extension`,
`bootstrap → common`.

**Begründung.** Extensions laufen in eigenen Classloadern. Eine Extension, die
eine andere direkt referenziert, funktioniert im Test und bricht im Deployment —
oder erzwingt eine Ladereihenfolge, die niemand dokumentiert hat. Kommunikation
zwischen Extensions läuft über Events oder die `ServiceRegistry`, nie über
direkte Typen. Titan macht das an der kritischsten Stelle bereits richtig:
`TitanPermissionBridge` reicht nur JDK-Typen über die Classloader-Grenze.

**Durchsetzung.** ArchUnit in `olf.quality-conventions`:

```java
@ArchTest
static final ArchRule modules_respect_dependency_direction =
    layeredArchitecture().consideringOnlyDependenciesInLayers()
        .layer("api").definedBy("..titan.api..")
        .layer("common").definedBy("..titan.common..")
        .layer("extension").definedBy("..titan.lobby..", "..titan.setup..", "..titan.bridge..")
        .whereLayer("extension").mayNotBeAccessedByAnyLayer()
        .whereLayer("common").mayOnlyBeAccessedByLayers("extension")
        .as("OLF-L2-01");
```

---

### OLF-L2-02: `:api` enthält Contracts, niemals Implementierungen

**Regel.** Das öffentliche API besteht aus Interfaces, Records als reine
Wertträger, Enums und Konstanten. Implementierungen dürfen im selben Modul liegen,
müssen dann aber **package-private** sein und über Factory-Methoden am Interface
erreicht werden. Öffentliche `*Impl`-Typen sind verboten.

**Positivbeispiel: Titans `Deliver`-API.** Entgegen dem, was die reine Dateizählung
nahelegt, ist dieses API bereits vorbildlich gebaut — es ist die interne Vorlage
für diese Regel:

```java
public sealed interface DeliverComponent permits DeliverComponent.TaskComponent,
        DeliverComponent.ServerDeliverComponent, TaskComponentImpl, ServerDeliverComponentImpl {

    DeliverType type();
    UUID playerId();

    static TaskBuilder taskBuilder() { return new TaskBuilderImpl(); }
    static ServerBuilder serverBuilder() { return new ServerBuilderImpl(); }

    sealed interface TaskComponent extends DeliverComponent permits TaskComponentImpl {
        String taskName();
    }

    sealed interface Builder<T extends Builder<T>> {
        T playerId(UUID playerId);
        default T player(Player player) { return playerId(player.getUuid()); }
        DeliverComponent build();
    }
}
```

Alle vier `*Impl` sind package-private (`final class TaskBuilderImpl`,
`record TaskComponentImpl`), der Contract wird ausschließlich über
Factory-Methoden betreten, und der Builder nutzt self-typed Generics. Ein
Konsument des Moduls sieht die Implementierungen nicht.

**Warum `permits` die Impls nennen darf.** Die `permits`-Klausel eines `sealed`
Typs muss jeden direkten Subtyp benennen — das ist eine Sprachanforderung, kein
Leck. Sichtbarkeit entscheidet über die Kapselung, nicht die Erwähnung im
`permits`. Ein `permits`-Eintrag auf einen package-private Typ ist genau richtig.

**Was hier trotzdem zu tun bleibt.** Die Einträge `TaskComponentImpl` und
`ServerDeliverComponentImpl` in der **Wurzel**-`permits`-Klausel sind redundant,
weil beide Typen bereits über `TaskComponent` bzw. `ServerDeliverComponent`
erreichbar sind. Aufräumen ist kosmetisch, kein Regelverstoß.

Der reale Mangel liegt woanders: `:api` trägt kein `maven-publish`. Publiziert
werden nur `titan-app`, `titan-setup` und `titan-bridge`. Das erklärte Ziel — ein
Contract, den andere Projekte konsumieren — ist damit nicht erreichbar; wer ihn
will, müsste den Fat-Jar der Lobby ziehen. Siehe offener Punkt 3.

**Die Gegenprobe.** ManisGames `shared:api` enthält bei 359 Projektdateien
**null** `*Impl`-Klassen und deklariert Minestom konsequent als `compileOnly` —
das API-Modul zieht also nicht einmal eine Laufzeitabhängigkeit auf den Server
nach:

```kotlin
plugins { id("manis.library-conventions") }

dependencies {
    api(platform(libs.manis.bom))
    compileOnly(libs.minestom)      // API beschreibt, sie führt nicht aus
    implementation(libs.caffeine)
}
```

Dass das bei fast fünffacher Projektgröße durchgehalten wird, entkräftet das
übliche Gegenargument, saubere API-Grenzen seien nur bei kleinen Projekten
praktikabel.

**Korrekte Form** — `sealed` erlaubt genau die Sub-Contracts, die Implementierung
liegt in `:common` und wird über eine Factory-Methode erreicht (Vorbild: Aves
`MapEntry`):

```java
public sealed interface DeliverComponent
        permits DeliverComponent.TaskComponent, DeliverComponent.ServerDeliverComponent {

    @Contract(pure = true, value = "_, _ -> new")
    static ServerDeliverComponent server(String name, UUID id) { ... }

    DeliverType type();
}
```

**Durchsetzung.**

```java
@ArchTest
static final ArchRule api_has_no_implementations =
    noClasses().that().resideInAPackage("..titan.api..")
        .should().haveSimpleNameEndingWith("Impl")
        .as("OLF-L2-02");
```

---

### OLF-L2-03: Ein Paketwurzel-Präfix pro Projekt

**Regel.** Alle Typen eines Projekts liegen unterhalb genau eines
Wurzelpakets: `net.onelitefeather.<projekt>`.

**Begründung.** Titans `:api`-Modul hat heute **zwei** Wurzeln:
`net.onelitefeather.deliver` (6 Klassen) und `net.onelitefeather.titan.api.deliver`
(1 Klasse) — wobei die eine Klasse aus der anderen Wurzel importiert. Das ist
nicht nur unordentlich, es macht auch jede paketbasierte Regel (ArchUnit,
Checkstyle, `@NotNullByDefault`) unzuverlässig, weil sie eine der beiden Wurzeln
übersieht.

**Durchsetzung.**

```java
@ArchTest
static final ArchRule single_root_package =
    classes().should().resideInAPackage("net.onelitefeather.titan..")
        .as("OLF-L2-03");
```

---

### OLF-L2-04: Reuse vor Reimplementierung

**Regel.** Bevor eine Abstraktion in `:common` neu entsteht, ist zu prüfen, ob
Aves, Xerus, Coris, Guira oder pica sie bereits liefert. Eine bewusste
Eigenimplementierung braucht einen Kommentar, der begründet, warum die
Library-Variante nicht passt.

**Begründung.** Das ist der teuerste Verstoß in Titan. Titan importiert Aves
bereits (`net.theevilreaper.aves.map.BaseMap`, `GsonFileHandler`,
`PositionGsonAdapter`) — und baut die Kernabstraktion daneben trotzdem selbst:

| Titan (Eigenbau) | Aves (vorhanden) |
|---|---|
| `final class MapProvider` | `interface MapProvider` + `abstract class AbstractMapProvider` |
| `record MapEntry(Path path)` | `sealed interface MapEntry permits BaseMapEntry` mit `of(...)`-Factories |
| `Function<Stream<Path>, List<MapEntry>>` | `@FunctionalInterface PathFilter<T>` |
| `MapPool`, `LobbyMap`, `LobbyMapBuilder` | `BaseMap`, `BaseMapBuilder`, `ChunkLoaderFactory` |

Titans `MapProvider` ist eine `final class` ohne Interface — also weder
mockbar noch erweiterbar, obwohl Aves genau dafür `AbstractMapProvider` als
Erweiterungspunkt anbietet. Dass es anders geht, zeigt ManisGames
`SetupExtension`: es importiert `net.theevilreaper.aves.map.provider.MapProvider`
direkt und leitet für den Sonderfall `SetupMapProvider` ab, statt daneben etwas
Eigenes zu bauen.

**Der org-weite Fall: `ThreadHelper`.** Diese Klasse existiert **viermal** im
OneLiteFeather-Bestand, drei Kopien davon byte-identisch:

| Ort | Inhalt |
|---|---|
| `titan/common/utils/ThreadHelper.java` | identisch |
| `butterfly/minestom/feature/ThreadHelper.java` | identisch |
| `butterfly/bukkit/utils/ThreadHelper.java` | identisch |
| `manis/common/util/ThreadHelper.java` | leicht abweichende Variante |

Alle vier lösen dasselbe Problem: den Context-Classloader für einen
`ServiceLoader`-Aufruf temporär umzubiegen. Das ist keine Titan-Nachlässigkeit,
sondern eine org-weite Lücke — es fehlt schlicht der Ort, an den solcher Code
gehört. Dieselbe Geschichte bei `SingletonFeatureManagerProvider`
(Titan + Butterfly).

**Konsequenz für den Standard.** Querschnittscode dieser Art gehört in eine
Library (Aves für Allgemeines, Butterfly für LuckPerms-/Feature-Flag-Nahes), nicht
in das jeweilige `common` des Projekts. Die Regel lautet daher konkret: **Wird
eine Hilfsklasse in einem zweiten Projekt gebraucht, wird sie nicht kopiert,
sondern in die passende Library gehoben.**

**Durchsetzung.** Nicht automatisierbar, aber im Review verbindlich: Eine neue
Klasse in `:common`, deren Name einem Aves-/Xerus-Typ entspricht
(`*Provider`, `*Entry`, `*Pool`, `*Builder`, `Kit*`, `Team*`, `Phase*`), erfordert
die explizite Begründung im Klassen-Javadoc.

---

### OLF-L2-05: Kein statischer Zustand über Modulgrenzen

**Regel.** Statische Felder mit veränderlichem Zustand sind auf Modulebene
verboten. Muss Zustand eine Classloader-Grenze queren, geschieht das über einen
Holder, der ausschließlich JDK-Typen in seiner Signatur führt.

**Begründung.** Voraussetzung für das Extension-Modell (Abschnitt 4). Titan
erfüllt das an der schwierigsten Stelle bereits: `TitanPermissionBridge` reicht
`(UUID, String) -> boolean` über die Grenze zwischen Anwendungs- und
Extension-Classloader — kein LuckPerms-Typ, kein CloudNet-Typ. Diese Lösung ist
die Blaupause, nicht die Ausnahme.

**Bekannte Abweichung.** `SingletonFeatureManagerProvider` hält ein statisches,
lazy initialisiertes `FeatureManager`-Feld. Das ist durch das Togglz-SPI
vorgegeben (`FeatureManagerProvider` wird per ServiceLoader instanziiert) und
bleibt zulässig — aber genau deshalb gehört die Klasse nach Butterfly und nicht
in beide Projekte kopiert.

---

## 3. Ebene L3 — Paket

### OLF-L3-01: `package-info.java` mit `@NotNullByDefault` in jedem Paket

**Regel.** Jedes Paket mit mindestens einem öffentlichen Typ hat eine
`package-info.java`. Nullability wird dort einmal deklariert, nicht pro Parameter
verteilt.

```java
@NotNullByDefault
package net.onelitefeather.titan.common.map;

import org.jetbrains.annotations.NotNullByDefault;
```

Innerhalb eines so annotierten Pakets wird nur noch `@Nullable` gesetzt — als
bewusste Ausnahme. Ein `@NotNull` dort ist dann redundant und wird entfernt.

**Begründung.** Das ist die durchgängigste Konvention der Referenzprojekte:
Cygnus 54 Pakete, Aves 14, pica 11, Coris 8, Guira 4. Titan hat **null** — und
stattdessen 24 Dateien mit handverteilten `@NotNull`. Handverteilte Annotationen
sind unvollständig per Konstruktion: sie stehen dort, wo jemand daran gedacht hat.
Die Paket-Variante kehrt den Default um, wodurch die Lücke sichtbar statt still
wird.

**Durchsetzung.** Nicht selbst schreiben — Falcos
`PublicApiTest.everyPublishedPackageDeclaresNullness` übernehmen. Die Annotation
wird dort bewusst **als String** gematcht, damit die Regel nicht davon abhängt,
dass das Annotations-Artefakt zur Importzeit auflösbar ist; ein Paket ganz ohne
`package-info` fällt ebenfalls durch — der Fall, um den es eigentlich geht:

```java
private static final ArchCondition<JavaClass> IN_NOT_NULL_BY_DEFAULT_PACKAGE =
        new ArchCondition<>("reside in a @NotNullByDefault package") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                Optional<? extends HasAnnotations<?>> info = item.getPackage().tryGetPackageInfo();
                boolean ok = info.isPresent()
                        && item.getPackage().isAnnotatedWith(
                                "org.jetbrains.annotations.NotNullByDefault");
                events.add(new SimpleConditionEvent(item, ok,
                        "package " + item.getPackageName() + " has no @NotNullByDefault package-info"));
            }
        };

@ArchTest
static final ArchRule everyPublishedPackageDeclaresNullness = classes()
        .that().resideInAnyPackage(PUBLISHED)
        .and(not(PACKAGE_INFO))
        .should(IN_NOT_NULL_BY_DEFAULT_PACKAGE)
        .because("not one bare @NotNull exists in the three modules; the non-nullability of the "
                + "whole API rests on the three package-info lines");
```

---

### OLF-L3-02: Fachliche Paketnamen, kein `utils`/`helper`-Sammelbecken

**Regel.** Pakete werden nach Fachlichkeit geschnitten, nicht nach technischer
Kategorie. `util` ist zulässig für echte, zustandslose Querschnittsfunktionen
(Aves hat `util/vector`, `util/collection`, `util/functional` — jeweils fachlich
scharf). Ein `utils`-Paket als Ablage für alles, was sonst nirgends passt, ist es
nicht.

**Begründung.** Titans `common/utils` enthält heute sieben Typen ohne
gemeinsamen Nenner:

| Klasse | gehört fachlich nach |
|---|---|
| `Cancelable` | `common/event` |
| `CloudNetEnvironment` | `common/bootstrap` |
| `Items` | `common/item` (oder ersetzbar durch Aves `item`) |
| `SingletonFeatureManagerProvider` | Butterfly (Duplikat, siehe OLF-L2-04) |
| `Tags` | `common/tag` (zusammen mit `tags/PosTagSerializer`) |
| `ThreadHelper` | Butterfly (Duplikat) |
| `TitanFeatures` | `common/feature` |

Ein `utils`-Paket wächst monoton: es hat keine Definition, also kann nichts
hineinpassen und nichts wieder hinaus. Dasselbe gilt für `common/helper`
(`BlockHandlerHelper`, `SitHelper`) — siehe OLF-L4-04.

**Durchsetzung.**

```java
@ArchTest
static final ArchRule no_generic_util_package =
    noClasses().should().resideInAnyPackage("..utils", "..helper", "..misc", "..common.impl")
        .as("OLF-L3-02");
```

---

### OLF-L3-03: Events liegen im `event`-Unterpaket ihres Fachpakets

**Regel.** Ein Event gehört in das `event`-Unterpaket der Fachlichkeit, die es
auslöst — nicht in ein globales Event-Paket.

**Begründung.** Das Muster ist in allen sechs geprüften Referenzprojekten
identisch: Coris `door/event`, `floor/event`; Xerus `kit/event`, `team/event`;
pica `dialog/event`; Cygnus `map/event`, `page/event`, `player/event`, `view/event`;
Tamias `common/map/event`, `game/round/event`; ManisGame `api/door/event`,
`lobby/event`, `setup/event/dialog`, `hud/event`.

Der Vorteil ist Lokalität: wer das Feature liest, sieht seine Events, ohne ein
Sammelpaket zu durchsuchen. Tamias treibt es am weitesten und spiegelt die
Event-Struktur auch bei den Listenern (`game/listener/area`, `listener/round`,
`listener/team`) — bei 181 Dateien ist das der Unterschied zwischen Navigieren
und Suchen.

Titan hat aktuell ein flaches `common/event` mit einer Klasse — bei der
derzeitigen Größe unkritisch, aber beim Ausbau die falsche Weiche. Titans
`app/listener` mit 14 Listenern in einem flachen Paket ist bereits jetzt der Fall,
in dem sich die Tamias-Untergliederung lohnen würde (`listener/sit`,
`listener/elytra`, `listener/player`).

---

## 4. Ebene L4 — Klasse

### OLF-L4-01: Javadoc-Header auf jedem öffentlichen Typ

**Regel.** Jeder öffentliche Typ trägt einen Javadoc-Block mit Beschreibung und
den Tags `@author`, `@version`, `@since`. `@version` wird bei jeder
API-relevanten Änderung erhöht, `@since` nie.

**Referenz** (Aves `MapProvider`):

```java
/**
 * The {@link MapProvider} interface is responsible for managing the available maps.
 * It will load all maps data from the given path and store them.
 * It would not load the map itself over a {@link AnvilLoader} instance.
 * This behavior is handled by another class.
 *
 * @author theEvilReaper
 * @version 1.1.0
 * @since 1.6.0
 */
public interface MapProvider {
```

**Begründung.** `@since` ist der einzige Weg, ohne `git blame` zu erkennen, ob
ein Typ zum stabilen Kern gehört oder letzte Woche entstand — bei einer Library,
die andere Projekte konsumieren, ist das der Unterschied zwischen "kann ich mich
darauf verlassen" und "muss ich nachfragen". Zahlen siehe Abschnitt 0.

**Durchsetzung.** Checkstyle `JavadocType` mit
`allowedAnnotations=""` und `authorFormat`/`versionFormat` gesetzt, plus
`MissingJavadocType` auf `scope=public`.

---

### OLF-L4-02: Interface, dann `Abstract`/`Base`, dann Factory-Methode

**Regel.** Ein Typ, den andere Module benutzen, wird als Interface eingeführt.
Wiederverwendbare Teilimplementierung heißt `Abstract*` oder `Base*`. Die
Instanziierung läuft über eine statische Factory-Methode auf dem Interface, nicht
über einen öffentlichen Konstruktor.

**Referenz** (Aves `MapEntry` — Interface, `sealed`, Factory, `@Contract`):

```java
public sealed interface MapEntry permits BaseMapEntry {

    String MAP_FILE = "map.json";

    @Contract(pure = true, value = "_ -> new")
    static MapEntry of(Path directoryRoot) {
        return new BaseMapEntry(directoryRoot, MAP_FILE);
    }

    @Contract(pure = true, value = "_, _ -> new")
    static MapEntry of(Path directoryRoot, String mapFileNaming) {
        return new BaseMapEntry(directoryRoot, mapFileNaming);
    }
}
```

**Begründung.** Die Factory-Methode ist der Punkt, an dem die Implementierung
wechseln kann, ohne dass ein Aufrufer es merkt — der Kern des Dependency
Inversion Principle. `@Contract(pure = true, value = "_ -> new")` teilt der IDE
und statischen Analyse mit, dass der Aufruf nebenwirkungsfrei ist und ein neues
Objekt liefert.

Titan hat dieses Muster bereits an mehreren Stellen (`MapProvider.create(...)`,
`AppConfigProvider.create(...)`, `DeliverProvider.create()`, `Titan.instance()`),
aber jeweils ohne Interface — die Factory liefert die konkrete `final class`
zurück. Damit fehlt genau die Austauschbarkeit, für die die Factory da ist, und
Tests müssen die echte Klasse konstruieren statt eine Test-Implementierung
einzusetzen.

---

### OLF-L4-03: Builder für Objekte mit mehr als drei optionalen Feldern

**Regel.** Konfigurationsobjekte mit mehr als drei optionalen Feldern bekommen
einen Builder. Der Builder ist eine eigene Klasse `*Builder` und wird über eine
Factory-Methode am Zieltyp erreicht.

**Begründung.** Aves nutzt das durchgängig (8 Builder-Klassen: `InventoryBuilder`,
`PageableInventoryBuilder`, `BaseMapBuilder`, …), pica ebenso (7). Titan hat mit
`AppConfigBuilder` und `LobbyMapBuilder` bereits zwei — die Konvention ist also
schon da und muss nur festgeschrieben werden. `AppConfig` mit seinen elf
Zugriffsmethoden ist genau der Fall, für den die Regel existiert.

---

### OLF-L4-04: Statische Helfer werden Services

**Regel.** Eine Klasse mit ausschließlich statischen Methoden und mehr als
~50 Zeilen Fachlogik wird ein Service mit Interface und Instanz. Rein
funktionale, zustandslose Umrechnungen (Vektor-Mathematik, Farbkonvertierung)
dürfen statisch bleiben — dann aber als `final class` mit privatem Konstruktor.

**Begründung.** `SitHelper` in Titan hat 102 Zeilen statische Sitz-Logik, gegen
die 378 Zeilen Testcode anschreiben — das schlechteste Test-zu-Code-Verhältnis im
Projekt, weil jeder Testfall den globalen Zustand selbst herstellen muss, statt
eine konfigurierte Instanz zu bekommen. Statische
Fachlogik lässt sich weder ersetzen noch in einem anderen Kontext anders
konfigurieren — sie ist der Gegenentwurf zu OLF-L4-02. `NavigationHelper` macht
es bereits besser (`NavigationHelper.instance(deliver)` mit injizierter
Abhängigkeit) und ist die Vorlage für `SitHelper`.

`BlockHandlerHelper.registerAll()` ist der zulässige Gegenfall: eine einmalige
Registrierung ohne Zustand und ohne Variantenbedarf.

---

### OLF-L4-05: `final class` + privater Konstruktor für echte Utilities

**Regel.** Nicht instanziierbare Klassen sind `final` und haben einen privaten
Konstruktor ohne Rumpf.

**Referenz** (Cygnus `ServiceBootstrap`):

```java
public final class ServiceBootstrap {
    private ServiceBootstrap() {
    }
}
```

**Durchsetzung.** Checkstyle `HideUtilityClassConstructor` + `FinalClass`.

---

### OLF-L4-06: `@ApiStatus` für nicht-stabile API

**Regel.** Öffentliche Typen, die noch nicht stabil sind, tragen
`@ApiStatus.Experimental`. Typen, die technisch öffentlich sein müssen, aber
nicht zur API gehören, tragen `@ApiStatus.Internal`.

**Begründung.** Aves nutzt das in 8, pica in 5 Dateien; Coris markiert seine
Kern-Interfaces bewusst als `@ApiStatus.Experimental`. Ohne diese Markierung ist
jede öffentliche Klasse implizit ein Versprechen. Titan nutzt `@ApiStatus`
bislang gar nicht — bei einem Modul namens `:api`, das publiziert wird, ist das
eine Lücke.

---

### OLF-L4-07: Ein Test pro Fachklasse, Cyano für Minestom-Integration

**Regel.** Jede Klasse mit Fachlogik hat eine Testklasse gleichen Namens mit
Suffix `Test` im spiegelbildlichen Paket. Tests, die einen laufenden
Minestom-Kontext brauchen (Player, Instance, Pakete), nutzen Cyano und heißen
`*IntegrationTest`.

**Begründung.** Cygnus zeigt die Spiegelstruktur konsequent — zu
`common/page/PageFactory.java` gehört `common/page/PageFactoryTest.java`, zu
`player/PermissionAwarePlayer.java` gehört
`player/PermissionAwarePlayerIntegrationTest.java`. Titan folgt dem in `:common`
und `:app` bereits gut; die Lücke ist `:api` (7 Klassen, 0 Tests), `:setup`
(7 Klassen, 0 Tests) und `:bridge` (1 Klasse, 0 Tests).

**Durchsetzung.** Jacoco-Coverage-Gate in `olf.java-conventions`, zunächst als
`violationRules` mit einer Schwelle, die den Ist-Stand nicht unterschreitet, und
dann schrittweise angehoben. Ein Gate, das sofort auf 80 % springt, wird
umgangen statt erfüllt.

---

### OLF-L4-08: Ein Logger pro Klasse, keine `System.out`

**Regel.** `private static final Logger LOGGER = LoggerFactory.getLogger(X.class);`
— SLF4J, keine Ausnahme. Jede Anwendung deklariert genau ein SLF4J-Binding als
`runtimeOnly`.

**Begründung.** Cygnus dokumentiert die Falle im Buildfile: *"SLF4J needs a
binding at runtime; without one it falls back to NOP and the server logs nothing
at all."* — und deklariert entsprechend `libs.slf4j.api` als `implementation` und
`libs.slf4j.simple` als `runtimeOnly`. Titan deklariert **weder API noch Binding**
in irgendeinem Buildfile, nutzt `LoggerFactory` aber in `MapProvider` und
`MapPool`. Beides kommt derzeit transitiv über Minestom herein. Das funktioniert,
solange Minestom es mitbringt — und hört ohne Vorwarnung auf, wenn ein
Minestom-Update die Abhängigkeit umstellt. Der Ausfall ist dabei still: SLF4J
fällt auf NOP zurück und protokolliert einfach nichts mehr.

---

## 5. Extension-Modell

### 5.1 Zielbild

Der Host-Prozess enthält keine Fachlogik. Er initialisiert Minestom, startet den
`ExtensionBootstrap` und übergibt.

```
app-titan.jar                    Host: main(), ServiceBootstrap, Minestom, Auth
└─ extensions/
   ├─ butterfly-minestom.jar     Chat-Format, Prefix, LuckPerms-Anbindung
   ├─ titan-lobby.jar            Sit, Elytra, Tickle, Navigation, Commands
   ├─ titan-setup.jar            Map-Setup-Modus
   ├─ titan-bridge.jar           CloudNet -> LuckPerms
   └─ luckperms.jar              JarInJar-Loader
```

**Was das löst.**

- Butterfly wird nicht mehr in den Fat-Jar geshadet. Die byte-identischen Kopien
  `ThreadHelper` und `SingletonFeatureManagerProvider` in Titans `common/utils`
  entfallen ersatzlos (OLF-L2-04).
- Lobby-Features werden einzeln deaktivierbar, ohne Rebuild des Hosts.
- Der Host wird testbar, weil er nichts Fachliches mehr tut.
- `:setup` und `:lobby` teilen denselben Host, statt zwei getrennte
  `application`-Module mit je eigenem `Titan.java` zu sein.

### 5.2 OLF-L5-01: `extension.json` wird generiert, nicht gepflegt

**Regel.** Extension-Metadaten entstehen über den Annotation Processor
`minestom-extensions-processor`, nicht als handgepflegte Ressource.

**Begründung.** Titan pflegt `bridge/src/main/resources/extension.json` von Hand
und stanzt die Version über `ReplaceTokens` und einen `@version@`-Platzhalter
ein — inklusive `inputs.properties`-Boilerplate im Buildfile. Cygnus lässt den
Processor die Datei erzeugen und übergibt nur die Version als Compiler-Argument:

```kotlin
compileOnly(libs.minestom.extensions.processor)
annotationProcessor(libs.minestom.extensions.processor)
// ...
options.compilerArgs.add("-Aminestom.extension.version=${rootProject.version}")
```

Der Processor validiert dabei, dass der Entrypoint existiert und die richtige
Basisklasse erweitert — die handgepflegte Variante fällt erst zur Laufzeit auf.

### 5.3 OLF-L5-02: OLF-Fork statt archiviertes Upstream

**Regel.** Extension-fähige Projekte nutzen `net.onelitefeather:minestom-extensions`
(über `minestom-extensions-bom`).

**Begründung.** Titan nutzt heute `dev.hollowcube:minestom-ce-extensions:1.2.0`.
Das Upstream-Repository `hollow-cube/minestom-ce-extensions` ist archiviert; der
OneLiteFeather-Fork (bei Cygnus in Version 2.1.1 im Einsatz) hat identische
Paketnamen, wird gepflegt und liefert zusätzlich den Annotation Processor aus
OLF-L5-01. Beim Wechsel entfällt außerdem die JitPack-Proxy-Repository-Definition,
die Titan nur wegen `DependencyGetter` braucht.

### 5.4 OLF-L5-03: Alles, was der Host liefert, ist `compileOnly`

**Regel.** Ein Extension-Modul deklariert Minestom, das Extension-Framework,
CloudNet und `:common` als `compileOnly`. Gebündelt wird nur, was ausschließlich
diese Extension braucht.

**Begründung.** Titans `:bridge` macht das bereits vollständig richtig und ist
die Vorlage für die neuen Extension-Module. Wird eine vom Host gelieferte
Bibliothek mitgebündelt, existiert sie zur Laufzeit zweimal in zwei
Classloadern — die Fehlerbilder daraus (`NoSuchMethodError`, `ClassCastException`
zwischen identisch benannten Typen) sind teuer zu diagnostizieren.

---

## 6. Anhang A — Checkstyle- und ArchUnit-Regelabbildung

> **Die Durchsetzungsschicht existiert bei OLF bereits.** Falco hat ein eigenes,
> nie publiziertes Modul `falco-archunit` mit **41 ArchUnit-Regeln** in fünf
> Testklassen, die als normale JUnit-Tests laufen. Mehrere davon sind exakt die
> Regeln dieses Dokuments — `everyPublishedPackageDeclaresNullness` ist OLF-L3-01,
> `publishedModulesOnlyUseDeclaredDependencies` ist OLF-L2-01,
> `publicApiIsMarkedExperimental` ist OLF-L4-06, `publicClassesAreFinal` und
> `utilityClassesHideTheirConstructor` sind OLF-L4-05, `noMutableStaticFields`
> ist OLF-L2-05, `onlySlf` / `loggerFieldShape` / `noPrintStackTrace` sind
> OLF-L4-08.
>
> Die Tabelle unten ist damit **keine Konstruktionsanleitung mehr, sondern eine
> Abbildung auf vorhandenen Code.** Falcos Implementierung wird kopiert, nicht
> nachgebaut. Ihr Stil ist ebenfalls zu übernehmen: jede Regel endet auf
> `.because(...)` mit der echten Begründung, Ausnahmen (Records, Enums,
> Throwables, Minestom-Subklassen) stehen in der Regel statt in einer
> Unterdrückungsliste, und das Regel-Javadoc benennt die Grenze der Regel
> ausdrücklich — die Nullability-Regel etwa prüft, *dass* der Default deklariert
> ist, nicht dass eine Signatur ihn einhält.

| Regel-ID | Werkzeug | Konkret |
|---|---|---|
| OLF-L1-01 | Review + Grep-Task | Kein `toolchain`/`publishing`-Block im Modul-Buildfile |
| OLF-L2-01 | ArchUnit | `layeredArchitecture()` |
| OLF-L2-02 | ArchUnit | `noClasses().that().arePublic().and().resideInAPackage("..api..").should().haveSimpleNameEndingWith("Impl")` — die Sichtbarkeit ist der Punkt, nicht der Name |
| OLF-L2-03 | ArchUnit | `classes().should().resideInAPackage("net.onelitefeather.titan..")` |
| OLF-L2-05 | ArchUnit | `fields().that().areStatic().and().arePublic().should().beFinal()`, plus Ausnahmeliste für SPI-Holder |
| OLF-L3-01 | ArchUnit | Paket-Annotation `@NotNullByDefault` vorhanden |
| OLF-L3-02 | ArchUnit | `noClasses().resideInAnyPackage("..utils", "..helper", "..misc")` |
| OLF-L4-01 | Checkstyle | `MissingJavadocType`, `JavadocType` mit `authorFormat` |
| OLF-L4-05 | Checkstyle | `HideUtilityClassConstructor`, `FinalClass` |
| OLF-L4-07 | Jacoco | `violationRules`, Schwelle schrittweise angehoben |
| OLF-L4-08 | Checkstyle | `RegexpSinglelineJava` auf `System\.(out\|err)` |

**Bewusst nicht automatisiert.** OLF-L1-02 (BOM-Nutzung), OLF-L1-03 (Modulrolle),
OLF-L1-04 (Release-Tooling), OLF-L2-04 (Reuse vor Reimplementierung),
OLF-L3-03 (Event-Platzierung), OLF-L4-02 bis OLF-L4-04 (Pattern-Wahl),
OLF-L4-06 (`@ApiStatus`) und OLF-L5-01 bis OLF-L5-03 (Extension-Setup) sind
Entwurfsentscheidungen, die ein Werkzeug nicht beurteilen kann. Sie gelten im
Review — die Regel-ID im PR-Kommentar ersetzt die Diskussion über Geschmack.

**Einführungsstrategie.** Alle Gates starten als Warnung mit einer Baseline, die
den Ist-Stand einschließt. Pro Migrationsphase (Anhang B) wird die Baseline für
den berührten Bereich entfernt und das Gate dort scharf geschaltet. Ein Gate, das
beim Einschalten 400 Verstöße meldet, wird abgeschaltet statt behoben.

---

## 7. Anhang B — Migrationsplan Titan

Fünf Phasen. Jede ist für sich lauffähig, testbar und mergebar; keine Phase
setzt voraus, dass eine spätere bereits begonnen wurde.

### Phase 1 — `buildSrc` einführen (kein Produktionscode betroffen)

- `buildSrc` mit `olf.java-conventions`, `olf.publish-conventions` anlegen
- Toolchain (5×), Test-Konfiguration (4×) und Publishing-Block (3×) dorthin
  zusammenführen
- Checkstyle/ArchUnit als Warnung mit Voll-Baseline aktivieren
- `slf4j-api` als `implementation` und ein Binding als `runtimeOnly` explizit
  deklarieren, statt sich auf Minestoms transitive Abhängigkeit zu verlassen
  (OLF-L4-08)
- **Prüfbar:** `./gradlew build` liefert byte-gleiche Artefakte wie vorher

### Phase 2 — Paket- und API-Hygiene

- `net.onelitefeather.deliver` nach `net.onelitefeather.titan.api.deliver`
  verschieben (OLF-L2-03)
- Redundante `permits`-Einträge (`TaskComponentImpl`,
  `ServerDeliverComponentImpl`) aus der Wurzelklausel von `DeliverComponent`
  entfernen — kosmetisch, kein Verstoß (OLF-L2-02). Die vier `*Impl` bleiben, wo
  sie sind: package-private hinter Factory-Methoden ist das Zielbild.
- `package-info.java` mit `@NotNullByDefault` in allen Paketen anlegen,
  redundante `@NotNull` entfernen (OLF-L3-01)
- Javadoc-Header auf öffentlichen Typen ergänzen (OLF-L4-01)
- **Prüfbar:** ArchUnit-Regeln L2-02, L2-03, L3-01 ohne Baseline grün

### Phase 3 — Reuse statt Eigenbau

- `common/utils` und `common/helper` fachlich auflösen (OLF-L3-02, Tabelle in
  Abschnitt 3). **Anmerkung:** `Items` hat in Stufe 5 die Factory
  `navigatorBuildServer(...)` dazubekommen und liegt damit weiter in `utils`.
  Bewusst nicht vorgezogen: die Methode gehört zu den `NAVIGATOR_*`-Konstanten
  daneben, ein Einzelumzug würde die Navigator-Icons auf zwei Pakete verteilen.
  Sie zieht mit der ganzen Klasse nach `common/item`.
- `app/listener` nach Fachlichkeit untergliedern (OLF-L3-03)
- `ThreadHelper` und `SingletonFeatureManagerProvider` in Titan löschen und aus
  Butterfly beziehen (OLF-L2-04). **Vorbedingung:** Butterfly muss sie als
  konsumierbares Artefakt bereitstellen — heute liegen sie dort in
  `minestom/feature` und `bukkit/utils` doppelt und gehören plattformneutral nach
  `butterfly:api`. Diese Konsolidierung ist ein eigener Butterfly-PR und blockiert
  die übrigen Punkte dieser Phase nicht.
- Titans `MapProvider`/`MapEntry`/`MapPool` auf Aves' `AbstractMapProvider`,
  `MapEntry.of(...)` und `PathFilter` umstellen. (`LobbyMap` erbt bereits von
  `BaseMap` und hat bereits `sealed interface Builder permits LobbyMapBuilder`
  plus Factory — dort ist nichts zu tun.)
- `GsonFileHandler` → `ModernGsonFileHandler`: die von `MapProvider` genutzte
  Variante ist in Aves `@Deprecated(since = "1.9.0", forRemoval = true)`
- `SitHelper` zum Service mit Interface machen, `NavigationHelper` als Vorlage
  (OLF-L4-04)
- **Prüfbar:** Bestehende Tests grün, `common`-Klassenzahl sinkt

### Phase 4 — Bootstrap extrahieren

- `:bootstrap`-Modul anlegen; aus `TitanApplication.main()` werden
  `ServiceBootstrap` (Bind-Host/Port, Working-Dir, Konsole),
  `VelocityAuthResolver` und `LuckPermsBootstrap`
- Cygnus' `ServiceBootstrap` ist die Vorlage — inklusive Tests
  (`ServiceBootstrapTest`, `StopCommandTest`)
- Der Reflection-Hack für `ExtensionBootstrap(MinecraftServer)` wird beim
  Wechsel auf den OLF-Fork (OLF-L5-02) geprüft; bietet der Fork einen
  öffentlichen Konstruktor, entfällt er ersatzlos
- `app/Titan.java` und `setup/Titan.java` teilen sich die extrahierte
  Registrierungslogik
- **Prüfbar:** Bootstrap-Klassen haben Tests; `main()` unter 30 Zeilen

### Phase 5 — Extension-Split

- Wechsel auf `net.onelitefeather:minestom-extensions` (OLF-L5-02)
- `:app` wird zu `:lobby` als Extension-Modul, `:setup` ebenso; beide mit
  `compileOnly` auf Host-Abhängigkeiten (OLF-L5-03)
- `extension.json` überall über Annotation Processor (OLF-L5-01)
- Butterfly als Extension deployen statt shaden; Butterfly-Repo auf Release
  Please migrieren (OLF-L1-04)
- AOT-Cache-Training auf den Host-Jar plus Extensions anpassen
- **Prüfbar:** Host startet ohne Extensions; Lobby-Features erscheinen erst mit
  `titan-lobby.jar` in `extensions/`

### Was ausdrücklich nicht Teil des Plans ist

- Kein Umschreiben funktionierender Fachlogik ohne Regelbezug. Sit-, Elytra- und
  Tickle-Verhalten bleiben identisch; sie ziehen nur um.
- Kein Wechsel des Formatierungswerkzeugs. Titans Spotless-Setup mit
  `header.java` und dem Eclipse-Profil bleibt; Checkstyle prüft Struktur, nicht
  Formatierung. (`.editorconfig` aus Cygnus/Butterfly kann ergänzt werden, ist
  aber keine Regel.)
- Keine Änderung am CloudNet-Deployment oder an der LuckPerms-Einbindung über
  das hinaus, was Phase 4 und 5 ohnehin berühren.

---

## 8. Offene Punkte

1. **Reflection-Hack im Bootstrap.** Ob `ExtensionBootstrap` im OLF-Fork einen
   öffentlichen Konstruktor mit `MinecraftServer` anbietet, ist noch zu prüfen.
   Falls nicht, ist ein Upstream-PR gegen den Fork der saubere Weg — der Hack
   bleibt sonst dauerhaft in Phase 4 stehen.
2. **Coverage-Schwelle.** Der Startwert für das Jacoco-Gate ist noch zu
   bestimmen. Vorschlag: Ist-Stand pro Modul messen, minus 5 Prozentpunkte
   Puffer, danach pro Phase anheben.
3. **`:api` als publiziertes Artefakt.** Titan publiziert derzeit `titan-app`,
   `titan-setup` und `titan-bridge`, aber nicht `:api`. Wenn `:api` ein echter
   Contract für andere Projekte werden soll, gehört es publiziert — und braucht
   dann `@ApiStatus`-Disziplin (OLF-L4-06) und eine Kompatibilitätszusage.
   ManisGame versioniert sein `shared:api` bewusst eigenständig
   (`version = "0.5.0"` im Modul-Buildfile, unabhängig vom Root) — das ist die
   Vorlage, falls Titans `:api` publiziert werden soll.
4. **`ThreadHelper` org-weit konsolidieren.** Vier Kopien in drei Projekten
   (OLF-L2-04). Der Zielort ist zu entscheiden: `butterfly:api` (plattformneutral,
   nah an den bestehenden Nutzern) oder Aves (allgemeiner, aber
   theEvilReaper-Hoheit). Diese Entscheidung betrifft nicht nur Titan und sollte
   vor Phase 3 fallen.
5. **Verbindlichkeit für Bestandsprojekte.** Dieses Dokument beschreibt einen
   Standard, den derzeit kein Projekt vollständig erfüllt — Tamias wendet sein
   eigenes Convention-Plugin nicht überall an, Cygnus nutzt kein `@ApiStatus`,
   Xerus keine `package-info`. Zu klären ist, ob der Standard nur für neue
   Projekte und berührten Code gilt (Empfehlung) oder ob Bestandsprojekte
   nachziehen müssen.
