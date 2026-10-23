# Design

## Voraussetzung

`lobby-settings` (Otis-Einstellungen, Settings-Schnittstelle in `core`, Schlüssel-Register) wird **zuerst** implementiert und gemerged. `lobby-tag` ist eine Option, die darauf aufbaut: Ohne diese Schnittstelle gibt es keinen Opt-in-Schalter, und diese Änderung führt keinen eigenen Schalter ein.

## Context

Eine Column ist ein `@Singleton` unter `features/<name>`, das nur von `core` abhängt. Sie hängt ihren `FeatureNode` an den `titan`-Knoten; die Priorität muss je Elternknoten eindeutig sein (`FeatureNode.attach` wirft sonst). Belegt sind heute 100 (protection), 200 (spawn), 300 (respawn), 400 (navigator), 450 (lobbyswitcher), 500 (sit), 600 (tickle), 650 (dance), 700 (elytra), 800 (season), 900 (portal), 1000 (jumprun).

Fangen braucht drei Dinge, die die Lobby bisher nicht hat: einen Zustand pro Instanz, einen Nahkampf-Hook mit Abbruch und eine Einstellung pro Spieler. Die Einstellung kommt aus `lobby-settings`. Der Nahkampf-Hook ist `EntityAttackEvent`, den bereits `tickle` und `dance` benutzen. `protection` bricht Angriffe nicht ab; es bricht nur Aufnehmen, Ablegen, Tauschen, Inventar und Blöcke ab. Abgebrochen wird ein Angriff in dieser Lobby also nur über `Cancelable.cancel` in der jeweiligen Column.

## Goals / Non-Goals

**Goals**

- Opt-in über die Einstellung `titan:tag.enabled`, ohne eigenen Schalter.
- Genau ein Fänger je Instanz, sobald zwei aktive Spieler da sind; die Rolle wandert nur durch Treffer oder durch Austritt des Fängers.
- Keine Runden, keine Zeitlimits außer der Inaktivitätsregel und den Abklingzeiten.
- Jede Regel als reine Logik, die ohne Server getestet wird; Zeit und Zufall kommen von außen.

**Non-Goals**

- Settings-Menü, Otis-Speicherung, Typen der Einstellungen (`lobby-settings`).
- Statistiken über die Zähler hinaus, Persistenz, Bestenlisten.
- Projektile, Ressourcenpakete, konfigurierbare Werte.

## Decisions

### D0 Opt-in über die Settings-Schnittstelle

Die Einstellung `titan:tag.enabled` (Boolean, Standard `false`) liegt im Schlüssel-Register von `lobby-settings`. Fangen liest sie beim Beitritt in der Instanz und bei jeder Änderung über das Ereignis aus `lobby-settings`, nicht bei jedem Treffer. Schreibt der Spieler sie um, egal ob über das Menü oder `/tag`, ändert sich der aktive Zustand im selben Tick.

`/tag` ruft dieselbe Schnittstelle auf und dreht den Wert um. Ist die Schnittstelle nicht erreichbar, bleibt der Spieler inaktiv und bekommt eine Meldung, statt einen lokalen Zustand zu erfinden.

Fangen hat keinen Hotbar-Platz. Das Item, das in der ersten Version geplant war, entfällt.

### D1 Zustand pro Instanz, rein

`TagGame` hält für eine Lobby-Instanz: die Menge aktiver Spieler (UUIDs), die Id des aktuellen Fängers (oder keine), die Zeit des letzten Treffers je Paar (für den Rücktausch-Schutz) und die Zeit der letzten Bewegung oder Aktion je Fänger.

`TagGame` kennt Minestom nicht. Es bekommt Ereignisse als einfache Aufrufe (`join`, `leave`, `hit(attacker, target, now)`, `tick(now)`) und liefert Entscheidungen zurück (`TagOutcome`: `PASSED`, `COOLDOWN`, `IGNORED`; plus die neue Fänger-Id). Die Column übersetzt das in Effekte. Damit sind alle Regeln mit `AdjustableClock` und einem `Random` mit fixem Seed testbar.

Die Zufallswahl bekommt einen injizierten `Random`. Tests setzen einen festen Seed, damit der Fänger reproduzierbar ist.

### D2 Ablauf eines Treffers

1. `EntityAttackEvent` mit Spieler als Angreifer und Ziel in derselben Instanz.
2. Sind beide aktiv: Angriff wird abgebrochen (`Cancelable.cancel`). Damit gibt es weder Schaden noch Rückstoß.
3. Ist der Angreifer der Fänger und das Ziel nicht der Fänger:
   - Rücktausch-Schutz greift (Ziel war in den letzten 5 Sekunden Fänger-Gegner des Angreifers): `COOLDOWN`, nur Actionbar-Hinweis.
   - Sonst wechselt die Rolle zum Ziel: `PASSED`. Neue Effekte: Ton für beide, Titel für das Ziel, Actionbar für alle aktiven Spieler, Zählung `tagged`.
4. Sonst (kein Fänger angreift, oder der Fänger trifft den Fänger nicht): nur Abbruch, keine Rollenänderung.

Die Trefferanimation (`EntityAnimationPacket` mit Typ „Schaden nehmen“) geht an alle Zuschauer des Ziels. Das ist die einzige visuelle Antwort, die der Treffer hat.

### D3 Reihenfolge der Ereignisse

Fangen hat die Priorität 620. Sie liegt nach `tickle` (600), damit Tickle und Fangen nicht gegeneinander laufen: Tickle prüft nur, ob die Feder in der Hand ist, und seine Listener sehen den Treffer vor dem Abbruch durch Fangen. Vor `dance` (650), damit der Abbruch dort mit `onIncludingCancelled` weiterhin den Tanz beendet.

Fangen registriert Angriffe mit `onIncludingCancelled`, weil andere Module (z. B. ein späteres Schutzmodul) einen Angriff vorher abbrechen können, der Treffer für die Rolle aber trotzdem zählen soll. Nur die Entscheidung selbst läuft über den Zustand.

### D4 Austritte und Ruhezustände

Ein Spieler verlässt das Spiel (wird inaktiv) bei:

- Einstellung auf „aus“ (Ereignis aus `lobby-settings`),
- `PlayerDisconnectEvent`,
- `RemoveEntityFromInstanceEvent` (Instanzwechsel),
- `LobbyPlayerBusyEvent(player, true)` aus dem neuen Core-Ereignis.

`LobbyPlayerBusyEvent` ist ein Record in `core` (`net.onelitefeather.titan.core.module`). Es wird gesendet:

- von `jumprun` beim Start eines Laufs mit `busy=true` und beim Ende mit `busy=false`,
- von `portal` beim Start einer Übergabe zu einem anderen Server mit `busy=true`. Die Übergabe endet mit dem Verbindungsabbruch.

So bleibt `features/tag` nur von `core` abhängig, und keine Column ruft eine andere auf. Der Lauf ist ein Einzelspieler-Modus, in dem der Spieler nicht gejagt werden soll, und die Übergabe verlässt die Lobby. Das Ereignis ist allgemein benannt; `lobby-throw` kann es ebenfalls nutzen.

Ein Spieler, der aus dem Spiel fällt, während er Fänger ist, verliert die Rolle. Die Rolle geht dann zufällig an einen verbleibenden aktiven Spieler, wenn es noch zwei gibt. Kommt er zurück, ist er ein normaler aktiver Spieler (keine Rückkehr in die Rolle).

### D5 Inaktivität des Fängers

Der Fänger verliert die Rolle nach 120 Sekunden ohne Bewegung, Treffer oder Eingabe. Die Zeit kommt aus der injizierten `Clock`. Geprüft wird jede Sekunde über einen Task (`Scheduler`). Inaktive Nicht-Fänger bleiben im Spiel und können getroffen werden. Läuft die Frist eines Fängers ab, geht die Rolle an einen anderen aktiven Spieler, nie an denselben.

Warum nicht ohne Regel: Ein AFK-Fänger würde das Spiel für alle anderen dauerhaft blockieren. Warum 120 Sekunden: lang genug, um still zu stehen und zu lesen, kurz genug, damit niemand eine Lobby lang festhängt. Die Zahl ist eine Konstante im Code, wie die anderen Abklingzeiten.

Das Spiel kennt keine AFK-Erkennung der Lobby. Die Regel nutzt nur die Bewegung, die Lobby ohnehin mitschreibt (`PlayerMoveEvent`), und Eingaben (`ClientInputPacket`), wie `dance` es tut.

### D6 Interaktionen mit anderen Features

- **Tanzen:** Tanzt der Fänger und trifft, endet sein Tanz durch den bestehenden Angriffs-Stopp von `dance`. Die Rolle wird trotzdem weitergegeben, denn ein Angriff ist Spielhandlung. Wird ein tanzender Spieler getroffen, läuft sein Tanz weiter; ein Treffer beendet fremde Tänze nicht. Begründung: Das Spiel soll nicht durch eine Pose blockiert werden, und die Pose gehört dem Tanzenden, nicht dem Angreifer.
- **Sitzen:** Sitzende Spieler sind aktiv und können getroffen werden oder treffen. Sitzen ändert nichts an der Teilnahme. Ein sitzender Fänger, der trifft, bleibt sitzen (der Angriff ist kein Aufstehen). Begründung: Sitzen ist eine Position, keine Abwesenheit; das Spiel soll die Lobby nicht umbauen.
- **Elytra:** Während eines Gleitflugs ist die Rolle eingefroren: Treffer zählen nicht, weder für den Fänger noch gegen ihn. Die Rolle bleibt beim Fänger, und nach der Landung läuft das Spiel normal weiter. Begründung: Treffer im Flug sind mit hoher Geschwindigkeit unfair und unlesbar. Der Zustand kommt aus den Minestom-Ereignissen `PlayerStartFlyingWithElytraEvent` und `PlayerStopFlyingWithElytraEvent`, die `elytra` schon nutzt, nicht aus der Elytra-Column.
- **Sprungtest (`jumprun`):** Ein laufender Sprungtest nimmt den Spieler aus dem Spiel (D4). Ist er Fänger, verliert er die Rolle. Das Spiel läuft für die anderen weiter.
- **Serverwechsel (`portal`):** Die Übergabe nimmt den Spieler aus dem Spiel, sobald sie beginnt (D4). Der Verbindungsabbruch ist die Sicherung.
- **AFK:** siehe D5.
- **Schutz (`protection`):** Unverändert. Fangen bricht nur Angriffe zwischen aktiven Spielern ab. Der Schutz für inaktive Spieler bleibt, wie er ist.
- **Hotbar und Lobby-Items:** Fangen fügt kein Item hinzu.
- **Spawn und Respawn:** Rückkehr zum Spawn ändert den Zustand nicht. Die Rolle bleibt, wenn der Fänger zurückgesetzt wird (der Spawn ist nicht der Instanzwechsel).

### D7 Sichtbarkeit ohne Ressourcenpaket

Der Fänger bekommt `setGlowing(true)` mit der Standardfarbe (weiß). Es gibt kein eigenes Scoreboard-Team und keine Team-Farbe. Ein Team würde mit den Namenspräfixen von Butterfly kollidieren, weil ein Spieler nur in einem Team sein kann. Alle 10 Ticks spawnen rote Partikel (`ParticlePacket`) um den Fänger, an die Zuschauer der Instanz. Die Actionbar aller aktiven Spieler nennt den Fänger.

### D8 Telemetrie

- Zähler `titan.tag.tags{result}` mit `tagged` und `cooldown`. Keine Spieler-Id, kein Span (Treffer sind häufig).
- Zähler `titan.tag.toggles{state}` mit `on` und `off`.
- Kein Span. Der Befehl bekommt den Span `tag.toggle` nur, wenn er über `/tag` läuft, wie andere Befehle.

### D9 Abklingzeit des Rücktauschs

5 Sekunden, aus der injizierten `Clock`. Gilt nur für das Paar: Fänger B darf A innerhalb von 5 Sekunden nach A's Treffer nicht zurückjagen. Andere Paare sind nicht betroffen. Der Eintrag wird beim Austritt eines der beiden entfernt.

## Test Strategy

- **Unit (rein):** `TagGameTest` für Übergaben, Rücktausch, Austritte, Zufallswahl mit festem Seed, Mindestzahl zwei, Inaktivität. Alles mit `AdjustableClock`.
- **Unit:** `TagOptInTest` für Schalter an und aus, `/tag` dreht den Wert um, Schnittstelle nicht erreichbar bleibt inaktiv.
- **Unit:** `TagMessagesTest` für gleiche Schlüssel in beiden Sprachen.
- **Env (Minestom, `env.tick()`):** Treffer zwischen aktiven Spielern (Schaden und Rückstoß unterdrückt), Treffer mit Fänger (Rollenwechsel), Treffer ohne Fänger, Treffer auf inaktiven Spieler (Schutz unverändert), Sitzen, Tanz, Elytra, `LobbyPlayerBusyEvent`, Disconnect.
- Kein `Thread.sleep`; Zeit nur über `AdjustableClock`.

## Offene Fragen

1. **Settings-Menü:** Die Position des Schalters im Menü legt `lobby-settings` fest.
