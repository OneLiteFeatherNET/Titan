# Design

## Context

Die Saison-Column (`features/season`) wählt die Welt der Lobby über die SPI `LobbyWorldChoice`, die `PlatformBeans` per `ServiceLoader` lädt. Die Lobby-Instanz entsteht in `PlatformBeans.instanceContainer()` mit der Overworld; danach lädt `MapProvider` die Welt. Die Nebel-Atmosphäre der Slender-Lobby ist eine eigene `DimensionType` in Cygnus, die dort vor dem ersten Login registriert wird (`GameMapProvider.registerLobbyDimension`). Titan muss dieselbe Reihenfolge einhalten: Dimension registrieren, dann Instanz anlegen.

Quelle der Werte: Cygnus `common/.../dimension/BlendedAtmosphere.java` mischt `StaticDimensionPreset.BRIGHT` und die Granskoga-Atmosphäre (`game/game/maps/Granskoga/map.json`) mit `share = 0.3f`, wie `GameMapProvider` es für die Lobby tut (`DEFAULT_LOBBY_ATMOSPHERE_SHARE`). Die Ambient-Farbe fehlt im Map-File der Granskoga-Atmosphäre; `MapAtmosphere` leitet sie aus der Nebelfarbe mit dem Faktor `AMBIENT_LIGHT_SHARE = 0.10` ab.

Minestom: `2026.10.07-26.2`. `DimensionType.builder().setAttribute(...)`, `defaultClock(...)`, `MinecraftServer.getDimensionTypeRegistry().register(...)` und `InstanceManager.createInstanceContainer(RegistryKey<DimensionType>)` existieren in dieser Version; die `EnvironmentAttributes` (FOG_*, SKY_*, AMBIENT_LIGHT_COLOR, SUN_ANGLE, MOON_ANGLE) ebenfalls.

Saisonwechsel heute: Die Welt wird beim Start gewählt. Ändert sich die gewünschte Welt, merkt `SeasonModule` einen Neustart vor und stoppt die leere Lobby; der Supervisor startet sie neu. Es gibt keinen Live-Wechsel.

## Goals / Non-Goals

**Goals**

- Pro Saison optional eine Atmosphäre, im Config-Schema `seasons.<id>.atmosphere`.
- Die Nebelwerte der Slender-Lobby, exakt berechnet und im Code dokumentiert.
- Keine Änderung für Lobbys ohne Atmosphäre.

**Non-Goals**

- Live-Dimensionswechsel, Neustart-Auslöser für reine Atmosphärenänderungen, konfigurierbare Werte, Nebel für Minispiele.

## Decisions

### D1 Atmosphäre über die bestehende SPI

`LobbyWorldChoice` bekommt eine Default-Methode `Optional<LobbyAtmosphere> atmosphere()` mit leerem Ergebnis. `SeasonWorldChoice` überschreibt sie. Eine eigene zweite SPI hätte eine zweite `ServiceLoader`-Auswahl mit eigener Eindeutigkeitsregel und zweifache Konfigurationsleserei bedeutet; die Welt und die Atmosphäre kommen aus derselben Saisonentscheidung.

Die Eindeutigkeitsregel (mehr als eine Auswahl bricht ab) wird in `PlatformBeans.onlyChoice(...)` herausgezogen. `loadMapProvider(base, instance, choices)` bleibt in seiner Signatur, weil `LobbyWorldResolutionTest`, `LobbyPortalsBeanTest` und `SeasonLobbyWorldTest` sie nutzen.

`instanceContainer()` und `mapProvider()` lesen die Auswahl je einmal beim Start. Das folgt dem Muster, das `CalendarStartedWorld` schon hat.

### D2 Enum `LobbyAtmosphere` in `core`

`core` kennt die Atmosphären als Enum mit Konfigurationsschlüssel: `SLENDER_FOG("slender-fog")`, `fromKey(String)` liefert `Optional`. Die Column braucht nur den Enum-Typ und bleibt bei `core` als einziger Abhängigkeit (`ColumnArchitectureRules`). Die Zahlenwerte liegen nicht in `core`, sondern bei der Dimension (D4).

### D3 Lesen der Atmosphäre nur für die aktive Saison, nur beim Start

`SeasonAtmospheres` liest `seasons.<id>.atmosphere` für die Saison, die `atStartup()` liefert. Es ist kein Feld von `Season`, damit die Live-Pfade (`SeasonConfigReader.readLive`, `RestartPolicy`, `SeasonModule`) unverändert bleiben und keine Warnung im Minutentakt entsteht. Ein unbekannter Wert wird genau einmal beim Start geloggt (`WARN`), der Start läuft ohne Atmosphäre weiter.

Die Atmosphäre fließt nicht in `RestartPolicy`, der Vergleich bleibt bei der Welt. Eine Atmosphärenänderung bei gleicher Welt wirkt daher beim nächsten Start (Spec, Anforderung „wirkt beim Start, nicht live“). Der Saisonbeginn wechselt die Welt und startet die Lobby ohnehin neu.

### D4 Dimension in `common`, Werte als Konstanten

`common/.../dimension/LobbyDimensions` registriert die Dimension und hält die Werte. Es gibt keine Konfiguration dafür; die Werte stehen als `static final` mit einem Kommentar, der die Quelle nennt (Cygnus `BlendedAtmosphere`, `share 0.3`). `LobbyDimensions.register(Optional<LobbyAtmosphere>)` liefert den `RegistryKey`; ohne Atmosphäre `DimensionType.OVERWORLD`.

Die Werte sind Literale in Titan, nicht aus Cygnus importiert: Titan hängt nicht von Cygnus ab. Ein Test rechnet die Mischung aus den Quellwerten (BRIGHT und Granskoga) mit `float`-Arithmetik nach und vergleicht mit den Konstanten. So ist die Quelle im Test sichtbar, und eine Abweichung fällt auf.

`defaultClock` wird aus `MinecraftServer.getDimensionTypeRegistry().get(DimensionType.OVERWORLD).defaultClock()` übernommen, wie Cygnus es tut. Ohne die Uhr hätte die Dimension die Standarduhr des Builders, und `daytime` würde ins Leere greifen.

Die Dimension bekommt `SUN_ANGLE` und `MOON_ANGLE` = 180 wie in Cygnus. Das fixiert den Sonnenstand im Nebel; eine Sichtprüfung auf dem Testserver ist Teil der Aufgaben.

### D5 Reihenfolge beim Start

`PlatformBeans.instanceContainer()` liest die Atmosphäre der Auswahl, ruft `LobbyDimensions.register(...)` und legt die Instanz mit `createInstanceContainer(key)` an. Die Registrierung läuft einmal pro Start im Bean, also vor jedem Login. Ohne Atmosphäre ist der Schlüssel `DimensionType.OVERWORLD`, identisch zum bisherigen Default von `createInstanceContainer()`.

### D6 Konfigurationsschlüssel und Beispiel

```yaml
seasons:
  halloween:
    world: halloween            # worlds/halloween mit map.json
    from: "2026-10-25T00:00:00"
    to: "2026-11-02T00:00:00"
    atmosphere: slender-fog     # optional; fehlt er, bleibt die Overworld
```

Unbekannte Werte: `WARN` beim Start mit Schlüssel und gültigen Werten, Start ohne Atmosphäre.

## Risks / Trade-offs

- **Lesung beim Start**: Die Saisonkonfiguration wird beim Start an mehreren Stellen gelesen (`instanceContainer`, `mapProvider`, `CalendarStartedWorld`), wie heute schon. Wechselt ein Fenster genau zwischen zwei Lesungen (Millisekunden am Saisonbeginn), kann die Instanz mit Nebel in der Standardwelt starten, bis zum nächsten Neustart. Der Fall ist in Kauf genommen und nicht eigens behandelt.
- **Fixierter Sonnenstand**: Mit `SUN_ANGLE = 180` zeigt die Lobby keinen Sonnenstand im Verlauf. Das entspricht der Slender-Lobby; Titans `daytime`-Modul setzt nur die Uhrzeit.
- **Werte-Drift**: Ändert Cygnus die Slender-Lobby, bleibt Titan bei den Konstanten, bis sie hier angepasst werden. Der Test mit der Quellrechnung hält die Begründung aktuell, sobald die Quellwerte im Test geändert werden.
