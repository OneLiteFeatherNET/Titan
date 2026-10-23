# Design

## Kontext

Titan-Features sind Columns unter `features/*`, die nur von `core` abhängen. Berechtigungen laufen über den `PermissionService` aus `core` (Abfrage per UUID, Ergebnis `ALLOWED`, `DENIED` oder `NOT_SET`), wie bei Dance und Portal. Sitzen funktioniert über ein unsichtbares Passagier-Entity; das Sit-Modul erkennt Schleichen über `ClientInputPacket.shift()`. Der Feuerwerks-Boost in `elytra` zeigt das Muster „Client wendet den Impuls an, die Lobby setzt keine Geschwindigkeit“. Der Settings-Port aus `lobby-settings` liefert typisierte Schlüssel für den Opt-out.

Die Minestom-Quellen (2026.10.07-26.2) liefern für das Aufladen die Signale, auf die dieser Entwurf baut:
- Start: `ClientUseItemPacket` löst `PlayerBeginItemUseEvent` aus, mit der Animation (`ItemAnimation`) und der Nutzdauer in Ticks. Die Animation kommt bei Items mit Komponente `consumable` aus deren Feld `animation`.
- Loslassen: `ClientPlayerActionPacket` mit `UPDATE_ITEM_STATE` löst `PlayerCancelItemUseEvent` aus, mit der bisherigen Nutzdauer in Ticks (`getUseDuration()`).
- Wechsel des Hotbar-Platzes: `PlayerHeldListener` löscht die Nutzung still, ohne Cancel-Event. Dieser Fall braucht eine eigene Behandlung.
- Der Abschluss nach `itemUseTime` Ticks feuert `PlayerFinishItemUseEvent`. Mit einer Nutzdauer von 72000 Ticks passiert das praktisch nie.

## Ziele

- Nur Spieler mit `titan.throw` können werfen, und das Ziel braucht keine aktive Zustimmung.
- Ein Ziel kann sich jederzeit lösen: durch Schleichen, durch Opt-out oder durch Immunität.
- Der Wurf ist aufgeladen: Stärke hängt von der Haltedauer ab, mit Obergrenze.
- Die Hand des Werfers ist nach jedem Pfad exakt wiederhergestellt.
- Kein Belästigungs-Dauerfeuer: Sperren pro Werfer, pro Paar und pro Ziel.

## Nicht-Ziele

- Keine Persistenz von Tragzustand, Ladezustand oder Sperren über Neustarts.
- Keine Resource-Pack-Assets, keine eigenen Modelle.
- Keine Team-Erkennung im Code. Team-Mitglieder werden über das Recht `titan.throw.immune` markiert, das LuckPerms vergibt.

## Entscheidungen

### D1 Aufladen: Verbrauchsgegenstand als Ladeträger (ersetzt die Linksklick-Entscheidung)

Während der Trage hält der Werfer in seinem Hotbar-Platz einen temporären Ladeträger: ein Stock mit der Komponente `consumable` (`consume_seconds` 3600, also 72000 Ticks, `animation` = `bow`, keine Partikel, kein Ton, kein Food-Feld). Der Client zeigt dann vanilla die Zieh-Animation des Bogens, ohne Resource Pack.

- Rechtsklick startet die Nutzung. Das liefert `PlayerBeginItemUseEvent`. Die Lobby startet die Ladezeit, sobald dieses Event kommt. Der Event-Parameter `animation` muss `BOW` sein; das ist im Spike zu bestätigen.
- Loslassen liefert `PlayerCancelItemUseEvent` mit der Ladezeit in Ticks. Daraus folgt die Stärke (D2). Die Lobby wirft.
- Der Ladeträger ist kein Bogen. Ein echter Bogen würde ohne Pfeile vanilla nicht gezogen, daher scheidet er aus. Auch die Trident-Animation wurde verworfen, weil die Waffe Haltbarkeit hat und ihre Nutzung an Bedingungen gebunden ist.
- Der Ladeträger ist nie im Inventar außerhalb der Trage. Er wird beim Start der Trage in den Hotbar-Platz gelegt und beim Ende exakt zurückgetauscht (D6).

**Begründung:** Beide Signale sind vanilla und ohne Resource Pack sichtbar. Das Loslassen ist ein echtes Client-Ereignis (`UPDATE_ITEM_STATE`), kein Timeout, und es liefert die Ladezeit selbst mit. Eine eigene Zeitmessung entfällt.

**Fallback, falls das Halten im Spike unzuverlässig ist:** Zwei-Klick-Aufladung. Ein Linksklick startet die Ladung, der nächste Linksklick wirft, und die Ladezeit ist der Abstand zwischen beiden Klicks (in Ticks gemessen). Sie braucht kein Halte-Signal, aber mehr Bedienung. Schleichen-Halten scheidet aus (siehe D5).

**Bekannte Lücke:** Ein Wechsel des Hotbar-Platzes während des Ladens löscht die Nutzung still. Deshalb gilt: Während der Trage wird der Hotbar-Platzwechsel abgefangen (`PlayerChangeHeldSlotEvent`). Der Ladeträger bleibt im Platz, die Ladung wird **abgebrochen** (kein Wurf, Ziel bleibt auf dem Kopf, Ladung startet bei einem neuen Rechtsklick neu). Das Abbrechen ist kein Ablehnungsgrund und zählt nicht in die Sperren.

### D2 Stärke: lineare Abbildung mit Obergrenze

- Ladezeit `t` in Ticks, aus dem Cancel-Event. Maximale Ladezeit `T_max` = 30 Ticks (1,5 s).
- Stärke `p = min(t, T_max) / T_max` im Bereich 0 bis 1. Überladen (`t > T_max`) bleibt bei `p = 1`.
- Geschwindigkeit `v = v_min + (v_max - v_min) * p` mit `v_min` = 8 Blöcke/s, `v_max` = 16 Blöcke/s.
- Vertikaler Anteil höchstens 10 Blöcke/s.
- Ein Wurf mit `t` unter 2 Ticks (Tippen) wirft mit `p = 0` und bekommt den Mindestimpuls. So lässt sich immer ein Wurf auslösen, und der Abbruch ist nur bewusst über den Hotbar-Platzwechsel möglich.
- Die Ladung läuft über `getAliveTicks`, also deterministisch über die Server-Ticks. Tests steuern die Ticks über `env.tick()`.

### D3 Geschwindigkeit und Impuls
Der Impuls ist `v * richtung`, wobei `richtung` der normalisierte Blickvektor des Werfers ist. Blickt der Werfer steil nach unten, wird der Vertikalanteil nicht unter 0 gedrückt; der Betrag bleibt gleich. Die Lobby setzt nur den Impuls, die Bewegung führt der Client des Ziels aus (wie beim Feuerwerks-Boost). Die Werte werden im Spike gegen einen vanilla Knockback-Schlag geprüft; die Einheit von `setVelocity` ist im Spike zu bestätigen.

### D4 Vorschau der Flugbahn (Partikel)

Während des Ladens sieht nur der Werfer eine vorhergesagte Flugbahn als vanilla Partikel.

- **Berechnung:** Pure Funktion `ThrowTrajectory.points(origin, richtung, v, maxSchritte, istFest)`. Pro Tick: Geschwindigkeit mit Luftwiderstand `0,98` multipliziert, dann `y -= 0,08` (vanilla Gravitation), dann Position += Geschwindigkeit. Ende bei Kontakt mit einem festen Block (`istFest`) oder nach `maxSchritte`.
- **Geschwindigkeit der Vorschau:** derselbe Wert `v` wie der spätere Wurf, aus dem aktuellen Ladezustand `p`.
- **Darstellung:** Partikel `end_rod` in Punkten entlang der Bahn. Am Landepunkt ein Marker, z. B. `dust` in einer gut sichtbaren Farbe. Höchstens 40 Punkte pro Aktualisierung; jeder zweite Tick neu gesendet, Punkte nach vorne gestaffelt, damit keine Flut entsteht.
- **Nur für den Werfer:** Die Pakete werden ausschließlich über `player.sendPacket(...)` an den Werfer geschickt. Es gibt kein `sendPacketToViewers`. Ein Test prüft, dass andere Spieler keine Partikel-Pakete empfangen.
- **Hinweis:** Die Vorschau ist eine Vorhersage. Die Client-Physik kann leicht abweichen, weil Luftwiderstand und Kollisionen der Client-Seite teilweise anders gerechnet werden. Die Vorschau dient der Orientierung, nicht als garantierte Landestelle.

### D5 Abbruch durch Schleichen
Schleicht das Ziel, steigt es ab, ohne Impuls. Das zählt als Abbruch und setzt die Paar-Sperre von 60 s. Das Sit-Modul reagiert auf dasselbe Signal, aber nur, wenn das Ziel tatsächlich auf einem Sitz sitzt; ein getragenes Ziel sitzt nicht. Tragen und Sitzen schließen sich aus.

**Warum nicht Schleichen-Halten als Ladung:** Schleichen ist schon das Abbruch-Signal des Ziels. Ein Doppelgebrauch würde die Ladung beim Ziel-Abbruch auslösen, und das Ziel könnte einen Wurf erzwingen. Das ist ausgeschlossen.

### D6 Hand wiederherstellen (jeder Pfad)
Beim Start der Trage wird ein Snapshot genommen: Hotbar-Platz und der Original-`ItemStack` in der Haupthand. Der Ladeträger wird dort hineingelegt. Jeder Pfad, der die Trage beendet, setzt den Snapshot exakt zurück, bevor das Modul den Zustand verlässt:

| Pfad | Verhalten |
|---|---|
| Wurf (Release) | Hand zurück, dann Impuls |
| Schleichen des Ziels | Hand zurück, kein Impuls |
| Auto-Drop nach 15 s | Ladung abbrechen, Hand zurück, kein Impuls |
| Opt-out eingeschaltet (D9) | Hand zurück, kein Impuls |
| Immunität des Ziels (nachträglich) | Hand zurück, kein Impuls |
| Werfer oder Ziel verlässt das Spiel | Snapshot im Speicher zurück, kein Impuls |
| Instanzwechsel (Werfer oder Ziel) | Hand zurück vor dem Wechsel, kein Impuls |
| Werfer stirbt | Hand zurück vor dem Respawn, bevor die Lobby Items neu ausgibt |
| Modul-Stopp / Serverstopp | Hand für alle Online-Spieler zurück, kein Impuls |
| Hotbar-Platzwechsel während der Trage | Platzwechsel wird abgefangen (D1), Trage bleibt |

Der Ladeträger wird nie gespeichert. Wirft der Werfer den Ladeträger über Drop (Q) oder Tausch (F), greift der Protection-Modul bereits (`ItemDropEvent`, `PlayerSwapItemEvent` werden abgebrochen). Inventar-Klicks sind ebenfalls abgefangen (`InventoryPreClickEvent`).

### D7 Fortschritt anzeigen
Während des Ladens sieht der Werfer eine Actionbar mit einer Leiste (z. B. zehn Blöcke, gefüllt nach `p`). Ein vanilla Ton (z. B. eine Erfahrungskugel) steigt in der Tonhöhe mit `p`, nur alle 5 Ticks. Die Erfahrungsleiste wird nicht genutzt: Sie würde einen weiteren Zustand zum Wiederherstellen erfordern und bringt keinen Mehrwert.

### D8 Sperren (Mitigation)
- Werfer: 5 s nach jedem Wurf, damit nicht in Serie geworfen wird.
- Paar (Werfer, Ziel): 60 s nach einem Abbruch oder einer Ablehnung durch Schleichen.
- Ziel: höchstens eine Aufnahme alle 20 s, unabhängig vom Werfer.
- Tragzeit: 15 s ab Aufnahme, danach Auto-Drop. Die Ladezeit zählt in dieser Zeit mit; ist ein Wurf bei Auto-Drop schon geladen, wird er nicht ausgelöst.

### D9 Opt-out und Immunität
- Opt-out `titan:throw.target` (Standard an) über den Settings-Port aus `lobby-settings`. Gelesen bei jeder Aufnahme.
- **Ausschalten während einer Trage:** Das Ziel wird sofort ohne Impuls freigegeben (Hand des Werfers zurück, Ladung abgebrochen). Zustimmung kann jederzeit zurückgezogen werden. Dafür braucht der Port eine Änderungsbenachrichtigung (Ergänzung zu `lobby-settings`); ohne sie greift ein Abgleich alle 10 Ticks als Rückfall.
- Immunität `titan.throw.immune` am Ziel: geprüft bei jeder Aufnahme und bei jedem Wurf. Erhält der Ziel-Spieler das Recht während der Trage, wird die Trage ohne Impuls beendet.
- Beim Abweisen erhält nur der Werfer eine kurze Nachricht. Das Ziel bekommt keine Nachricht (kein Spam, kein Hinweis-Leak).

### D10 Portal-Flugschutz
Geworfene Spieler lösen während des Flugs und 2 s nach der Landung keine Portal-Übergabe aus. Das Portal-Modul bleibt unverändert; der Throw-Zustand wird vor der Übergabe abgefragt, und eine Übergabe wird ohne Sperre verworfen.

### D11 Grenzen und Fallen
Die Spawn-Höhengrenze (`SpawnBoundsListener`) bringt Ziele zurück, die aus der Höhe fallen. Ein Sturz in die Void führt sonst zum Tod, den das Respawn-Modul abfängt. Horizontal gibt es keine Weltgrenze im Code; der Spike prüft, ob die Impulsgrenzen eine Landung im Lobbybereich sicherstellen. Eine maximale Flugzeit von 5 s beendet den Flugzustand auch ohne Landung.

### D12 Feedback beim Wurf
Vanilla-Aufnahmeton beim Aufnehmen, Vanilla-Knockback-Ton beim Wurf, Vanilla-Partikelwolke am Werfer. Keine Resource Pack.

### D13 Telemetrie
Zähler `titan.throw.pickups`, `titan.throw.throws`, `titan.throw.escapes` (Abbruch durch Schleichen), `titan.throw.refusals` (Attribut `reason`: `permission`, `opt_out`, `immune`, `cooldown`, `state`) und `titan.throw.charge_aborts` (Abbruch durch Hotbar-Platzwechsel). Keine Spieler-UUIDs in Attributen.

## Zustandsmodell

```
Idle --(Aufnahme erlaubt, Snapshot, Ladeträger)--> Carrying(werfer, ziel)
Carrying --(Rechtsklick: Ladung beginnt)--> Charging(werfer, ziel, ticks)
Charging --(Loslassen: Cancel-Event)--> Thrown(ziel)                 Impuls gemäß D2, Hand zurück
Charging --(Hotbar-Platzwechsel)--> Carrying                         Ladung abgebrochen, kein Impuls
Carrying / Charging --(Ziel schleicht)--> Idle                       Abbruch, Paar-Sperre, Hand zurück
Carrying / Charging --(Tragzeit 15 s)--> Idle                        Auto-Drop, Hand zurück, kein Impuls
Carrying / Charging --(Opt-out an, Immunität, Disconnect, Instanzwechsel, Tod, Stopp)--> Idle   ohne Impuls
Thrown --(Landung + 2 s oder max. 5 s Flug)--> Idle
```

Ein Ziel ist in höchstens einem Zustand. Ein Werfer trägt höchstens ein Ziel. Die Ladung wird nie ohne Snapshot betreten.

## Randfälle

- **Verkettung:** Ein Werfer, der selbst getragen wird, kann nichts aufnehmen. Ein getragenes Ziel kann nicht aufgenommen werden. Ein Werfer, der ein Ziel trägt, kann nicht selbst aufgenommen werden. Die Prüfungen zählen in beide Richtungen.
- **Ladung und Rechtsklick auf andere Spieler:** Während der Ladung ist kein weiteres Aufnehmen möglich. Der Zustand `Charging` hat keine Aufnahme.
- **Nutzung ohne Ladeträger:** Ein Rechtsklick auf andere Items während der Trage (Hotbar-Items) ist nicht möglich, weil der Ladeträger in der Haupthand liegt.
- **Recht entzogen während der Trage:** Der Werfer kann nicht mehr werfen; die Trage endet spätestens mit dem Auto-Drop. Kein sofortiger Abbruch, damit ein kurzer Rechte-Neuladen nicht zu Chaos führt.
- **Sitzendes oder tanzendes Ziel, Elytra-Flug, Jump-and-Run:** Nicht aufnehmbar, Ablehnung mit Grund `state`.
- **Sichtbarkeit für andere:** Andere Spieler sehen die Zieh-Animation des Werfers (vanilla Verhalten bei Bogen-Nutzung). Die Vorschau sehen nur der Werfer.

## Abhängigkeiten

- **Voraussetzung `lobby-settings` (zuerst umgesetzt und gemergt):** Settings-Port in `core` mit typisierten Schlüsseln und einer Änderungsbenachrichtigung (Ergänzung zu `lobby-settings`, siehe D9). Der Opt-out `titan:throw.target` ist eine Option darauf, kein eigenständiger Speicher.
- Ohne den Port (Fake im Test) entwickelt sich der Kern; der Opt-out-Pfad wird erst gegen den echten Port integriert.
- Befehle, falls welche entstehen: immer Englisch, keine deutschen Aliase (Nutzerentscheidung).

## Offene Fragen

- Stimmen die Konstanten (`T_max` 30 Ticks, 8 bis 16 Blöcke/s, Vertikalgrenze 10) gegen den vanilla Knockback? Spike entscheidet (Task 1.2).
- Passt der Ladeträger-Stock mit `consumable` und `animation` = `bow` im Client ohne Verhaltensauffälligkeit? Spike entscheidet (Task 1.1 und 1.5).
- Soll der Hotbar-Platzwechsel während der Ladung die Ladung nur abbrechen (Vorschlag) oder den Wurf auslösen? Vorschlag: nur abbrechen, damit der Wurf immer bewusst ist.
