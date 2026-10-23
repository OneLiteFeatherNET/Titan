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

### D2 Tag-Eintrag über eine reflektierte Methode

Minestom `2026.10.07-26.2` hat keine öffentliche Methode, um Einträge in einen Tag zu schreiben. `Registry#getOrCreateTag` liefert ein `RegistryTagImpl.Backed`, dessen einzige Schreibmethode `add(RegistryKey)` paketprivat und `@ApiStatus.Internal` ist (per `javap` im Jar geprüft; die Tags des Dialog-Registrys kommen sonst nur aus Minestoms eigenen Ressourcen). Die Column ruft diese eine Methode per Reflexion auf: `Class.forName("net.minestom.server.registry.RegistryTagImpl$Backed")`, `getDeclaredMethod("add", RegistryKey.class)`, `setAccessible(true)`. Auf dem Klassenpfad (Shadow-Jar) liegt Minestom im unbenannten Modul, dort ist das ohne `--add-opens` möglich.

Schlägt die Reflexion fehl, protokolliert die Column eine WARN-Zeile und läuft weiter: Der Befehl `/tanz` funktioniert dann trotzdem. Ein Test sichert den Weg für die gepinnte Minestom-Version ab, damit ein Versionssprung auffällt. Die Reflexion wird entfernt, sobald Minestom einen öffentlichen Weg bietet.

### D3 Schnellaktion als `Dialog.Notice`

`Dialog.Notice` hat genau einen Knopf (`DialogActionButton`), also passt er zur Vorgabe „ein Knopf“. Titel und Knopf sind feste Komponenten ohne Übersetzungsschlüssel, weil ein unbekannter Schlüssel beim Client als Rohtext erscheinen würde.

### D4 Pro Spieler: `Dialog.MultiAction` nach dem Klick

Auf den Klick hin baut `DanceMenu` einen `Dialog.MultiAction` für genau diesen Spieler: Titel und Knopftexte über `GlobalTranslator.render` mit `player.getLocale()`, Knöpfe nur für erlaubte Tänze, dazu der Stopp-Knopf. `DialogAction.Custom(titan:dance, {id: "<id>"})` für Tänze, `DialogAction.Custom(titan:dances/stop, null)` für Stopp. Die Berechtigung wird beim Bauen und beim Starten geprüft; die Auswahl ist nur eine Anzeige.

### D5 Tanz als Folge von Schritten

Ein `Dance` ist ein Enum mit Id, optionaler Berechtigung, Textschlüssel und `DanceSequence`. Eine `DanceSequence` hat eine Länge in Ticks und eine Liste von `DanceStep(tick, action)`. Ein `DanceAction` ist eines von: `SWING_MAIN_HAND`, `SWING_OFF_HAND`, `HEAD_DOWN`, `HEAD_UP`, `SNEAK`, `STAND`, `SPIN`, `JUMP`, `NOTE`, `HEART`. Die Sequenz liefert `actionsAt(tick)` als reine Funktion; Wiederholung geschieht über Helfer (`every(period, from, to, action)`, `alternating(period, from, to, a, b)`), damit die Tanzdefinition lesbar bleibt.

Die Tanzdefinitionen (Länge in Ticks):

- `wave` (60): Hauptarm alle 20 Ticks, Kopf wechselt alle 10 Ticks zwischen `HEAD_DOWN` und `HEAD_UP`.
- `groove` (80): `SNEAK` und `STAND` wechseln alle 10 Ticks, Arme alternierend alle 10 Ticks, `NOTE` alle 5 Ticks.
- `party` (80): `JUMP` alle 20 Ticks, `NOTE` alle 10 Ticks, `HEART` alle 20 Ticks versetzt.
- `spin` (60): `SPIN` bei Tick 0, `HEART` alle 10 Ticks.

### D6 Zustand pur, Effekte an Minestom gebunden

`DanceSession` (ohne Minestom) zählt die Ticks, liefert pro Tick die Aktionen und meldet das Ende. Die Grenzen zwischen Verantwortungen:

- `DanceEffects` führt die Aktionen am Spieler aus (Pose, Schwung, Blickrichtung, Sprung über `setVelocity`, Partikel über `ParticlePacket`, Ton über `Instance#playSoundExcept(null, ...)`).
  Die Pose setzt die Column über die Flags Ducken (`setSneaking`) und Riptide-Drehung (`LivingEntityMeta#setInRiptideSpinAttack`), nicht über `setPose`: Minestom berechnet die Pose in jedem Tick aus diesen Flags neu, ein direktes `setPose` hält nicht.
- `DanceCamera` besitzt die Kameraentität: erzeugt sie (`EntityTypes.MARKER`, `setAutoViewable(false)`, `setInstance(...).join()`, `addViewer(player)`), setzt die Kamera per `Player#spectate`, und `close()` ruft `stopSpectating()` und danach `remove()`.
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

`EVENT_PRIORITY = 650`: zwischen Tickle (600) und Elytra (700) frei. Der Befehl `DanceCommand` (Name `tanz`, Alias `dance`) wird von `DanceCommands` (`@Singleton`, wie `AdminCommands`) über `CommandManager` registriert und in `@PreDestroy` abgemeldet.

### D10 Telemetrie und Fehlerbehandlung

`DanceTelemetry` besitzt den Zähler `titan.dance.started` (Einheit `{dance}`, Attribut `dance`). Keine Spans: Starts sind selten, Bewegungsereignisse wären zu häufig. Abgelehnte Starts und unbekannte Payloads gehen in Debug-Logs ohne Spielerdaten außer der UUID.

### D11 Keine Konfiguration

Die Tänze stehen im Code; eine Konfigurationsdatei und ein `features.*`-Flag sind nicht nötig, weil die Column nur aus Klasse und Dialog besteht. Ein späteres Flag wäre ein eigener Change.

### D12 Texte

`DanceMessages` (wie `NavigatorMessages`) lädt `titan/dance/messages_de.properties` und `messages_en.properties` in einen `MiniMessageTranslationStore`, der beim Start in den `GlobalTranslator` gelegt und beim Stopp entfernt wird. Ein Bundle-Test prüft, dass beide Dateien dieselben Schlüssel haben.

## Risks / Trade-offs

- **Reflexion auf Minestom-Interna**: Ein Versionssprung kann `add` umbenennen. Die WARN-Zeile und der Test fangen das ab; der Befehl bleibt nutzbar.
- **Kamera-Blick fest**: Die Kamera steht an der Startposition und schaut auf den Tanzenden; Kopfnicken dreht den Körper, nicht die Kamera. Die Pose wird also von der Seite gesehen, nicht von vorne.
- **`setInstance(...).join()`**: Das Blockieren auf dem Tick-Thread ist nur harmlos, wenn die Chunks der Lobby geladen sind; die Lobby lädt sie beim Start.
- **Notenblock-Töne für alle**: Töne gehen an alle Spieler in der Instanz, nicht nur an den Tanzenden. Das ist beabsichtigt, weil die Lobby gemeinsam genutzt wird.
- **Kein echter Client getestet**: Die Sichtprüfung (Schnellaktion im Client, Kamerabild, Pose) ist Teil der Aufgaben und im PR als offen vermerkt, falls sie nicht stattfindet.
