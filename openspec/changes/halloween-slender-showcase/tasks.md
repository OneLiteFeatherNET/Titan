# Tasks

Zweig: `feat/halloween-slender-showcase` von `origin/main`. Jeder Commit hat genau einen Typ. Vor dem Abhaken einer Aufgabe läuft `./gradlew build`. Jeder Commit endet mit `Claude-Session: https://claude.ai/code/session_01AM6yGQ33vvJqWg39vCAcYt`.

## 1. Vorlauf (`docs(openspec)`)

- [x] 1.1 Koordinaten der Halloween-Welt aus einer Kopie der Live-Welt bestimmen (Anvil-Ladung, Blockprofil). Nachweis: Begründung je Punkt in `docs/halloween-slender-showcase.md`, Abschnitt „Ergebnis der Blockanalyse“.
- [x] 1.2 OpenSpec-Artefakte schreiben und mit `openspec validate --strict halloween-slender-showcase` prüfen (nur Warnungen zu den deutschen Formulierungen, wie bei den übrigen Änderungen). Commit `docs(openspec): propose halloween-slender-showcase`.

## 2. Gemeinsame Auswahl nach `core` (`refactor(core)`)

- [x] 2.1 Test zuerst (`TaskJoinTest` in `core`): `pick` nimmt den fertigen, nicht vollen Dienst mit den meisten Spielern, bei Gleichstand den kleinsten Namen; `joinable` verlangt angekündigtes Limit und nicht voll.
- [x] 2.2 `TaskJoin` nach `core.portal` verschoben; `QuickJoin` und `ServerPicker.joinable` entfallen, `TaskPlayers.services` delegiert. Nachweis: 264 Tests in `features/navigator` und `TaskJoinTest` grün.
- [x] 2.3 Commit `refactor(core): move the task join selection to core`.

## 3. Kartenmodell und Lesen (`core`, `common`, `runtime`, `setup`)

- [x] 3.1 Test zuerst (`NpcValidatorTest`, `AmbienceValidatorTest` in `core`): fehlende Pflichtfelder, verkehrte Quader, unbekannte Partikel und Geräusche, fehlende Schildschlüssel.
- [x] 3.2 Test zuerst (`NpcAmbienceMapTest` in `common`): die Beispiel-Karte wird gelesen; fehlende Felder bleiben `null` oder leer; Schreiben und Lesen halten die Liste.
- [x] 3.3 `LobbyNpcs` und `LobbyAmbiences` in `core`; Felder in `LobbyMap` und `LobbyMapBuilder`; der Kopierpfad `lobbyMapBuilder(map)` übernimmt sie (Test `copyKeepsNpcsAndZones`); `PlatformBeans` liefert die Ports.

## 4. Die Column `features/npc` (`feat(npc)`)

- [x] 4.1 `ClickCooldownTest`: zweiter Klick innerhalb der Abklingzeit abgewiesen, danach wieder erlaubt; pro Spieler getrennt. Injiziertes `Clock`.
- [x] 4.2 `JoinChoiceTest`: fertiger Dienst gewählt; kein fertiger Dienst ergibt `NONE_READY`; nicht lesbar ergibt `UNAVAILABLE`; ohne Recht `DENIED`.
- [x] 4.3 `ZoneAreaTest`, `ApparitionRulesTest`, `SoundScheduleTest`: Quader und Umkreis; Erscheinung ab 14 Blöcken, verschwunden ab 4 Blöcken, nie zweimal je Spieler; Geräuschtakt mit Grenzen.
- [x] 4.4 `NpcModuleTest` (Env): NPC steht an der Position mit Profil; 100 Ticks ohne Bewegung; Klick joint den bereiten Dienst; zweiter Klick in der Abklingzeit wirkt nicht; ohne Recht und ohne bereiten oder lesbaren Dienst niemand gesendet; anderes Entity ist kein NPC.
- [x] 4.5 `AmbienceModuleTest` (Env): Erscheinung nur für den Spieler in der Nähe; verschwunden bei Annäherung; Verbindungsende entfernt sie; Partikel und Geräusche nur an Spieler in oder nahe der Zone.
- [x] 4.6 `NpcMessagesBundleTest`: jeder Schlüssel in Deutsch und Englisch; Sprache folgt dem Client.
- [x] 4.7 `MapDataSkipTest`: ungültige NPCs und Zonen werden mit WARN übersprungen, der unbekannte Schildschlüssel entfernt nur das Schild (Log über Appender geprüft).
- [x] 4.8 `ColumnArchitectureTest` für `features/npc`.
- [x] 4.9 Commit `feat(npc): add join npcs and ambience zones from map data`.

## 5. Beispiel und Dokumentation (`feat(npc)`)

- [x] 5.1 `docs/halloween-slender-showcase.md`: Beispielblock für `halloween_lobbyleyna` mit Begründung der Koordinaten. Geprüft: der Block wird über `MapGson` gelesen und von beiden Validatoren ohne Problem akzeptiert (Prüfung einmalig, Test danach entfernt).
- [x] 5.2 Abschnitt „Join-NPCs und Lebendige Zonen (`npc`)“ in `docs/lobby-modules.md`.

## 6. Pull Request

- [ ] 6.1 `./gradlew build` grün; `openspec validate --strict halloween-slender-showcase` ohne Fehler. Push des Zweigs; Pull-Request-Titel `feat(npc): add join npcs and a slender showcase for the halloween lobby` auf Englisch, Beschreibung auf Englisch mit der Session-URL. Nachweis: PR-URL, CI-Ergebnis (`gh pr checks --watch`).
