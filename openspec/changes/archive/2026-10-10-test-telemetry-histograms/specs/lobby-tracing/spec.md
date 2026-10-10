# Spec Delta

## ADDED Requirements

### Requirement: Die Testhilfe liest Histogramme wie Zähler
Die Testhilfe `TestTelemetry` MUSS neben Spans, Zählern und Gauges auch Histogramme auslesen können: alle aufgezeichneten Punkte eines Histogramms sowie Anzahl und Summe für genau einen Attributsatz. Sie MUSS dabei, wie für Zähler, je Test einen eigenen Zustand verwenden. Ein Test DARF dafür KEINEN eigenen Metrik-Reader aufbauen müssen.

#### Scenario: Anzahl und Summe je Attributsatz
- **WHEN** ein Test zwei Werte (3 und 4) mit den Attributen `mode=MEDIUM` und einen Wert (10) mit `mode=HARD` in dasselbe Histogramm aufzeichnet
- **THEN** liefert die Abfrage für `mode=MEDIUM` die Anzahl 2 und die Summe 7, und die für `mode=HARD` die Anzahl 1 und die Summe 10

#### Scenario: Nichts aufgezeichnet
- **WHEN** ein Histogramm nie befüllt wurde oder ein Attributsatz nie vorkam
- **THEN** liefert die Abfrage Anzahl 0 und Summe 0, ohne Ausnahme

#### Scenario: Alle Punkte
- **WHEN** ein Test Werte unter zwei Attributsätzen aufzeichnet
- **THEN** liefert die Punkteliste genau einen Punkt je Attributsatz

#### Scenario: Zwei Tests ohne gemeinsamen Zustand
- **WHEN** zwei Tests je ein eigenes `TestTelemetry` benutzen und nur einer ein Histogramm befüllt
- **THEN** sieht der andere keinen Punkt
