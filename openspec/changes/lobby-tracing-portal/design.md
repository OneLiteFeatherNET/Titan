# Design

## Context

Grundregeln stehen in `lobby-tracing/design.md` (D1 bis D5, D9, D10): `Telemetry` per Konstruktor, Span nur für seltene Operationen, Zähler oder Span-Event für häufige, Spieler nur als `user.id` (UUID), keine Zähler-Attribute mit hoher Kardinalität. Dieser Change wendet sie auf portal, `Deliver` und die Spielerzahl-Abfrage an.

- `PlayerMoveEvent` löst die Portalprüfung aus und bleibt **ohne Span**. Der Span `portal.transfer` entsteht erst in `PortalModule.deliver`, also nur wenn ein Portal tatsächlich auslöst (Abkühlzeit 3 s je Spieler). Verweigerte Rechte und Abkühlung zählen nur (`portal.denied`).
- `Deliver` ist ein Interface aus `core`, die Implementierung (`MessageChannelDeliver`/`DebugDeliver`) kommt aus `PlatformBeans`. Ein `TracedDeliver`-Dekorator (Konstruktor mit `Telemetry` und dem echten `Deliver`) umspannt `sendPlayer`; so deckt ein Dekorator Portal **und** Navigator ab, ohne die Statics `TitanServerConnector`/`TitanPlayerCountLookup` anzufassen.
- **Entschieden:** Der Transfer wird über den `TracedDeliver`-Dekorator in `runtime` getraced, die Statics bleiben unberührt. `bridge` ist eine isolierte Extension und bekommt keine OTel-Abhängigkeit und keine eigenen Spans (Classloader-Isolation, Gefahr von Klassenkonflikten mit dem Agent). Ein Fehler in der Brücke zeigt sich als `titan.deliver.result=error` bzw. als fehlgeschlagene Abfrage am Span `portal.labels.refresh`, mit der Ausnahme als `recordException`.
- `portal.labels.refresh` ist ein Span je Zyklus (die Periode ist in Sekunden), nicht je Portal.
- `portal.result`: `delivered`, `error`. `portal.id` und `portal.task` sind Konfigurationswerte mit kleiner Menge, auch als Zähler-Attribut zulässig.
