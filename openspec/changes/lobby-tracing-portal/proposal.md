# Proposal

## Why

Wenn ein Spieler ein Portal betritt und nicht ankommt, bleibt unsichtbar, wo es scheiterte: Rechteprüfung, Zielauswahl, CloudNet oder die Brücke. Auch die Spielerzahl-Labels (Abfrage über die CloudNet-Brücke) liefern bei Fehlern nur Logzeilen.

Teil der Aufteilung aus `lobby-tracing` (Fundament): Dieser Change nutzt `Telemetry`, `FeatureNode.attach(…, telemetry)` und `onTraced` und setzt voraus, dass das Fundament auf `main` ist.

## What Changes

- `portal.transfer` (Span je Portal-Betreten, das ausgeliefert wird): `portal.id`, `portal.task`, `user.id`, `portal.result`.
- `deliver.send_player` (Span, Kind davon und auch für den Navigator): `Deliver` wird in `runtime` von einem `TracedDeliver` umhüllt. Attribute `titan.deliver.target_type` (`task`/`server`), `titan.deliver.target`, `titan.deliver.result` (`ok`/`error`).
- `portal.labels.refresh` (Span je Label-Zyklus, nicht je Portal): Anzahl Labels, Anzahl fehlgeschlagener Abfragen. Damit ist die Abfrage über die Brücke umspannt.
- Zähler: `titan.portal.transfers{result}`, `titan.portal.denied{portal.id}` (Rechte verweigert, aus dem Move-Pfad, daher nur Zähler), `titan.portal.player_count.lookups{result}`.
- `FeatureNode.attach(…, telemetry)`; das Rechte-Span-Event kommt aus `lobby-tracing-admin-permissions`.

## Capabilities

### New Capabilities

### Modified Capabilities
- `lobby-tracing`: neue Anforderungen für portal und den CloudNet-Transfer.

## Impact

- **Code:** `features/portal` (`PortalModule`, `PortalTrigger` nur für den Zähler, `LabelRefresh`), `runtime` (`PlatformBeans`: `Deliver` umhüllen), `common` oder `runtime` für `TracedDeliver` (siehe Design).
- **Nicht:** `bridge` (keine OTel-Abhängigkeit, keine eigenen Spans) und die Statics `TitanServerConnector`/`TitanPlayerCountLookup` (unverändert), siehe Design.
- **Rollout:** nach Fundament und `lobby-tracing-jumprun`.
- **Abhängigkeiten:** keine neuen (`opentelemetry-api` kommt aus `core`).
- **Spielerverhalten, Nutzertexte, Logs:** unverändert. Ohne Agent sind alle Spans und Zähler No-ops.

## Delivery

Pull-Request-Titel und Squash-Commit: `feat(telemetry): trace portal transfers and player count lookups`
