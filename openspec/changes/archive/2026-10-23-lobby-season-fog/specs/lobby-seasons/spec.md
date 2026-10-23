# Spec Delta

## ADDED Requirements

### Requirement: Eine Saison kann eine Atmosphäre wählen
_A season MAY name an atmosphere; the lobby applies it at start when that season is active._

Jede Saison MAY `seasons.<id>.atmosphere` setzen. Der einzige gültige Wert ist `slender-fog`. Ist das Feld nicht gesetzt oder leer, MUSS die Lobby die Overworld verwenden wie bisher. Die Atmosphäre gilt nur, wenn die Saison beim Start aktiv ist; eine abgeschaltete oder inaktive Saison hat keine Atmosphäre. Ein unbekannter Wert MUSS beim Start als Warnung mit dem vollständigen Schlüssel und den gültigen Werten geloggt werden; die Lobby startet dann ohne Atmosphäre und bricht nicht ab.

#### Scenario: Aktive Saison mit Nebel
- **WHEN** die Saison `halloween` aktiv ist, `seasons.halloween.atmosphere` auf `slender-fog` steht und die Lobby startet
- **THEN** lädt die Lobby `worlds/halloween` in einer Dimension mit dem Nebel aus der Anforderung „Der Nebel hat die festen Werte der Slender-Lobby“

#### Scenario: Saison ohne Atmosphäre
- **WHEN** die aktive Saison kein Feld `atmosphere` hat und die Lobby startet
- **THEN** nutzt die Lobby die Overworld, wie vor dieser Änderung

#### Scenario: Unbekannter Wert
- **WHEN** `seasons.halloween.atmosphere` den Wert `fog` hat und die Lobby startet
- **THEN** startet die Lobby ohne Nebel und loggt eine Warnung, die `seasons.halloween.atmosphere` und `slender-fog` nennt

#### Scenario: Inaktive Saison mit Atmosphäre
- **WHEN** `seasons.halloween.atmosphere` auf `slender-fog` steht, das Fenster aber nicht aktiv ist, und die Lobby startet
- **THEN** nutzt die Lobby die Standardwelt in der Overworld, ohne Nebel

### Requirement: Der Nebel hat die festen Werte der Slender-Lobby
_The fog is a fixed dimension: the Slender waiting lobby's values, not configurable._

Die Dimension `slender-fog` MUSS die Werte tragen, die die wartende Slender-Lobby hat: die Granskoga-Atmosphäre, zu 30 % in Richtung der Vorgabe `BRIGHT` gemischt (Cygnus `GameMapProvider`, `lobbyAtmosphereShare`). Die Werte sind im Code festgelegt und nicht über die Konfiguration einstellbar. Die Sichtweite endet bei 189,4 Blöcken, der Himmelsnebel bei 119,8 Blöcken.

| Attribut | Wert |
| --- | --- |
| Nebelfarbe `FOG_COLOR` | `#2B817C` |
| Himmelslicht `SKY_LIGHT_COLOR` | `#4EA5A4` |
| Himmelsfarbe `SKY_COLOR` | `#091B21` |
| Umgebungslicht `AMBIENT_LIGHT_COLOR` | `#050D0D` |
| Himmelslichtfaktor `SKY_LIGHT_FACTOR` | `0.0362` |
| Nebelbeginn `FOG_START_DISTANCE` | `22.4` |
| Nebelende `FOG_END_DISTANCE` | `189.4` |
| Himmelsnebelende `SKY_FOG_END_DISTANCE` | `119.8` |
| Sonnen- und Mondwinkel `SUN_ANGLE`, `MOON_ANGLE` | `180` |

#### Scenario: Werte der Dimension
- **WHEN** die Lobby mit `slender-fog` startet
- **THEN** trägt die Dimension der Lobby genau die Werte der Tabelle

#### Scenario: Werte stammen aus der Quelle
- **WHEN** die Werte aus den Quellwerten der Granskoga-Atmosphäre und der Vorgabe `BRIGHT` neu berechnet werden
- **THEN** ergeben sie die Werte der Tabelle, auf die angegebene Stellenzahl gerundet

### Requirement: Die Nebel-Dimension behält den Tagesverlauf
_The fog dimension keeps the overworld day clock, so the daytime module still works._

Die Dimension `slender-fog` MUSS die Standarduhr der Overworld als `defaultClock` tragen. Dadurch MUSS das Modul `daytime` auch mit Nebel die Uhr einfrieren und die Uhrzeit setzen können.

#### Scenario: Tageszeit mit Nebel
- **WHEN** die Lobby mit `slender-fog` läuft und `daytime` die Uhrzeit setzt
- **THEN** wirkt die Uhrzeit in der Lobby wie ohne Nebel, und die `defaultClock` der Dimension ist die der Overworld

### Requirement: Die Dimension wird vor der Lobby-Instanz registriert
_The fog dimension is registered once at startup, before the lobby instance and before any player joins._

Die Lobby MUSS die Dimension der gewählten Atmosphäre einmal beim Start registrieren, bevor sie die Lobby-Instanz anlegt. Die Instanz MUSS mit dieser Dimension angelegt werden, wenn eine Atmosphäre gewählt ist, und mit der Overworld, wenn keine gewählt ist. Ein Spieler kann nur in die Dimension gelangen, die beim Login schon registriert ist.

#### Scenario: Atmosphäre gewählt
- **WHEN** die Lobby mit aktiver Saison und `slender-fog` startet
- **THEN** ist die Dimension `slender-fog` vor der Lobby-Instanz registriert, und die Instanz nutzt sie

#### Scenario: Keine Atmosphäre
- **WHEN** die Lobby ohne Atmosphäre startet
- **THEN** nutzt die Lobby-Instanz die Overworld und es wird keine Dimension registriert

### Requirement: Die Atmosphäre wirkt beim Start, nicht live
_The atmosphere is read at startup only; a change to it never restarts or switches the lobby on its own._

Die Lobby MUSS die Atmosphäre nur beim Start lesen. Eine Änderung von `seasons.<id>.atmosphere` an einer laufenden Lobby DARF keinen Neustart vormerken, solange sich die gewünschte Welt nicht ändert, und DARF die Dimension nicht live wechseln. Die Änderung wirkt beim nächsten Start. Der Saisonbeginn oder das Saisonende ändert die Welt und löst damit den bestehenden Neustart aus; mit ihm kommt die Atmosphäre der neuen Saison.

#### Scenario: Atmosphäre an laufender Saison geändert
- **WHEN** die Lobby in der Welt `halloween` läuft und `seasons.halloween.atmosphere` geändert wird
- **THEN** wird kein Neustart vorgemerkt, und die Lobby läuft mit der bisherigen Dimension weiter

#### Scenario: Saisonbeginn mit Nebel
- **WHEN** die Lobby in der Standardwelt läuft und das Fenster der Saison `halloween` mit `atmosphere: slender-fog` beginnt
- **THEN** ist ein Neustart für `halloween` vorgemerkt, und nach dem Neustart trägt die Lobby den Nebel
