# Tasks

Zweig: `feat/navigator-staging-on-demand` von `origin/main`. Vor dem Abhaken einer Aufgabe läuft der Modultest des betroffenen Moduls, und vor dem Abhaken der letzten Aufgabe einer Gruppe `./gradlew build`. Alle Tests folgen F.I.R.S.T.: keine Sleeps, keine Systemzeit (injiziertes `Clock`), frische Fixtures je Test, Ticks explizit. Jede Aufgabe nennt ihre Testpyramiden-Stufe: Unit, Integration oder E2E (manuell).

## Ausführungsplan

| Welle | Agent | Aufgaben | Modell | darf ändern | darf nicht ändern |
| --- | --- | --- | --- | --- | --- |
| 1 | Core-Agent (Worktree) | 1.1, 1.2 | haiku, effort medium | `core/src/**` | `features/**`, `bridge/**`, `platform/**` |
| 1 | Navigator-Agent A (Worktree) | 3.1, 3.2, 6.1, 6.2 | haiku, effort medium | `features/navigator/src/main/java/**/Destination.java`, `NavigatorMessages.java`, Bundles, zugehörige Tests | `NavigatorModule.java`, `core`, `bridge` |
| 2 | Adapter-Agent (Worktree, nach Welle 1) | 2.0 (Spike), 2.1 bis 2.6 | haiku, effort high | `common/**`, `platform/cloudnet/**`, `bridge/**`, `apps/cloudnet` (nur Verdrahtungstest) | `features/**`, `core` (Port ist fix), Docs |
| 2 | Navigator-Agent B (Worktree, nach Welle 1) | 3.3 bis 3.6 | haiku, effort medium | `features/navigator` Layout, `NavigatorModule.java`, `SharedNavigator.java`, `NavigatorLayout.java`, Tests | `bridge`, `platform`, `common` |
| 3 | Logik-Agent (Worktree, nach Welle 2) | 4.1 bis 4.4, 5.1 bis 5.4 | haiku, effort high | neue Klassen `StagingStarts`, `StagingIdle`, `NavigatorSettings`, `navigator.yaml`, Tests | `bridge`, `platform`, `core` |
| 4 | Telemetrie-Agent (Worktree, nach Welle 3) | 6.3, 6.4 | haiku, effort medium | `NavigatorTelemetry.java`, Telemetrie-Tests | übrige Navigator-Klassen |
| 4 | Doku-Agent (Worktree, parallel zu Welle 4) | 7.3 | haiku, effort low | `README.md`, `docs/lobby-modules.md` | jeder Java-Code |
| 5 | Hauptsitzung | 7.1, 7.2, 8.1 | - | - | - |

Welle 1 und 2 laufen je parallel, Welle 3 und 4 nacheinander, weil sie dieselben Navigator-Dateien berühren. Die Hauptsitzung prüft jeden Diff und jedes Testergebnis, bevor die nächste Welle startet. Jeder Agent bekommt die Regeln aus `design.md` (D1 bis D10), eingebaute Lösungen zuerst, Java-Idiome (Records, Pattern-Matching-`switch`), i18n in den Bundles und OpenTelemetry nur über die API.

Commit-Schema: `feat(navigator)` für Code und Tests (Wellen 1 bis 4), `docs(navigator)` für 7.3. Jeder Commit endet mit der Zeile `Claude-Session: https://claude.ai/code/session_01T1vdbUkCCTsMRsnGsQJtGL`.

## 1. Port in `core`

- [x] 1.1 Test (Unit, `core/src/testFixtures`): `OnDemandServicesContract` für den Fake: `start` nimmt die erste Anfrage an und lässt den Dienst als `STARTING` zählen; `ServiceState.ready()` ist nur für `RUNNING` mit `max > 0` wahr; `stop` ist idempotent (zweiter Aufruf liefert `false` ohne Ausnahme); `services` für einen unbekannten Task liefert eine leere Liste. Nachweis: rot vor 1.2 (Kompilierfehler gilt als rot).
- [x] 1.2 Port `OnDemandServices`, Record `ServiceState` mit Enum `Phase` (`STARTING`, `RUNNING`, `STOPPED`) und `FakeOnDemandServices` in `core` umsetzen. Nachweis: 1.1 grün; `./gradlew :core:test` grün; ArchUnit-Regeln grün.

## 2. CloudNet-Seite

- [ ] 2.0 Spike (kein Testlevel, Lookup mit `javap` gegen `driver-api-4.0.0-RC18-SNAPSHOT` aus dem Gradle-Cache): (a) wie `ServiceConfiguration` für einen Task gebaut wird, (b) ob `CloudServiceFactory.createCloudService` den Dienst startet oder nur vorbereitet (dann zusätzlich `SpecificCloudServiceProvider.start()`), (c) welche Lebenszyklen ein frisch angelegter Dienst durchläuft. Nachweis: Ergebnis im PR-Text. Weicht es ab, wird D4 angepasst, nicht der Port.
- [ ] 2.1 Test (Unit, `common`): `TitanServiceControl` ohne Adapter liefert leere Dienstliste, `start` und `stop` liefern `false`; mit einem Fake-Adapter werden die Aufrufe unverändert durchgereicht. Nachweis: rot vor 2.2.
- [ ] 2.2 Halter `TitanServiceControl` in `common` mit JDK-Typen nach dem Muster von `TitanServerConnector`. Nachweis: 2.1 grün.
- [ ] 2.3 Test (Unit, `platform/cloudnet`): `HolderOnDemandServices` erfüllt `OnDemandServicesContract` gegen einen Fake-Adapter; ohne Adapter Ablehnung ohne Ausnahme. Nachweis: rot vor 2.4.
- [ ] 2.4 `HolderOnDemandServices` als Bean im Profil `CLOUDNET`. Nachweis: 2.3 grün; Verdrahtungstest `PlatformBeansWiringTest` in `apps/cloudnet` um den Bean erweitert und grün.
- [ ] 2.5 Test (Unit, `bridge`): reine Abbildung von `ServiceLifeCycle` auf `Phase` (`PREPARED` → `STARTING`, `RUNNING` → `RUNNING`, `STOPPED` und `DELETED` → `STOPPED`, unbekannt → `STARTING`); `ServiceListing` liefert `STARTING`-Dienste mit, während der bestehende Schnellbeitritt weiter nur `RUNNING` liest. Nachweis: rot vor 2.6.
- [ ] 2.6 CloudNet-Adapter in `bridge` (in `TitanBridgePermissionExtension` oder einer eigenen Klasse): `servicesByTask`, `createCloudService`, `serviceProvider(uuid).stop()`; Holder installieren; INFO-Zeile „Service control installed“. Nachweis: 2.5 grün; `./gradlew :bridge:build` grün. Echte Aufrufe prüft 7.2.

## 3. Ziele und Layout (`features/navigator`)

- [ ] 3.1 Test (Unit, `NavigatorDestinationTest`): TAMIAS auf Platz 14 mit Recht `titan.navigator.tamias`, Task `Tamias`, in `ServerPicker.DESTINATIONS`; die vier Staging-Einträge mit Platz, Recht und Task aus `design.md` D3; `onDemand` nur bei Staging-Einträgen; `SLENDER_STAGING` nicht mehr auf Platz 16; `visible(Set.of())` enthält keinen Staging-Eintrag. Nachweis: rot vor 3.2.
- [ ] 3.2 `Destination` um TAMIAS und die drei neuen Staging-Einträge erweitern; Eigenschaft `onDemand` und Staging-Markierung einführen; Klassen-Javadoc an die Reihen anpassen. Nachweis: 3.1 grün.
- [ ] 3.3 Test (Unit/Integration, `NavigatorLayoutTest` und neu `NavigatorStagingMenuTest`): Hauptmenü ohne Rechte zeigt graue Glasscheiben auf 14 und 16; mit einem Staging-Recht steht der Hub auf 16; Staging-Einträge erscheinen nicht im Hauptmenü; Untermenü zeigt Slender (Staging), Bounce (Staging), ElytraRace (Staging), Tamias (Staging) auf 0 bis 3 und die Zurück-Taste auf 8. Nachweis: rot vor 3.4.
- [ ] 3.4 `NavigatorLayout`, `NavigatorModule` und `SharedNavigator` um den Hub-Eintrag und die Untermenü-Ansicht mit eigenem Schlüssel (vier Rechte) ergänzen; Hauptmenü-Schlüssel mit fünf Bits (vier Live-Rechte und „mindestens ein Staging-Recht“). Nachweis: 3.3 grün.
- [ ] 3.5 Test (Unit, `NavigatorPermissionTest`, `NavigatorModuleLeakTest`): beim Start genau 96 Inventare, 64 Hauptmenü und 32 Untermenü; jedes trägt die Ziele seines Schlüssels; die Zahl der Listener ändert sich beim Öffnen nicht. Nachweis: rot vor 3.6.
- [ ] 3.6 `NavigatorModule.start()`: Schlüsselbildung auf zwei Ansichten umstellen. Die Slot-Listen in `NavigatorModuleTest` und `NavigatorLayoutTest` an Platz 14 und 16 anpassen, ohne die Aussagen zum öffentlichen Menü zu ändern. Nachweis: 3.5 grün; `./gradlew :features:navigator:test` grün.

## 4. Start und Warten (`features/navigator`)

- [ ] 4.1 Test (Unit, `StagingStartsTest`, Fake-Port, Fake-`Clock`, ohne Ticks): Lesefehler führt zu Ablehnung ohne Start; kein Dienst führt zu genau einem Start; `PREPARED` führt zu keinem Start und zum Warten; `RUNNING` und bereit führt zu sofortigem Verbinden; `RUNNING` ohne Limit führt zum Warten; Cooldown nach eigenem Start verhindert einen Start; nach 90 Sekunden ergibt sich die Ablehnung und kein Stopp; Logout entfernt aus der Warteliste; zweiter Klick startet nichts; entzogenes Recht verbindet nicht. Nachweis: rot vor 4.2.
- [ ] 4.2 Klasse `StagingStarts` ohne Minestom-Typen; Abhängigkeiten sind `OnDemandServices`, `Clock` und zwei kleine Schnittstellen für Verbinden und Aktionsleiste. Nachweis: 4.1 grün.
- [ ] 4.3 Test (Integration, `NavigatorStagingStartTest`, Cyano-Env, Ticks explizit): Klick auf Slender (Staging) ohne Dienst startet genau einen Dienst; nach Bereitschaft des Fake-Dienstes und einem Refresh-Tick wird der Spieler verbunden; ein zweiter Klick startet nichts. Nachweis: rot vor 4.4.
- [ ] 4.4 Verdrahtung in `NavigatorModule`: Klick-Pfad, Prüfung im Refresh-Zyklus, `PlayerDisconnectEvent`-Listener genau einmal beim Start. Nachweis: 4.3 grün; `NavigatorModuleLeakTest` zeigt keinen Listener pro Spieler.

## 5. Leerlauf-Stopp und Konfiguration

- [ ] 5.1 Test (Unit, `StagingIdleTest`, Fake-`Clock`): nach 600 Sekunden gestoppt, bei 599 nicht; ein Spieler setzt die Dauer zurück; Lesefehler stoppt nichts und friert die Dauer ein; Live-Tasks werden nie gestoppt; ein frisches Lesen geht dem Stopp voraus (Fake zählt die Lesevorgänge); doppelter Stopp ist ohne Fehler. Nachweis: rot vor 5.2.
- [ ] 5.2 Klasse `StagingIdle`, verdrahtet im Refresh-Zyklus. Nachweis: 5.1 grün.
- [ ] 5.3 Test (Unit, `NavigatorSettingsTest`): Standard 600; 0, 59, 86401 und Text brechen den Start mit Fehler ab. Nachweis: rot vor 5.4.
- [ ] 5.4 `NavigatorSettings` und `features/navigator/src/main/resources/titan/defaults/navigator.yaml` um `stagingIdleSeconds: 600` mit Kommentar im bestehenden Stil ergänzen. Nachweis: 5.3 grün.

## 6. Texte und Telemetrie

- [ ] 6.1 Test (Unit, i18n-Prüfung im Navigator): die Schlüssel aus `design.md` D10 stehen in `messages_de.properties` und `messages_en.properties`; jeder im Code verwendete Schlüssel existiert im englischen Fallback. Nachweis: rot vor 6.2.
- [ ] 6.2 Schlüssel in beiden Bundles und in `NavigatorMessages.KEYS` ergänzen. Nachweis: 6.1 grün.
- [ ] 6.3 Test (Unit, `NavigatorStagingTelemetryTest`, `OpenTelemetryExtension` mit In-Memory-Metrik-Reader und Log-Appender): `titan.navigator.staging.starts` (Einheit `{start}`, Attribute `destination` und `result`), `titan.navigator.staging.stops` (Einheit `{stop}`), `titan.navigator.staging.wait` (Histogramm, Einheit `s`, Attribut `result`); Name, Einheit, Wert und Attribute werden geprüft; die INFO-Zeile mit Task und Dienst wird über den Log-Appender geprüft; keine Spielernamen oder UUIDs als Attribut. Nachweis: rot vor 6.4.
- [ ] 6.4 Instrumente einmal bei der Initialisierung in `NavigatorTelemetry` anlegen; Aufrufe aus `StagingStarts` und `StagingIdle`. Nachweis: 6.3 grün; kein Aufruf blockiert den Tick-Thread.

## 7. Betrieb und Verifikation

- [ ] 7.1 `./gradlew spotlessApply` und `./gradlew build` grün; `ModuleBoundariesTest` bestätigt, dass `features/navigator` nur von `core` abhängt. Nachweis: Build-Protokoll im PR.
- [ ] 7.2 Manuelle Abnahme (E2E) auf einem lokalen Testserver mit CloudNet-Testinstanz, von einer Kopie des Jars aus gebaut und nicht im laufenden Worktree: Staging-Klick startet genau einen Dienst; Aktionsleiste auf Deutsch und Englisch erscheint; Verbindung nach Bereitschaft; zweiter Klick startet nichts; Stopp nach Leerlauf (verkürzter Wert im Testlauf); Live-Task bleibt; entzogenes Recht löst nichts aus; zwei Lobbys starten keinen Dienst doppelt (oder der überzählige wird gestoppt). Nachweis: Protokoll im PR.
- [ ] 7.3 Doku (Doku-Agent): `README.md` und `docs/lobby-modules.md` mit Tamias (Platz 14), Staging-Untermenü (Platz 16), Ops-Checkliste (Tasks `Bounce-Staging`, `ElytraRace-Staging`, `Tamias`, `Tamias-Staging` anlegen; alle Staging-Tasks mit `minServiceCount: 0`; `requiredPermission` wie im Code; LuckPerms-Rechte), Schlüssel `navigator.stagingIdleSeconds` und 96 Inventaren. Nachweis: `grep -n "stagingIdleSeconds\|tamias.staging.join" README.md docs/lobby-modules.md` trifft in beiden Dateien.

## 8. Pull Request

- [ ] 8.1 Pull Request auf `main` mit dem Titel `feat(navigator): add on-demand staging entries and tamias`. Beschreibung auf Englisch: Zweck; Layout mit Untermenü; Start bei Bedarf, Warten und Zeitlimit; Leerlauf-Stopp nur für Staging-Tasks; Betriebsschritte (CloudNet-Tasks, `minServiceCount: 0`, `requiredPermission`, LuckPerms); Rollout-Reihenfolge (`bridge` und Lobby gemeinsam ausrollen); Ergebnis des Spikes 2.0; Testnachweis (`./gradlew build`) und Ergebnis von 7.2; Schlusszeile `https://claude.ai/code/session_01T1vdbUkCCTsMRsnGsQJtGL`. Nachweis: `gh pr view` zeigt Titel und Beschreibung, und die CI läuft grün.
- Nach dem Merge archiviert der Maintainer die Änderung als `docs(openspec): archive navigator-staging-on-demand`.
