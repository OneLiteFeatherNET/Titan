# Tasks

## 0. Voraussetzung: lobby-settings zuerst

- [ ] 0.1 `lobby-settings` (Settings-Schnittstelle in `core`, Schlüssel-Register, Otis-Anbindung, Menü) ist implementiert und gemerged. Ohne diesen Schritt beginnt Abschnitt 2 nicht.
- [ ] 0.2 Schlüssel `titan:tag.enabled` (Boolean, Standard `false`) im Schlüssel-Register von `lobby-settings` eintragen.

## 1. Vorlauf

- [x] 1.1 OpenSpec-Artefakte (`proposal.md`, `design.md`, `specs/lobby-tag/spec.md`, `tasks.md`) schreiben und mit `openspec validate --strict lobby-tag` prüfen. Commit `docs(openspec): propose lobby-tag`.
- [x] 1.2 Minestom-Schnittstellen am Quelltext `2026.10.07-26.2` geprüft: `EntityAttackEvent` ist nicht abbrechbar (Abweichung, siehe `design.md` Kontext und D2); `EntityDamageEvent` ist abbrechbar; `Entity#setGlowing`, `Particles.DUST`, `EntityAnimationPacket.Animation.TAKE_DAMAGE`, `PlayerStart/StopFlyingWithElytraEvent` vorhanden; kein Team nötig.
- [x] 1.3 Offene Fragen entschieden: kein eigenes Scoreboard-Team (Glowing in Standardfarbe, rote Partikel, Actionbar); 120 s Inaktivität und 5 s Rücktausch-Sperre; inaktive Nicht-Fänger bleiben im Spiel; `LobbyPlayerBusyEvent` akzeptiert und für `lobby-throw` nutzbar.

## 2. Core-Ereignis und Opt-in (`core`, `features/tag`)

- [x] 2.1 Test zuerst (Unit, `LobbyPlayerBusyEventTest`): Record trägt Spieler und Zustand. Verifikation: rot, dann grün.
- [x] 2.2 `LobbyPlayerBusyEvent` in `core` (`net.onelitefeather.titan.core.module`).
- [x] 2.3 Test zuerst (Unit, `TagOptInTest`): Standard aus; Umschalten über die Schnittstelle; `/tag` dreht den Wert; Schnittstelle nicht erreichbar lässt den Zustand unverändert und meldet.
- [x] 2.4 `TagOptIn` gegen die Schnittstelle aus Abschnitt 0; `TagCommand` (`tag`, englisch) und `TagCommands` (`@Singleton`, registriert und meldet ab). Konsole erhält Meldung.
- [x] 2.5 Meldungen `titan/tag/messages_de.properties` und `messages_en.properties`; Test `TagMessagesTest` (gleiche Schlüssel, alle gerendert).

## 3. Spielzustand (`features/tag`, rein)

- [x] 3.1 Test zuerst (Unit, `TagGameTest`, je ein Verhalten pro Test, `AdjustableClock`, `Random` mit festem Seed): zweiter aktiver Spieler bekommt Rolle; weniger als zwei → kein Fänger; Fänger verlässt → zufälliger Nachfolger; Austritt ohne Fänger ändert nichts.
- [x] 3.2 Test zuerst (Unit): Treffer des Fängers überträgt Rolle; Rücktausch innerhalb 5 s gesperrt; nach 5 s erlaubt; Treffer ohne Fänger ändert nichts; Sperre gilt nur für das Paar.
- [x] 3.3 Test zuerst (Unit): 120 s ohne Bewegung, Treffer oder Eingabe gibt die Rolle weiter; Bewegung setzt die Zeit zurück.
- [x] 3.4 `TagGame`, `TagOutcome`, `TagCooldown` implementieren.

## 4. Minestom-Verdrahtung (`features/tag`)

- [x] 4.1 Test zuerst (Env, `TagModuleIntegrationTest`, `env.tick()`, je ein Verhalten pro Test): Schaden zwischen aktiven Spielern wird abgebrochen (`EntityDamageEvent`), also ohne Schaden und Rückstoß; Treffer des Fängers überträgt Rolle mit Ton und Titel; inaktiver Spieler wird nicht verändert; Disconnect entfernt Spieler; Instanzwechsel entfernt Spieler.
- [x] 4.2 Test zuerst (Env): Fänger leuchtet, Actionbar nennt ihn; Partikel laufen nur für die Instanz; kein Scoreboard-Team wird angelegt.
- [x] 4.3 Test zuerst (Env): Gleitflug (Treffer zählt nicht), `LobbyPlayerBusyEvent` (Fänger verliert Rolle). Tanz und Sitzen haben keinen Zustand im Fangen-Modul (ein Treffer bleibt gültig, die Pose wird nicht gelesen); sie werden in 8.2 sichtgeprüft.
- [x] 4.4 `TagModule` (`@PostConstruct`: `FeatureNode` mit Priorität 620; Listener für `EntityAttackEvent`, `EntityDamageEvent` (Schaden abbrechen), `PlayerMoveEvent` und `ClientInputPacket` (Aktivität), `PlayerSpawnEvent`, `PlayerDisconnectEvent`, `RemoveEntityFromInstanceEvent`, `LobbyPlayerBusyEvent`, `PlayerStartFlyingWithElytraEvent`, `PlayerStopFlyingWithElytraEvent`; `@PreDestroy`: Knoten schließen, Zustand aufräumen).
- [x] 4.5 `TagEffects` (Glowing in Standardfarbe, Partikel, Ton, Titel, Actionbar, Trefferanimation; kein Team).
- [x] 4.6 Test zuerst (Env): Nach `close()` löst kein Event mehr Code des Moduls aus (Muster `SpawnModuleTest#stopLeavesNoListenerBehind`).
- [x] 4.7 `package-info.java` mit `@InjectModule(name = "tagColumn", ...)`, `ColumnArchitectureTest` (wie bei den übrigen Columns).

## 5. Ereignis-Sender in anderen Columns

- [x] 5.1 Test zuerst (Env, `jumprun`): Lauf-Start sendet `LobbyPlayerBusyEvent(true)`, Lauf-Ende `(false)`.
- [x] 5.2 `jumprun` sendet das Ereignis an Lauf-Start und -Ende.
- [x] 5.3 Test zuerst (Env, `portal`): Übergabe-Start sendet `LobbyPlayerBusyEvent(true)`.
- [x] 5.4 `portal` sendet das Ereignis beim Start der Übergabe.

## 6. Telemetrie

- [x] 6.1 Test zuerst (Unit, `TagTelemetryTest` mit `TestTelemetry`): `titan.tag.tags{result}` und `titan.tag.toggles{state}`, keine Spieler-Id im Attribut.
- [x] 6.2 `TagTelemetry` implementieren und in Treffer und Schalter verdrahten.

## 7. Dokumentation

- [ ] 7.1 `docs/lobby-modules.md`: Abschnitt „Fangen (`tag`)“ mit Opt-in, Befehl, Regeln und Interaktionen; Ereignis `LobbyPlayerBusyEvent` in der Core-Liste.
- [ ] 7.2 `README.md`: Befehl `/tag` ergänzen, sofern dort Befehle aufgeführt sind.
- [ ] 7.3 Spec-Delta mit der Implementierung abgleichen (Szenario-Titel, Zeitwerte, Priorität).

## 8. Prüfung

- [ ] 8.1 `./gradlew spotlessApply` und `./gradlew build` grün; `openspec validate --strict lobby-tag` grün.
- [ ] 8.2 Sichtprüfung auf einem Testserver: Opt-in über das Menü, `/tag`, Glow, Partikel und Actionbar, Treffer ohne Schaden, Butterfly-Präfixe unverändert. Nicht ausgeführt, falls kein Testserver verfügbar; im PR als offen vermerken.

## 9. Commits und Pull Request

- [ ] 9.1 Commit `docs(openspec): propose lobby-tag` (Artefakte und Aufgaben).
- [ ] 9.2 Commits je Typ: `feat(tag): ...` für Code und Tests, `docs(...)` für Dokumentation, getrennt nach Typ.
- [ ] 9.3 Pull Request öffnen mit dem Titel `feat(tag): add an always-open tag game to the lobby`. Beschreibung auf Englisch, mit Verweis auf `lobby-settings` als Voraussetzung. Abschließend `https://claude.ai/code/session_01AM6yGQ33vvJqWg39vCAcYt`. Danach nur auf grüne CI warten; Merge und Release macht der Nutzer.
