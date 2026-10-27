# Design

## Kontext

Der Navigator ist ein Enum (`Destination`). Jedes Hauptmenü-Ziel trägt heute einen festen Platz. `NavigatorLayout` füllt den Rahmen mit abwechselnden Scheiben und die übrigen Plätze mit grauen Scheiben. `NavigatorModule` baut je Rechte-Kombination und Sprache ein geteiltes Inventar und aktualisiert es beim Takt `navigator.refreshSeconds`; die Spielerzahlen kommen aus `TaskPlayers`. Die Lore entsteht in `NavigatorMessages`. Das Untermenü „Staging“ hat eigene feste Plätze 0 bis 8 und bleibt unverändert.

## Ziele / Nicht-Ziele

**Ziele:**

- Die Platzierung im Hauptmenü ergibt sich aus der Gruppe und der Zahl der sichtbaren Ziele, nicht aus einer festen Nummer.
- Rahmen und leere Plätze wirken ruhig: eine dunkle Scheibe, keine Füllscheiben.
- Spielerzahl, Stapelanzahl und Glanz zeigen Aktivität, ohne neue Konfiguration oder neue Listener.

**Nicht-Ziele:**

- Keine Änderung an Rechten, Klicks, Weiterleitungen, Schnellbeitritt oder Serverauswahl.
- Keine Änderung am Untermenü „Staging“ und seinen Plätzen.
- Keine neuen Konfigurationsschlüssel und keine zusätzlichen Datenabfragen.

## Entscheidungen

- **Gruppe statt Slot.** `Destination` bekommt eine Gruppe (MINIGAME, SERVER, STAGING) statt eines festen Hauptmenü-Platzes. Hub (35) und Spawn (31) sind Sonderplätze des Layouts. Die Staging-Einträge behalten ihren Platz im Untermenü. Verworfen: feste Plätze je Ziel, weil jede Rechte-Kombination dann eine eigene Lücke hätte.
- **Zentrierung als reine Funktion.** `NavigatorLayout` berechnet für eine Gruppe mit Anfang `a` und Breite 7 bei `n` sichtbaren Zielen die Plätze `a + ⌊(7 − n) / 2⌋ + i` für `i` von 0 bis `n − 1`, in der Reihenfolge der Aufzählung. Minispiele beginnen bei 10, Server bei 19. Beispiele: vier Minispiele belegen 11 bis 14, ein Minispiel belegt 13, drei Server belegen 21 bis 23. Die Funktion braucht keinen Server und ist ein reiner Unit-Test.
- **Dunkle Rahmenscheibe.** `BLACK_STAINED_GLASS_PANE` mit leerem Namen auf Reihe 0, Reihe 3 und den Spalten 0 und 8. Dass die untere Reihe zum Rahmen gehört, folgt der heutigen Definition des Rahmens; Spawn (31) und Hub (35) ersetzen ihre Scheibe. Verworfen: `GRAY`, weil es vom grauen Füllglas des Untermenüs kaum zu unterscheiden wäre.
- **Leere innere Plätze.** Ein innerer Platz ohne sichtbares Ziel bleibt ohne Item.
- **Statuszeile und Beschreibung aus Schlüsseln.** Die Statuszeile nutzt den Schlüssel für die Spielerzahl, ergänzt um den farbigen Punkt. Je Ziel kommt eine Beschreibungszeile hinzu, in beiden Bundles. Verworfen: Beschreibungen im Code, weil sie dann nicht übersetzt wären.
- **Stapelanzahl und Glanz aus derselben Zahl.** Die Spielerzahl des letzten Taktes setzt Anzahl (geklemmt auf 1 bis 64) und Glanz (`DataComponents.ENCHANTMENT_GLINT_OVERRIDE`, wenn die Zahl größer als null ist). Eine einzige Quelle verhindert, dass Lore, Anzahl und Glanz auseinanderlaufen.
- **Titel.** Der Schlüssel für den Titel bleibt; der Wert bekommt Symbole und einen MiniMessage-Verlauf.

## Tests (F.I.R.S.T.)

Die Tests entstehen vor dem Code (rot): Unit für `NavigatorLayout` (Zentrierung und Rahmenmaterial), Unit für Lore, Anzahl und Glanz, danach Integration über Env mit explizitem Tick für Aktualisierung und Sichtbarkeit. Keine Sleeps, keine Systemzeit, keine geteilten Zustände zwischen Tests.

## Risiken

- Die Rahmenannahme für die untere Reihe ist eine Entscheidung, die im Review bestätigt werden sollte.
- Die Deltas hängen an `navigator-staging-on-demand`. Würde diese Änderung vor jener archiviert, liefen die Deltas ins Leere.
