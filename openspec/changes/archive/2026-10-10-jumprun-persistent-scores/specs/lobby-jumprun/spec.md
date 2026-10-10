# Spec Delta

## MODIFIED Requirements

### Requirement: Score und Rekord
Während eines Laufs MUSS der Spieler seinen aktuellen Score in der Action Bar sehen. Am Ende eines Laufs MUSS er eine Meldung mit dem erreichten Score erhalten. Übertrifft ein Lauf den Rekord des Spielers in diesem Modus, MUSS die Meldung das als neuen Rekord kennzeichnen.

Ist eine Datenbank konfiguriert, gilt Folgendes:
- Jeder Lauf, dessen Ende den Score wertet, MUSS mit Spieler, aktuellem Spielernamen, Modus, Score, Endgrund und Zeitpunkt dauerhaft gespeichert werden, auch bei Score 0.
- Der Rekord eines Spielers in einem Modus MUSS der höchste gespeicherte Score dieses Spielers in diesem Modus sein. Er gilt in jeder Lobby und über Verlassen und Neustart hinweg.
- Die Rekorde eines Spielers MÜSSEN geladen sein, bevor er die Lobby betritt.
- Ein gerade beendeter Lauf MUSS sofort für den nächsten Lauf in derselben Lobby zählen, auch wenn er noch nicht gespeichert ist.

Ohne Datenbank MUSS die Lobby den Rekord pro Spieler und Modus im Speicher halten, solange der Spieler in der Lobby ist, und ihn beim Verlassen löschen.

#### Scenario: Neuer Rekord
- **WHEN** ein Spieler mit bisherigem Rekord 12 einen Lauf mit Score 15 beendet
- **THEN** meldet die Lobby Score 15 als neuen Rekord, und sein Rekord ist danach 15

#### Scenario: Kein neuer Rekord
- **WHEN** derselbe Spieler danach einen Lauf mit Score 9 beendet
- **THEN** meldet die Lobby Score 9 ohne Rekord-Hinweis, und sein Rekord bleibt 15

#### Scenario: Rekord überdauert das Verlassen
- **WHEN** bei konfigurierter Datenbank ein Spieler mit Hard-Rekord 15 die Lobby verlässt und später eine andere Lobby betritt
- **THEN** ist sein Hard-Rekord dort 15, und ein Lauf mit Score 15 ist kein neuer Rekord

#### Scenario: Jeder gewertete Lauf wird gespeichert
- **WHEN** bei konfigurierter Datenbank ein Spieler einen Lauf mit Score 0 durch Absturz beendet
- **THEN** steht dieser Lauf mit Score 0, Modus und Endgrund in der Historie

#### Scenario: Rekord endet mit dem Verlassen
- **WHEN** ohne Datenbank ein Spieler die Lobby verlässt und später wiederkommt
- **THEN** hat er keinen Rekord mehr, und sein nächster Lauf mit Score > 0 ist ein neuer Rekord

#### Scenario: Speichern schlägt fehl
- **WHEN** bei konfigurierter Datenbank das Speichern eines Laufs fehlschlägt
- **THEN** bekommt der Spieler trotzdem seine Endmeldung, und der Rekord gilt in dieser Lobby bis zum Verlassen

## ADDED Requirements

### Requirement: Sidebar während des Laufs
Während eines Laufs MUSS der Läufer rechts eine Sidebar sehen. Sie MUSS den Spielnamen mit dem Modus des Laufs als Titel tragen und den aktuellen Score sowie seinen Rekord in diesem Modus zeigen. Score und Rekord MÜSSEN als Werte in einer eigenen, rechtsbündigen Spalte stehen. Ist eine Datenbank konfiguriert und gibt es in diesem Modus mindestens einen Rekord, MUSS sie zusätzlich unter einer Überschrift die bis zu drei Spieler mit den höchsten Rekorden in diesem Modus zeigen, in absteigender Reihenfolge, je mit Spielerkopf, Spielername und Rekord, wobei der Rekord den Platz durch seine Farbe (Gold, Silber, Bronze) kenntlich macht. Jeder Spieler darf dabei nur einmal vorkommen. Bei gleichem Rekord steht vorn, wer ihn früher erreicht hat. Ist der Läufer selbst unter den drei, MUSS seine Zeile hervorgehoben (Markierung, fetter Name) erscheinen.

Die Sidebar MUSS sich bei jeder Score-Änderung aktualisieren. Rekorde aus anderen Lobbys DÜRFEN mit einer Verzögerung von höchstens einer Minute erscheinen, ein eigener neuer Rekord MUSS sofort einsortiert werden. Außerhalb eines Laufs DARF der Spieler KEINE Jump-and-Run-Sidebar sehen, und er DARF NIE Werte eines anderen Modus als des laufenden sehen. Andere Spieler DÜRFEN die Sidebar des Läufers NICHT sehen. Die Beschriftungen „Score“, „Rekord“ und die Überschrift der Bestenliste MÜSSEN in der Sprache des Spielers erscheinen, mit Englisch als Fallback. Titel, Modusname und Spielernamen sind sprachneutral. Die Action Bar mit dem Score bleibt zusätzlich bestehen.

#### Scenario: Sidebar beim Start
- **WHEN** ein Spieler mit Hard-Rekord 42 einen Lauf in Hard startet
- **THEN** sieht er rechts eine Sidebar mit Titel „Jump & Run · Hard“, Score 0 und Rekord 42 in der Wertespalte

#### Scenario: Score steigt
- **WHEN** der Läufer Score 7 erreicht
- **THEN** zeigt die Sidebar Score 7, und die Action Bar zeigt ebenfalls 7

#### Scenario: Top 3 des Modus
- **WHEN** in Hard die Rekorde Alex 88, Steve 61, Notch 42 und Jeb 30 gespeichert sind und Jeb einen Lauf in Hard startet
- **THEN** zeigt seine Sidebar in dieser Reihenfolge Alex 88 (gold), Steve 61 (silber), Notch 42 (bronze), je mit Kopf, und keine dieser Zeilen ist hervorgehoben

#### Scenario: Eigener Platz fett
- **WHEN** Steve mit Hard-Rekord 61 einen Lauf in Hard startet
- **THEN** ist seine Zeile (Steve 61, silber) hervorgehoben, die anderen nicht

#### Scenario: Neuer Rekord rückt sofort auf
- **WHEN** Jeb im laufenden Hard-Lauf Score 50 erreicht
- **THEN** zeigt seine Sidebar sofort Jeb 50 auf dem dritten Platz (bronze) hervorgehoben, und Notch ist nicht mehr unter den drei

#### Scenario: Nur der eigene Modus
- **WHEN** ein Spieler mit Easy-Rekord 30 und Hard-Rekord 8 einen Lauf in Hard startet
- **THEN** zeigt die Sidebar Rekord 8 und die Top 3 von Hard, nichts aus Easy

#### Scenario: Sidebar verschwindet
- **WHEN** der Lauf endet, egal aus welchem Grund
- **THEN** sieht der Spieler keine Jump-and-Run-Sidebar mehr

#### Scenario: Nicht für andere
- **WHEN** A einen Lauf spielt und B ihm zusieht
- **THEN** sieht B keine Jump-and-Run-Sidebar

#### Scenario: Ohne Datenbank
- **WHEN** ohne Datenbank ein Spieler einen Lauf startet
- **THEN** zeigt die Sidebar Titel, Score und Rekord, aber keine Überschrift und keine Top 3

#### Scenario: Deutscher Client
- **WHEN** ein Spieler mit deutscher Client-Sprache einen Lauf startet
- **THEN** zeigt die Sidebar die deutschen Beschriftungen, Spielernamen und Modus bleiben unverändert
