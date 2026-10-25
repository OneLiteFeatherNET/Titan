# Proposal

## Why

Lobby-Spieler wollen sich gegenseitig necken, und Team-Mitglieder (Betreiber, Helfer) sollen Besucher spielerisch durch die Lobby schleudern können. Heute gibt es dafür kein Feature. Das Schleudern ist bewusst auf Spieler mit einem Recht beschränkt, damit es nicht zum Belästigungswerkzeug für alle wird. Der Ziel-Spieler muss nicht zustimmen, sondern kann sich nur per Opt-out aus dem Schleudern nehmen, den das Settings-System (Change `lobby-settings`) bereitstellt.

## What Changes

- Neues Feature-Modul `features/throw` (Gradle-Name und Column `throw`, Java-Paket `net.onelitefeather.titan.feature.throwing`, weil `throw` ein Java-Schlüsselwort ist).
- **Aufnehmen:** Ein Spieler mit dem Recht `titan.throw` rechtsklickt (bei leerer Haupthand) einen anderen Spieler. Das Ziel sitzt danach als Passagier auf dem Kopf des Werfers.
- **Aufladen statt Sofortwurf:** Während der Trage hält der Werfer einen temporären vanilla Ladeträger (Stock mit Komponente `consumable`, Animation `bow`). Rechtsklick startet die Ladung, Loslassen wirft. Die Stärke wird linear auf die Geschwindigkeit abgebildet, mit Obergrenze von 1,5 s Ladezeit. Die Hand des Werfers wird nach jedem Pfad exakt wiederhergestellt. Begründung und verworfene Alternativen stehen in `design.md` (D1).
- **Wurf:** Das Ziel fliegt in Blickrichtung mit 18 bis 50 Blöcke/s (ein Tipp landet etwa 6 Blöcke weit, eine volle Ladung bei 45 Grad etwa 20 Blöcke). Die Geschwindigkeit setzt der Client des Ziels selbst um (wie beim bestehenden Feuerwerks-Boost), die Lobby schreibt nur den Impuls.
- **Flugbahnvorschau:** Während des Ladens sieht nur der Werfer die vorhergesagte Flugbahn als vanilla Partikel, mit Landemarker. Die Vorschau ist eine Näherung.
- **Fortschritt:** Actionbar-Leiste und steigender vanilla Ton, nur für den Werfer.
- **Abbruch durch das Ziel:** Schleicht das Ziel, steigt es ab. Das zählt als Ablehnung und setzt eine kurze Sperre zwischen diesem Werfer und diesem Ziel.
- **Opt-out (Abhängigkeit `lobby-settings`):** Jeder Spieler kann in den Einstellungen „Kann geworfen werden“ ausschalten (Standard: an, Schlüssel `titan:throw.target`). Ein abgemeldeter Spieler kann nicht aufgenommen werden; der Werfer erhält eine kurze Nachricht. Schaltet ein Ziel den Opt-out während einer Trage ein, wird es sofort ohne Impuls freigegeben. Das Schleudern selbst bleibt an das Recht `titan.throw` gebunden.
- **Immunität:** Spieler mit `titan.throw.immune` (z. B. Team-Mitglieder über LuckPerms) können nicht aufgenommen werden. Der Werfer erhält dieselbe kurze Nachricht.
- **Sperren:** Cooldowns pro Werfer, pro Paar und ein Auto-Drop nach maximaler Tragzeit.
- **Flugschutz:** Geworfene Spieler lösen während des Flugs und einer kurzen Landephase keine Portale aus.
- **Rückmeldung:** Vanilla-Töne und -Partikel (keine Resource Pack), XP-Leiste als Ladeanzeige, farbiges Messgerät mit Prozentzahl in der Actionbar und einmaliger Hinweis „Rechtsklick halten zum Aufladen, loslassen zum Werfen“ für den Werfer.
- **Telemetrie:** Zähler für Aufnahmen, Würfe, Abbrüche durch das Ziel, Ablehnungen und Ladeabbrüche.
- **Texte:** Deutsch und Englisch über Message-Bundles nach dem Muster von `DanceMessages`.

## Capabilities

### New Capabilities

- `lobby-throw`: Aufnehmen, Werfen, Sperren, Opt-out-Prüfung und Flugschutz für Spieler in der Lobby.

### Modified Capabilities

Keine. Der Opt-out liest über das neue Settings-Port aus `lobby-settings`; dieser Change definiert die Speicherung selbst.

## Dependencies

- **Voraussetzung `lobby-settings` (Reihenfolge fest):** `lobby-settings` (Otis-Speicherung, typisierter Settings-Port in `core`) wird zuerst implementiert und auf `main` gemergt. Erst danach startet die Umsetzung von `lobby-throw`. Der Opt-out `titan:throw.target` ist eine Option, die auf diesem Port aufsetzt; er ist kein eigenständiger Speicher. Der Port braucht zusätzlich eine Änderungsbenachrichtigung, damit das Freigeben einer Trage sofort wirkt (Ergänzung zu `lobby-settings`, siehe `design.md` D9).

## Impact

- **Code:** neues Modul `features/throw` (Column, Modul, Telemetrie, Messages), `features/throw/build.gradle.kts`, Einträge in `settings.gradle.kts` und im Lobby-Aggregat. Nur Abhängigkeit auf `core` (plus `PermissionService`, `Telemetry`, Settings-Port aus `core`). Keine Abhängigkeit auf andere Features.
- **Config:** keine neuen Schlüssel in `application*.yaml`. Die Grenzwerte sind Konstanten im Modul, weil sie spielbalancierend sind und nicht pro Betreiber schwanken sollen (KISS).
- **Berechtigungen:** `titan.throw` (werfen), `titan.throw.immune` (nicht werfbar). Lokal gilt `DenyAllPermissionService`, dort wirft niemand.
- **Docs:** `docs/lobby-modules.md` um das Modul ergänzen.
- **Nutzertexte:** neue Texte für Actionbar, Ablehnung, Immunität und Opt-out (de/en).

## Non-goals

- Kein Schleudern durch Spieler ohne Recht `titan.throw`. Das Ziel braucht keine aktive Zustimmung, nur den Opt-out-Standard „an“.
- Keine Fallschaden-Mechanik (in der Lobby irrelevant).
- Keine Resource-Pack-Assets, keine eigenen Modelle.
- Keine Persistenz von Sperren oder Tragzustand über Neustarts hinaus.
- Keine Chat-Befehle in diesem Change. Falls ein Befehl dazukommt (z. B. `/throw`), ist sein Name immer Englisch, ohne deutsche Aliase.

## Delivery

PR-Titel: `feat(throw): let permitted players pick up and throw others in the lobby`
