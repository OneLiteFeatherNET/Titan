# Proposal

## Why

Der PR-Build führt heute jeden Test auf drei Betriebssystemen aus, unabhängig davon, welche Dateien geändert wurden. Ein Lauf dauert 13 bis 17 Minuten, der Windows-Job ist der Engpass (765 s bzw. 983 s, Median 899 s). Der Gradle-Build-Cache existiert, wird aber auf PRs nie gefüllt, weil es keinen Lauf auf `main` gibt. Der offene PR #472 schließt diese Lücke; lokal braucht eine Änderung in einer einzelnen Column dann 1 min 09 s statt 8 min 51 s.

Zwei Dinge fehlen, damit der Cache den Lauf wirklich verkürzt: die Tests des vollen Scopes laufen auf allen Betriebssystemen und werden durch jede Feature-Änderung neu ausgeführt, und es gibt keine Regel, welche Tests auf main, nachts oder im PR laufen.

## What Changes

- **Auswahl über den Gradle-Build-Cache:** Ein PR führt nur die Testtasks aus, deren Eingaben sich geändert haben, samt Abhängigkeiten (Modul, Varianten). Kein eigenes Erkennungswerkzeug. Build-Logik-Änderungen führen von selbst zu einem vollen Lauf.
- **Smoke-Tests einmal je PR:** Die Tasks `smokeTest` (Scope, Jar, Kindprozesse) laufen nur im Ubuntu-Job.
- **Mac und Windows im PR mit `test`:** Die beiden Jobs führen Unit- und Modul-Tests aus, ohne Smoke- und Sweep-Tests. Entschieden: beide führen `test` aus, siehe `design.md`.
- **Main und Nacht:** Ein Push auf `main` führt `build sweepTest` auf allen Betriebssystemen aus und schreibt den Cache. Ein nächtlicher Lauf prüft zusätzlich ohne Build-Cache.
- **Laufzeitziele:** Docs-PR höchstens 30 s, Feature-PR mit warmem Cache höchstens 5 min, PR mit Änderung in `core` höchstens 10 min, jeweils aus Job-Zeiten gemessen.

Nicht Teil dieser Änderung:

- Die Testebenen und die Tags `smoke` und `sweep`. Das ist `module-test-levels`, und diese Änderung setzt sie voraus.
- Ein Remote-Build-Cache oder ein Umbau des geteilten Workflows im Repository `OneLiteFeatherNET/workflows`.
- Release- und Publish-Workflows.

## Delivery

Der Pull-Request-Titel und der Squash-Commit sind `ci(build): run smoke tests once and sweeps on main`. Die Änderung ist nicht breaking. Die Typen der Änderungen sind getrennt: die Testebenen liegen in `test(modules)` (`module-test-levels`), die Workflows in `ci(build)` (hier). `ci(build)` passt zum Titel von #472 (`ci(build): speed up pull request builds`).

Die OpenSpec-Artefakte stehen in `docs(openspec): propose module-test-levels and ci-test-selection`.

Abhängigkeiten, in dieser Reihenfolge: PR #472 gemergt (Push-Trigger für `main`, Cache wird geschrieben); `module-test-levels` gemergt (Tags und Tasks `smokeTest`, `sweepTest` existieren).

## Capabilities

### New Capabilities

- `ci-test-selection`: welche Tests ein PR, `main` und der Nachtlauf ausführen, und die Laufzeitziele.

### Modified Capabilities

Keine. Es gibt keine bestehende Spec für CI.

## Impact

- **Workflows:**
  - `.github/workflows/build-pr.yml`: Matrix auf Ubuntu, `gradle-task: build`.
  - `.github/workflows/build-pr-cross-os.yml` (neu): Windows und macOS, `gradle-task: test`.
  - `.github/workflows/build-main.yml` (neu): Push auf `main` und Nachtlauf mit `force-build`.
- **Code:** keine Änderung an Produktionscode oder Tests. Die Tasks kommen aus `module-test-levels`.
- **Abhängigkeiten:** keine neuen Actions; alle Aufrufe nutzen den bestehenden geteilten Workflow `OneLiteFeatherNET/workflows/gradle-build-pr.yml@v2.9.0`.
- **Nutzertexte:** keine.
- **Betrieb:** Ein Push auf `main` startet den vollen Lauf auf drei Betriebssystemen; geschätzt rund 36 Runner-Minuten je Merge (Summe der Median-Job-Zeiten aus #472: 599 s + 677 s + 899 s). Der Nachtlauf kostet zusätzlich einen vollen Kaltlauf je Nacht. Der GitHub-Actions-Cache (10 GB je Repository) wird vom Build-Cache belegt.
