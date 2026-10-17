# Proposal

## Why

Zwei Fehler am Jump-and-Run-Item, beide durch doppelte Pakete eines einzigen Klicks:

- Ein Schleichklick kann den Item-Gebrauch mehrfach auslösen (Klick auf den Block, dann das einfache Benutzungspaket). Jeder Durchlauf schaltet den Modus weiter, sodass Moduswechsel übersprungen werden.
- Ein Doppelklick oder das Folgepaket eines Klicks auf einen echten Lobby-Block beendet einen frisch gestarteten Lauf. In der Woche vor dem Fix endeten 94 Läufe mit Score 0 nach wenigen Sekunden.

Der Fix (PR #421) führt eine Entprellung für den Moduswechsel und eine Startsperre für Läufe ein. Die Spec sagt aber an zwei Stellen das Gegenteil: „jeder solche Klick MUSS zum nächsten Modus wechseln“ und „ein Lauf MUSS enden, wenn er das Item erneut benutzt“. Diese Anforderungen müssen das Fenster und die Sperre genau benennen, sonst widerspricht die Spec dem Verhalten.

## What Changes

- Moduswechsel: Ein Schleichklick innerhalb von 5 Ticks (250 ms) nach dem letzten Moduswechsel desselben Spielers wechselt nichts. Das Fenster läuft ab dem Wechsel, nicht ab dem ignorierten Klick.
- Startsperre: Ein Lauf ignoriert die Benutzung des Items in den ersten 20 Ticks (1 s) nach seinem Start. Die Sperre endet mit dem Lauf, aus welchem Grund auch immer.
- Blockklick: Die Folgebenutzung eines Klicks auf einen Block wird nur während eines Laufs ignoriert. Außerhalb eines Laufs startet oder wechselt sie wie eine gewöhnliche Benutzung.
- Keine Code-Änderung in diesem Change: Das Verhalten steht im PR #421. Dieser Change hält es in der Spec fest.

## Non-goals

- Keine Änderung der Werte 5 und 20 Ticks.
- Keine Änderung an Rekorden, Modi oder Endgründen.

## Delivery

Der Pull Request ist #421 und trägt den Squash-Commit-Titel `fix(jumprun): debounce the mode switch and accidental stops` (Conventional Commits, Typ `fix`, Scope `jumprun`).
