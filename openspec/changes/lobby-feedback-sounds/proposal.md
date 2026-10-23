# Proposal

## Why

Kitzeln und Portale laufen in der Lobby stumm ab. Ein Kitzel-Treffer zeigt nur eine Chat-Zeile, und ein Sprung in ein Portal oder durch einen Ring hat kein Echo. Ein kurzer Vanilla-Ton macht beides spürbar, ohne Resource Pack und ohne eigene Sound-Events.

## What Changes

- **Kitzeln:** Ein erfolgreicher Kitzel-Treffer spielt den verspielten Vanilla-Ton `entity.allay.item_given` mit zufälliger Tonhöhe. Er ist positional an der Position des gekitzelten Spielers zu hören und geht an alle Spieler in 16 Blöcken Umkreis, also auch an den Kitzelnden. Lautstärke 0,6.
- **Pro Paar begrenzt:** Dasselbe geordnete Paar (Kitzelnder, Ziel) hört höchstens alle 2 Sekunden einen Kitzel-Ton. Das Kitzeln selbst (Nachricht, Abklingzeit) bleibt unberührt; nur der Ton entfällt.
- **Box-Portal (Übergabe an einen Task):** Der passierende Spieler hört kurz vor der Übergabe ein Teleport-Geräusch (`entity.enderman.teleport`, Lautstärke 0,5, Tonhöhe 1,0). Nur er hört es.
- **Scheiben-Portal (Ring):** Der passierende Spieler hört einen hellen Ring-Ton (`entity.experience_orb.pickup`, Lautstärke 0,6, Tonhöhe 1,3). Ein Ring spielt diesen Ton statt des Teleport-Tons, nicht zusätzlich.
- **Abgewiesener Betritt:** Betritt ein Spieler ein Portal, für das er kein Recht hat, hört er einmal einen tiefen Abweisungston (`block.note_block.bass`, Lautstärke 0,8, Tonhöhe 0,7), nur er selbst. Während der Abklingzeit gibt es keinen Ton, genau wie dort keine Abweisung gemeldet wird.
- Alle Töne sind Konstanten im Code. Keine Konfiguration.

Nicht Teil dieser Änderung:

- Eigene Sound-Events, Resource Packs, Musik.
- Ein Abweisungston, wenn das Ziel offline oder voll ist. Die Lobby sieht den Ausgang der Übergabe nicht (`Deliver.sendPlayer` liefert nichts zurück), daher gibt es dafür kein Signal. Siehe `design.md`.
- Konfigurierbare Lautstärken, Tonhöhen oder Abklingzeiten.
- Töne in anderen Columns.

## Delivery

Der Pull-Request-Titel und der Squash-Commit lauten `feat(sounds): play sounds when tickling and passing portals`. Die OpenSpec-Artefakte stehen im Commit `docs(openspec): propose lobby-feedback-sounds`, der Code folgt in `feat(sounds)`. Die Änderung ist nicht breaking.

## Capabilities

### New Capabilities

- `lobby-tickle`: Ton beim Kitzeln, Hörweite und Begrenzung pro Paar. Das Kitzeln als solches ist in den Specs noch nicht beschrieben; diese Änderung ergänzt nur den Ton.

### Modified Capabilities

- `lobby-portals`: Neues Requirement für die Töne beim Passieren und beim Abweisen. Das Requirement „Ein Recht am Portal schützt die Weiterleitung“ ändert sich, weil es bisher verlangt, dass bei Abweisung nichts passiert. Weiterleitung und Chat-Verhalten bleiben gleich; der Abweisungston kommt dazu.

## Impact

- **Code:** `features/tickle` (neu: `TickleSounds`, `TicklePairLimiter`) und `features/portal` (neu: `PortalSounds`; `PortalTrigger` meldet Abweisungen mit dem Spieler). Keine Änderung an `core`.
- **Abhängigkeiten:** keine. Minestom bleibt bei `2026.10.07-26.2`.
- **Betrieb:** keine Konfiguration, keine Welt, keine Ressourcen.
