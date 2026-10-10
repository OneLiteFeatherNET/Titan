# Tasks

## Execution Plan

Integrationszweig: `feat/lobby-tracing-admin-permissions` von `origin/main`, **nachdem `lobby-tracing` (Fundament) auf `main` ist**. Unabhängig von den anderen Modul-Changes, sie berühren verschiedene Module. Ein Agent (sonnet, eigener Worktree) je Gruppe; Gruppen ohne Abhängigkeit laufen parallel. Vor dem Abhaken läuft `./gradlew build`.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | admin | 1.1–1.2 | sonnet | `features/admin/**` | übrige Module |
| 1 | hotbar | 2.1–2.2 | sonnet | `features/hotbar/**` | übrige Module |
| 1 | luckperms | 3.1–3.2 | sonnet | `platform/luckperms/**` | übrige Module |
| 2 | smoke / verify | 4.1–4.2 | sonnet + Mensch / haiku | lokale Läufe / read-only | Code |
| 3 | pr | 5.1 | sonnet | Git/GitHub | Code |

Regeln für jeden Agent-Prompt:
- **Built-in first:** `Telemetry` aus `core` per Konstruktor, `FeatureNode.attach(…, telemetry)` und `onTraced`; kein `GlobalOpenTelemetry`, kein SDK.
- **Test zuerst** mit `TestTelemetry` (je Test frisch), **F.I.R.S.T.:** kein `Thread.sleep`, `Clock` injiziert, frische `Env` je Test, `env.tick()`, Erfolg nur über Assertions.
- **Kein Span für Hochfrequenz-Events** (PlayerMove, Packet, Chunk, Tick): Zähler. Metriken ohne `user.id`, nur Attribute mit kleiner fester Wertemenge. Spieler nur als UUID.
- Drei unabhängige Gruppen, ein Agent je Gruppe parallel; das Szenario „Prüfung innerhalb eines Spans“ nutzt in luckperms einen Test-Span statt des Portals.
- **Commits:** `feat(telemetry): …`, ein Typ.

## 1. admin

- [x] 1.1 Test zuerst (Unit/Integration): `/stop` und `/end` erzeugen `admin.command` mit Befehl, Absender, Ergebnis, `user.id` bei Spielern; ein Spieler ohne Recht ergibt `denied` und keine Ausführung. Rot, dann `AdminCommands`/`StopCommand`/`EndCommand` mit `Telemetry` (`requires Telemetry.class`). Grün.
- [x] 1.2 Test zuerst: Der Span von `stop` endet, bevor der Stopp-Thread startet (Fake-Stopper statt `MinecraftServer.stopCleanly`); `admin.commands` stimmt. Rot, dann grün. Nachweis: `./gradlew :features:admin:build`.

## 2. hotbar

- [x] 2.1 Test zuerst (Integration, `Env`): `equip` erzeugt `hotbar.equip` mit `hotbar.items`, `user.id`; eine Item-Nutzung erzeugt `hotbar.item.use` mit `hotbar.item`, `hotbar.item.uses{item}` steigt; ein Klick ohne Lobby-Item erzeugt keinen Span. Rot, dann `HotbarLobbyItems` (`Telemetry` per Konstruktor). Grün.
- [x] 2.2 Test zuerst (Unit, `ItemConflicts`): Ein Konflikt hängt `hotbar.item_conflict` an den aktuellen Span. Rot, dann grün. Nachweis: `./gradlew :features:hotbar:build`.

## 3. luckperms

- [x] 3.1 Test zuerst (Unit, Fake-LuckPerms wie in den bestehenden Tests): Jede Prüfung erhöht `permission.checks{result}`; läuft sie in einem Test-Span, trägt dieser `permission.check` mit `permission` und `result`; ohne aktiven Span passiert nichts außer dem Zähler. Rot, dann `LuckPermsPermissionService` (`Telemetry`; Events über `Span.current()`). Grün.
- [x] 3.2 Test zuerst: `permission.platform.start` mit `permission.platform` beim Start; ein fehlendes Extension-Jar (`LuckPermsExtensionCheck`) setzt ERROR und die Ausnahme geht weiter. Rot, dann grün. Nachweis: `./gradlew :platform:luckperms:build`; `docs/lobby-modules.md` nennt Spans, Zähler und das Span-Event-Muster.

## 4. Abnahme

- [ ] 4.1 Smoke-Test mit Shaded-Jar, Agent und `logging`-Exporter: `/stop` als Konsole, Navigator-Item benutzen, ein Portal ohne Recht betreten (`permission.check`-Event am `portal.transfer`, falls dessen Change schon da ist). Ohne Agent keine Fehler. Nachweis: Checkliste im PR-Text. Nach dem Deploy (Mensch): Spans in Tempo, Zähler in Mimir.
- [x] 4.2 Verifikation (read-only): Jedes Szenario ist einem Test oder Smoke-Punkt zugeordnet, F.I.R.S.T. erfüllt. Nachweis: Zuordnungstabelle im PR-Text.

## 5. Pull Request

- [ ] 5.1 Pull Request vom Zweig `feat/lobby-tracing-admin-permissions` auf `main` unter dem Titel `feat(telemetry): trace admin commands, hotbar items and permission checks` öffnen (Titel und Beschreibung Englisch), mit Checkliste und Zuordnung. Nachweis: PR-URL, CI grün.
