# Spec Delta

## ADDED Requirements

### Requirement: Spans des Herunterfahrens enden, bevor das Abschalten zurückkehrt
Der Span `titan.shutdown` und jeder Span, der während des Abschaltens der Features endet (etwa `jumprun.end` mit dem Grund SHUTDOWN), MUSS beendet sein, wenn der Shutdown-Task der Lobby zurückkehrt, auch wenn das Abschalten eine Ausnahme wirft. Die Lobby DARF dafür KEIN OpenTelemetry-SDK und KEINE Agent-Interna voraussetzen. Die Entwicklerdokumentation MUSS festhalten, welche Spans damit garantiert beendet sind und welche nicht (harter Kill, Spans außerhalb des Tasks, das letzte Metrik-Intervall), und welche Agent-Optionen das Zeitfenster verkleinern.

#### Scenario: Normales Abschalten
- **WHEN** der Shutdown-Task läuft und zurückkehrt
- **THEN** ist der Span `titan.shutdown` beendet und mit den `feature.stopped`-Events lesbar

#### Scenario: Abschalten mit Ausnahme
- **WHEN** das Abschalten eines Features eine Ausnahme wirft
- **THEN** ist `titan.shutdown` beendet, trägt die Ausnahme und den Fehlerstatus, und die Ausnahme erreicht den Aufrufer

#### Scenario: Span im Abschaltcode
- **WHEN** ein Feature beim Abschalten einen eigenen Span beendet
- **THEN** ist dieser Span beendet, bevor der Shutdown-Task zurückkehrt

#### Scenario: Ohne Agent
- **WHEN** die Lobby ohne Agent herunterfährt
- **THEN** läuft das Abschalten ohne Ausnahme und ohne zusätzliche Wartezeit
