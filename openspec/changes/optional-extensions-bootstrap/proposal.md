# Proposal

## Why

Titan startet heute immer über `ExtensionBootstrap` aus minestom-extensions. Der Aufruf initialisiert `MinecraftServer`, lädt den Ordner `extensions/` und startet den Server. Extensions braucht aber nur der Betrieb unter CloudNet, weil die CloudNet-Bridge und die Titan-`:bridge` Minestom-Extensions sind. Lokal kostet der Loader Startzeit und bringt Abhängigkeiten ohne Nutzen mit. Setzt dieser Change auf den Columns und Varianten aus `split-titan-into-columns` auf, wird der Extension-Loader zu einer Plattform, die nur die CloudNet-Variante einbindet.

## What Changes

- **`core`**: Neuer Vertrag `ServerBootstrap` (Server initialisieren, u. a. mit Velocity-Auth, und auf Host/Port starten).
- **`runtime`**: `@Secondary`-Fallback `PlainMinestomBootstrap` über `MinecraftServer.init(auth)` und `start(...)`, ohne Extensions.
- **Startablauf**: `runtime` wählt den `ServerBootstrap` per `ServiceLoader` (keiner → Fallback, mehrere → Abbruch), initialisiert damit den Server, baut danach den `BeanScope` und startet zuletzt. Die heute implizite Reihenfolge „Server da, dann Features“ wird damit explizit. Ein zweiter Avaje-Scope ist verworfen (design.md D2).
- **`platform/extensions`**: Neues Modul mit `ExtensionServerBootstrap` über `ExtensionBootstrap.init(...)`/`start(...)`, lädt `extensions/`.
- **`platform/cloudnet`**: Übernimmt das CloudNet-Wissen aus `common`: die Wahl des `Deliver` samt `.wrapper`-Erkennung und `MessageChannelDeliver`. Es setzt `platform/extensions` voraus. Bind-Host und -Port bleiben in `runtime`, weil beide Varianten sie wie heute lesen. `:bridge` bleibt eine Extension, weil die CloudNet-Bridge nur als Extension geladen wird (design.md D5).
- **Varianten**: `apps/cloudnet` bindet `platform/extensions` und `platform/cloudnet` ein, `apps/local` keins von beiden.
- **BREAKING**: Die lokale Variante lädt keinen `extensions/`-Ordner mehr.

## Capabilities

### New Capabilities

- `server-bootstrap`: Titan startet den Server über die Bootstrap-Plattform seiner Variante. Ohne Extension-Plattform startet er ohne Extension-Loader; mit ihr lädt er den `extensions/`-Ordner vor dem Start. Features starten erst, wenn der Server initialisiert ist. Velocity-Forwarding und Bind-Adresse verhalten sich in beiden Fällen wie heute.

### Modified Capabilities

_Keine._

## Impact

- **Code**: `TitanApplication.main()` und `bootstrap()` ziehen nach `runtime` bzw. `platform/*`. Die Konsolen-Eingabe und der AOT-Trainingsmodus bleiben in `runtime`.
- **Abhängigkeiten**: `minestom-extensions` nur noch in `platform/extensions`. Keine neuen.
- **Tests**: Unit-Tests für die Reihenfolge Server-Init vor Scope vor Start. Ein Starttest je Variante prüft, welcher `ServerBootstrap` aktiv ist.
- **Nutzertexte**: keine.
- **Betrieb (BREAKING)**: Nur `apps/cloudnet` lädt Extensions. Lokale Setups mit Extensions sind nicht mehr vorgesehen.
- **Voraussetzung**: `split-titan-into-columns`. Unabhängig von `permission-spi`.

## Delivery

PR-Titel: `refactor(bootstrap)!: make minestom extensions a platform module`

`BREAKING CHANGE: only the apps/cloudnet variant loads the extensions folder; apps/local starts plain Minestom without the extension loader.`
