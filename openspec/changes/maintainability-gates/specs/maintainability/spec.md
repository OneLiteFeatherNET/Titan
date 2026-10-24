# Spec Delta

## Purpose

Legt fest, welche Wartbarkeitsregeln der Build von Titan prüft: Modulgrenzen, Architektur- und Code-Regeln, Größen- und Komplexitätsgrenzen, Javadoc der öffentlichen API in `core` sowie der Umgang mit Bestandsverstößen und Ausnahmen.

## ADDED Requirements

### Requirement: Projekt-Abhängigkeiten folgen einer Erlaubnisliste
Der Build MUSS (MUST) für jedes Modul prüfen, dass es nur Projekt-Abhängigkeiten aus der Erlaubnisliste deklariert. Eine Column DARF nur `core` und `persistence` deklarieren, ein Plattform-Modul nur `core`, `common` und `platform/extensions`, und `common` nur `core`. Die Prüfung MUSS auch `testFixtures`-Abhängigkeiten umfassen. Eine Abweichung bricht den Build ab und nennt Quell- und Zielmodul.

#### Scenario: Column hängt von einer anderen Column ab
- **WHEN** das Modul `features/navigator` das Modul `features/jumprun` als Abhängigkeit deklariert
- **THEN** bricht der Build ab und nennt `features/navigator` und `features/jumprun`

#### Scenario: Column hängt von der Laufzeit oder einer App-Variante ab
- **WHEN** eine Column `runtime` oder ein Modul unter `apps/` als Abhängigkeit deklariert
- **THEN** bricht der Build ab

#### Scenario: Plattform hängt von einem Feature ab
- **WHEN** das Modul `platform/cloudnet` ein Modul unter `features/` als Abhängigkeit deklariert
- **THEN** bricht der Build ab

#### Scenario: Erlaubte Abhängigkeiten
- **WHEN** eine Column `core`, `persistence` und die Test-Fixtures von `core` deklariert
- **THEN** läuft der Build ohne Abhängigkeitsfehler durch

### Requirement: Pakete innerhalb eines Moduls hängen nicht zyklisch voneinander ab
Pakete eines Moduls DÜRFEN NICHT (MUST NOT) zyklisch voneinander abhängen. Bestehende Zyklen stehen in der Baseline; ein neuer Zyklus bricht den Build ab. Zyklen zwischen Modulen verhindert Gradle bereits und bleiben unverändert.

#### Scenario: Neuer Paketzyklus
- **WHEN** Paket `a` eine Klasse aus Paket `b` verwendet und Paket `b` eine Klasse aus Paket `a`
- **THEN** schlägt der Architekturtest des Moduls fehl und nennt beide Pakete

#### Scenario: Bestehender Zyklus
- **WHEN** der Zyklus in der Baseline des Moduls verzeichnet ist und sich nicht vergrößert
- **THEN** schlägt der Architekturtest nicht fehl

### Requirement: Jedes Modul mit Produktionscode führt die gemeinsamen Architekturregeln aus
Jedes Modul mit Produktionscode MUSS (MUST) einen Architekturtest haben, der die gemeinsamen Regeln auf die eigenen Pakete anwendet. Fehlt dieser Test, bricht der Build ab.

#### Scenario: Neues Modul ohne Architekturtest
- **WHEN** ein neues Modul mit Produktionscode keinen Architekturtest enthält
- **THEN** bricht der Build ab und nennt das Modul

### Requirement: Logger sind private, statisch und final
Produktionscode MUSS (MUST) Logger als `private static final` deklarieren. Produktionscode loggt über SLF4J; `System.getLogger` DARF NICHT (MUST NOT) verwendet werden. Tests DÜRFEN Logger als Instanzfelder halten, um Log-Ausgaben zu erfassen.

#### Scenario: Logger als Instanzfeld im Produktionscode
- **WHEN** eine Klasse unter `src/main` ein Logger-Feld ohne `static final` deklariert
- **THEN** bricht der Build ab und nennt die Klasse

#### Scenario: System.getLogger im Produktionscode
- **WHEN** eine Klasse unter `src/main` `System.getLogger` aufruft
- **THEN** bricht der Build ab und nennt die Klasse

#### Scenario: Log-Erfassung im Test
- **WHEN** ein Test einen Logger als Instanzfeld hält, um Ausgaben zu erfassen
- **THEN** bricht der Build deswegen nicht ab

### Requirement: Produktionscode schreibt nicht auf die Konsole und druckt keine Stacktraces
Produktionscode DARF NICHT (MUST NOT) `System.out` oder `System.err` verwenden und DARF NICHT `printStackTrace()` aufrufen. Meldungen laufen über den Logger.

#### Scenario: Ausgabe auf die Konsole
- **WHEN** eine Klasse unter `src/main` `System.out.println` aufruft
- **THEN** bricht der Build ab und nennt Datei und Zeile

#### Scenario: Ausgabe über den Logger
- **WHEN** dieselbe Meldung über den Logger der Klasse ausgegeben wird
- **THEN** bricht der Build nicht ab

### Requirement: Produktionscode umgeht Sichtbarkeiten nicht per Reflexion
Produktionscode DARF NICHT (MUST NOT) `setAccessible(true)` aufrufen. Das gilt für `src/main` und `src/testFixtures`. Ausnahmen stehen namentlich in der Erlaubnisliste des Architekturtests.

#### Scenario: Reflexion in der Produktion
- **WHEN** eine Klasse unter `src/main` `setAccessible(true)` auf einem Feld aufruft
- **THEN** bricht der Build ab und nennt die Klasse

#### Scenario: Benannte Ausnahme
- **WHEN** die Test-Fixture `EventListenerCounter` in `core` Reflexion verwendet
- **THEN** bricht der Build nicht ab, weil die Klasse namentlich als Ausnahme geführt wird

### Requirement: Zeit kommt aus einem injizierten Clock
Produktionscode DARF NICHT (MUST NOT) die Systemuhr direkt lesen. Verboten sind `Instant.now()`, `LocalDate.now()`, `LocalDateTime.now()`, `ZonedDateTime.now()`, `OffsetDateTime.now()` und `System.currentTimeMillis()`. Die Zeit kommt über einen injizierten `java.time.Clock`. Dauermessungen mit `System.nanoTime()` bleiben erlaubt.

#### Scenario: Direkter Zugriff auf die Systemuhr
- **WHEN** eine Klasse unter `src/main` `Instant.now()` aufruft
- **THEN** bricht der Build ab und nennt die Klasse

#### Scenario: Zeit über einen Clock
- **WHEN** eine Klasse die Zeit über einen per Konstruktor injizierten `Clock` liest
- **THEN** bricht der Build nicht ab

### Requirement: Tests warten nicht auf die Uhr
Tests DÜRFEN NICHT (MUST NOT) `Thread.sleep` aufrufen. Zeitabhängiges Verhalten wird über einen Clock und explizit gesteuerte Ticks geprüft.

#### Scenario: Warten im Test
- **WHEN** ein Test `Thread.sleep` aufruft
- **THEN** bricht der Build ab und nennt die Testklasse

### Requirement: Statische Analyse prüft Produktionscode
Der Build MUSS (MUST) PMD mit der gemeinsamen, im Repository versionierten Regelmenge auf Produktionscode (`src/main`) ausführen. Testcode ist in dieser Stufe nicht Teil der PMD-Prüfung. Die Prüfung ist Teil von `check`. Eine Verletzung einer aktiven Regel bricht den Build ab und nennt Datei, Zeile und Regel. Einzelne Module DÜRFEN die Regelmenge NICHT abschwächen.

#### Scenario: Unbenutzte private Methode
- **WHEN** eine Klasse eine private Methode enthält, die nirgends aufgerufen wird
- **THEN** bricht der Build ab und nennt Datei, Zeile und Regel

#### Scenario: Saubere Klasse
- **WHEN** alle Klassen die aktiven Regeln erfüllen oder im Bestand verzeichnet sind
- **THEN** läuft `check` ohne PMD-Fehler durch

#### Scenario: Testcode wird nicht geprüft
- **WHEN** eine Testklasse unter `src/test` eine unbenutzte private Methode enthält
- **THEN** bricht der Build wegen PMD nicht ab

### Requirement: Methoden und Klassen bleiben klein
Eine Methode MUSS (MUST) höchstens eine zyklomatische Komplexität von 15 und höchstens 60 NCSS-Zeilen haben. Eine Klasse MUSS höchstens 500 NCSS-Zeilen haben. Diese Grenzen sind Startwerte, die vor der Einführung gegen den Bestand gemessen werden.

#### Scenario: Methode über der Komplexitätsgrenze
- **WHEN** eine Methode eine zyklomatische Komplexität von 16 hat
- **THEN** bricht der Build ab und nennt die Methode

#### Scenario: Methode unter der Grenze
- **WHEN** eine Methode eine zyklomatische Komplexität von 15 hat
- **THEN** bricht der Build deswegen nicht ab

### Requirement: Bestandsverstöße stehen in einer versionierten Baseline
Verstöße, die bei der Einführung bereits bestehen, MUSS (MUST) der Build aus einer versionierten Baseline übernehmen. Die Baseline MUSS je Modul, Datei und Regel die erlaubte Anzahl nennen. Ein Verstoß über diese Anzahl hinaus bricht den Build ab. Sinkt die Anzahl, MUSS der Build melden, dass die Baseline verkleinert werden kann. Eine Erweiterung der Baseline MUSS im Pull Request begründet und von den Maintainern freigegeben werden.

#### Scenario: Neuer Verstoß in einer Bestandsdatei
- **WHEN** eine Datei mit Baseline-Eintrag einen zusätzlichen Verstoß der Regel bekommt
- **THEN** bricht der Build ab und nennt Datei und Regel

#### Scenario: Behobener Verstoß
- **WHEN** ein Baseline-Verstoß behoben wird
- **THEN** bricht der Build nicht ab, meldet aber, dass die Baseline verkleinert werden kann

#### Scenario: Verstoß in einer neuen Datei
- **WHEN** eine neue Datei eine Regel verletzt, für die sie keinen Baseline-Eintrag hat
- **THEN** bricht der Build ab

### Requirement: Einzelne Ausnahmen sind im Code begründet
Eine einzelne Ausnahme von einer Regel MUSS (MUST) im Code mit Regelnamen und Begründung markiert sein. Bei PMD ist das `@SuppressWarnings("PMD.<Regel>")` mit einem Kommentar, bei ArchUnit eine benannte Ausnahme mit `because`-Begründung. Eine Ausnahme ohne Begründung DARF NICHT gemergt werden.

#### Scenario: Unterdrückung ohne Begründung
- **WHEN** ein Pull Request `@SuppressWarnings("PMD.<Regel>")` ohne Kommentar hinzufügt
- **THEN** lehnen die Maintainer den Pull Request im Review ab

#### Scenario: Begründete Unterdrückung
- **WHEN** ein Pull Request `@SuppressWarnings("PMD.<Regel>")` mit einem Kommentar zur Begründung hinzufügt
- **THEN** ist die Ausnahme im Review nachvollziehbar

### Requirement: Öffentliche API von core ist dokumentiert
Jeder öffentliche Typ und jede öffentliche Methode in den Paketen `net.onelitefeather.titan.core` und `net.onelitefeather.titan.api` des Moduls `core` MUSS (MUST) einen Javadoc-Kommentar haben. Die Präsenz prüft PMD (`CommentRequired`) im Modul `core`; eine Bestandslücke steht nicht in einer Baseline, sie wird behoben. `:core:javadoc` MUSS (MUST) ohne Warnungen laufen, mit doclint ohne die Gruppe `missing`, die zusätzlich `@param` und `@return` verlangt.

#### Scenario: Öffentliche Methode ohne Javadoc
- **WHEN** eine öffentliche Methode in `core` keinen Javadoc-Kommentar hat
- **THEN** bricht `:core:pmdRatchet` ab (Teil von `check`) und nennt Datei, Methode und Zeile

#### Scenario: Öffentliche Methode mit Javadoc
- **WHEN** jede öffentliche Methode in `core` einen Javadoc-Kommentar hat
- **THEN** läuft `:core:javadoc` ohne Warnungen durch und `:core:pmdRatchet` ohne Fehler

### Requirement: Jede Column beschreibt ihren Zweck in package-info
Jede Column MUSS (MUST) in `package-info.java` ihres Basispakets eine Javadoc mit Zweck und Abhängigkeiten haben. Eine README je Modul ist nicht vorgesehen.

#### Scenario: Column ohne Beschreibung
- **WHEN** die `package-info.java` einer Column keine Javadoc enthält
- **THEN** bricht der Build ab und nennt das Modul

#### Scenario: Column mit Beschreibung
- **WHEN** die `package-info.java` einer Column eine Javadoc mit Zweck und Abhängigkeiten enthält
- **THEN** bricht der Build deswegen nicht ab

