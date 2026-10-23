# Proposal

## Why

Die Lobby hat bisher keinen Grund, länger stehen zu bleiben. Vanilla-Tänze geben Spielern eine kleine, sichtbare Geste ohne Mod und ohne Resource Pack: Winken, Groove, Party oder eine Drehung. Der Einstieg läuft über die Schnellaktion (Taste G, Client 1.21.6 und neuer), dort suchen Spieler ohnehin nach Aktionen. Zusätzlich braucht es einen Befehl als Rückfall, falls die Schnellaktion nicht erscheint.

Beim Tanzen sieht der Spieler sich selbst von außen: Eine Kamera steht vor ihm und zeigt ihn in seiner Pose.

## What Changes

- Beim Start registriert die Column die Tanzlisten (je Sprache und Berechtigungsvariante) in der Dialog-Registry. Jeder Spieler bekommt per Tag `minecraft:quick_actions` genau die Liste seiner Sprache und seiner Rechte; die Schnellaktion zeigt sie direkt.
- Ein Klick auf den Knopf öffnet dem Spieler persönlich einen Mehrfach-Dialog: nur die Tänze, die er nutzen darf, dazu „Stopp“. Die Texte gibt es auf Deutsch und Englisch, wie in den anderen Modulen. Jeder Tanz-Knopf sendet `titan:dance` mit der Id des Tanzes als Payload. Die Lobby prüft Id und Berechtigung erneut, bevor sie startet.
- Der Befehl `/dance` (ohne Alias, englisch) öffnet dieselbe Liste.
- Vier Tänze, im Code festgelegt: `wave` (Winken), `groove`, `party` und `spin`. Die ersten drei sind frei, `spin` braucht `titan.dance.spin`. Ein Tanz ist eine feste Folge von Schritten über Ticks: Armschwünge, Kopfnicken, Ducken, kleine Sprünge, Noten- und Herzpartikel und Notenblock-Töne.
- Ein Spieler tanzt höchstens einen Tanz gleichzeitig. Zwischen zwei Starts liegt eine feste Abklingzeit von 3 Sekunden.
- Während eines Tanzes sieht der Spieler über die Kamera-Ansicht (`CameraPacket`) eine unsichtbare Kameraentität, die etwa 3,5 Blöcke vor ihm auf Augenhöhe steht und auf ihn blickt. Jeder Stopp-Pfad setzt die Kamera auf den Spieler zurück und entfernt die Entität.
- Ein Tanz endet, wenn er fertig ist, bei Bewegung über eine kleine Schwelle, bei Bewegungseingaben (vorwärts, rückwärts, links, rechts, springen), bei Angriff oder Interaktion, beim Verlassen der Instanz, beim Verbindungsabbruch, beim Stopp-Knopf oder bei einem Fehler im Tanz. Die Pose kehrt immer auf `STANDING` zurück.
- Telemetrie: Zähler `titan.dance.started` mit dem Attribut `dance` (Id des Tanzes).

Nicht Teil dieser Änderung:

- Konfiguration der Tänze, Abklingzeit oder Kameradistanz. Die Werte stehen im Code, bis ein Betreiber sie braucht.
- Eigene Tanzanimationen über Resource Packs oder Mods.
- Kamerafahrt (Umkreisen des Spielers). Die Kamera steht fest.
- Tänze für andere Spieler als Zuschauer-Ansicht. Andere Spieler sehen nur den NPC, nicht den echten Spieler.

## Delivery

Der Pull-Request-Titel und der Squash-Commit sind `feat(dance): add lobby dances with quick-action dialog and camera view`. Die Änderung ist nicht breaking. Die OpenSpec-Artefakte stehen in einem eigenen Commit `docs(openspec): propose lobby-dances`; der Code folgt in `feat(dance): ...`.

## Capabilities

### New Capabilities

- `lobby-dances`: Schnellaktion und Befehl, Auswahl-Dialog, Tänze, Abklingzeit, Kameraansicht und Stopp-Pfade.

### Modified Capabilities

Keine.

## Impact

- **Code:** neues Modul `features/dance` (Column `danceColumn`, Paket `net.onelitefeather.titan.feature.dance`). Es hängt nur an `core`, wie jede Column. Keine Änderung an `core`, `common` oder `runtime`; die Column findet sich über den Verzeichnis-Scan in `settings.gradle.kts`.
- **Abhängigkeiten:** keine neuen Bibliotheken. Minestom bleibt bei `2026.10.07-26.2`. Die Tag-Eintragung nutzt eine reflektierte, paketprivate Methode von Minestom; das ist im Design begründet und durch einen Test abgesichert.
- **Nutzertexte:** Dialogtexte in `titan/dance/messages_de.properties` und `messages_en.properties`; der Befehl heißt immer `dance`.
- **Betrieb:** Keine neue Konfiguration und keine Welt. Der Client braucht 1.21.6 oder neuer für die Schnellaktion; ältere Clients erreichen die Liste über `/dance`.
