# Proposal

## Why

Die Lobby hat bisher keine Stelle, an der ein Spieler Funktionen für sich ein- oder ausschaltet. Die Funktionen `lobby-tag` (Fangen) und `lobby-throw` (Schleudern-Ziel sein) brauchen aber genau das: Fangen ist etwas, dem Spieler bewusst zustimmen, Schleudern-Ziel sein etwas, das er ablehnen kann. Die Entscheidung soll den Spieler über Lobbys, Server und Sitzungen begleiten. Otis, der Profil-Dienst des Netzwerks, hat seit Version 1.17.0 eine versionierte Settings-API pro Spieler (`/v1/players/{uuid}/settings/{key}`). Diese Änderung macht sie in der Lobby nutzbar: ein Einstellungsmenü plus eine generische Registrierung, über die jede Funktion ihre Option einhängt.

Diese Änderung kommt zuerst. `lobby-tag` und `lobby-throw` folgen darauf und registrieren ihre Optionen über die hier gebaute Schnittstelle.

## What Changes

- **Core-Schnittstelle:** Ein neuer Port `PlayerSettings` in `core` mit typisierten Schlüsseln (`BooleanSetting`), Standardwert, Opt-in- oder Opt-out-Semantik und Änderungs-Listenern. Columns (`features/*`) registrieren ihre Optionen über eine `SettingsRegistry`. Doppelte Schlüssel werden beim Start abgelehnt.
- **Column `features/settings`:** Ein Einstellungsmenü mit Aves, geöffnet über ein Hotbar-Item (Slot 7) und den Befehl `/settings`. Pro Option ein Vanilla-Item (Lime-/Grau-Farbstoff, ohne Resource Pack), ein Klick schaltet um. Texte auf Deutsch und Englisch.
- **Adapter `platform/otis`:** Ein Client gegen die Otis-Settings-API mit `java.net.http` (keine neue Bibliothek), Timeouts, Wiederholungen, Circuit Breaker und Fallback auf Standardwerte und zuletzt bekannte Werte. Laden beim Verbinden asynchron, Schreiben verzögert gebündelt (Write-behind). Die Lobby blockiert weder den Tick-Thread noch den Beitritt.
- **Offline-Variante:** Ist `otis.url` leer, wird ein In-Memory-Speicher verdrahtet (z. B. `apps/local`). Eine laufende Lobby ohne Otis-URL meldet das beim Start als Warnung.
- **Registrierte Optionen dieser Änderung:** Keine fachliche Option. Diese Änderung liefert Mechanik und Menü. Die erste Option ist `lobby-tag` (Fangen, Opt-in, Standard aus), danach `lobby-throw` (Schleudern-Ziel sein, Opt-out, Standard an). Beide sind eigene Changes.
- **Befehle:** `/settings` (nur Englisch, ohne Alias). Der Befehl öffnet dasselbe Menü wie das Hotbar-Item.
- **Konfiguration:** Schlüssel `settings.enabled`, `otis.url`, `otis.token` (aus `TITAN_OTIS_TOKEN`), Timeouts, Wiederholungen, Debounce und Circuit-Breaker-Werte. Standardwerte in `titan/defaults/*.yaml`.
- **Telemetrie:** Ladedauer und Ergebnis, Schreibfehler, Umschaltungen je Schlüssel, Zustand des Circuit Breakers.
- **Migration:** Keine. Es gibt heute keine persistierte Spieleroption in Titan; die Jump-and-Run-Bestwerte bleiben eigene Daten und sind keine Einstellungen.

Nicht Teil dieser Änderung:

- Die Option Fangen (`lobby-tag`) und die Option Schleudern-Ziel sein (`lobby-throw`). Sie hängen von dieser Änderung ab und folgen als eigene Changes.
- Jede Änderung in Otis. Dienst-Authentifizierung, Konfliktprüfung über `If-Match` und Prüfung der Produktionsmigration sind Voraussetzungen in Otis, siehe unten.
- Nicht-boolesche Einstellungen. Die Schnittstelle erlaubt später andere Typen, v1 kennt nur Schalter.
- Synchronisation zwischen Lobby und Proxy. Velocity liest die Einstellungen nicht.
- Ein lokaler Cache auf der Festplatte. Der Speicher lebt nur im Prozess.

## Abhängigkeiten

**Reihenfolge:** `lobby-settings` wird zuerst umgesetzt und gemergt. `lobby-tag` und `lobby-throw` bauen darauf auf und registrieren ihre `BooleanSetting` über die `SettingsRegistry`. Ihre Tasks beginnen erst nach dem Merge dieser Änderung.

**Otis-Voraussetzungen (eigene Changes in `OneLiteFeatherNET/Otis`, nicht Teil dieser Änderung):**

1. **Dienst-Authentifizierung (blockierend für die Produktion):** Otis hat heute keine Authentifizierung, auch nicht für Schreibzugriffe auf `/v1/players/{uuid}/settings`. Vor dem Einschalten in Produktion braucht Otis einen Mechanismus (z. B. Bearer-Token je Dienst). Titan sendet das Token, sobald `otis.token` gesetzt ist. Bis dahin gilt das Netz als einzige Schranke, siehe `design.md`, Sicherheit.
2. **Produktionsmigration prüfen (blockierend):** Die Tabelle `player_setting` kommt aus der Flyway-Migration V2, die mit 1.17.0 eingeführt wurde. Der Stand der Produktion ist nicht verifiziert. Die Konfiguration `hbm2ddl.auto: update` ist im Repository gesetzt; der Prod-Override ist nicht eingesehen.
3. **Otis-Version ausgerollt (blockierend):** Otis mit der Settings-API (ab 1.17.0) muss in Produktion laufen. Die Client-Abhängigkeit `net.onelitefeather.otis:otis-client` muss eine Version mit den Settings-Pfaden liefern. Das Repository ist anonym nicht lesbar; die Version ist zu prüfen.
4. **Optional, nicht blockierend:** `If-Match` mit Versionskonflikt (409). Ohne sie gilt Last-Write-Wins je Schlüssel, was hier genügt, weil ein Spieler immer nur auf einer Lobby ist.

**Lobby-seitig:** Das Hotbar-Item braucht einen freien Slot (Vorschlag: Slot 7). Die Hotbar-Konfliktprüfung meldet einen Konflikt beim Start.

## Capabilities

### New Capabilities

- `lobby-settings`: Schnittstelle für Spieleroptionen, Einstellungsmenü mit Hotbar-Item und Befehl, Otis-Anbindung mit Fallback, Speicherung im Hintergrund, Konfiguration und Telemetrie.

### Modified Capabilities

Keine.

## Delivery

Der Pull-Request-Titel und der Squash-Commit lauten `feat(settings): add a lobby settings menu backed by otis player settings`. Typ `feat`, Scope `settings`. Die Änderung ist nicht breaking. Die OpenSpec-Artefakte stehen in einem eigenen Commit `docs(openspec): propose lobby-settings`. Der Code folgt in Commits je Typ; die Implementierung und ihre Tests sind `feat(settings)`, die Dokumentation in `docs/` ist ein eigener `docs(settings)`-Commit.

## Impact

- **Code:** neues Modul `features/settings` (Column `settingsColumn`, Paket `net.onelitefeather.titan.feature.settings`); neues Modul `platform/otis` (Adapter, Paket `net.onelitefeather.titan.platform.otis`); neue Typen im Paket `net.onelitefeather.titan.core.settings` in `core`. Die Column hängt nur an `core`, wie jede Column.
- **Abhängigkeiten:** keine neuen Bibliotheken. `java.net.http` aus dem JDK; für Tests ein JDK-HTTP-Server auf einem freien Port in `integrationTest`. Otis-Client nur, wenn die Prüfung in den Voraussetzungen ergibt, dass er lauffähig ist; sonst eigene minimale JSON-Abbildung.
- **Betrieb:** Neue Konfigurationsschlüssel mit Standardwert `settings.enabled: false`. Produktion schaltet die Funktion nach den Otis-Voraussetzungen ein. Neues Secret `TITAN_OTIS_TOKEN` (nur Umgebungsvariable, nie geloggt).
- **Nutzertexte:** `titan/settings/messages_de.properties` und `messages_en.properties`.
- **Hotbar:** Slot 7 wird für das Einstellungs-Item belegt; Slot 0 (Jump and Run), 4 (Navigator) und 8 (Lobby-Switcher, nur Profil `cloudnet`) bleiben unverändert.
