# Design

## Context

Die Lobby lädt ihre Kartendaten aus `map.json` der aktiven Welt. `MapProvider` liest die Datei über `MapGson` (Adapter in `common/map`), `LobbyMap` trägt die Portale, `PlatformBeans` gibt sie als Port `LobbyPortals` an die Columns. Der Setup-Editor schreibt die Datei über `LobbyMap.lobbyMapBuilder(...)` zurück; jeder Kopierpfad muss die Felder mitnehmen, sonst gehen sie beim Speichern verloren (so ist es schon bei den Portalen geregelt).

Der Navigator wählt den Schnellbeitritt über `QuickJoin.pick` (nur fertige, nicht volle Dienste; der meiste Zulauf zuerst) und `ServerPicker.joinable`; die Dienste liest `TaskPlayers.services` über den `PlayerCounts`-Port, auf einem Hintergrund-Thread, und die Entscheidung folgt auf dem nächsten Tick. Das Recht für Slender ist `titan.navigator.slender`.

Im Halloween-Lobby-Welt-Kontext liegt der Gang bei z ≈ 157,5 zwischen x ≈ 82 und x ≈ 123. Die Auswertung der Live-Welt (Anvil, Minestom-Loader) zeigt jedoch ein anderes Bild als die Beschreibung: kein Steinziegelgang mit Dunkeleichen-Säulen, sondern ein offener Pfad aus Grobe-Erde-, Podsol- und Erdwegblöcken auf y = 79 (Standhöhe y = 80), mit einem Dach aus Fichtenhalbsteinen auf y = 85 über x ≈ 83–121, z ≈ 154–160, Fichtenbalken als Stützen alle 8 Blöcke bei x = 82, 90, 98, 106, 114, 122 und nur zwei Steinziegelpfosten bei x = 83 und x = 121. Seitenwände gibt es nicht. Die Koordinaten im Beispiel folgen deshalb der Karte, nicht der Beschreibung (siehe `docs/halloween-slender-showcase.md`).

Nebenbei: Der Dekorationsgang nutzt nur Vanilla-Blöcke und Vanilla-Partikel; der Kopf des NPCs braucht nur einen Spielerskin.

## Constraint: vanilla client only

Der Server stellt kein Ressourcenpaket bereit. Alles muss mit den Standarddaten des Vanilla-Clients funktionieren:

- Partikel nur aus dem Vanilla-Register (`white_ash`, `ash`, `soul`, `sculk_soul`).
- Geräusche nur vorhandene Vanilla-Ereignisse (`ambient.cave`, `entity.enderman.stare`). Keine eigenen Ereignisse.
- Modelle und Items nur Vanilla-Gegenstände (Papier für die Seiten). Kein `item_model` und keine `custom_model_data` auf Nicht-Vanilla-Modelle.
- Texttafeln nutzen die Standardschrift. Keine eigenen Glyphen oder Schriften.
- Der NPC nutzt eine Mannequin-Figur mit Profil. Eine Skin-Textur über `textures`-Eigenschaft gehört zur Spielerfigur des Clients und ist kein Ressourcenpaket; sie ist erlaubt und bleibt optional.

## Goals / Non-Goals

**Goals**

- Die Halloween-Lobby hat eine Slender-Zone mit Gang, NPC und Erscheinung, nur über `map.json`.
- Generisch: jede Welt kann NPCs und Zonen haben, ohne Code.
- Beitritt wie beim Navigator, mit derselben Auswahl aus `core`.
- Kosten: pro Zone ein Takt, nur Spieler in oder nahe der Zone bekommen Pakete.

**Non-Goals**

- Ein Zustand „ingame“, Skin-Lookup per Mojang, Ressourcenpakete, Spielmechanik von Slender. Der Gang ist Kulisse.

## Decisions

### D1 Eine Column `features/npc`

Beide Module (`NpcModule`, `AmbienceModule`) liegen in einer Column, weil beide dieselben Karten-Ports und dieselbe Validierung nutzen. Die Column hängt nur an `core` (ArchUnit-Regel). Die Slender-Zone ist Kartendaten; die Column kennt Halloween nicht.

### D2 Kartenmodell

- `core.npc.LobbyNpc(id, task, position, skin, label, lookAtPlayers, permission)`. `skin` ist optional (`NpcSkin(value, signature)`). `label` ist ein `PortalLabel`, damit das Schildformat der Portale gilt (Text mit `<online>/<max>`, Offline-Text, Quelle `task`/`group`/`service`/`local`). Das Recht ist wie beim Portal optional.
- `core.ambience.AmbienceZone(id, area, particles, sounds, signs, pages, apparitions)` mit dem Quader `core.portal.Box` als Gebiet. Jede Unterstruktur ist optional und leer, wenn sie fehlt.
- Die Ports `LobbyNpcs` und `LobbyAmbiences` sind funktionale Schnittstellen wie `LobbyPortals`.
- Gson liest in `common`: `NpcGsonAdapter` und `AmbienceGsonAdapter`. Der Label-Teil nutzt die vorhandene Lesung des Portal-Adapters, damit es nur eine Definition des Schildformats gibt.

### D3 Ungültige Einträge werden übersprungen, nicht abgebrochen

Anders als bei den Portalen (dort bricht der Start ab) gilt für NPCs und Zonen: `NpcValidator` und `AmbienceValidator` melden jeden Fehler. Die Column loggt pro Eintrag eine WARN-Zeile mit Welt und Grund und überspringt ihn. Eine fehlerhafte Zone soll nicht die Lobby stoppen, weil sie nur Kulisse ist.

### D4 Beitritt: `TaskJoin` in `core`

`core.portal.TaskJoin` übernimmt die Auswahl, die heute im Navigator steht:

- `joinable(service)`: angekündigtes Limit und nicht voll (wie `ServerPicker.joinable`).
- `pick(services)`: der fertige, nicht volle Dienst mit den meisten Spielern, bei Gleichstand der kleinste Name (wie `QuickJoin.pick`).
- `services(counts, task)`: die laufenden Dienste über `PlayerCounts`, oder leer, wenn der Port fehlt, das Lesen fehlschlägt (wie `TaskPlayers.services`).

Der Navigator ruft die Methoden aus `core` auf; sein Verhalten bleibt gleich, die Tests des Navigators bleiben grün. Der NPC nutzt dieselben Methoden: Lesen auf einem virtuellen Thread, Entscheidung auf dem nächsten Tick, Beitritt über `Deliver.sendPlayer` mit `serverBuilder`. Gibt es keinen fertigen Dienst, bekommt der Spieler eine Meldung statt der Auswahlliste, weil ein NPC keine Liste öffnen kann.

Zu „ingame“: Die Dienstbeschreibung `ServiceCount` kennt keinen Zustand „ingame“, und der Navigator hat kein Recht `titan.navigator.ingame`. Beides kommt mit `navigator-ingame-servers`. Diese Änderung führt keinen Zustand ein; sie übernimmt die heutige Regel.

### D5 NPC-Figur und Klick

Der NPC ist eine `Mannequin` mit `setProfile(ResolvableProfile)` (Skin optional), `setImmovable(true)`, ohne Schwerkraft und unverwundbar. Rechts- und Linksklick auf den NPC lösen den Beitritt aus (`PlayerEntityInteractEvent` und `EntityAttackEvent`). Die Abklingzeit pro Spieler kommt aus einem injizierten `Clock`; ein zweiter Klick innerhalb der Abklingzeit wird still abgewiesen, nur der Zähler zählt. Das Recht wird bei jedem Klick erneut geprüft. Ein NPC mit `lookAtPlayers` dreht den Kopf zum nächsten Spieler im Umkreis von 8 Blöcken, über einen Takt pro Zone-Ticker.

### D6 Zonen: ein Takt je Zone, Pakete nur nahe Spieler

`AmbienceModule` plant einen Takt je Zone (alle 5 Ticks), keinen je Entität. Jeder Takt betrachtet nur Spieler, die im Quader oder höchstens 6 Blöcke davor stehen:

- Partikel: pro Zone ein konfigurierbarer Typ und eine Anzahl je Takt, als Paket nur an diese Spieler, an zufälligen Punkten im Quader.
- Geräusche: pro Spieler ein Zähler in Ticks; nach einem zufälligen Intervall aus dem konfigurierten Bereich spielt der Spieler ein Geräusch (`playSound` an den Spieler, nicht an die Welt).
- Texttafeln und schwebende Seiten: gemeinsame Entitäten in der Welt (`TEXT_DISPLAY`, `ITEM_DISPLAY` mit Papier). Sie sind für alle sichtbar und werden nur einmal gespawnt, beim Start.
- Erscheinung: je Spieler und Erscheinungspunkt eine Mannequin-Figur mit `setAutoViewable(false)`, nur diesem Spieler über `addViewer` gezeigt. Sie erscheint, wenn der Spieler innerhalb von 14 Blöcken steht, spielt `entity.enderman.stare` und verschwindet, sobald der Spieler innerhalb von 4 Blöcken ist, die Zone verlässt oder die Verbindung endet. Jeder Punkt erscheint pro Spieler höchstens einmal, damit der Gang nicht zum Dauerauftritt wird.

Die Auswahl der Spieler liegt in einem reinen Rechenteil (`ZoneVisitors`), die Entitäten und Pakete in einem dünnen Teil. So prüfen Unit-Tests die Regeln ohne Server.

### D7 Telemetrie und Texte

- Metriken: `titan.npc.interactions{npc, result}` (`joined`, `cooldown`, `unavailable`, `none_ready`, `denied`), `titan.ambience.apparitions{zone}`. Keine Spans für die Kulisse.
- Übersetzungen `de` und `en` unter `features/npc/src/main/resources/titan/npc/`, Schlüssel `titan.npc.*` und `titan.ambience.*`. Schildtexte in der Karte sind MiniMessage-Text mit `<online>`/`<max>`-Platzhaltern, wie die Portale.

## Risks / Trade-offs

- Die Koordinaten stammen aus einer Kopie der Welt vom Server (Stand 23.10.2026, 4_3_0_halloween). Ändert sich die Welt, stimmen Koordinaten nicht mehr; deshalb liegen sie in der Karte, nicht im Code.
- Das Licht des Westbogens lässt sich aus der Blockdarstellung nicht sicher ablesen. Die Behauptung „beleuchtet“ wird im Spiel geprüft.
- Die Erscheinung ist pro Spieler. Bei vielen Spielern gleichzeitig steigt die Zahl der Entitäten; die Obergrenze liegt bei den Zonen der Karte (in der Praxis ein bis zwei Zonen mit je drei Erscheinungspunkten).
- `TaskJoin` wandert in `core`. Der Navigator-Test für den Schnellbeitritt zieht mit um; die Verhaltensgleichheit prüft die Navigator-Suite.
