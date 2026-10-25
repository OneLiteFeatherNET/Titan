# Design

## Kontext

Das Item ruft in Minestom zwei Pakete pro Klick auf: bei einem Klick auf einen Block zuerst das Benutzungspaket auf den Block, dann das einfache Benutzungspaket. Beide erreichen `JumprunModule.use`. Zeit misst die Tick-Zählung des Spielers (`getAliveTicks`), damit Tests mit `env.tick()` laufen und ohne Systemuhr.

## Entscheidungen

- **Moduswechsel-Fenster: 5 Ticks ab dem letzten Wechsel.** Ein Schleichklick im Fenster wird ignoriert und verlängert das Fenster nicht. Ein fester Zeitpunkt pro Wechsel reicht, weil ein Doppelpaket im selben Tick oder im nächsten Tick ankommt.
- **Startsperre: 20 Ticks ab dem Start, über den bestehenden Mechanismus `suppressedUntil`.** Sie nutzt dieselbe Unterdrückung wie das Folgepaket eines Blockklicks und verkürzt keine laufende Sperre.
- **Blockklick unterdrückt nur im Lauf.** Außerhalb eines Laufs gibt es kein Folgepaket, das einen Lauf beenden könnte; die Unterdrückung würde nur den Start oder den Moduswechsel verhindern.
- **Sperren enden mit dem Lauf.** `end` räumt den Klickzustand des Spielers auf, egal welcher Grund den Lauf beendet. Sonst verhindert eine Sperre aus dem alten Lauf den Neustart innerhalb einer Sekunde.

## Tests (F.I.R.S.T.)

Die Szenarien der Spec haben Tests in `JumprunModeTest`, `JumprunStopTest` und `JumprunClickTest`. Die Tests laufen über `useItem` oder die Paket-Schnittstelle und treiben die Zeit mit `env.tick()`, ohne Warten und ohne Systemuhr.
