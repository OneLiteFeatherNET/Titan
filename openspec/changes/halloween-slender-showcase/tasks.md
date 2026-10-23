# Tasks

Zweig: `feat/halloween-slender-showcase` von `origin/main`. Jeder Commit hat genau einen Typ. Vor dem Abhaken einer Aufgabe läuft `./gradlew build`. Jeder Commit endet mit `Claude-Session: https://claude.ai/code/session_01AM6yGQ33vvJqWg39vCAcYt`.

## 1. Vorlauf (`docs(openspec)`)

- [x] 1.1 Koordinaten der Halloween-Welt aus einer Kopie der Live-Welt bestimmen (Anvil-Ladung, Blockprofil). Nachweis: Begründung je Punkt in `docs/halloween-slender-showcase.md`, Abschnitt 5.1.
- [x] 1.2 OpenSpec-Artefakte (`proposal.md`, `design.md`, `specs/npc-join`, `specs/lobby-ambience`, `tasks.md`) schreiben und mit `openspec validate --strict halloween-slender-showcase` prüfen. Commit `docs(openspec): propose halloween-slender-showcase`.

## 2. Gemeinsame Auswahl nach `core` (`refactor(core)`)

- [ ] 2.1 Test zuerst (Unit, `TaskJoinTest` in `core`): `pick` nimmt den fertigen, nicht vollen Dienst mit den meisten Spielern, bei Gleichstand den kleinsten Namen; `joinable` verlangt angekündigtes Limit und nicht voll; leere Liste liefert leer. Verifikation: rot, dann grün.
- [ ] 2.2 `TaskJoin` nach `core.portal` verschieben (`pick`, `joinable`, `services(counts, task)`); `QuickJoin` und `ServerPicker.joinable` im Navigator rufen sie auf; `TaskPlayers.services` delegiert. Nachweis: `features/navigator`-Tests grün, Verhalten unverändert.
- [ ] 2.3 Commit `refactor(core): move the task join selection to core`.

## 3. Kartenmodell und Lesen (`core`, `common`, `runtime`, `setup`)

- [ ] 3.1 Test zuerst (Unit): `NpcValidator` und `AmbienceValidator` liefern Probleme für fehlende Pflichtfelder, falsche Typen, verkehrte Quader, unbekannte Partikel und Geräusche. Verifikation: rot, dann grün.
- [ ] 3.2 Test zuerst (Unit, `common`): `NpcGsonAdapter` und `AmbienceGsonAdapter` lesen die Beispiel-Karte; fehlende Felder bleiben `null`/leer; das Schild nutzt die Portal-Lesung. Verifikation: rot, dann grün.
- [ ] 3.3 `LobbyNpcs` und `LobbyAmbiences` in `core`, Felder in `LobbyMap` und `LobbyMapBuilder`, Kopierpfad `lobbyMapBuilder(map)` übernimmt sie; `PlatformBeans` liefert die Ports. Test zuerst (Unit, `LobbyMapBuilderTest`): ein Speichern mit `setspawn` lässt `npcs` und `ambience` unverändert.

## 4. Die Column `features/npc` (`feat(npc)`)

- [ ] 4.1 Test zuerst (Unit): `ClickCooldown` mit injiziertem `Clock`: zweiter Klick innerhalb der Abklingzeit wird abgewiesen, danach wieder erlaubt.
- [ ] 4.2 Test zuerst (Unit): `JoinChoice` über `TaskJoin`: fertiger Dienst wird gewählt; kein fertiger Dienst ergibt `none_ready`; nicht lesbar ergibt `unavailable`; ohne Recht `denied`.
- [ ] 4.3 Test zuerst (Unit): `ZoneVisitors` wählt Spieler im Quader oder höchstens 6 Blöcke davor; `ApparitionRules` zeigt im 14-Block-Umkreis, entfernt innerhalb von 4 Blöcken, nie zweimal pro Spieler.
- [ ] 4.4 Test zuerst (Env, `NpcSpawnTest`): ein NPC steht an der Position mit Profil und ist unverwundbar; nach 100 `env.tick()` unverändert. Dann `NpcModule`.
- [ ] 4.5 Test zuerst (Env, `NpcJoinTest`): Klick (Rechts und Links) führt zu genau einem Beitritt mit einem Test-Double für `Deliver`; zweiter Klick innerhalb der Abklingzeit führt zu keinem Beitritt; Zähler `titan.npc.interactions` wird erhöht.
- [ ] 4.6 Test zuerst (Env, `AmbienceModuleTest`): Partikel und Geräusche werden nur an Spieler in der Zone gesendet; Erscheinung nur dem betroffenen Spieler sichtbar; Verschwinden bei Nähe, Verlassen und Verbindungsende; Schilder und Seiten sind einmal gespawnt.
- [ ] 4.7 Übersetzungen `de` und `en` für `titan.npc.*` und `titan.ambience.*`; Test (Bundle): jeder Schlüssel in beiden Sprachen.
- [ ] 4.8 `ColumnArchitectureTest` für `features/npc` (nur `core`).
- [ ] 4.9 Commit `feat(npc): add join npcs and ambience zones from map data`.

## 5. Beispiel und Dokumentation (`feat(npc)`)

- [ ] 5.1 `docs/halloween-slender-showcase.md`: ein `npcs`- und `ambience`-Block für `halloween_lobbyleyna` zum Einfügen in die Live-`map.json` (Koordinaten aus der Blockanalyse, Begründung je Punkt, Hinweis zum fehlenden Skin und zu den Vanilla-Grenzen). Nachweis: JSON gegen den Validator geprüft.
- [ ] 5.2 Abschnitt in `docs/lobby-modules.md` zu `npcs` und `ambience`, kurz.

## 6. Pull Request

- [ ] 6.1 `./gradlew build` grün; `openspec validate --strict halloween-slender-showcase` ohne Fehler. Push des Zweigs; Pull-Request-Titel `feat(npc): add join npcs and a slender showcase for the halloween lobby` auf Englisch, Beschreibung auf Englisch mit der Session-URL. Nachweis: PR-URL, CI-Ergebnis (`gh pr checks --watch`).
