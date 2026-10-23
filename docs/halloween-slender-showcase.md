# Slender-Showcase in der Halloween-Lobby

Dieses Dokument enthält die Kartendaten für die Welt `halloween_lobbyleyna` (Saison `halloween`). Es beschreibt die Felder `npcs` und `ambience` aus `map.json`, die Koordinaten und die Begründung je Punkt aus der Blockanalyse der Live-Welt. Eingefügt werden die Blöcke in die `map.json` der Live-Welt, nachdem sie geprüft sind; die Welt selbst wird nicht verändert.

## Ergebnis der Blockanalyse

Die Welt wurde aus einer Kopie des Servers (`4_3_0_halloween`) mit dem Anvil-Lader von Minestom geladen und an den Koordinaten x 60–130, y 74–90, z 145–170 untersucht. Die Beschreibung der Schlucht weicht von der Karte ab:

- **Kein Steinziegelgang mit Dunkeleichen-Säulen.** Im Bereich liegt kein Dunkeleichenblock. Der Gang besteht aus Erdweg (`dirt_path`), Podsol, Grobe Erde und Erde auf der Höhe y = 79. Die Stehhöhe ist y = 80.
- **Dach.** Fichtenhalbstufen (`spruce_slab`) auf y = 85 über x ≈ 83–121 und z ≈ 154–160. Darüber und darunter Fichtenbalken.
- **Stützen.** Fichtenstämme (`spruce_log`) alle 8 Blöcke bei x = 82, 90, 98, 106, 114, 122, sowie Balken bei z = 152 und z = 162.
- **Steinziegelpfosten.** Nur zwei Pfosten aus `stone_bricks` bei x = 83 und x = 121, y 80–84.
- **Keine Seitenwände.** Auf y = 80 bis 83 liegen zwischen den Pfosten keine Blöcke. Der Gang ist seitlich offen, die Ränder sind Packed Mud und Gras.
- **Der Pfad läuft nach Westen weiter.** Bei x = 75–81 bleibt der Pfad (`dirt_path`, Podsol) auf y = 79 mit Stehhöhe y = 80 und Luft darüber, mittig bei z ≈ 155–158. Bei x ≤ 70 liegt kein begehbarer Boden auf y = 79.

Daraus folgen die Koordinaten:

- **Zone `slender-tunnel`.** Der Quader x 82–123, y 78–90, z 152–163 deckt das überdachte Stück (x 82–122, z 152–162) und den Pfad ab. Der Gang liegt auf der Achse z ≈ 157,5, wie gewünscht.
- **Der Slender-NPC auf (76,5; 80; 157,5), Blick nach Osten (yaw −90).** Der Block darunter ist Packed Mud, darüber ist mindestens 3 Blöcke Luft (y 80–82). Der Stand liegt auf der Achse, fünf Blöcke westlich des Gangendes, so dass Spieler ihn beim Laufen nach Westen die ganze Zeit sehen.
- **Erscheinungspunkte** auf der Achse bei x = 112,5, 98,5 und 86,5 (Stehhöhe y = 80, Luft darüber). Jeder liegt einen Posten-Abstand von 12–14 Blöcken auseinander und erscheint in 14 Blöcken Umkreis, so dass die Gestalt vor dem Spieler auftaucht und beim Näherkommen verschwindet.
- **Texttafeln** schweben vor dem nördlichen Balken (z = 152,5, y = 82) bei x = 100,5 und x = 110,5. Beide Stellen liegen zwischen Stützen (Stützen bei 98 und 106, 106 und 114), also frei von Blöcken. Der Gang hat dort keine Wand, die Tafeln stehen frei.
- **Schwebende Seiten** (Papier) an den Stützen bei (90,5; 81,5; 151,5), (106,5; 81,5; 151,5) und (114,5; 81,5; 162,5).

Was die Analyse nicht sicher zeigt: ob der Westbogen beleuchtet ist. Die Blockdarstellung enthält keine Lichtwerte. Der Bogen ist im Spiel zu prüfen.

## Skin

In der Organisation OneLiteFeatherNET liegt keine Skin-Textur für Slender. Das Minispiel Cygnus nutzt Spielerskins zur Laufzeit. **Platzhalter:** Das Feld `skin` des NPCs fehlt im Beispiel. Der NPC zeigt dann die Standardfigur. Sobald eine Textur vorliegt, kommt sie als `skin: { "value": "...", "signature": "..." }` in den NPC-Eintrag. Die Signatur wird nicht erfunden.

## Beispiel für `map.json`

Der Block passt in die bestehende `map.json` neben `spawn` und `portals`. Die Felder `npcs` und `ambience` sind optional.

```json
{
  "npcs": [
    {
      "id": "slender",
      "task": "Slender",
      "position": { "x": 76.5, "y": 80.0, "z": 157.5, "yaw": -90.0, "pitch": 0.0 },
      "lookAtPlayers": true,
      "permission": "titan.navigator.slender",
      "label": {
        "position": { "x": 76.5, "y": 82.9, "z": 157.5 },
        "text": "<gold>Slender<newline><gray><online>/<max> Online",
        "offlineText": "<red>Slender offline",
        "source": { "type": "task", "name": "Slender" },
        "billboard": "center"
      }
    }
  ],
  "ambience": [
    {
      "id": "slender-tunnel",
      "area": {
        "min": { "x": 82.0, "y": 78.0, "z": 152.0 },
        "max": { "x": 123.0, "y": 90.0, "z": 163.0 }
      },
      "particles": { "type": "white_ash", "count": 3 },
      "sounds": { "key": "ambient.cave", "minSeconds": 20, "maxSeconds": 60 },
      "signs": [
        { "position": { "x": 100.5, "y": 82.0, "z": 152.5 }, "key": "titan.ambience.sign.watching" },
        { "position": { "x": 110.5, "y": 82.0, "z": 152.5 }, "key": "titan.ambience.sign.pages" }
      ],
      "pages": [
        { "position": { "x": 90.5, "y": 81.5, "z": 151.5 } },
        { "position": { "x": 106.5, "y": 81.5, "z": 151.5 } },
        { "position": { "x": 114.5, "y": 81.5, "z": 162.5 } }
      ],
      "apparitions": [
        { "position": { "x": 112.5, "y": 80.0, "z": 157.5 } },
        { "position": { "x": 98.5, "y": 80.0, "z": 157.5 } },
        { "position": { "x": 86.5, "y": 80.0, "z": 157.5 } }
      ]
    }
  ]
}
```

## Vanilla-Grenzen

Der Server liefert kein Ressourcenpaket. Die Zone verwendet nur Inhalte, die der Vanilla-Client kennt:

- Partikel: `white_ash`, `ash`, `soul`, `sculk_soul`, `end_rod`.
- Geräusche: `ambient.cave`, `entity.enderman.stare`, `ambient.soul_sand_valley.mood`, `ambient.crimson_forest.mood`.
- Gegenstände: Papier. Texttafeln nutzen die Standardschrift.
- Der Skin läuft über die Spielerprofil-Textur, keine Ressource.

Andere Namen lehnt die Validierung ab und überspringt die Zone mit einer Warnung im Log.

## Bedeutung der Felder

- `npcs[].permission`: ohne Recht klickt der Spieler ins Leere, und es gibt eine Meldung. Hier das Recht für Slender aus dem Navigator.
- `npcs[].lookAtPlayers`: der Kopf folgt dem nächsten Spieler im Umkreis von 8 Blöcken.
- `ambience[].sounds`: ein zufälliges Intervall zwischen `minSeconds` und `maxSeconds` pro Spieler.
- `ambience[].sign.key`: nur die Schlüssel `titan.ambience.sign.watching` und `titan.ambience.sign.pages`. Die Texte stehen in `titan/npc/messages_de.properties` und `messages_en.properties`.
- `ambience[].apparitions`: eine Gestalt pro Punkt und Spieler, sichtbar ab 14 Blöcken, verschwunden ab 4 Blöcken, höchstens einmal je Spieler.

## Prüfung nach dem Einfügen

Im Spiel prüfen:

1. Der NPC steht bei (76,5; 80; 157,5), schaut nach Osten, zeigt das Schild `Slender <online>/<max>`.
2. Ein Klick (links oder rechts) mit `titan.navigator.slender` schickt in einen bereiten Slender-Dienst. Ohne Recht erscheint die Meldung.
3. Beim Gang nach Westen erscheint die erste Gestalt bei x = 112,5, verschwindet bei 4 Blöcken.
4. Die Tafeln sind auf Deutsch oder Englisch lesbar, je nach Client.
5. Das Licht des Westbogens ist ausreichend.
