# Design

## Context

Siehe proposal.md für Ursache und Umfang. `SeasonConfigReader` liest über die statische `Config`-Fassade; `Config` initialisiert sich einmal je JVM, deshalb lässt sich darüber keine echte YAML-Datei pro Test laden. `SeasonSettings` und `seasonIds()` (Erkennung über `Config.asProperties()`) bleiben unverändert. Der Change ist klein und berührt ein Modul; dieses Dokument hält nur die eine Entscheidung fest, die den Test erst möglich macht.

## Goals / Non-Goals

**Goals:**
- Ein Regressionstest lädt echte YAML wie die Produktion (SnakeYAML im Klassenpfad).
- Die Fehlermeldung für fehlendes `from`/`to` nennt den Quoting-Hinweis, beim Start und im Live-WARN.

**Non-Goals:**
- Kein `Date`-Support, kein eigener YAML-Lader (siehe proposal.md, verworfen).
- Keine Änderung an Erkennung der Saison-Ids, Fensterlogik oder Neustart-Ablauf.

## Decisions

**D1: `SeasonConfigReader` bekommt eine `Configuration` per Konstruktor.** Produktion übergibt `Config.asConfiguration()` (lebt weiter, Live-Lesen und `Config.setProperty` in `SeasonModuleTest` bleiben gültig); der neue Test baut `Configuration.builder().load(datei).build()` aus einer YAML-Datei im `@TempDir`. Eingebaut geprüft: `Config.asConfiguration()` existiert in avaje-config 5.2 und hat `getOptional`, `getBool`, `getAs`, `asProperties`. Verworfen: `props.file`/Systemeigenschaft setzen (Singleton, nicht wiederholbar, bricht Unabhängigkeit). Test: Unit, ohne Server. SOLID: Abhängigkeit über Konstruktor statt statischem Singleton (DIP).

**D2: Hinweis nur bei `from`/`to`.** `value(...)` hängt ` (quote date-times: from: "2026-12-01T00:00:00")` an „is required“, nur für die Felder `from` und `to`; `world` behält den bisherigen Text. Es entsteht kein neuer Prüfpfad: Die Saison wird weiter über ihre übrigen Schlüssel erkannt. Test: Unit auf Startfehler (`IllegalStateException`-Text) und auf die WARN-Zeile über einen aufgefangenen `ListAppender`. SOLID: keine neue Verantwortung, die Meldung bleibt in einer Methode (SRP).

**D3: SnakeYAML nur im Test.** `testRuntimeOnly(libs.snakeyaml)` in `features/season`, Version aus dem Katalog (2.7, dieselbe wie in `common`; `features/navigator` macht es genauso). Produktion bleibt unberührt, da `apps/cloudnet` es schon transitiv liefert. Der Katalog-Eintrag hält Test und Produktion auf derselben Version.

**Logging:** keine neue Zeile; die bestehende WARN-Zeile aus `readLive()` trägt den erweiterten Text (WARN, einmal je unverändertem Wert). Keine Metriken, keine Spans, keine Nutzertexte.

## Risks / Trade-offs

- [SnakeYAML im Test ändert das Verhalten bestehender Tests] → Sie nutzen `Config.setProperty` und laden keine Datei; `./gradlew :features:season:test` bestätigt das.
- [Betreiber bemerken die Doku nicht] → Die Fehlermeldung nennt selbst das Quoting; das ist der eigentliche Schutz, die Doku ergänzt ihn.
