# Proposal

## Why

Admin-Befehle (`/stop`, `/end`), die Hotbar-Items und die Rechteprüfung (LuckPerms über die CloudNet-Brücke) laufen unbeobachtet. Wer wann stoppt, welche Items genutzt werden und ob Rechteprüfungen verweigert werden, ist nicht sichtbar.

Teil der Aufteilung aus `lobby-tracing` (Fundament): Dieser Change nutzt `Telemetry`, `FeatureNode.attach(…, telemetry)` und `onTraced` und setzt voraus, dass das Fundament auf `main` ist.

## What Changes

- **admin:** Span `admin.command` je Ausführung: `admin.command` (`stop`/`end`), `admin.sender` (`console`/`player`), `admin.result` (`executed`/`denied`), `user.id` bei Spielern. Zähler `admin.commands{command,result}`. Der Span endet **vor** dem Herunterfahren, der Agent flusht im JVM-Hook.
- **hotbar:** Span `hotbar.equip` (Ausrüsten beim Beitritt/Respawn/Spawn-Rückkehr: `hotbar.items`, `user.id`) und `hotbar.item.use` (Span je Item-Nutzung über `PlayerUseItemEvent`: `hotbar.item` = Item-Schlüssel). Zähler `hotbar.item.uses{item}`. `ItemConflicts` (Konflikt beim Start) wird als Span-Event `hotbar.item_conflict` an `titan.startup` gehängt.
- **luckperms:** Zähler `permission.checks{result}` je `PermissionService.check` (hochfrequent, aus Portal-Pfad und Befehls-Bedingungen) und ein **Span-Event** `permission.check` (`permission`, `result`) am gerade aktiven Span, falls einer läuft (No-op sonst). Span `permission.platform.start` beim Start (`permission.platform`, `luckperms.extension.loaded`).
- `FeatureNode.attach(…, telemetry)` in admin und hotbar (soweit sie einen Knoten haben).

Rollout: später, nach Fundament und `lobby-tracing-jumprun`.

## Capabilities

### New Capabilities

### Modified Capabilities
- `lobby-tracing`: neue Anforderungen für admin, hotbar und die Rechteprüfung.

## Impact

- **Code:** `features/admin`, `features/hotbar`, `platform/luckperms`, jeweils `package-info.java` mit `requires Telemetry`.
- **Abhängigkeiten:** keine neuen (`opentelemetry-api` kommt aus `core`).
- **Spielerverhalten, Nutzertexte, Logs:** unverändert. Ohne Agent sind alle Spans und Zähler No-ops.

## Delivery

Pull-Request-Titel und Squash-Commit: `feat(telemetry): trace admin commands, hotbar items and permission checks`
