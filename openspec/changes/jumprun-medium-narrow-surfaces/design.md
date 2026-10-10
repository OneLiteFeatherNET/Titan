# Design

## Kontext

Die Freischaltung je Modus steht in `Mode.Params.unlocks(gentle, narrow, narrowest)`, und `Mode.minScore(surface)` liefert den Punktestand, ab dem eine Form erscheint. Die Auswahl im Generator nimmt den Kandidaten, dessen Kosten der Zielkosten am nächsten liegen; die Zielkosten sind `level(score) * hardest + Rauschen`, wobei `hardest = Jump.maxCost(mode, unlockedAt(score))` nur die freigeschalteten Formen berücksichtigt (`Difficulty.targetCost`). Eine Gewichtung je Modus gibt es nicht, nur Schwellen.

## Entscheidungen

- **Ausschluss über die Schwellen, nicht über Gewichte.** Medium setzt `narrow` und `narrowest` auf `Integer.MAX_VALUE` (`Mode.Params.NEVER`). Damit gibt es keinen neuen Mechanismus: `minScore` und `unlockedAt` liefern die Form nie. Das entspricht der bestehenden Struktur und lässt Hard unberührt.
- **Keine Verschiebung der Schwelle statt Ausschluss.** Ein späterer Schwellwert in Medium würde die Schwierigkeit nur verschieben, und die Beschwerde betrifft genau die Dauerpräsenz der schmalen Formen auf dem Plateau ab Score 40.
- **Zielkosten passen sich von selbst an.** Da `hardest` aus den freigeschalteten Formen folgt, sinkt das Ziel in Medium auf die Kosten der Stufe und der breiten Lücke. Keine Formel wird geändert; ein Test prüft das Maximum (2 · 1 + 1,5 · 3 = 6,5).
- **Schmal = Tier narrow oder narrowest.** Im Code gibt es dafür keine eigene Eigenschaft. `Surface.hasNarrowFootprint()` erfasst nur Kopf, Blumentopf und Kerze, weil Zaun, Scheibe und Pfosten mit Absicht über die ganze Zelle lenient kollidieren. Die Tiers sind deshalb die Quelle der Wahrheit, und die Tests listen die Formen explizit.
- **Rainbow teilt die Parameter von Medium** (`Params.MEDIUM`), und ein bestehender Test pinnt „Rainbow erzeugt wie Medium“. Rainbow verliert die schmalen Formen also mit. Das entspricht der Spec („Rainbow: Schwierigkeit wie Medium“). Die Alternative, Rainbow eigene Parameter zu geben, wäre ein zusätzlicher Mechanismus ohne Anforderung.
- **Sprunglängen unverändert.** `maxGap`, `maxGapAscent`, `MAX_RISE` und die Typkosten bleiben. Dass #367 die Höhendifferenz eines Sprungs von der Unterkante der Quelle aus misst (`Jump.rise`), beeinflusst Sprünge ab Stufen und wurde nicht geändert; siehe PR-Text.
- **Türme** (`climbs`) bleiben in Medium bei 30 und 40. Die Höhe der Türme ändert ein anderer Change.

## Tests (F.I.R.S.T.)

- Generator-Tests mit festen Seeds (1 bis 12, je 400 Sprünge): Medium erzeugt nie eine schmale Form; Hard erzeugt alle schmalen Formen.
- Modus-Tests: Medium hat für keine schmale Form einen Schwellwert, Hard und Ultra behalten ihre Schwellen.
- Schwierigkeits-Tests: Die schwerste Medium-Stufe bei großem Score ist 6,5, und das mittlere Ziel nähert sich dieser Grenze.
- Env-Tests mit Blockpaketen für Kerzen entfallen, weil die feste Welt im Hard-Lauf vor der ersten Kerze endet. Die Geometrie der Kerzenlandung ist in `StepLandingTest` abgedeckt.
