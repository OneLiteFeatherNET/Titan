# Proposal

## Why

Spieler melden, dass der Modus Medium seit `jumprun-more-surfaces` (#367) zu schwer ist. Der Kern der Beschwerde sind Sprünge auf schmale Blöcke mit winziger Trittfläche („Nippel-Sprünge“): Zäune, Mauern, Kerzen, Blumentöpfe, Köpfe, Gitter, Scheiben, Pfosten. Medium schaltet diese Formen ab Score 25 bzw. 40 frei, und ihr Anteil an den Sprüngen ist dort fast vollständig: gemessen an 20 festen Läufen mit je 300 Sprüngen liegt er bei Score 40 bei rund 96 % und bei Score 80 bei rund 99,8 %.

Die Ursache ist die Freischaltung je Modus: Medium hat die Tiers `narrow` und `narrowest` mit Schwellen 25 und 40, und die Zielkosten skalieren auf die schwersten freigeschalteten Formen. Sobald die schmalen Formen da sind, wählt der Generator sie fast immer, weil die Lücke bei höchstens 4 Blöcken gedeckelt ist und nur hohe Formkosten die hohen Zielkosten erreichen.

## What Changes

- Medium schaltet nie eine schmale Form frei. Schmal sind die Formen, deren Lauffläche schmaler ist als ein Vollblock: Zaun/Mauer, Glasscheibe/Gitter, Pfosten (Stange, Kette, Blitzableiter), Kopf, Blumentopf und Kerze. Medium erzeugt damit nur Vollblöcke, Falltüren, Platten, Stufen, Teppiche und Schnee.
- Die Zielkosten in Medium skalieren automatisch mit: die schwerste freigeschaltete Form ist dann eine Stufe, und das Maximum der Kosten sinkt von 10,5 auf 6,5 (Typkosten 1 statt 3, Lücke bleibt bei 4). Die Berechnung selbst ändert sich nicht.
- Hard, Ultra und Rainbow bleiben unverändert. Rainbow spielt laut Spec wie Medium, also verliert auch Rainbow die schmalen Formen; das ist so gewollt und wird im PR-Text ausgewiesen.
- Sprunglängen (Lücke, Aufstieg, Höhenunterschied) und Türme bleiben unverändert.
- Keine Konfigurationsänderung: die Paletten bleiben, sie werden nur in Medium nicht gezogen.

## Delivery

Pull-Request-Titel und Squash-Commit (Conventional Commits, Typ `fix`, Scope `jumprun`): `fix(jumprun): keep narrow surfaces out of medium courses`
