# Design

## Context

Eine Column ist ein `@Singleton` unter `features/<name>`, das nur von `core` abhängt; `start()` hängt seinen `FeatureNode` an den `titan`-Knoten, `stop()` hängt ihn ab. Die Plattform liefert `PermissionService`, `Scheduler`, `Clock`, `CommandManager` und `Telemetry` als Beans (`runtime`s `PlatformBeans`).

Der `BeanScope` wird im Konstruktor von `Titan` gebaut, und das passiert vor `bootstrap.start()`. Spieler können sich erst danach verbinden (`Titan#finishStartup`). Eine Registrierung in `@PostConstruct` liegt also vor dem ersten Login, so wie die Registrierung der Saison-Dimension in `PlatformBeans` vor der Instanz liegt.

Minestom `2026.10.07-26.2` bringt die Dialog-API mit: `MinecraftServer.getDialogRegistry()`, `Dialog.Notice`, `Dialog.MultiAction`, `DialogAction.Custom(Key, BinaryTag)`, `Player#showDialog(DialogLike)` und `PlayerCustomClickEvent`. Die Kamera ist `Player#spectate(Entity)` (sendet `CameraPacket(entityId)`) und `Player#stopSpectating()`.

## Goals / Non-Goals

**Goals**

- Schnellaktion und Befehl führen zur selben, pro Spieler gefilterten Auswahl.
- Vier Tänze als reine Daten; die Schritte sind ohne Server testbar.
- Jeder Stopp-Pfad stellt Pose, Kamera und Aufgabe wieder her, auch bei einem Fehler.

**Non-Goals**

- Konfiguration der Tänze, Kamerafahrt, eigene Animationen, übersetzte Schnellaktion (siehe Proposal).

## Decisions

### D1 Registrierung im `@PostConstruct` der Column

Die Column registriert Dialog und Tag selbst, beim Aufbau des `BeanScope`. Das erfüllt die Anforderung „vor dem Serverstart“: Der Aufbau liegt vor `bootstrap.start()`. Eine Registrierung in `common` oder `PlatformBeans` wäre nur nötig, wenn eine andere Komponente den Eintrag bräuchte; das braucht keine. Die Column bleibt dadurch bei `core` als einziger Abhängigkeit.

### D2 Tag pro Spieler, ohne Pica

Minestom `2026.10.07-26.2` hat keine öffentliche Methode, um Einträge in einen Tag zu schreiben, und Pica 0.1.5 (`DialogTagRegistry`) schickt denselben Tag an jeden Spieler. Die Listen brauchen aber je Sprache und Berechtigung einen eigenen Eintrag. Deshalb registriert `DanceMenu` die Listen beim Start einmal in `MinecraftServer.getDialogRegistry()` (`DanceList`: je Sprache, Deutsch sonst Englisch, und je Kombination der berechtigungsgebundenen Tänze). Jeder Spieler bekommt per `QuickActionTags` ein eigenes `TagsPacket` für `minecraft:dialog` mit dem Tag `minecraft:quick_actions`, das nur die Id seiner Liste enthält. Das Paket baut Minestom öffentlich, ohne Reflexion. Versand: beim ersten Spawn, bei `PlayerSettingsChangeEvent` (Sprachwechsel) und bei jedem `/dance`. Die Abhängigkeit auf Pica entfällt.

Die Anzahl der Listen bleibt klein: Sprachen mal 2^k, k ist die Zahl der berechtigungsgebundenen Tänze (heute einer, `spin`), also vier Listen.

Die Tests `QuickActionTagsIntegrationTest` prüfen die Id je Sprache und Berechtigung, den Versand beim Sprachwechsel und den Nicht-Erststart; `DanceListTest` prüft Schlüssel und Inhalt der Listen.

### D3 Die Schnellaktion ist die Tanzliste

Die Schnellaktion zeigt direkt die Liste des Spielers (`Dialog.MultiAction`, siehe D4). Ein Zwischenschritt mit einem „Öffnen“-Knopf ist nicht nötig; die Liste ist bereits übersetzt, weil jede Sprache eine eigene registrierte Liste hat.

### D4 Listen mit Knöpfen pro Tanz

`DanceMenu` baut für eine Sprache und eine Menge erlaubter Tänze einen `Dialog.MultiAction`: Titel und Knopftexte über `GlobalTranslator.render` mit der Sprache der Liste, ein Knopf je erlaubtem Tanz, dazu der Stopp-Knopf. `DialogAction.Custom(titan:dance, {id: "<id>"})` für Tänze, `DialogAction.Custom(titan:dances/stop, null)` für Stopp. Die Berechtigung wird beim Bauen des Tag-Eintrags und beim Starten geprüft; die Liste ist nur eine Anzeige. Ein `/dance` zeigt dieselbe Liste per `showDialog`.

### D5 Tanz als Folge von Schritten

Ein `Dance` ist ein Enum mit Id, optionaler Berechtigung, Textschlüssel und `DanceSequence`. Eine `DanceSequence` hat eine Länge in Ticks und eine Liste von `DanceStep(tick, action)`. Ein `DanceAction` ist eines von: `SWING_MAIN_HAND`, `SWING_OFF_HAND`, `HEAD_DOWN`, `HEAD_UP`, `SNEAK`, `STAND`, `SPIN`, `SPIN_LAND`, `JUMP`, `NOTE`, `HEART`. Die Sequenz liefert `actionsAt(tick)` als reine Funktion; Wiederholung geschieht über Helfer (`every(period, from, to, action)`, `alternating(period, from, to, a, b)`), damit die Tanzdefinition lesbar bleibt.

Die Tanzdefinitionen (Länge in Ticks):

- `wave` (60): Hauptarm alle 20 Ticks, Kopf wechselt alle 10 Ticks zwischen `HEAD_DOWN` und `HEAD_UP`.
- `groove` (80): `SNEAK` und `STAND` wechseln alle 10 Ticks, Arme alternierend alle 10 Ticks, `NOTE` alle 5 Ticks.
- `party` (80): `JUMP` alle 20 Ticks, `NOTE` alle 10 Ticks, `HEART` alle 20 Ticks versetzt.
- `spin` (60): `SPIN` bei Tick 0 (der NPC hebt sich 1,25 Blöcke und schwebt), `HEART` alle 10 Ticks, `SPIN_LAND` bei Tick 40 (Schwebe endet, Schwerkraft, Drehung endet).

### D6 Zustand pur, Effekte an Minestom gebunden

`DanceSession` (ohne Minestom) zählt die Ticks, liefert pro Tick die Aktionen und meldet das Ende. Die Grenzen zwischen Verantwortungen:

- `DanceEffects` führt die Aktionen nur am NPC aus (Pose, Schwung, Blickrichtung, Sprung über `setVelocity`, Schweben über `setNoGravity` und `teleport`, Partikel über `ParticlePacket`, Ton über `Instance#playSoundExcept(null, ...)`). Der echte Spieler bekommt keine Geschwindigkeit und keine Pose.
  Die Pose setzt die Column am NPC über das Ducken (`setSneaking`) bzw. `setPose` und die Riptide-Drehung über `LivingEntityMeta#setInRiptideSpinAttack`. Der NPC hat keinen Spieler-Tick, der die Pose aus den Flags neu berechnet; `Entity#updatePose` läuft nur bei `setSneaking` und ähnlichen Aufrufen.
- `DanceNpc` besitzt den NPC: `LivingEntity` vom Typ `MANNEQUIN` mit `MannequinMeta` (Profil aus dem Skin des Spielers, sonst Name und Id; `setImmovable(true)`), Namensschild über `DataComponents.CUSTOM_NAME` und `setCustomNameVisible(true)`, auto-sichtbar für alle Spieler der Instanz (`setInstance(...).join()`). Der NPC behält die Schwerkraft, damit der Sprung zurück auf den Boden fällt. Der echte Spieler wird per `updateViewableRule(viewer -> false)` für alle anderen unsichtbar; das gilt auch für Spieler, die während des Tanzes dazukommen, weil Minestom die Regel beim Hinzufügen eines Betrachters prüft. `close()` setzt die Regel mit `updateViewableRule(null)` zurück.
- `DanceCamera` besitzt die Kameraentität: erzeugt sie (`EntityTypes.ITEM_DISPLAY` ohne Item, `setAutoViewable(false)`, `setInstance(...).join()`, `addViewer(player)`), setzt die Kamera per `Player#spectate`, und `close()` ruft `stopSpectating()` und danach `remove()`. Ein Marker ist für Vanilla-Server kein Kameraziel, das der Client sicher annimmt; ein Display ohne Item hat keine Größe und rendert nichts.
- `CameraPlacement` berechnet den Standort: Augenhöhe des Tanzenden plus 3,5 Blöcke in Blickrichtung, Blick per `Pos#withLookAt` auf den Tanzenden.
- `DanceService` hält die Sitzungen je Spieler (`Map<UUID, ActiveDance>`), prüft Berechtigung, Abklingzeit und Einzelstart, startet den Tick-Task über `Scheduler#submitTask` und zählt den Start.

### D7 Stopp-Pfade und Reihenfolge

Jeder Stopp läuft über eine Methode `DanceService#stop(player, reason)`. Sie entfernt die Sitzung zuerst aus der Map (dann ist ein doppelter Aufruf ein No-op), bricht den Task ab, und stellt in verschachtelten `try`/`finally`-Blöcken zuerst Pose und Blick des Spielers wieder her, dann schließt sie die Kamera. So bleibt die Kamera auch dann entfernt, wenn ein Schritt vorher wirft.

Auslöser: Dauer erreicht (`FINISHED`), Bewegung (`MOVED`), Bewegungseingabe (`INPUT`), Angriff oder Interaktion (`INTERACTED`), Instanz verlassen (`LEFT_INSTANCE`), Verbindungsabbruch (`DISCONNECTED`), Stopp-Knopf (`STOP_BUTTON`), Fehler (`FAILED`, im Tick abgefangen und geloggt), Herunterfahren (`SHUTDOWN`, `@PreDestroy`).

Die Bewegungsschwelle ist ein horizontaler Abstand von 0,25 Blöcken zur Startposition; vertikale Bewegung (Sprung, Fall) zählt nicht. `PlayerMoveEvent` prüft sie; Drehen des Kopfes ohne Positionsänderung stoppt nicht. Bewegungseingaben (`ClientInputPacket`: vorwärts, rückwärts, links, rechts, springen) stoppen direkt; Ducken nicht, weil die Pose des Tanzes selbst Ducken setzt.

Angriff und Interaktion hören mit `onIncludingCancelled`, weil Schutzmodule Klicks abbrechen können und der Tanz trotzdem enden soll.

### D8 Abklingzeit mit injizierter Uhr

`DanceCooldown` hält je Spieler den Zeitpunkt des letzten erfolgreichen Starts, gelesen über die `Clock`-Bean. Drei Sekunden sind eine Konstante. Der Eintrag wird bei `PlayerDisconnectEvent` entfernt (Regel „Spielerbezogener Zustand“ in `docs/lobby-modules.md`).

### D9 Priorität und Befehl

`EVENT_PRIORITY = 650`: zwischen Tickle (600) und Elytra (700) frei. Der Befehl `DanceCommand` (Name `dance`, ohne Alias; Befehlsnamen sind immer englisch) wird von `DanceCommands` (`@Singleton`, wie `AdminCommands`) über `CommandManager` registriert und in `@PreDestroy` abgemeldet.

### D10 Telemetrie und Fehlerbehandlung

`DanceTelemetry` besitzt den Zähler `titan.dance.started` (Einheit `{dance}`, Attribut `dance`). Keine Spans: Starts sind selten, Bewegungsereignisse wären zu häufig. Abgelehnte Starts und unbekannte Payloads gehen in Debug-Logs ohne Spielerdaten außer der UUID.

### D11 Keine Konfiguration

Die Tänze stehen im Code; eine Konfigurationsdatei und ein `features.*`-Flag sind nicht nötig, weil die Column nur aus Klasse und Dialog besteht. Ein späteres Flag wäre ein eigener Change.

### D12 Texte

`DanceMessages` (wie `NavigatorMessages`) lädt `titan/dance/messages_de.properties` und `messages_en.properties` in einen `MiniMessageTranslationStore`, der beim Start in den `GlobalTranslator` gelegt und beim Stopp entfernt wird. Ein Bundle-Test prüft, dass beide Dateien dieselben Schlüssel haben.

## Risks / Trade-offs

- **Keine Abhängigkeit auf Pica mehr**: Die Tag-Einträge baut die Column selbst (D2).
- **Kamera-Blick fest**: Die Kamera steht an der Startposition des NPCs und schaut auf dessen Augen; Kopfnicken dreht den Kopf des NPCs, nicht die Kamera. Der Tanz wird also von vorne gesehen.
- **Kein Resource Pack**: NPC (Mannequin mit Skin), Kameraentität (leeres Display), Partikel und Töne sind alles Vanilla-Inhalte; der Tanz braucht keinen Client-Mod und kein Pack.
- **`setInstance(...).join()`**: Das Blockieren auf dem Tick-Thread ist nur harmlos, wenn die Chunks der Lobby geladen sind; die Lobby lädt sie beim Start.
- **Notenblock-Töne für alle**: Töne gehen an alle Spieler in der Instanz, nicht nur an den Tanzenden. Das ist beabsichtigt, weil die Lobby gemeinsam genutzt wird.
- **Kein echter Client getestet**: Die Sichtprüfung (Schnellaktion im Client, Kamerabild, Pose) ist Teil der Aufgaben und im PR als offen vermerkt, falls sie nicht stattfindet.
