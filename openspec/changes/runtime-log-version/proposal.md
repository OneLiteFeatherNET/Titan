# Proposal

## Why

Titan schreibt beim Start keine Version ins Log. Im Betrieb (Loki) lässt sich deshalb nicht erkennen, welcher Build auf einem Server läuft, etwa nach einem Deploy oder wenn ein Server mit altem Jar hängen geblieben ist.

## What Changes

- Titan loggt beim Start genau eine INFO-Zeile mit Version und Build-Commit, z. B. `Titan 3.2.1 (commit 99a7f62) starting`.
- Version und Commit kommen aus dem Manifest des ausgelieferten Jars: `Implementation-Version` (bereits gesetzt) und ein neues Attribut `Git-Commit` mit dem kurzen Git-Hash.
- Fehlt das Manifest (IDE, Tests) lautet die Version `dev`, fehlt der Commit `unknown`.
- Der Build bricht nicht ab, wenn kein Git verfügbar ist.

## Capabilities

### New Capabilities

### Modified Capabilities
- `server-bootstrap`: neue Anforderung für die Start-Zeile mit Version und Commit.

## Impact

- **Code:** `runtime` (`bootstrap/`: neue Start-Log-Klasse, Build-Info), `TitanApplication` ruft sie beim Start auf.
- **Build:** `buildSrc/.../titan.java-conventions.gradle.kts` setzt `Git-Commit` in das Manifest aller Jars.
- **Abhängigkeiten:** keine.
- **Nutzertexte:** keine.

## Delivery

Pull-Request-Titel und Squash-Commit: `feat(runtime): log the titan version and commit at startup`
