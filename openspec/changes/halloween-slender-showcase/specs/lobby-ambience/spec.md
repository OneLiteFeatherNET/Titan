# Spec Delta

## ADDED Requirements

### Requirement: Lebendige Zonen stehen in der Kartendatei
_Ambience zones are map data: an optional `ambience` list in `map.json`, read at start._

Die Datei `map.json` einer Welt MAY ein Feld `ambience` tragen. Jede Zone MUSS `id` und `area` (ein Quader mit `min` und `max`, `min` nicht größer als `max` auf jeder Achse) haben. Optional sind `particles`, `sounds`, `signs`, `pages` und `apparitions`; fehlende Teile bleiben leer. Eine ungültige Zone wird beim Start mit einer WARN-Zeile übersprungen, die Zone und Grund nennt; die übrigen Zonen laufen weiter.

#### Scenario: Gültige Zone
- **WHEN** `map.json` eine Zone mit `id`, `area` und `apparitions` enthält
- **THEN** startet die Zone mit diesem Quader und diesen Erscheinungspunkten

#### Scenario: Quader verkehrt
- **WHEN** `min.x` größer als `max.x` ist
- **THEN** wird die Zone übersprungen und die WARN-Zeile nennt `area` und die Achse

### Requirement: Vanilla-Client genügt
_Everything the zone shows works with the vanilla client; no resource pack._

Die Kulisse MUSS mit den Standarddaten des Vanilla-Clients funktionieren. Partikel, Geräusche und Gegenstände MÜSSEN Vanilla-Werte sein. Ein Ressourcenpaket, eigene Geräuschereignisse, eigene Modelle, Schriften oder Glyphen sind nicht zulässig. Die Konfiguration nimmt nur Vanilla-Namen an; ein unbekannter Name wird wie ein ungültiger Eintrag übersprungen.

#### Scenario: Unbekanntes Partikel
- **WHEN** ein Partikel-Name in der Zone kein Vanilla-Partikel ist
- **THEN** wird die Zone übersprungen und die WARN-Zeile nennt den Namen

### Requirement: Partikel nur für Spieler in der Nähe
_Particles are sent only to players inside the zone or within 6 blocks of it._

Partikel MÜSSEN nur an Spieler im Quader oder höchstens 6 Blöcke davor gesendet werden. Pro Zone läuft ein Takt, nicht ein Takt je Entität.

#### Scenario: Spieler außerhalb
- **WHEN** ein Spieler 20 Blöcke vom Quader entfernt steht und der Takt läuft
- **THEN** erhält er kein Partikelpaket dieser Zone

#### Scenario: Spieler im Quader
- **WHEN** ein Spieler im Quader steht und der Takt läuft
- **THEN** erhält er Partikelpakete dieser Zone

### Requirement: Geräusche nur für Spieler in der Zone
_Ambient sounds are played to the player only, at a random interval inside the configured range._

Umgebungsgeräusche MÜSSEN je Spieler gespielt werden, nur für Spieler im Quader, in einem zufälligen Intervall aus dem konfigurierten Bereich (in Sekunden). Sie sind leise (Lautstärke unter 1).

#### Scenario: Geräusch im Quader
- **WHEN** ein Spieler im Quader steht und sein Intervall abläuft
- **THEN** hört er das konfigurierte Geräusch, und kein anderer Spieler hört es

### Requirement: Texttafeln und Seiten sind für alle sichtbar
_Signs and pages are shared entities spawned once at start._

Jede Texttafel MUSS als `text_display` an ihrer Position stehen, mit dem Text in der Sprache des Spielers (Übersetzung). Jede Seite MUSS als `item_display` mit Papier schweben. Beides ist für alle Spieler der Welt sichtbar; es entsteht nur einmal beim Start.

#### Scenario: Tafel an der Wand
- **WHEN** die Zone eine Tafel mit dem Text `Folge den Seiten` hat
- **THEN** steht eine Texttafel mit diesem Text an der Position, für jeden Spieler sichtbar

### Requirement: Erscheinung je Spieler
_An apparition is a view-only silhouette for one player, shown when he is near and removed when he comes close._

Jede Erscheinung MUSS eine Slender-Silhouette sein, die nur einem Spieler gezeigt wird. Sie erscheint, sobald der Spieler der Zone innerhalb von 14 Blöcken des Punktes steht, spielt `entity.enderman.stare` und verschwindet, wenn der Spieler innerhalb von 4 Blöcken ist, die Zone verlässt oder die Verbindung endet. Jeder Punkt erscheint pro Spieler höchstens einmal.

#### Scenario: Spieler nähert sich dem Punkt
- **WHEN** ein Spieler 12 Blöcke vom Erscheinungspunkt entfernt steht
- **THEN** sieht nur er die Silhouette, und das Geräusch `entity.enderman.stare` spielt für ihn

#### Scenario: Spieler kommt nahe
- **WHEN** der Spieler auf 3 Blöcke an die Silhouette herankommt
- **THEN** verschwindet die Silhouette für ihn und erscheint nicht wieder

#### Scenario: Spieler geht weg
- **WHEN** der Spieler den Quader verlässt, während die Silhouette sichtbar ist
- **THEN** verschwindet sie für ihn

#### Scenario: Verbindung endet
- **WHEN** der Spieler die Verbindung beendet, während eine Silhouette sichtbar ist
- **THEN** wird die Silhouette entfernt und kein Eintrag bleibt zurück

### Requirement: Erscheinungen zählen
_Each apparition shown is counted by zone._

Jede gezeigte Erscheinung MUSS den Zähler `titan.ambience.apparitions` mit dem Attribut `zone` erhöhen.

#### Scenario: Zähler bei Erscheinung
- **WHEN** eine Erscheinung für einen Spieler erscheint
- **THEN** zählt `titan.ambience.apparitions` mit `zone` um eins
