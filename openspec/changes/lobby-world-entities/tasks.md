# Tasks

## Execution Plan

Voraussetzung ist ein Falco-Release 3.1.0 mit Entity-Loading (Falco-Design `docs/superpowers/specs/2026-10-02-anvil-entity-loading-design.md`). Welle 1 läuft schon mit Falco 3.0.0. Integrationszweig: `feat/world-entities` von `origin/main`. Jede Welle endet mit grünem `./gradlew build` und geprüften Diffs.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | loader-swap | 1.1–1.3 | sonnet | `settings.gradle.kts` (Katalog), `common/**` | `features/**`, `apps/**` (außer Tests), `runtime/**` |
| 2 | entity-bridge | 2.1–2.4 | sonnet | `settings.gradle.kts` (Version), `common/**` | `features/**`, `runtime/**` |
| 3 | docs | 3.1 | sonnet | `docs/world-conversion.md`, `README.md` | Code |
| 3 | smoke | 3.2 | sonnet | nur lokale Läufe | Code |
| 4 | verify | 3.3 | haiku | read-only | alles |
| 5 | pr | 4.1 | sonnet | Git/GitHub | Code |

Jeder Agent-Prompt nennt diese Regeln: erst Vorhandenes nutzen (Falcos Bridge, Translatoren und `UnhandledEntityHandler`, Minestom-Standardverhalten), Java 25 ohne Preview, keine Nutzertexte, SLF4J mit Parametern und Key-Values (nur das WARN aus D3), keine Metriken oder Spans, Test zuerst, schlanke Kommentare nur fürs Warum, Conventional Commits `feat(world): …`. F.I.R.S.T.: Testwelten nur als `@TempDir`-Kopie, frische `Env` je Test, `env.tick()` statt Warten, kein `Thread.sleep`, keine Systemzeit, Logs nur über einen aufgefangenen Appender prüfen.

## 1. Loader-Wechsel ohne Verhaltensänderung (Welle 1)

- [ ] 1.1 Eine kleine Testwelt als Test-Ressource in `common/src/test/resources` anlegen: wenige Chunks mit Block, Schild, Kopf, Licht, dazu `entities/` mit Rüstungsständer (klein, ohne Bodenplatte, Pose, Lederhelm), Itemrahmen (Diamant, Drehung 3), Gemälde, Text-Display („Willkommen“, Billboard center, Skalierung 2), Villager und Minecart; sie wird im echten Minecraft 1.21.11 erzeugt und per Skript reduziert. Nachweis: Ressource vorhanden, Größe unter 1 MB, README-Absatz im Testordner, wie sie entstand.
- [ ] 1.2 Charakterisierung zuerst (Integration, Cyano, `@TempDir`-Kopie der Testwelt): Mit dem heutigen `AnvilLoader` stehen Block, Schild, Kopf und Lichtwert an festen Positionen. Nachweis: Test grün vor der Änderung.
- [ ] 1.3 `falco-anvil` (3.0.0) in den Versionskatalog und in `common` aufnehmen und `MapProvider` auf `FalcoAnvilLoader` umstellen (D1, D6). Nachweis: Test aus 1.2 bleibt grün, `./gradlew build` grün, Abhängigkeitsbaum zeigt eine einzige Minestom-Version.

## 2. Entities laden (Welle 2, ab Falco 3.1.0)

- [ ] 2.1 Falco auf 3.1.0 anheben. Test zuerst (Integration, Testwelt): Rüstungsständer, Itemrahmen, Gemälde und Text-Display erscheinen nach Chunk-Load und einem Tick mit den Daten aus den Spec-Szenarien; Saisonwelt-Pfad lädt ebenso (Testwelt unter anderem Namen per `LobbyWorldChoice`/Weltname). Dann die Bridge in `MapProvider` anhängen (D2). Nachweis: Test grün.
- [ ] 2.2 Test zuerst (Integration): Chunk entladen und neu laden ergibt jede Entity genau einmal; nach dem Entladen existiert keine der Entities des Chunks. Nachweis: Test grün (Verhalten kommt aus Falco, der Test sichert die Verdrahtung).
- [ ] 2.3 Test zuerst (Unit mit aufgefangenem Appender, dann Integration): `WarnOncePerType` loggt je Typ genau ein WARN mit Typ und Welt; Villager und Minecart erscheinen nicht, alle Deko-Entities schon; ein Entity mit beschädigtem NBT fehlt allein, der Chunk lädt. Dann `WarnOncePerType` (D3). Nachweis: Tests grün.
- [ ] 2.4 Test zuerst (Integration): Angriffs- und Interaktionspakete an Rüstungsständer und Itemrahmen verändern weder Entity noch Spielerinventar; ein Rüstungsständer ohne gespeichertes `NoGravity` steht nach 100 `env.tick()` an derselben Position; nach Laden, Entladen und Herunterfahren sind die SHA-256 aller Dateien unter `entities/` unverändert. Dann den `EntitySpawnEvent`-Listener (D4). Nachweis: Tests grün, `./gradlew build` grün.

## 3. Doku, Smoke-Test, Verifikation (Welle 3–4)

- [ ] 3.1 `docs/world-conversion.md` und README: Entities unter `entities/` werden jetzt gelesen; welche Typen erscheinen (Rüstungsständer, Itemrahmen, Gemälde, Displays), dass andere Typen mit einer Warnung übersprungen werden, dass die Lobby nichts zurückschreibt. Nachweis: Doku nennt Typen und Verhalten.
- [ ] 3.2 Smoke-Test mit `titan-local.jar` und echtem Client gegen `world`, `winter` und `halloween`: Blöcke, Schilder, Köpfe, Licht wie vorher; Deko-Entities sichtbar; Schlagen und Rechtsklick ändern nichts; Server-Log ohne unerwartete Warnungen. Nachweis: Checkliste mit Ergebnis im PR-Text.
- [ ] 3.3 Verifikation (Haiku, read-only): jedes Szenario aus `specs/lobby-world-entities` einem Test zuordnen; F.I.R.S.T.-Check (Testwelt nur in `@TempDir`, keine Sleeps, keine Systemzeit, Logs über Appender). Nachweis: Bericht ohne Lücken.

## 4. Pull Request

- [ ] 4.1 Pull Request vom Integrationszweig `feat/world-entities` auf `main` unter dem Titel `feat(world): show decoration entities from lobby worlds via falco` öffnen (Titel und Beschreibung Englisch), mit Smoke-Test-Ergebnis und Verweis auf das Falco-Release. Nachweis: PR-URL, CI grün.
