# Design

## Context

Grundregeln stehen in `lobby-tracing/design.md` (D1 bis D5, D9, D10): `Telemetry` per Konstruktor, Span nur für seltene Operationen, Zähler oder Span-Event für häufige, Spieler nur als `user.id` (UUID), keine Zähler-Attribute mit hoher Kardinalität. Dieser Change wendet sie auf admin, hotbar und luckperms an.

- Admin-Befehle sind selten. `StopCommand` führt `MinecraftServer.stopCleanly()` auf einem eigenen Thread aus; der Span `admin.command` umschließt nur das Anstoßen (Start des Threads), nicht das Herunterfahren, und endet davor. Das Herunterfahren hat seinen eigenen Span `titan.shutdown` (Fundament). Die Rechteprüfung der Bedingung (`canStop`) ergibt `admin.result=denied` bei fehlendem Recht.
- Die Rechteprüfung (`PermissionService.check`) wird von Portal-Bewegungen und Befehls-Bedingungen aufgerufen und ist hochfrequent: Zähler plus **Span-Event** am aktuellen Span. `permission` ist ein Konfigurationsschlüssel (kleine Menge) und steht nur am Span-Event, nicht am Zähler. Die Brücke (`bridge`, isolierter Classloader) bekommt keine OTel-Abhängigkeit; die Prüfung ist im Plattformmodul umspannt, in dem das Ergebnis ankommt.
- `HotbarLobbyItems` ist ein `@Singleton` ohne eigene Konfiguration: Der Konstruktor bekommt `Telemetry`; der `PlayerUseItemEvent`-Dispatcher ist selten genug (Mausklick mit Item) für einen Span. `hotbar.item` ist der Wert des `titan:item`-Tags (feste Menge).
