# Proposal

## Why

Spieler sehen vor einem Lobby-Portal nur den Rahmen, nicht wohin es führt und ob dort etwas los ist. Wer ein Portal betritt, landet auf einem Server, dessen Name und Spielerzahl er vorher nicht kannte. Ein Schild aus Text über dem Portal, das Ziel und (optional) die Spielerzahl zeigt, löst das und macht Portale als Einstieg in die Netzwerk-Server attraktiver.

## What Changes

- **Voraussetzung:** baut auf `lobby-portals` und `setup-portal-command` auf; beide Codestände sind in `main` (`/setup portal`, `PortalDraft`, `PortalEditor`, `PortalGsonAdapter`, `PortalValidator`).
- **Kartendaten:** Ein Portal in der `map.json` darf einen optionalen Block `label` tragen: `position`, `text` (MiniMessage), optional `offlineText`, optional `source` (`type` = `task`, `group`, `service` oder `local`, dazu `name`; außer bei `local`), optional `billboard` (`center`, Standard, oder `fixed` mit `yaw`). Portale ohne `label` verhalten sich wie bisher, es entsteht keine Anzeige.
- **Anzeige:** Für jedes Portal mit `label` zeigt die Lobby genau eine gemeinsame `TextDisplay`-Entität an der Position. Der Text ist MiniMessage mit den Platzhaltern `<online>`, `<max>`, `<task>` und dem globalen `<prefix>`. Keine Argument-Tags wie `<online:group:x>`.
- **Spielerzahl:** Je Anzeige gibt es genau eine Quelle: `task` (Summe über alle Server eines Ziels), `group` (Summe über alle Server einer Gruppe), `service` (genau eine benannte Server-Instanz) oder `local` (nur diese Lobby, Verbindungsverwaltung). `task`, `group` und `service` sind anbieterneutrale Begriffe; das System, das die Zahlen liefert, bildet sie auf seine Konzepte ab. Ohne `source` gilt der Task des Portals. `<online>` und `<max>` beziehen sich auf diese Quelle.
- **Austauschbarer Zähler:** Die Spielerzahlen kommen über eine Anbieter-Schnittstelle (SPI) in `core`, die von CloudNet unabhängig ist. CloudNet (in `bridge`) ist nur eine Implementierung; eine andere (anderes Cloud-System, Redis, Proxy) lässt sich als Avaje-Bean bzw. Extension liefern, ohne `features/portal` anzufassen. Es ist genau ein Anbieter aktiv (keine Zusammenführung); ein echter Anbieter hat Vorrang vor dem eingebauten Ersatz, der für entfernte Quellen „läuft nicht“ meldet. `local` läuft nie über den Anbieter.
- **Offline:** Läuft für die Quelle kein Server (oder ist kein Anbieter verfügbar bzw. unterstützt der Anbieter den Quellentyp nicht, z. B. lokal oder im Setup-Server), zeigt die Anzeige `offlineText`, sonst `text` mit 0/0. `local` funktioniert immer.
- **Aktualisierung:** Alle 5 Sekunden, einstellbar über das Modul-Konfigurationsfeld `portal.labelRefreshSeconds`; Metadaten gehen nur an die Clients, wenn sich der gerenderte Text geändert hat.
- **Setup-Befehl:** `/setup portal <id> label here | text <minimessage> | offline <minimessage> | source <type> [name] | remove`; die Partikel-Vorschau markiert zusätzlich den Anker des Labels. Gespeichert wird wie bei den übrigen Portaldaten nur per `save`.
- **Validierung beim Start:** Ungültiges MiniMessage in `text`/`offlineText`, ein unbekannter `source.type`, ein fehlender Name bei `task`/`group`/`service`, ein unbekanntes `billboard` oder eine fehlende `position` brechen den Start ab, wie ungültige Portale. Ein Ziel, eine Gruppe oder ein Server, den der Anbieter zur Laufzeit nicht kennt, und ein vom Anbieter nicht unterstützter Quellentyp sind kein Startfehler, sondern nur eine einmalige Warnung im Log. Das Vokabular von `source.type` (`task`, `group`, `service`, `local`) bleibt fest.
- **Wechselwirkung:** `optional-extensions-bootstrap` verschiebt das CloudNet-Wissen nach `platform/cloudnet`, lässt `:bridge` aber eine Extension. Die CloudNet-Anbindung dieses Changes (JDK-only-Halter in `common`, Installation durch `:bridge`) ist davon unabhängig; in der lokalen Variante ohne Bridge greift der Ersatz.
- Kein Übersetzungs-Mechanismus: der Anzeigetext ist Kartendaten-Text des Build-Teams, nicht Teil des Spracheninventars (siehe design.md D8).

Lieferung als **ein** Change unter dem Titel `feat(portal): show labels with player counts in front of portals`.

## Capabilities

### New Capabilities

- `lobby-portal-labels`: Portale tragen optional ein Label, das als geteilte TextDisplay in der Lobby Ziel und Spielerzahl zeigt, mit Platzhaltern, Quellen, Offline-Text, Aktualisierung und Start-Validierung.
- `setup-portal-labels`: Das Build-Team legt das Label eines Portals im Setup-Server per `/setup portal <id> label ...` an, ändert und entfernt es; die Vorschau markiert den Anker, gespeichert wird nur per `save`.

### Modified Capabilities

Keine. `lobby-portals` bleibt unverändert (ein Portal ohne `label` verhält sich wie bisher); `setup-portals` liegt noch nicht als Hauptspec vor (liegt im Change `setup-portal-command`), deshalb gibt es die Setup-Seite als eigene, neue Capability.

## Impact

- **Code**: `core` (`Portal` bekommt eine optionale Komponente `label`, neue Typen `PortalLabel`, `LabelSource`, Anbieter-SPI `PlayerCounts` mit eingebautem Ersatz, Validierung), `common` (`PortalGsonAdapter` liest/schreibt `label`; JDK-only-Halter und `PlayerCounts`-Implementierung), `bridge` (CloudNet als eine Implementierung des Zähler-SPI), `features/portal` (Label-Anzeige, Rendering, Aktualisierung, Konfiguration), `setup` (`PortalCommand`, `PortalEditor`, `PortalDraft`, `PortalMessages`, Vorschau).
- **Abhängigkeiten**: keine neuen Bibliotheken; CloudNet-Provider-APIs kommen aus der bereits vorhandenen Bridge-Abhängigkeit.
- **Konfiguration**: neuer Schlüssel `portal.labelRefreshSeconds` (Standard 5, mindestens 1), Env-Variable `PORTAL_LABELREFRESHSECONDS`; README (Konfigurationstabelle) und Standardwerte werden ergänzt.
- **Kartendaten**: neues optionales JSON-Feld `label`, abwärtskompatibel.
- **Nutzertexte**: Anzeigetext kommt aus der Karte (kein Bundle). Neue Chat-Rückmeldungen des Setup-Servers, englisch im Stil von `PortalMessages` (das Repository hat keine Übersetzungsinfrastruktur).
- **Tests**: Unit (Rendering/Platzhalter, Anzeige mit einem Fake-Anbieter ohne CloudNet, Quellen-Parsing und Validierung, Gson-Round-Trip, Änderungserkennung, Editor), Integration mit Cyano-`Env` und `env.tick()` (Anzeige, Aktualisierung, Setup-Befehl).
- **Betrieb**: eine Warnzeile je unbekannter Quelle; kein neuer Dienst.
