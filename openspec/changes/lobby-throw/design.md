# Design

## Kontext

Titan-Features sind Columns unter `features/*`, die nur von `core` abhängen. Berechtigungen laufen über den `PermissionService` aus `core` (Abfrage per UUID, Ergebnis `ALLOWED`, `DENIED` oder `NOT_SET`), wie bei Dance und Portal. Sitzen funktioniert über ein unsichtbares Passagier-Entity; das Sit-Modul erkennt Schleichen über `ClientInputPacket.shift()`. Der Feuerwerks-Boost in `elytra` zeigt das Muster „Client wendet den Impuls an, die Lobby setzt keine Geschwindigkeit“. Der Settings-Port aus `lobby-settings` liefert typisierte Schlüssel für den Opt-out.

## Ziele

- Nur Spieler mit `titan.throw` können werfen, und das Ziel braucht keine aktive Zustimmung.
- Ziele können sich per Opt-out oder Immunität schützen, und ein Abbruch durch Schleichen ist jederzeit möglich.
- Kein Belästigungs-Dauerfeuer: Sperren pro Werfer, pro Paar und pro Ziel.

## Nicht-Ziele

- Keine Persistenz von Tragzustand oder Sperren über Neustarts.
- Keine Team-Erkennung im Code. Team-Mitglieder werden über das Recht `titan.throw.immune` markiert, das LuckPerms vergibt.

## Entscheidungen

### D1 Wurf-Geste: Linksklick
Der Werfer wirft mit Linksklick (`PlayerHandAnimationEvent`). Begründung: Das getragene Ziel sitzt über dem Werfer und ist für dessen Strahl nicht treffbar, daher greift `EntityAttackEvent` nicht. Die Hand-Animation kommt bei Luft- und Blockklick. Rechtsklick wird verworfen: Er ist Item-Benutzung, kollidiert mit Navigator, Dance und Jump-and-Run, und ein wiederholter Rechtsklick würde beim Aufnehmen versehentlich werfen. Gegen versehentliches Werfen direkt nach der Aufnahme gilt eine Schutzzeit von 500 ms.

### D2 Aufnahme: Rechtsklick auf Spieler mit leerer Haupthand
Aufnahme über `PlayerEntityInteractEvent`. Die leere Haupthand verhindert Konflikte mit Lobby-Items.

### D3 Geschwindigkeit
Betrag des Impulses liegt in einem Korridor und wird begrenzt: Standard 12 Blöcke/s, Maximum 16 Blöcke/s, Mindestwert 8 Blöcke/s. Die vertikale Komponente ist auf höchstens 10 Blöcke/s begrenzt. Die Werte werden im Spike gegen einen vanilla Knockback-Schlag (ca. 0,4 Blöcke/Tick) geprüft; die Einheit der Minestom-API ist im Spike zu bestätigen. Blickt der Werfer steil nach unten, wird der Impuls nicht in den Boden gedrückt: der Vertikalanteil hat eine Untergrenze von 0, der Betrag bleibt gleich.

### D4 Abbruch durch Schleichen
Schleichen des Ziels beendet die Trage. Dies zählt als Ablehnung und setzt die Paar-Sperre. Das Sit-Modul reagiert auf dasselbe Signal, aber nur, wenn das Ziel tatsächlich auf einem Sitz sitzt; ein getragenes Ziel sitzt nicht, daher stören sich beide Module nicht. Tragen und Sitzen schließen sich aus.

### D5 Sperren (Mitigation)
- Werfer: 5 s nach jedem Wurf, damit nicht in Serie geworfen wird.
- Paar (Werfer, Ziel): 60 s nach einem Abbruch oder einer Ablehnung durch Schleichen.
- Ziel: höchstens eine Aufnahme alle 20 s, unabhängig vom Werfer.
- Tragzeit: 15 s, danach Auto-Drop ohne Impuls.

### D6 Opt-out und Immunität
- Opt-out `titan:throw.target` (Standard an) über den Settings-Port aus `lobby-settings`. Gelesen bei jeder Aufnahme-Prüfung, nicht gecacht, damit eine Änderung sofort wirkt.
- Immunität: Recht `titan.throw.immune` am Ziel; Prüfung bei jeder Aufnahme.
- Beide Fälle: der Werfer bekommt eine kurze Nachricht; das Ziel bekommt keine Nachricht (kein Spam, kein Hinweis-Leak).
- Opt-out ist keine Berechtigung für den Werfer. Ein Werfer ohne `titan.throw` kann auch ein Ziel ohne Opt-out nicht aufnehmen.

### D7 Portal-Flugschutz
Geworfene Spieler lösen während des Flugs und 2 s nach der Landung keine Portal-Übergabe aus. Begründung: Eine ungewollte Server-Übergabe wäre ein Belästigungseffekt, den der Werfer nicht kontrolliert. Das Portal-Modul bleibt unverändert; der Throw-Zustand wird über eine kleine Abfrage vor der Übergabe berücksichtigt, und die Übergabe wird ohne Sperre verworfen.

### D8 Grenzen und Fallen
Die Spawn-Höhengrenze (`SpawnBoundsListener`) bringt Ziele zurück, die aus der Höhe fallen. Ein Sturz in die Void führt sonst zum Tod, den das Respawn-Modul abfängt. Horizontal gibt es in dieser Lobby keine Weltgrenze im Code; der Spike prüft, ob die Impulsgrenzen eine Landung im Lobbybereich sicherstellen. Eine maximale Flugzeit von 5 s beendet den Flugzustand auch ohne Landung.

### D9 Feedback
Töne: Vanilla-Aufnahmeton beim Aufnehmen, Vanilla-Knockback-Ton beim Werfen. Partikel: Vanilla-Wolken am Werfer beim Wurf. Actionbar für den Werfer während der Trage: „Linksklick zum Werfen“ (und Ablauf-Hinweis bei Auto-Drop). Keine Resource Pack.

### D10 Telemetrie
Zähler `titan.throw.pickups`, `titan.throw.throws`, `titan.throw.escapes` (Abbruch durch Schleichen), `titan.throw.refusals` (Attribut `reason`: `permission`, `opt_out`, `immune`, `cooldown`, `state`). Keine Spieler-UUIDs in Attributen.

## Zustandsmodell

```
Idle --(Aufnahme erlaubt)--> Carrying(werfer, ziel)
Carrying --(Linksklick)--> Thrown(ziel)            Impuls gesetzt
Carrying --(Ziel schleicht)--> Idle                 Abbruch, Paar-Sperre
Carrying --(Tragzeit 15 s)--> Idle                  Auto-Drop, ohne Impuls
Carrying --(Werfer/Ziel disconnect, Instanzwechsel, Serverstopp)--> Idle   ohne Impuls
Thrown --(Landung + 2 s)--> Idle
Thrown --(max. 5 s Flug)--> Idle
```

Ein Ziel ist in höchstens einem Zustand. Ein Werfer trägt höchstens ein Ziel.

## Randfälle

- **Verkettung:** Ein Werfer, der selbst getragen wird, kann nichts aufnehmen. Ein getragenes Ziel kann nicht aufgenommen werden. Ein Werfer, der ein Ziel trägt, kann nicht selbst aufgenommen werden. Die Prüfungen zählen über beide Richtungen.
- **Disconnect:** Verlässt der Werfer das Spiel, endet die Trage ohne Impuls. Verlässt das Ziel das Spiel, wird die Trage des Werfers gelöst. Beides über `PlayerDisconnectEvent`.
- **Instanzwechsel:** Wechselt der Werfer oder das Ziel die Instanz (z. B. Navigator, Rückkehr zum Spawn), endet die Trage ohne Impuls. Die Prüfung läuft über den Instanzwechsel-Event.
- **Serverstopp:** Alle Tragzustände werden beim Stopp des Moduls abgebaut; kein Impuls, kein Persistenzschritt.
- **Recht entzogen während der Trage:** Der Werfer kann nicht mehr werfen; die Trage endet spätestens mit dem Auto-Drop. Kein sofortiger Abbruch, damit ein kurzer Rechte-Neuladen nicht zu Chaos führt.
- **Opt-out während der Trage:** Die Trage läuft weiter, bis sie endet. Opt-out wirkt nur auf neue Aufnahmen. Begründung: Eine laufende Aktion nicht mitten drin abbrechen. Offene Frage, siehe unten.
- **Sitzendes Ziel:** Nicht aufnehmbar, Ablehnung mit Grund `state`.
- **Ziel in Tanz, Jump-and-Run, Elytra-Flug:** Nicht aufnehmbar. Werfer in diesen Zuständen kann nicht aufnehmen oder werfen.

## Abhängigkeiten

- **Voraussetzung `lobby-settings` (zuerst umgesetzt und gemergt):** Settings-Port in `core` mit typisierten Schlüsseln. Der Opt-out `titan:throw.target` ist eine Option darauf, kein eigenständiger Speicher. Ohne den Port (Fake im Test) entwickelt sich der Kern; der Opt-out-Pfad wird erst gegen den echten Port integriert, nachdem `lobby-settings` gemergt ist.
- Befehle, falls welche entstehen: immer Englisch, keine deutschen Aliase (Nutzerentscheidung).

## Offene Fragen

- Soll ein Opt-out während einer laufenden Trage das Ziel sofort freigeben? Vorschlag: nein, siehe Randfälle.
- Sollen Sperren beim Serverstopp nicht persistiert werden? Vorschlag: ja, nicht persistiert.
