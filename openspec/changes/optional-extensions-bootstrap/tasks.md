# Tasks

## Execution Plan

Voraussetzung: `split-titan-into-columns` ist auf `main` gemergt. Integrationszweig: `refactor/optional-extensions-bootstrap` von `origin/main`. Welle 2 verzweigt nach dem Merge von Welle 1 vom Integrationszweig. Jede Welle endet mit grünem `./gradlew build` und geprüften Diffs.

| Wave | Agent | Task IDs | Model | May Touch | Must Not Touch |
| ---- | ----- | -------- | ----- | --------- | -------------- |
| 1 | startup | 1.1–1.5 | sonnet | `core/**`, `runtime/**` | `features/**`, `platform/**`, `apps/**`, `common/**` |
| 2 | extensions-platform | 2.1–2.3 | sonnet | `platform/extensions/**`, `settings.gradle.kts` | `runtime/src/main/**` außer Build-Datei, `features/**`, `common/**` |
| 2 | cloudnet-platform | 3.1–3.3 | sonnet | `platform/cloudnet/**`, `common/src/**/deliver/**`, `common/src/**/utils/CloudNetEnvironment.java`, `runtime/src/**/bootstrap/**` (nur Deliver-Fallback) | `features/**`, `platform/extensions/**`, `bridge/**` |
| 3 | variants-docs | 4.1–4.4 | sonnet | `buildSrc/**`, `apps/**`, `platform/luckperms/**` (nur 4.2), `README.md`, `docs/**` | `features/**`, `core/src/main/**` |
| 3 | verify | 4.5 | haiku | read-only | alles |
| 4 | pr | 5.1 | sonnet | Git/GitHub | Code |

Jeder Agent-Prompt nennt die Regeln, die für seinen Task gelten: erst Vorhandenes nutzen (`ServiceLoader`, Avaje `builder().bean(...)`/`@Secondary`, minestom-extensions unverändert), Java 25 ohne Preview (Pattern-Matching-`switch` für die Auswahl), keine neuen Nutzertexte, SLF4J mit Parametern (INFO `Server bootstrap: {}`, eine Ausnahme wird einmal geloggt), keine neuen Metriken/Spans, F.I.R.S.T. (kein Arbeitsverzeichnis, sondern `@TempDir` und injizierte Pfade), Test zuerst, schlanke Kommentare, Conventional Commits `refactor(bootstrap): …`.

## 1. Vertrag und Startablauf (Welle 1)

- [ ] 1.1 `ServerBootstrap` in `core` anlegen (D1). Nachweis: `./gradlew :core:build` grün.
- [ ] 1.2 Test zuerst (Unit): `ServerBootstraps.select` liefert ohne Kandidaten den Rückfall, bei einem genau diesen und wirft bei zweien eine Ausnahme mit beiden Namen (D2). Danach umsetzen. Nachweis: Tests grün.
- [ ] 1.3 `PlainMinestomBootstrap` in `runtime` (D3). Nachweis: `./gradlew :runtime:build` grün.
- [ ] 1.4 Charakterisierung zuerst (Unit): Das Auslesen von Secret und Bind-Adresse als reine Funktionen aus `TitanApplication` herausziehen und testen (Datei vor Property, leere Datei ignoriert, Standard `localhost:25565`). Pfad und Properties werden übergeben, statt global gelesen. Nachweis: Tests grün, Verhalten unverändert.
- [ ] 1.5 Test zuerst (Unit): `TitanStartup` ruft mit Fake-Schritten in der Reihenfolge `init` → Exception-Handler → Scope bauen → `start` auf, und bei einem Fehler beim Scope-Bau kein `start`. Danach `main` auf `TitanStartup` umstellen; die gewählte Instanz kommt per `builder().bean(ServerBootstrap.class, …)` in den Scope; INFO `Server bootstrap: {}`; `runtime` hängt nicht mehr an `minestom-extensions`, und bis Welle 2 fällt der Start auf `PlainMinestomBootstrap` zurück. Nachweis: Tests grün, `grep -rn hollowcube runtime/src/main` ohne Treffer.

## 2. `platform/extensions` (Welle 2)

- [ ] 2.1 Modul `platform/extensions` anlegen (Scan von `platform/*` in `settings.gradle.kts`, falls noch nicht vorhanden), Abhängigkeit `minestom-extensions`. Nachweis: `./gradlew :platform:extensions:build` grün.
- [ ] 2.2 Test zuerst (Unit): Die Hilfsfunktion „geladene Extensions → Namen“ liefert die Namen in stabiler Reihenfolge. Danach `ExtensionServerBootstrap` samt Eintrag in `META-INF/services` umsetzen (D4). Nachweis: Tests grün, die Service-Datei liegt im Jar.
- [ ] 2.3 Integration, soweit Cyano es zulässt: Ein Start mit `ExtensionServerBootstrap` und leerem `extensions/` im `@TempDir` meldet `extensions` als aktiven Bootstrap. Andernfalls die Begründung im Agent-Ergebnis festhalten und auf 4.4 verweisen. Nachweis: Test grün bzw. Begründung.

## 3. `platform/cloudnet` (Welle 2)

- [ ] 3.1 Modul `platform/cloudnet` anlegen (Abhängigkeiten `core`, `common`, `platform/extensions`). Nachweis: `./gradlew :platform:cloudnet:build` grün.
- [ ] 3.2 Test zuerst (Unit, `@TempDir`): Mit `.wrapper` ist der Deliver-Bean `MessageChannelDeliver`, ohne `.wrapper` `DebugDeliver`. Danach `MessageChannelDeliver` und `CloudNetEnvironment` (mit injiziertem Pfad) aus `common` verschieben und `DeliverProvider` entfernen (D6); bestehende Deliver-Tests ziehen mit. Nachweis: Tests grün, `grep -rn DeliverProvider` ohne Treffer.
- [ ] 3.3 `DebugDeliver` als `@Secondary`-Fallback in `runtime`; `PlatformBeans` erzeugt keinen `Deliver` mehr. Nachweis: `NavigatorModuleTest` und der Starttest von `apps/local` grün (Deliver ist `DebugDeliver`).

## 4. Varianten, Doku und Abnahme (Welle 3)

- [ ] 4.1 `apps/cloudnet` setzt `platform("extensions")` und `platform("cloudnet")`, `apps/local` keins; führt `platform("…")` ein, falls `permission-spi` noch nicht gemergt ist (D7). Die Prüfung erwarteter Module zählt `ServerBootstrap.name()` mit. Test zuerst (Unit): `missingModules` meldet `extensionsPlatform`, wenn der aktive Bootstrap `minestom` heißt, aber `extensions` erwartet wird. Starttests beider Varianten prüfen den aktiven Bootstrap. Nachweis: `./gradlew build` grün.
- [ ] 4.2 Nur wenn `permission-spi` gemergt ist: Die Prüfung auf doppeltes LuckPerms in `platform/luckperms` auf `ServerBootstrap.loadedExtensions()` umstellen und die Abhängigkeit auf minestom-extensions dort entfernen. Test zuerst (Unit mit Fake-`ServerBootstrap`). Nachweis: Tests grün, `./gradlew :apps:local:build -Ptitan.luckperms` grün. Ist `permission-spi` nicht gemergt: Task mit Verweis auf D7 abhaken, nachdem das `permission-spi`-Design entsprechend angepasst ist.
- [ ] 4.3 README und Deploy-Doku: `local` lädt keine Extensions, `cloudnet` wie bisher; Start-Log-Zeile `Server bootstrap: …`. Nachweis: Die Doku nennt beide Varianten mit ihrem Bootstrap.
- [ ] 4.4 Manuelle E2E-Abnahme: `titan-cloudnet.jar` mit CloudNet-Bridge und Titan-Bridge in `extensions/`: Beide sind geladen, und das Log zeigt `Server bootstrap: extensions`. Die Navigator-Weiterleitung in CloudNet klappt, außerhalb von CloudNet kommt die Chat-Nachricht. `titan-local.jar` mit gefülltem `extensions/`: nichts geladen, Log `Server bootstrap: minestom`. `forwarding.secret` und `-Dservice.bind.port` wirken in beiden. Nachweis: Protokoll im PR.
- [ ] 4.5 Verifikation (Haiku, read-only): Szenarien von `server-bootstrap` Test für Test bzw. Abnahmepunkt zuordnen; F.I.R.S.T.-Check (keine Zugriffe auf das echte Arbeitsverzeichnis, keine System-Properties ohne Zurücksetzen). Nachweis: Bericht ohne Lücken.

## 5. Pull Request

- [ ] 5.1 Den Pull Request vom Integrationszweig auf `main` unter dem Titel `refactor(bootstrap)!: make minestom extensions a platform module` öffnen, mit dem BREAKING-CHANGE-Footer aus dem Proposal, dem Hinweis auf die Abweichungen (ServiceLoader statt zweitem Scope, Bind-Adresse bleibt in `runtime`) und dem Abnahmeprotokoll in der englischen Beschreibung. Nachweis: PR-URL, CI grün.
