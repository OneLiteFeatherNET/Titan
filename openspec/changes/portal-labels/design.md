# Design

## Context

Motivation: siehe proposal.md. Anforderungen: `specs/lobby-portal-labels` und `specs/setup-portal-labels`.

Ist-Zustand (Code in `main`):

- `Portal(String id, PortalShape shape, String task, @Nullable String permission)` ist ein reines Datenrecord in `core`; `PortalValidator.problems(List<Portal>)` prüft es, `PortalValidator.requireValid` bricht beim Laden der Karte ab. `PortalGsonAdapter` (`common`) liest und schreibt die `portals` der `map.json`.
- `features/portal` (`PortalModule`, `PortalTrigger`, `PortalIndex`) ist ein Avaje-`@Singleton` mit `@PostConstruct`/`@PreDestroy` und hängt einen `FeatureNode` an `TITAN_NODE`. Modulkonfiguration liest ein Modul mit `avaje-config` (`Config.getAs(KEY, Settings::parse)`, Muster `TickleSettings`); Werte sind Env-Variablen überschreibbar.
- CloudNet-Wissen liegt in der Extension `:bridge` (`TitanBridgePermissionExtension`), die eigene Klassenlader-Grenze wird mit JDK-only-Haltern in `common` überbrückt (`TitanPermissionBridge`, `TitanServerConnector`).
- `TitanMiniMessageImpl` (`common`) löst den globalen `<prefix>` auf. Im Setup-Server liefern `PortalCommand`, `PortalEditor`, `PortalDraft`, `PortalMessages` und die Partikel-Vorschau die Bearbeitung (`setup-portal-command`).
- Minestom 26.1: Scheduler-Aufgaben laufen auf dem `TickSchedulerThread`, nicht auf einem `TickThread`; Entity-Metadaten dürfen nur dort verändert werden, wo das sicher ist (siehe D6). Nur ein Smoke-Test findet solche Fehler zuverlässig.
- In Flug: `optional-extensions-bootstrap` verschiebt das CloudNet-Wissen aus `common` nach `platform/cloudnet` und lässt `:bridge` eine Extension. Dieser Change berührt nur die Stelle, an der der CloudNet-Zähler als Bean verdrahtet wird; er ist unabhängig von der Reihenfolge der Merges (nur der Ablageort der Bean ändert sich).

## Goals / Non-Goals

**Goals:**
- Label als reine Kartendaten, abwärtskompatibel; dieselbe Validierung in Lobby und Setup-Server.
- Zähler-Anbieter austauschbar, ohne `features/portal` zu ändern; CloudNet ist eine Implementierung.
- Rendering, Quellenauflösung, Änderungserkennung und Editor ohne Minestom-Server testbar.

**Non-Goals:**
- Argument-Tags (`<online:group:x>`), mehrere Quellen je Anzeige, Zusammenführung mehrerer Anbieter, Übersetzungen je Spieler.
- Befehle für `billboard` und `yaw` (nur per `map.json` in diesem Change), Animationen, Hintergrund- und Schattenoptionen der TextDisplay.
- Nachladen der Karte zur Laufzeit (Portale und Labels werden wie bisher beim Start gelesen).

## Decisions

### D1: Datenmodell: `PortalLabel` und sealed `LabelSource` in `core`

`Portal` bekommt eine fünfte Komponente `@Nullable PortalLabel label`; ein Konstruktor mit den bisherigen vier Parametern bleibt (`label = null`), damit bestehende Aufrufer und Tests unverändert laufen. `PortalLabel(Vec position, String text, @Nullable String offlineText, @Nullable LabelSource source, Billboard billboard, float yaw)` und `sealed interface LabelSource` mit `Task(name)`, `Group(name)`, `Service(name)`, `Local()` sind Records ohne Konstruktorprüfung (wie `Portal`, die Validierung liegt in D2). Ein fehlendes `source` bleibt `null` im Datenmodell, damit das Speichern es nicht in den Task des Portals verwandelt; aufgelöst wird es erst beim Lesen der Quelle (`LabelSource.orDefault(portal)`). `PortalGsonAdapter` liest und schreibt `label` (nur wenn vorhanden) mit Handparsern wie für `shape`; ein unbekannter Quellentyp liest als eigener Marker und wird von D2 abgelehnt, statt beim Parsen zu scheitern (gleiches Prinzip wie die „fehlende id“ im Adapter).

- Built-in first: Java-Records, sealed Interfaces, Pattern-Matching-`switch`. Keine Bibliothek nötig.
- Test (Unit): Gson-Round-Trip (alle vier Quellentypen, ohne `source`, `billboard` `fixed` mit `yaw`, ohne `label`), Portal ohne `label` serialisiert ohne den Schlüssel, alter 4-Parameter-Konstruktor.
- SOLID: Open/Closed (neuer Quellentyp = neuer sealed Fall, der `switch` wird vom Compiler erzwungen); Single Responsibility (Daten getrennt von Prüfung).

### D2: Validierung als Erweiterung von `PortalValidator`, eine Quelle der Wahrheit

`PortalValidator.problems(...)` prüft zusätzlich jedes Label: `position` vorhanden, `text` und `offlineText` als **strenges** MiniMessage parsbar, `source.type` im festen Vokabular, `name` bei `task`/`group`/`service`, `billboard` bekannt. Zum Parsen verwendet sie dieselben Platzhalter-Resolver wie das Rendering (`LabelPlaceholders`) mit festen Beispielwerten, damit `<online>`, `<max>`, `<task>` und `<prefix>` nicht als unbekannte Tags gelten. `PortalProblem.reason` nennt das Feld (`label.text: …`). Lobby-Start (`requireValid`) und `PortalEditor.save` rufen unverändert diese Methode; im Setup-Modul entsteht keine Regelkopie (wie D5 von `setup-portal-command`).

- Built-in first: Adventure-MiniMessage ist schon im Build; sein strenger Modus (`MiniMessage.builder().strict(true)`) meldet ungültige Tags als Fehler. Verworfen: eigener Tag-Scanner (würde MiniMessage nachbauen). `core` sieht über Minestom nur die Adventure-API, nicht MiniMessage; `core` bekommt deshalb `implementation(libs.adventure.minimessage)` (Version aus der vorhandenen BOM), damit die Prüfung in `PortalValidator` bleibt. `<prefix>` wird bei der Prüfung als Platzhalter mit Beispielwert aufgelöst; `TitanMiniMessageImpl` bleibt in `common`.
- Test (Unit): jedes Szenario der Spec (falsch geschlossener Tag, unbekannter Typ, Name fehlt/`local` ohne Namen, Position fehlt, unbekanntes Billboard, gültige Platzhalter), Editor und Lobby liefern für dieselbe Liste dieselben Gründe.
- SOLID: DRY, Single Responsibility; Erweiterung der bestehenden Naht statt neuer Prüfklasse im Setup.

### D3: Zähler-SPI `PlayerCounts` in `core`, unabhängig von CloudNet

```java
public interface PlayerCounts {
    boolean supports(SourceType type);          // default: true
    PlayerCount count(SourceType type, String name);
}
public record PlayerCount(int online, int max, boolean running) { static PlayerCount NOT_RUNNING = ...; }
public enum SourceType { TASK, GROUP, SERVICE }   // local läuft nie darüber
```

`task`, `group`, `service` sind anbieterneutrale Begriffe (Ziel des Portals, Gruppe von Servern, einzelne Instanz); der Anbieter bildet sie auf sein System ab. Der Rückgabewert beschreibt „läuft nicht“ ausdrücklich (`running = false`), damit weder `null` noch Ausnahmen den Offline-Fall tragen. `local` wird im Portal-Modul aus der Verbindungsverwaltung gelesen (D7) und nie an die SPI gereicht. Die Schnittstelle kennt keine CloudNet-Typen.

- Built-in first: ein Funktionsinterface in `core` wie `LobbyPortals`/`OnlinePlayers`; kein `ServiceLoader`, weil die Bean-Auswahl ohnehin über Avaje läuft (D4).
- Test (Unit): Quellen-Auflösung gegen ein `FakePlayerCounts` (Szenario „anderer Anbieter als CloudNet“: Portal-Modul zeigt 3/20 ohne jede CloudNet-Klasse im Klassenpfad des Tests).
- SOLID: Dependency Inversion (Portal-Modul hängt an der Abstraktion), Interface Segregation (zwei Methoden), Open/Closed (neuer Anbieter ohne Änderung am Portal-Modul).

### D4: Genau ein Anbieter, Avaje wählt; Ersatz ist `@Secondary`

Das Portal-Modul bekommt `PlayerCounts` per Konstruktor. In `features/portal` steht `NoPlayerCounts` als `@Secondary`-Bean: `supports` ist für alle Typen `true`, `count` liefert `NOT_RUNNING`. Jede andere Bean vom Typ `PlayerCounts` (CloudNet, später z. B. Redis) hat dadurch Vorrang, ohne Konfiguration. Zwei „echte“ Anbieter im selben Scope sind ein Verdrahtungsfehler und lassen den Start in Avaje scheitern (laut statt stillschweigend gemischt); ein Betrieb mit zwei Plattformen wählt mit `@Primary`. Keine Zusammenführung (KISS).

- Built-in first: Avaje-Inject `@Secondary`/`@Primary` ist genau dafür gedacht; ein eigener Registry-Mechanismus ist nicht nötig.
- Test (Integration, Avaje-`BeanScope` mit Testbeans): ohne Anbieter wird der Ersatz geliefert und meldet „läuft nicht“; mit Testanbieter wird dieser geliefert (Szenarien „Anbieter hat Vorrang“, „Ersatz ohne Anbieter“).
- SOLID: Liskov (Ersatz erfüllt den Vertrag vollständig), Open/Closed.

### D5: CloudNet als eine Implementierung: JDK-only-Halter, Installation in `:bridge`

Wie bei `TitanServerConnector` sieht die Anwendung die Bridge-Klassen nicht. In `common` entsteht der Halter `TitanPlayerCountLookup` mit einem JDK-only-Vertrag (`interface Lookup { boolean supports(String type); int[] lookup(String type, String name); }`, `null` = läuft nicht, sonst `{online, max}`); bis zur Installation ist er leer. `TitanBridgePermissionExtension` installiert die CloudNet-Implementierung (Task über die Services eines Tasks, Gruppe über die Services einer Gruppe, Service über den Namen; nur laufende Services, Summe der Bridge-Zähler) im selben `initialize()`. Eine Bean `HolderPlayerCounts implements PlayerCounts` (neben dem übrigen CloudNet-Wissen: heute `common`, nach `optional-extensions-bootstrap` `platform/cloudnet`) übersetzt Halter in SPI. Ist der Halter leer, meldet sie „läuft nicht“. CloudNet 4.0.0-RC18: `ServiceRegistry.registry().defaultInstance(CloudServiceProvider.class)`, dann `servicesByTask(name)`, `servicesByGroup(name)` bzw. `serviceByName(name)` (kann `null` sein); laufend heißt `snapshot.lifeCycle() == ServiceLifeCycle.RUNNING`; Zähler über `snapshot.readProperty(BridgeDocProperties.ONLINE_COUNT)` und `BridgeDocProperties.MAX_PLAYERS` (fehlender Wert zählt als 0). `platform/cloudnet` existiert auf `main` noch nicht; die Bean liegt daher vorerst in `common` neben `MessageChannelDeliver`. In der lokalen Variante ohne Bridge gibt es die CloudNet-Bean nicht, der Ersatz greift.

- Built-in first: der bestehende Halter-Weg (D1 des Bridge-Musters) statt einer zweiten Mechanik; eine Extension kann keine Beans der Anwendung beisteuern (Klassenlader).
- Test (Unit): Halter leer → „läuft nicht“; Halter mit Fake-Lookup → SPI-Werte; Summierung und Filter „nur laufende“ in einer reinen Funktion über Testdaten (ohne CloudNet-Klassen). Die Verdrahtung in der Extension prüft der manuelle Abnahmelauf auf einem CloudNet-Testsystem.
- SOLID: Dependency Inversion, Single Responsibility (Übersetzung getrennt von Abfrage).

### D6: Abfrage außerhalb des Ticks, Anwendung im Scheduler

Die `CloudServiceProvider`-Aufrufe sind `@RPCChained`, im Wrapper also synchrone RPCs zum Node; sie können blockieren. Ein wiederholender Scheduler-Task im Takt `portal.labelRefreshSeconds` stößt einen Abruf auf einem virtuellen Thread an (blockierendes I/O, Java 25); er liest die Zahlen aller Quellen und legt sie als unveränderlichen Schnappschuss ab. Anschließend plant er die Anwendung mit `scheduler.scheduleNextTick(...)`: Rendern und das Setzen der Entity-Metadaten laufen auf dem Scheduler-Thread, nie auf dem virtuellen. Läuft ein Abruf noch, wenn der nächste Takt kommt, wird der Takt übersprungen (kein Stau). Entity-Metadaten werden nur dort verändert, wo auch die anderen Module sie ändern (Scheduler der Instanz); da der Scheduler in 26.1 auf dem `TickSchedulerThread` läuft, stellt der Cyano-Env-Test sicher, dass `editEntityMeta` dort ohne Ausnahme funktioniert und die Pakete die Clients erreichen.

- Built-in first: Minestom-`Scheduler` für den Takt, virtuelle Threads für das blockierende Warten (kein eigener Thread-Pool, keine Bibliothek). Verworfen: Abfrage direkt im Scheduler-Task (blockiert den Tick, falls der Anbieter wartet).
- Test (Integration, Cyano-`Env`, `env.tick()`, ein direkter `Executor` statt virtuellem Thread, ein `FakePlayerCounts`): nach der eingestellten Anzahl Ticks erscheint der neue Text; ein langsamer Abruf lässt den nächsten Takt aus. Keine Sleeps, Takt über Ticks (F.I.R.S.T.). Zusätzlich ein Smoke-Test des Setups im Abnahmelauf.
- SOLID: Single Responsibility (Abruf, Rendering, Anwendung getrennt), Dependency Inversion (Executor injiziert).

### D7: Rendering als reine Funktion, Änderungserkennung über Komponentengleichheit

`LabelRenderer.render(Portal, PortalLabel, LabelReading)` ist eine reine Funktion: wählt `text` oder `offlineText` (siehe Spec), setzt `<online>`, `<max>` und `<task>` mit `Placeholder.unparsed` und löst `<prefix>` über `TitanMiniMessageImpl` auf. `LabelReading` ist `PlayerCount` oder das Ergebnis „lokal“ (`<max>` = `?`). Die Anzeige hält je Portal das zuletzt gesendete `Component`; `setText` und damit der Metadaten-Versand passieren nur, wenn `!rendered.equals(last)`. Je Portal gibt es genau eine `TextDisplay`-Entity (`EntityType.TEXT_DISPLAY`), erzeugt im `@PostConstruct` in der Lobby-Instanz, entfernt im `@PreDestroy`. `billboard` setzt `TextDisplayMeta`/`AbstractDisplayMeta.setBillboardRenderConstraints`; bei `fixed` gilt `yaw`. Die Quelle `local` liest die Anzahl der Spieler aus `MinecraftServer.getConnectionManager()` über einen injizierten `IntSupplier` (Muster `ConnectionOnlinePlayers`).

- Built-in first: Adventure-`Component` hat strukturelle Gleichheit, Minestom bringt `TextDisplay`-Meta mit; kein eigener Diff, kein eigener Textspeicher. Verworfen: Text als String vergleichen (würde Formatierung verschieden bewerten).
- Test (Unit, ohne Server): `LabelRendererTest` (Platzhalter, Offline mit und ohne `offlineText`, 0/0, `local`-Maximum `?`, `<prefix>`), `unparsed`-Schutz gegen `<`-Tags in Task-Namen; Änderungserkennung mit einer Zählerattrappe: gleicher Text = kein zweiter Versand. Integration (Cyano-`Env`): eine Entity je Label, keine bei Portal ohne Label, Entfernen beim Stoppen.
- SOLID: Single Responsibility, Open/Closed (neuer Platzhalter = neuer Resolver).
- Logging: INFO beim Start („Portal labels started with {} label(s)“); WARN einmal je unbekannter oder nicht unterstützter Quelle (`Portal label source {} '{}' of portal '{}' is unavailable`, dedupliziert über ein `Set` je Modul); DEBUG je Textänderung (Portal-Id, keine Spielerdaten). Keine Metriken (die Zahlen sind Anzeige, kein Betriebssignal), keine Spans.

### D8: Anzeigetext ist Kartendaten, kein i18n

Der Text einer Anzeige ist vom Build-Team verfasste Kartendaten, eine einzige geteilte Entität kann nicht je Locale rendern, und der Text ist absichtlich frei (MiniMessage, Platzhalter). Eine `TranslationStore` wird deshalb nicht verwendet; ein Build-Team, das mehrsprachige Anzeigen will, braucht ein anderes Konzept (nicht Teil dieses Changes). Neue Chat-Rückmeldungen im Setup-Server bleiben englisch in `PortalMessages` (das Repository hat dort keine Übersetzungsinfrastruktur, vgl. `setup-portal-command` D8). Spielertext in Rückmeldungen läuft über `Placeholder.unparsed`.

- Test (Unit): `PortalMessagesTest` für die neuen Meldungen, `<click:…>` im Label-Text wird wörtlich angezeigt.
- SOLID: Single Responsibility.

### D9: Konfiguration `portal.labelRefreshSeconds` mit `avaje-config`

`PortalSettings` parst und prüft den Wert nach dem Muster `TickleSettings` (`Config.getAs(PortalSettings.REFRESH_KEY, PortalSettings::refreshSeconds)`): Standard 5, ganze Zahl ≥ 1, sonst `IllegalArgumentException` mit Grund (Abbruch laut `lobby-module-config`). README (Optionen und Env-Tabelle) und der Standardwerte-Test (`ApplicationYamlDefaultsCharacterizationTest`-Muster) werden ergänzt.

- Built-in first: das Konfigurationsmuster der anderen Module (`avaje-config`, Profile, Env-Overrides). Kein neues Config-System.
- Test (Unit): Standard 5, `0`, `-1`, `abc`, `2`; Characterization-Test für den Standardwert.
- SOLID: Single Responsibility.

### D10: Setup-Seite erweitert den vorhandenen Entwurf

`PortalDraft` bekommt die Teile des Labels (Anker, Text, Offline-Text, Quelle); `PortalEditor` bekommt die Verben `labelHere`, `labelText`, `labelOffline`, `labelSource`, `labelRemove`, die den Entwurf ändern und mit dem Entwurfsstand antworten; `save` übernimmt das Label und ruft `PortalValidator.problems(...)` (D2). `PortalCommand` hängt `label here|text|offline|source|remove` als Syntaxen an (Text mit `ArgumentType.StringArray`, Typ mit Wortvorschlägen `task|group|service|local`); `PortalCompletions` ergänzt die Typen. Die Vorschau (`PortalOutline`/`DraftOutline`) erhält einen Ankerpunkt als zusätzlichen kleinen Partikelkreuz-Beitrag mit eigener Obergrenze, unter `MAX_POINTS`. `MapProviderPortalStore.save` schreibt das Portal samt Label; ohne Änderung am Speicherweg, weil `PortalGsonAdapter` (D1) es serialisiert.

- Built-in first: das vorhandene Minestom-Befehlssystem und die vorhandenen Klassen des Setups; keine Parallelstruktur.
- Test (Unit): `PortalEditorTest` je Szenario der Setup-Spec (inkl. `save` mit ungültigem Label, `label remove`, Label an gespeichertem Portal bleibt Entwurf), `PortalOutlineTest`/`DraftOutlineTest` für den Anker; Integration (Cyano-`Env`): jede Syntax erreicht den Editor, Konsole wird abgelehnt, Persistenz mit `@TempDir` (Label überlebt `save` und `setspawn`).
- SOLID: Open/Closed (neue Verben statt Umbau), Single Responsibility.

## Risks / Trade-offs

- [CloudNet-Provider-Aufruf blockiert den Tick] → Abruf auf einem virtuellen Thread, Anwendung im Scheduler (D6).
- [Entity-Metadaten auf dem `TickSchedulerThread` verhalten sich anders als erwartet (26.1)] → Cyano-Env-Test auf genau diesem Pfad plus Smoke-Lauf im Abnahmelauf.
- [Text-Anzeige zeigt veraltete Zahlen bis zu einem Takt] → bewusst (5 s Standard); einstellbar.
- [Zwei Anbieter-Beans in einem Betrieb] → Avaje bricht den Start ab; `@Primary` löst bewusst auf (D4).
- [Strenges MiniMessage lehnt bisher geduldete Texte ab] → Labels sind neu, nichts Bestehendes ist betroffen; Rückmeldung nennt Feld und Grund.
- [Feste Platzhalter reichen nicht für Mehrfach-Quellen] → bewusst ausgeschlossen (siehe Non-Goals); Argument-Tags können später kommen, ohne das Datenmodell zu ändern.
- [`optional-extensions-bootstrap` verschiebt die CloudNet-Bean] → die Bean liegt dort, wo der übrige CloudNet-Code liegt; nur der Ort ändert sich.
- [Gleiche Zählung an zwei Stellen (`local` hier, `OnlinePlayers` in `season`)] → bewusst klein gehalten; eine gemeinsame Extraktion wäre ein eigener `refactor`-Change.

## Migration Plan

Keine Migration: `label` ist optional, bestehende `map.json`-Dateien laden unverändert. Rollback: Label-Block entfernen oder die Version zurücknehmen; ältere Versionen ignorieren den unbekannten Schlüssel nicht automatisch (der Gson-Adapter liest nur bekannte Felder), die Lobby startet weiter.
