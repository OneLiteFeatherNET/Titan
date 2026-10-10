# Design

## Context

Motivation steht in `proposal.md` (Why). Ausgangslage auf `main` (`ffd6f66`):

- **Spawn-Position:** `LobbySpawn` in `core` liefert sie, bei jeder Abfrage frisch und `null`, wenn die Karte keinen Spawn-Punkt hat. Die Spawn-Column teleportiert beim Beitreten (`SpawnJoinListener`) und beim Verlassen der Höhengrenzen (`SpawnBoundsListener`).
- **Befehle:** Für Spieler gibt es keine. `AdminCommands` registriert `stop`/`end` in `@PostConstruct` beim `CommandManager`-Bean, entfernt sie in `@PreDestroy` und deklariert `requires = CommandManager.class`.
- **Navigator:** `NavigatorModule` baut die Aves-Inventare aus dem Enum `Destination` (Platz, Symbol, Task) und leitet per `Deliver.sendPlayer` weiter. Die Plätze 1, 2, 3 und 6 sind frei.
- **Jump and Run:** `JumprunModule` beendet Läufe über `end(run, EndReason)`. `ABORT` wertet den Score, räumt Blöcke ab und gibt die Ausstattung zurück.
- **Events:** Der Titan-EventNode hängt am `GlobalEventHandler`. Ein mit `EventDispatcher.call` ausgelöstes Event erreicht also jeden `FeatureNode`. Eigene Events gibt es in `core` noch keine.
- **Avaje-Zyklus:** Er ist schon zweimal aufgetreten (jumprun und Höhengrenzen): spawn braucht die Hotbar, die Hotbar braucht die Items von jumprun und navigator. Ein Modul, das ein Item beisteuert, kann deshalb keine Bean der Spawn-Column per `requires` verlangen.

## Goals / Non-Goals

**Goals:**
- Ein einziger Weg zurück zum Spawn, den Befehl und Navigator gemeinsam nutzen.
- Features räumen ihren Zustand vor dem Teleport auf, ohne dass die Spawn-Column sie kennt.

**Non-Goals:**
- Automatisches Erkennen von „steckt fest“.
- Abklingzeit, Countdown oder Rechte für `/spawn`.
- Aliase `/lobby`/`/hub`, weil der Proxy sie belegt.
- Rückkehr zu anderen Punkten als dem Spawn, etwa zum letzten Startpunkt.

## Decisions

### D1 `SpawnReturn` und `LobbyReturnToSpawnEvent` in `core`

```
 core:  interface SpawnReturn { Result sendToSpawn(Player player); }
        enum Result { RETURNED, NO_SPAWN }
        record LobbyReturnToSpawnEvent(Player player) implements PlayerEvent

 spawn: final class LobbySpawnReturn implements SpawnReturn   (@Singleton, provides SpawnReturn)
          sendToSpawn(p):
            pos = lobbySpawn.position()
            if pos == null            -> NO_SPAWN       (kein Event, kein Teleport)
            EventDispatcher.call(new LobbyReturnToSpawnEvent(p))   // Features räumen auf
            p.setFlyingWithElytra(false); p.setVelocity(Vec.ZERO)    // sonst behält der Client seinen Schwung
            p.teleport(pos).thenRun(() -> p.setVelocity(Vec.ZERO))  // Schwung auch nach dem Teleport tilgen
            -> RETURNED
```

`sendToSpawn` sendet selbst keine Nachricht. Den Text liefern die Aufrufer: Befehl und Navigator schicken bei `RETURNED` die Bestätigung, bei `NO_SPAWN` die Meldung. Dafür nutzen sie eine gemeinsame Text-Klasse der Spawn-Column (D4), die die Spawn-Column als Teil von `SpawnReturn` bereitstellt: `SpawnReturn.sendToSpawnAndTell(player)` ruft `sendToSpawn` auf und sendet den passenden Text. So hängt der Navigator nicht an Sprachdateien eines anderen Moduls. Das Event wird synchron im Thread des Aufrufers ausgelöst, Befehl und Klick laufen beide im Spieler-Kontext. Damit sind alle Listener fertig, bevor teleportiert wird.

- **Built-in geprüft:** Minestom hat kein Teleport-Event, an das sich jumprun hängen könnte. `PlayerMoveEvent` feuert bei einem Server-Teleport nicht verlässlich. Ein eigenes `PlayerEvent` über `EventDispatcher` ist Minestoms vorgesehener Weg für eigene Ereignisse und braucht keine Registry.
- **Alternative:** jumprun als Abhängigkeit der Spawn-Column, die dann `jumprun.end(...)` aufruft. Verworfen, weil es die Abhängigkeitsrichtung umdreht (Plattform-nahes Modul kennt Feature) und für jedes weitere Feature Änderungen an der Spawn-Column bräuchte (OCP).
- **Test:**
  - Unit (spawn): Mit Spawn-Position feuert `sendToSpawn` genau einmal das Event, und das vor dem Teleport (Reihenfolge über einen Listener, der die Position beim Event festhält); Rückgabe `RETURNED`.
  - Unit (spawn): Ohne Spawn-Position gibt es kein Event, keinen Teleport und `NO_SPAWN`.
  - Integration (Cyano-`Env`): Ein gleitender Spieler gleitet nach `sendToSpawn` nicht mehr.
- **SOLID:** DIP (Aufrufer kennen nur `core`), OCP (neue Features hören auf das Event), SRP (Spawn-Column besitzt das Wohin).

### D2 Befehl `/spawn` in der Spawn-Column

`SpawnCommand extends Command("spawn")` mit einem Default-Executor ohne Argumente. Ist der Absender ein Spieler, ruft er `spawnReturn.sendToSpawnAndTell(player)` auf. Ist es die Konsole, kommt eine kurze englische Betreiber-Meldung. Registriert wird er wie in `AdminCommands` in `@PostConstruct` und entfernt in `@PreDestroy`; die Spawn-Column deklariert dazu `requires = CommandManager.class`. Es gibt keine Bedingung (`setCondition`), jeder Spieler darf.

- **Built-in geprüft:** Minestom-`Command`/`CommandManager`, wie im Admin-Modul.
- **Test:**
  - Integration (Cyano-`Env`): Ein Spieler auf einem Dach führt `/spawn` aus, steht danach an `LobbySpawn.position()` und hat die Bestätigung erhalten.
  - Integration: `de_DE` bekommt den Text auf Deutsch, `ja_JP` auf Englisch.
  - Integration: Ohne Spawn-Punkt kommt die Meldung, und der Spieler wird nicht versetzt.
  - Unit: Nach `@PreDestroy` ist der Befehl nicht mehr registriert.
- **SOLID:** SRP (Befehl nur als Eingang).

### D3 Navigator-Eintrag „Spawn“ auf Platz 2

`NavigatorModule` bekommt einen festen Eintrag auf Platz 2: `Material.COMPASS`, Name `<!i><aqua>Spawn</aqua>`, sprachneutral wie die übrigen Ziele. Ein Klick schließt den Navigator und ruft `spawnReturn.sendToSpawnAndTell(player)` auf. Der Eintrag ist kein `Destination` mit Task, weil keine Weiterleitung stattfindet. Stattdessen setzt eine eigene kleine Methode `layout.setItem(2, …)` neben den Destinations. Der Navigator bekommt `SpawnReturn` als `Provider<SpawnReturn>` (Avaje-Zyklus, siehe Context) und löst ihn in `@PostConstruct` einmal auf. Fehlt die Bean, scheitert der Start mit klarer Meldung, wie jumprun bei `LobbyHeightBounds`.

- **Alternative:** Das `Destination`-Enum um eine Art „lokale Aktion“ zu erweitern, wurde verworfen. Das Enum beschreibt Weiterleitungen, und ein Sonderfall darin würde jede Stelle verzweigen, die Destinations verarbeitet.
- **Test:**
  - Unit/Integration (bestehende Navigator-Tests erweitern): Platz 2 zeigt einen Kompass „Spawn“ in beiden Menüs (mit und ohne Build-Recht).
  - Ein Klick schließt den Navigator, stößt keine `Deliver`-Weiterleitung an und ruft `sendToSpawnAndTell` genau einmal auf (Fake-`SpawnReturn`).
  - Das Szenario „Klick auf leeren Platz“ prüft jetzt Platz 1.
  - Der Navigator-Konfigurationstest nutzt jetzt Platz 3.
  - Ein fehlender `SpawnReturn` lässt den Start scheitern.
- **SOLID:** SRP, OCP (Destinations unverändert).

### D4 Texte der Spawn-Column

Die Spawn-Column bekommt eigene Sprachdateien nach dem Muster von jumprun: `titan/spawn/messages_en.properties` und `messages_de.properties` mit `titan.spawn.return.done` und `titan.spawn.return.no_spawn`. Sie registriert in `@PostConstruct` einen `TranslationStore` beim `GlobalTranslator`, entfernt ihn in `@PreDestroy` und rendert explizit mit `GlobalTranslator.render(Component.translatable(key), player.getLocale())`. Das ist nötig, weil `translatable` ohne Flag in Minestom 26.1 leer bleibt.

- **Built-in geprüft:** Adventure `TranslationStore`/`GlobalTranslator`, wie in jumprun.
- **Test:** Unit: Beide Bundles haben dieselben Schlüssel, und jeder verwendete Schlüssel steht im englischen Bundle. Der `de`/`ja`-Fall ist durch D2 abgedeckt.

### D5 jumprun beendet den Lauf bei Rückkehr zum Spawn

`JumprunModule` hört am eigenen `FeatureNode` auf `LobbyReturnToSpawnEvent`. Läuft für den Spieler ein Lauf, ruft es `end(run, EndReason.SPAWN_RETURN)` auf. Der neue Grund hat dieselben Pflichten wie `ABORT` (`Owed.everything()`: Blöcke zurück, Score werten, Meldung, Ausstattung). Er setzt den Spieler aber **nicht** an den Startpunkt zurück, weil der Teleport zum Spawn ohnehin folgt. Ein eigener Grund statt `ABORT` hält Logs und Endmeldung eindeutig. Falls `ABORT` bereits einen Startpunkt-Teleport enthält, ist der neue Grund genau dafür nötig; das prüft die Umsetzung an `EndReason`.

- **Test:** Integration (Cyano-`Env`, `env.tick()`): Ein Läufer mit Score 12 löst `sendToSpawn` aus. Erwartet wird: Laufende mit Score 12 und Endmeldung, keine Laufblöcke mehr, Elytra und Hotbar zurück, Spieler am Spawn statt am Startpunkt, und die Sidebar ist weg. Ein Spieler ohne Lauf ist vom Event nicht betroffen.
- **SOLID:** OCP (jumprun dockt an das Event an).

### D5b Elytra räumt die Rakete auf

Das Elytra-Modul gibt beim Start des Gleitens eine Rakete in die Nebenhand und entfernt sie nur bei `PlayerStopFlyingWithElytraEvent`. Endet das Gleiten serverseitig (`setFlyingWithElytra(false)`), feuert dieses Event nicht, und die Rakete bliebe in der Hand (lokaler Test). `ElytraModule` hört deshalb zusätzlich auf `LobbyReturnToSpawnEvent`, leert die Nebenhand und vergisst den Boost-Zähler, genau wie beim normalen Ende des Gleitens.

- **Alternative:** `LobbySpawnReturn` feuert selbst `PlayerStopFlyingWithElytraEvent`. Verworfen, weil die Spawn-Column damit ein Client-Event vortäuschen würde und jedes Feature mit Gleit-Zustand davon abhängen müsste.
- **Test:** Env: Ein gleitender Spieler mit Rakete in der Nebenhand löst das Event aus und hat danach eine leere Nebenhand, und sein Boost-Zähler ist zurückgesetzt. Ohne Gleiten bleibt alles unverändert.

### D6 Abnahme über alle Columns

Ein Integrationstest in `apps/cloudnet` verdrahtet alle Columns über Avaje und prüft drei Dinge:
1. Die Verdrahtung gelingt, `SpawnReturn` wird im Navigator aufgelöst und `/spawn` ist registriert.
2. `/spawn` während eines Jump-and-Run-Laufs beendet den Lauf und versetzt den Spieler an den Spawn.
3. Der Navigator-Klick auf Platz 2 tut dasselbe.

### Log

INFO gibt es keins. Eine Rückkehr wird pro Spieler auf DEBUG geloggt (`player`-UUID als Key-Value); das ist eine Spieleraktion. Den Auslöser (Befehl oder Navigator) kennt `sendToSpawn` nicht, und für die Diagnose reicht der Spieler. Ein fehlgeschlagener Teleport (die `CompletableFuture` von `teleport`) wird als WARN geloggt; `RETURNED` bedeutet „Teleport angestoßen“. `NO_SPAWN` geht ebenfalls auf DEBUG, weil eine Karte ohne Spawn-Punkt schon beim Start auffällt. Metriken und Spans gibt es keine (Titan exportiert noch nichts Eigenes).

## Risks / Trade-offs

- [Spieler nutzen `/spawn`, um einem Lauf mit gutem Score zu entkommen] → Der Score wird gewertet wie bei einem Abbruch, es gibt also keinen Vorteil.
- [Eine Karte ohne Spawn-Punkt] → Meldung statt Teleport; der Fehler liegt in den Kartendaten und wird beim Start schon geloggt.
- [Weitere Features mit Spielerzustand (künftige Minispiele) vergessen den Listener] → Das Event ist der dokumentierte Andockpunkt. `docs/lobby-modules.md` beschreibt es in der Liste der Andockpunkte.
- [`/spawn` kollidiert mit einem Proxy-Befehl gleichen Namens] → Velocity und CloudNet registrieren standardmäßig `/hub` und `/lobby`, kein `/spawn`. Im Smoke-Test über den Proxy prüfen.
