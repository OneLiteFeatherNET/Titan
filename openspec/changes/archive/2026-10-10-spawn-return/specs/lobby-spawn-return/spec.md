# Spec Delta

## Purpose

Legt fest, wie ein Spieler, der in der Lobby feststeckt, jederzeit an den Lobby-Spawn zurückkehrt, und dass andere Features davon erfahren, bevor er versetzt wird.

## ADDED Requirements

### Requirement: Rückkehr zum Spawn per Befehl
Jeder Spieler in der Lobby MUSS mit dem Befehl `/spawn` an den Spawn-Punkt der aktiven Lobby-Karte zurückkehren können, ohne dafür ein Recht zu brauchen. Die Lobby MUSS ihn sofort versetzen, ohne Wartezeit und ohne Bedingung, dass er stillsteht. Danach MUSS er im Chat eine Bestätigung erhalten. Die Bestätigung MUSS in der Sprache des Spielers erscheinen, mit Englisch als Fallback. Der Befehl MUSS auch funktionieren, während der Spieler mit der Elytra gleitet; das Gleiten MUSS dabei enden, der Spieler DARF am Spawn KEINEN Schwung mehr haben, und die Rakete, die er beim Gleiten in der Nebenhand bekommt, MUSS verschwinden.

#### Scenario: Festgesteckt auf einem Dach
- **WHEN** ein Spieler ohne besondere Rechte auf einem Dach steht und `/spawn` eingibt
- **THEN** steht er sofort am Spawn-Punkt der Lobby und erhält eine Bestätigung im Chat

#### Scenario: Beim Gleiten
- **WHEN** ein Spieler mit der Elytra gleitet und `/spawn` eingibt
- **THEN** steht er am Spawn-Punkt, gleitet nicht mehr, bewegt sich nicht weiter, und seine Nebenhand ist leer

#### Scenario: Deutscher Client
- **WHEN** ein Spieler mit deutscher Client-Sprache `/spawn` eingibt
- **THEN** erscheint die Bestätigung auf Deutsch

#### Scenario: Unbekannte Sprache
- **WHEN** ein Spieler mit einer Client-Sprache ohne eigene Übersetzung `/spawn` eingibt
- **THEN** erscheint die Bestätigung auf Englisch

### Requirement: Karte ohne Spawn-Punkt
Hat die aktive Lobby-Karte keinen Spawn-Punkt, DARF eine Rückkehr zum Spawn den Spieler NICHT versetzen. Er MUSS stattdessen eine Meldung in seiner Sprache erhalten, mit Englisch als Fallback. Andere Features DÜRFEN in diesem Fall NICHT über eine Rückkehr benachrichtigt werden.

#### Scenario: Kein Spawn-Punkt
- **WHEN** die aktive Karte keinen Spawn-Punkt hat und ein Spieler `/spawn` eingibt
- **THEN** bleibt er, wo er ist, und erhält die Meldung, dass es keinen Spawn-Punkt gibt

### Requirement: Andere Features erfahren von der Rückkehr
Bevor die Lobby einen Spieler zum Spawn zurückbringt, MUSS sie allen Lobby-Features mitteilen, dass dieser Spieler zum Spawn zurückkehrt, egal ob die Rückkehr über den Befehl oder über den Navigator ausgelöst wurde. Ein Feature MUSS dabei seinen Zustand für diesen Spieler aufräumen können, bevor er versetzt wird. Ein Feature, das mit der Rückkehr nichts zu tun hat, DARF davon NICHT betroffen sein.

#### Scenario: Gleicher Weg für Befehl und Navigator
- **WHEN** ein Spieler einmal per `/spawn` und einmal über den Navigator zum Spawn zurückkehrt
- **THEN** erhalten die Lobby-Features in beiden Fällen dieselbe Mitteilung, jeweils bevor er versetzt wird
