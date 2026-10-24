# Spec Delta

## Purpose

Legt fest, auf welcher Testebene ein Test liegt, wie ein Modul ohne den vollen Server getestet wird und welche Tests nur für den gesamten Scope laufen. Ziel ist, dass ein Gesamtlauf die Ausnahme ist und jedes Modul seine Prüfungen selbst mitbringt.

## ADDED Requirements

### Requirement: Jede Testklasse gehört genau einer Testebene an

Jede Testklasse MUSS genau einer Ebene zugeordnet sein: Unit, Modul-Integration, Docker-Integration, Smoke oder Sweep. Die Ebene MUSS sich aus Ort und Tag ergeben: Unit und Modul-Integration liegen in `src/test` ohne Tag, Docker-Integration in `src/integrationTest`, Smoke trägt `@Tag("smoke")` und liegt in `apps/*`, Sweep trägt `@Tag("sweep")`.

#### Scenario: Unit-Test ohne Server
- **WHEN** eine Klasse reine Logik oder einen Fake eines Ports prüft
- **THEN** liegt sie in `src/test` ohne Tag, läuft im Task `test` und braucht weder Server noch Docker

#### Scenario: Modul-Integration mit Env
- **WHEN** eine Klasse Event-Verdrahtung oder Spielerverhalten eines Moduls mit Cyano-`Env` prüft und fremde Ports durch Fakes ersetzt
- **THEN** läuft sie im Task `test` und nutzt je Test einen frischen `Env`

#### Scenario: Smoke-Test des Scopes
- **WHEN** eine Klasse den vollen Scope einer Variante aufbaut, ein Jar startet oder das zusammengeführte `application.yaml` liest
- **THEN** trägt sie `@Tag("smoke")`, liegt in `apps/*` und läuft nur im Task `smokeTest`

#### Scenario: Sweep über viele Seeds
- **WHEN** eine Klasse eine Eigenschaft über 12 oder mehr Seeds oder über lange Wege prüft
- **THEN** trägt sie `@Tag("sweep")` und läuft nur im Task `sweepTest`; im Task `test` steht eine kürzere Variante mit wenigen Seeds

### Requirement: Der Task test schließt Smoke- und Sweep-Tests aus

Der Task `test` MUSS Tests mit den Tags `smoke` und `sweep` ausschließen. Die Tasks `smokeTest` und `sweepTest` MÜSSEN je genau ihr Tag ausführen. Der Task `check` der Anwendungsmodule MUSS `smokeTest` einschließen, so dass ein vollständiger Build den Smoke-Test enthält.

#### Scenario: Entwickler führt test aus
- **WHEN** ein Entwickler `./gradlew test` startet
- **THEN** läuft keine Klasse mit Tag `smoke` oder `sweep`

#### Scenario: Vollständiger Build
- **WHEN** ein Entwickler `./gradlew build` startet
- **THEN** laufen die Unit- und Modul-Tests, der Docker-Integrationstest mit Docker und der Smoke-Test der Variante

### Requirement: Ein Modul ist ohne andere Columns testbar

Eine Column MUSS ihre Tests mit ihrem eigenen Code, den Ports aus `core` und Fakes für alle anderen Abhängigkeiten ausführen können. Ein Test einer Column DARF NICHT einen anderen Column-Code, einen Avaje-Scope mit allen Columns oder einen Kindprozess benötigen. Die Verdrahtung mehrerer Columns wird nur in der Smoke-Ebene geprüft.

#### Scenario: Column-Test ohne andere Columns
- **WHEN** die Tests einer Column im Task `test` laufen
- **THEN** ist im Testklassenpfad keine andere Column enthalten, und jede Abhängigkeit ist ein Fake oder ein Port aus `core`

#### Scenario: Zusammenspiel zweier Columns
- **WHEN** ein Test das Zusammenspiel zweier Columns prüft, etwa Navigator und Schnellbeitritt
- **THEN** liegt er in der Smoke-Ebene einer Variante und nicht in einer Column

### Requirement: Ports mit mehreren Nutzern haben einen Fake und einen Vertragstest

Ein Port, den mindestens zwei Module nutzen, MUSS in `core` `testFixtures` einen Fake und einen Vertragstest haben. Der Vertragstest MUSS sowohl die echte Implementierung als auch den Fake gegen dieselben Prüfungen laufen lassen. Ein Port mit nur einem Nutzer braucht keinen Vertragstest.

#### Scenario: Fake verhält sich wie die Implementierung
- **WHEN** der Vertragstest eines Ports mit dem Fake läuft
- **THEN** besteht er dieselben Prüfungen wie mit der echten Implementierung

#### Scenario: Port mit einem Nutzer
- **WHEN** ein Port nur in einem Modul genutzt wird
- **THEN** liegt sein Fake im Testcode dieses Moduls und wird nicht nach `core` verschoben

### Requirement: Testhelfer, die mehrere Module nutzen, liegen in core testFixtures

Ein Testhelfer, den mindestens zwei Module nutzen, MUSS in `core` `testFixtures` liegen. Jeder solche Helfer MUSS genau einmal im Code vorhanden sein. Ein Modul DARF KEINE Kopie eines Helfers aus `core` enthalten.

#### Scenario: Gleicher Helfer in zwei Modulen
- **WHEN** zwei Module eine feste oder verstellbare Uhr, einen Empfänger für Nachrichten oder einen Fake für Rechte brauchen
- **THEN** nutzen beide dieselbe Klasse aus `core` `testFixtures`

#### Scenario: Kopie eines Helfers
- **WHEN** ein Modul eine Klasse mit demselben Namen und derselben Aufgabe enthält wie ein Helfer in `core`
- **THEN** wird die Kopie entfernt und das Modul nutzt den Helfer aus `core`

### Requirement: Der Scope-Test liegt nur in apps

Tests, die den vollen Scope einer Variante aufbauen, DÜRFEN NUR in `apps/*` liegen. Tests, die nur eine Column betreffen, MÜSSEN in ihrem Modul liegen. Eine Variante DARF keine Klasse aus `features/*` testen.

#### Scenario: Feature-Test in apps
- **WHEN** ein Test in `apps/cloudnet` nur das Verhalten der Season-Column prüft
- **THEN** wird er in das Modul `features/season` verschoben

#### Scenario: Scope-Test in einem Feature
- **WHEN** ein Test in `features/*` den vollen Avaje-Scope mit allen Columns aufbaut
- **THEN** wird er in `apps/*` als Smoke-Test verschoben

### Requirement: Langläufer haben eine kurze Variante im Task test

Ein Test, dessen Laufzeit auf dem Referenzrunner (Ubuntu) 10 Sekunden überschreitet, MUSS entweder auf eine Laufzeit unter 10 Sekunden gebracht oder in eine Sweep-Variante mit `@Tag("sweep")` verschoben werden. Die Summe der Testzeit des Tasks `test` auf Ubuntu DARF 800 Sekunden NICHT überschreiten.

#### Scenario: ModeGenerationTest aufgeteilt
- **WHEN** der Task `test` auf Ubuntu läuft
- **THEN** braucht der PR-Teil von `ModeGenerationTest` höchstens 10 Sekunden, und die Sweeps laufen nur im Task `sweepTest`

#### Scenario: Budget überschritten
- **WHEN** eine Testklasse im Task `test` auf Ubuntu länger als 10 Sekunden läuft
- **THEN** begründet der Pull Request die Ausnahme oder die Klasse wird angepasst

### Requirement: Tests lesen keine versteckten Eingaben

Ein Test MUSS seine Eingaben selbst setzen: Zeit über eine injizierte Uhr, Zufall mit festem Seed, Dateien nur in einem temporären Verzeichnis und ohne Netz. Ein Test, der Systemzeit, Umgebungsvariablen oder das Netz liest, DARF NICHT im Task `test` laufen. Diese Regel ist Voraussetzung dafür, dass Gradle Testergebnisse aus dem Build-Cache wiederverwenden darf.

#### Scenario: Test mit Systemzeit
- **WHEN** ein Test `Instant.now()` oder `System.currentTimeMillis()` liest
- **THEN** wird der Aufruf durch eine injizierte `Clock` ersetzt, bevor die Klasse im Task `test` bleibt

#### Scenario: Test mit Warten
- **WHEN** ein Test mit `Thread.sleep` oder einem festen Zeitfenster auf ein Ereignis wartet
- **THEN** wird er so umgebaut, dass er den Zustand explizit weiterschaltet, etwa mit `env.tick()`

### Requirement: Der Umzug verliert keine Prüfung

Beim Umzug von Tests in eine andere Ebene oder in ein anderes Modul MUSS die Anzahl der Testfälle je Modul gleich bleiben. Eine Ausnahme ist nur ein dokumentierter Split, bei dem beide Teile die Prüfung zusammen abdecken.

#### Scenario: Testanzahl vor und nach dem Umzug
- **WHEN** die Änderung abgeschlossen ist
- **THEN** ist die Summe der Testfälle aller Ebenen je Modul gleich der Summe vor der Änderung, mit den dokumentierten Splits als einzige Abweichung
