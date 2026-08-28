# Event-Modi für Titan — Recherche und Plan

**Stand:** 2026-08-21
**Betrifft:** Titan (Lobby), perspektivisch alle OLF-Minestom-Projekte
**Baut auf:** `docs/olf-minestom-project-standard.md` (Extension-Modell, Abschnitt 5)

---

## 0. Der Befund, der alles andere steuert

Bevor es um Architektur geht: **Eine umdekorierte Lobby bewegt keine Spielerzahlen.**

Das ist keine Meinung. Deep Rock Galactic betreibt beides — kosmetische
Saison-Events *und* inhaltliche Seasons — im selben Spiel, mit derselben
Spielerschaft. Die Zahlen (SteamCharts, von mir stichprobenartig direkt am
Primärwert geprüft):

| Deep Rock Galactic | Veränderung ggü. Vormonat |
|---|---|
| Oktober 2022 (kosmetisches Event) | **−0,37 %** |
| Oktober 2023 (kosmetisches Event) | **−12,16 %** |
| Oktober 2024 (kosmetisches Event) | **−4,92 %** |
| November 2022 (Season 03, Inhalt) | **+154,37 %** |
| März 2023 (Season 04, Inhalt) | **+58,79 %** |
| Juni 2024 (Season 05, Inhalt) | **+141,50 %** |

Dead by Daylight liefert dasselbe Bild als zweites, unabhängiges
Within-Game-Experiment: Halloween-Oktober über zehn Jahre im Median **+1 bis
+3 %**; der Juni mit Jahrestag *und* neuem Kapitel dagegen sechsmal in Folge nie
unter **+20 %**, teils +53 %. Destiny 2 ist in fünf von sechs Oktobern sogar
**negativ** — die eine Ausnahme war der Monat, in dem bezahlter Inhalt erschien.

**Zwei Zusatzbefunde, die genauso wichtig sind:**

- **Nachhaltigkeit.** Vom Zugewinn eines Spitzenmonats sind nach einem Monat
  40–55 % weg, nach drei Monaten rund 80 %. Bei kosmetischen Events bleibt am
  Ende ≈ 0 %, teils unterschreitet es die Ausgangslage (Destiny 2 lag im Dezember
  2024 17 % *unter* dem Wert vor dem Oktober-Event). Bei Inhalt bleibt ein Boden
  von 20–35 % über der alten Baseline — sechs Monate und länger.
- **Kalender-Saisonalität ist genauso groß wie ein Deko-Event.** Der September
  ist in fünf voneinander unabhängigen Spielen verlässlich der Jahrestiefpunkt
  (−15 bis −20 %), Dezember/Januar der Hochpunkt. Ein Dezember-Effekt muss diese
  Grundlinie erst schlagen, bevor man ihn dem Event gutschreibt.

### Was daraus folgt — und was nicht

**Nicht:** „Lasst die Winter-Lobby sein." Deko ist Markenpflege, und die hat
ihren eigenen Wert: ein Server, der sichtbar gepflegt wird, wirkt lebendig.

**Sondern:** Der Aufwand für die Deko muss **klein und wiederholbar** sein. Jede
Personenstunde, die in ein einmaliges Winter-Setup fließt, fehlt beim Inhalt —
und nur Inhalt hebt den Boden. Der ganze folgende Plan ist darauf ausgelegt, die
Deko-Seite auf Stunden statt Wochen zu drücken und die frei werdende Zeit in
Spielbares zu lenken.

> **Belegqualität.** Die Steam-Zahlen sind gemessene Telemetrie und für
> Within-Game-Vergleiche sauber. Für Minecraft-Server existieren **keine**
> vergleichbaren öffentlichen Daten — Hypixel-Zahlen kursieren nur als
> Community-Behauptungen ohne Primärquelle, und `minecraft-stats.com` ist eine
> kaputte Vorlagenseite, die unausgefüllte Platzhalter ausliefert. Ebenso gibt es
> **keine** peer-reviewte Literatur, die den Effekt saisonaler Events auf
> Spielerbindung misst. Die Übertragung auf Titan ist begründet, aber sie ist
> eine Übertragung.

---

## 1. Ist-Zustand: Titan hat bereits ein Event-System

Es ist nur weder benannt noch abgesichert.

| Baustein | Zustand |
|---|---|
| Welt-Auswahl | `System.getProperty("TITAN_LOBBY_MAP", "world")` in `MapPool` |
| `halloween` | bereits verdrahtet — als Default im `setup`-Modul |
| Feature-Flags | Togglz, `TitanFeatures` mit 5 Navigator-Konstanten |
| Flag-Reload | funktioniert: `FileBasedStateRepository(File)` delegiert mit 1000 ms |
| Resource Packs | ungenutzt; Aves liefert `ResourcePackHandler` + `ResourcePackCondition` |
| Items | `Items`, statisch und hart verdrahtet |
| Navigator-Layout | im Code (`NavigationHelper`) |
| `app.json` | flach, ohne Saison-Bezug |

Der heutige Mechanismus ist also: **eine Welt pro Saison, Auswahl beim Start per
JVM-Argument.** Kein Zeitplan, kein automatischer Wechsel, kein Fallback.

### Das ist kein Provisorium — es ist der Branchenstandard

Wichtig für die Planung: Der naheliegende Wunsch „eine Welt mit saisonalen
Overlays statt mehrerer Welten" ist technisch **nicht umsetzbar**, und zwar aus
zwei unabhängigen Gründen:

- In Minestom teilen sich `SharedInstance`s die Chunks ihres Parent-Containers.
  Zwei SharedInstances können **nicht** unterschiedliche Blöcke zeigen.
- Resource-Pack-`overlays` werden ausschließlich über das *Pack-Format des
  Clients* ausgewählt. Es gibt kein Feld, kein Paket und keine Option, mit der
  ein Server ein Overlay aktiviert.

Entsprechend arbeiten alle nachprüfbaren Netzwerke mit getrennten Welten.
CubeCraft baut pro Saison einen neuen Hub und recycelt ihn über Jahre; Hypixel
reskinnt **sieben** Lobbys pro Event. Titans Ansatz ist damit richtig — er braucht
nur einen Zeitplan, einen Fallback und einen Rollout-Weg.

### Der Fehler, der beim ersten echten Event zuschlägt

```java
private void peekMap() {
    Check.argCondition(this.referenceList.isEmpty(), "The map list is empty");
    if (this.referenceList.size() == 1) {
        this.selectedMap = this.referenceList.getFirst();   // Property ignoriert
        return;
    }
    this.selectedMap = ...filter(...).findFirst().orElseThrow();   // ohne Meldung
}
```

Zwei Verhaltensweisen, die zusammen eine Falle bilden:

1. **Bei genau einer Welt wird die Property stillschweigend ignoriert.** Lokal
   fällt ein Tippfehler in `TITAN_LOBBY_MAP` deshalb nie auf.
2. **Bei mehreren Welten wirft `orElseThrow()` ohne Meldung.** Der Server startet
   nicht, und im Log steht eine `NoSuchElementException` ohne den Namen der
   gesuchten Welt.

Genau das Szenario „Winter-Welt nicht mitdeployt" endet damit in einem
Startabbruch, dessen Ursache man erst suchen muss. **Das ist Aufgabe 1 des
Plans**, unabhängig von allem anderen.

---

## 2. Leitentscheidung der Architektur

**Saisonaler Content ist bei euch ein Deployment-Problem, kein Feature-Flag-Problem.**

Feature-Flags existieren, weil Deployment teuer ist. Bei euch ist es das nicht:
CloudNet-Services sind kurzlebig und neustartbar, und das Zielbild aus dem
OLF-Standard lädt Fachlogik ohnehin als Extension-Jars. Damit fällt die
Hauptbegründung für Flags als Content-Träger weg.

Ein Halloween-Event ist außerdem nicht *ein* Feature, sondern zwölf: Deko,
Mob-Skins, Sounds, Scoreboard-Titel, Navigator-Icons, Drops. Wer das flaggt, hat
nach drei Jahren keine 15, sondern 150 Konstanten in `TitanFeatures` — und
N boolesche Flags ergeben 2^N Konfigurationen, von denen nie jemand mehr als drei
testet.

### Die Aufteilung

| Werkzeug | Wofür | Nicht wofür |
|---|---|---|
| **Season-Pack** (Extension-Jar) | alles Saisonale: Code, Listener, Deko-Logik | Werte, die sich pro Saison nur ändern |
| **Season-Daten** (JSON im Pack) | Zeitfenster, Items, Texte, Positionen, Loot | Kontrollfluss |
| **Togglz** | **ein** Flag pro Saison: Zeitfenster-Gate + Notausschalter | ein Flag pro Sub-Feature |

**Die Grenze zwischen Daten und Code:** Braucht es einen neuen *Wert* oder ein
neues *Verb*? „Der Kürbis ist orange" ist ein Wert → JSON. „Kürbisse explodieren,
wenn jemand drübergeht" ist ein Verb → Code.

Sobald das JSON `if`, `when` oder `onTick` bekommt, habt ihr eine schlechte
Programmiersprache gebaut — ohne Typprüfung, ohne Debugger, ohne Stacktrace. Das
gilt ausdrücklich auch für Togglz' `ScriptEngineActivationStrategy`: die Bedingung
wird unprüfbar, *und* Nashorn ist seit JDK 15 nicht mehr im JDK.

**Der Java-25-Ausweg — Daten, aber typisiert:**

```java
public sealed interface SeasonEffect
    permits ReplaceItem, PrefixTitle, PlaceDecoration, AmbientSound { }

public record ReplaceItem(NavigatorSlot slot, Material material, String nameKey)
    implements SeasonEffect { }
```

Jackson deserialisiert polymorph, ein `switch` über die versiegelte Hierarchie ist
erschöpfend. Ein neuer Effekt-Typ, den niemand behandelt, bricht den **Compile**,
nicht die Produktion.

### Zeitfenster: was Togglz nicht kann

`ReleaseDateActivationStrategy` hat exakt zwei Parameter, `PARAM_DATE` und
`PARAM_TIME` — **kein Enddatum, keine Zeitzone** (im Jar per `javap` geprüft).
Für „16.10. bis 08.11." ist sie unbrauchbar.

Der Ersatz ist klein: `ActivationStrategy` hat vier Methoden. Eine eigene
`SeasonWindowActivationStrategy` mit `from`, `to`, `zone`, registriert über
`META-INF/services`, ersetzt jedes handgeschriebene `if (LocalDate.now()...)` im
Projekt. Rund 60 Zeilen.

**Zeitzone einmal festlegen:** `Europe/Berlin` ist die Redaktionszeitzone.
Fenster als `2026-10-16T00:00` + `zone`, beim Laden **einmal** zu `Instant`
auflösen, danach nur noch `Instant` vergleichen. Nie `LocalDateTime.now()`, nie
`ZoneOffset` speichern (`+02:00` ist im November falsch).

**`Clock` injizieren** statt `Instant.now()` in der Fläche. Das erledigt drei
Dinge auf einmal: deterministische Tests ohne DST-Zufall, lokale Vorschau per
`-Dtitan.clock.offset=P45D`, und keine Tests, die im März grün und im November rot
sind.

### Vorschau ohne Flag-Umlegen

Das naheliegende „Flag kurz anschalten, gucken, ausschalten" ist für alle Spieler
sichtbar und hinterlässt Drift zwischen den Nodes. Stattdessen eine Preview-Ebene
im Gate, in dieser Reihenfolge:

```java
boolean active(SeasonId id, Player p) {
    if (killSwitch.isActive(id)) return false;                 // Ops, höchste Priorität
    if (p.hasPermission("titan.season.preview")) return true;  // Team sieht es vorab
    return window(id).contains(clock.instant());               // Zeitfenster
}
```

Der Preview-Zweig läuft über LuckPerms, das ihr schon habt — kein zweites
Berechtigungssystem.

---

## 3. Termine 2026

Aus ~25 Datenpunkten der Event-Historie von Dead by Daylight, Team Fortress 2,
Destiny 2 und Overwatch:

- **Modalwert der Laufzeit: exakt 21 Tage.** Spanne 11–28, praktisch nichts über
  vier Wochen. Der Trend geht zu kürzer.
- **Halloween beginnt 2–3 Wochen vor dem 31.10. und endet zwischen dem 1. und
  7. November. Kein einziges Event endet am 31.10.** Der Zahltag liegt mitten
  drin, nie am Rand.
- **Winter beginnt 1–2,5 Wochen vor dem 25.12. und endet in der ersten
  Januarwoche.** Alle überspannen Weihnachten *und* Neujahr.

Dazu die deutschen Schulferien 2026 (KMK/schulferien.org) — für ein
deutschsprachiges Netzwerk der wichtigere Faktor:

- **Herbstferien streuen über einen ganzen Monat: 05.10.–06.11.** Die maximale
  Überlappung liegt bei **19.–24.10. mit 11 von 16 Ländern**. An Halloween selbst
  sind nur noch etwa drei Länder frei, und **Bayern hat erst 02.–06.11. Ferien**.
  Es gibt **keinen einzigen bundesweit gemeinsamen Herbstferientag.**
- **Weihnachten dagegen: 23.12.–01.01. sind alle 16 Länder gleichzeitig frei.**

| | Halloween 2026 | Winter 2026/27 |
|---|---|---|
| **Live ab** | Freitag, **16.10.** | Dienstag, **01.12.** |
| **Ende** | Sonntag, **08.11.** | Mittwoch, **06.01.** |
| Laufzeit | ~3,5 Wochen | Deko 5 Wochen, Kernprogramm ~3 |
| Warum | trifft das 11-Länder-Fenster 19.–24.10., Ende nimmt Bayern mit | einziges bundesweites Fenster |

**Wenn die Zeit nur für ein gutes Event reicht: Winter.** Es ist das einzige mit
einem bundesweiten Ferienfenster.

---

## 3a. Die Terminfalle, die zwei Netzwerke zweimal getroffen hat

**Plattform-Migrationen fressen Saison-Events.** CubeCraft hat das zweimal in
Folge öffentlich dokumentiert. Der Project Lead zu Halloween 2022:

> „Because we're updating our networks to 1.19, all of our resources are focused
> on getting that out soon, so this Halloween event is a little simplified."

Und im Frühjahr 2023 dasselbe noch einmal — das Event kam im Mai statt im März,
weil das Entwicklerteam vollständig in einer Bedrock-Migration steckte. Halloween
war dort die einzige Ausnahme von einem viermonatigen Netzwerk-Freeze.

**Für Titan ist das unmittelbar relevant:** Der OLF-Standard sieht in Phase 4 und 5
eine Bootstrap-Extraktion und die Umstellung auf Extension-Jars vor — inklusive
Wechsel des Extension-Frameworks. Fällt diese Umstellung in den Oktober oder
November, ist das Event entweder abgespeckt oder verschoben.

**Konsequenz für die Reihenfolge:** Entweder die Extension-Migration ist **vor
Anfang Oktober** abgeschlossen, oder Schritt 3 dieses Plans (Season-Pack als
Extension) wird für 2026 **übersprungen** — dann gibt es die Winter-Lobby als
reinen Weltentausch über `TITAN_LOBBY_MAP`, so wie heute, und das Pack-Modell
kommt 2027. Beides ist vertretbar. Beides gleichzeitig im Oktober ist es nicht.

---

## 4. Umsetzung in vier Schritten

Jeder Schritt ist für sich nutzbar und einzeln mergebar.

### Schritt 1 — Die Falle entschärfen (halber Tag)

Vor allem anderen, weil es das Deployment jedes künftigen Events betrifft.

- `MapPool.peekMap()`: bei fehlender Zielwelt eine Meldung werfen, die den
  gesuchten Namen und die gefundenen Welten nennt.
- Den Sonderfall „genau eine Welt" abschaffen — die Property gilt immer, oder sie
  gilt nie. Ein stiller Sonderfall, der lokal anders wirkt als in Produktion, ist
  schlimmer als ein harter Fehler.
- **Fallback statt Absturz:** Ist die Event-Welt nicht da, mit `world` starten und
  eine Warnung loggen. Ein Server, der ohne Deko läuft, ist besser als einer, der
  nicht startet.

**Fertig, wenn:** ein falsch gesetztes `TITAN_LOBBY_MAP` den Server startet, eine
verständliche Warnung schreibt und die Standardwelt lädt.

### Schritt 2 — Zeitfenster und Gate (1–2 Tage)

- `SeasonWindowActivationStrategy` (`from`, `to`, `zone`) schreiben und über
  `META-INF/services` registrieren.
- `SeasonGate` als einzige Fassade vor Togglz, in der Reihenfolge
  Kill-Switch → Preview-Permission → Zeitfenster. **Kein anderer Code ruft
  `FeatureContext` direkt.**
- `Clock` in den Composition Root.
- Ein Command `/season` für Status und Notausschalter — die Togglz-Admin-Konsole
  ist Servlet-basiert und im Minestom-Prozess nicht nutzbar.

**Fertig, wenn:** ein Zeitfenster in der Konfiguration steht, das Team es per
Permission vorab sieht, und der Wechsel ohne Neustart passiert.

### Schritt 3 — Erstes Season-Pack (Winter zuerst)

Als eigenes Extension-Modul nach OLF-L5-01/L5-03: `extension.json` generiert,
alles vom Host Gelieferte `compileOnly`.

- Contract `SeasonalContent` ins `:api`-Modul: `id()`, `window()`, `activate()`,
  **`deactivate()`**.
- Registrierung über die `ServiceRegistry` des Extension-Frameworks, **nicht** über
  `ServiceLoader` — der ist über Classloader-Grenzen fragil, genau das Problem, um
  das die `ThreadHelper`-Krücke heute herumarbeitet.
- **`deactivate()` ist Pflicht, nicht optional.** Ohne symmetrisches Abräumen
  bleiben Deko-Blöcke, Boss-Bars und Scheduler-Tasks nach Saisonende stehen, und
  niemand findet die Ursache — das Pack ist ja „aus".
- Saisonale Optik als **Decorator** um bestehende Bausteine (`NavigationHelper`
  bekommt andere Items und Namen), nicht als zweite Implementierung. Bei zwei
  gleichzeitigen Packs entscheidet ein **Prioritätsfeld im Manifest**, nicht die
  Ladereihenfolge.

**Fertig, wenn:** der Host ohne Season-Pack sauber startet und das Pack sich durch
Löschen einer Zeile in `settings.gradle.kts` restlos entfernen lässt. Geht das
nicht, ist ein `if` in den Kern gewandert.

### Schritt 3b — Rollout: CloudNet kann das schon

Für den Saisonwechsel ist **kein eigener Code nötig**. CloudNet bringt alles mit:

| Mechanismus | Wofür |
|---|---|
| Templates werden beim Service-Start kopiert | neues Template auf den Lobby-Task → jeder neu startende Service ist winterlich |
| Smart-Modul (`autoStopTimeByUnusedServiceInSeconds`, `minNonFullServices`) | alte Lobbys laufen leer und beenden sich → **Drain-Rollout statt Big Bang** |
| `templateInstaller: INSTALL_RANDOM_ONCE` | eingebaute Map-Rotation; funktioniert genauso für „30 % Wintervariante" |
| `includes` (Download beim Prepare) | Saisonwelt aus Object Storage ziehen statt ins Template committen |
| `maintenance: true` auf einem Canary-Task | Staged Rollout: erst Staff, dann alle |

**Eine harte Grenze:** Das Deployment saisonaler Inhalte darf **niemals** einen
Neustart der Komponente erfordern, die Joins, Queueing oder Lobby-Transfers
abwickelt. Hypixels Dev-Blog beschreibt genau diesen Fehler: Ihr Monolith brauchte
60–80 Sekunden Boot, in denen niemand joinen, die Lobby wechseln oder Befehle
nutzen konnte — laut eigener Aussage *„the cause of nearly every network drop over
the past year"*. Saisonales gehört in einen Leaf-Service, nie in Node oder Bridge.

**Und: keine Lockstep-Migration erzwingen.** Alte und neue Lobby-Version müssen
während des Rollouts koexistieren können. Hypixel musste 2017 sichtbar unfertige
Lobbys ausrollen, weil ihr Backend-Wechsel alle Lobbys gleichzeitig verlangte —
mit dem eingestandenen Ergebnis „some lesser quality lobbies".

### Schritt 3c — Das Event-Ende ist der teuerste Moment

Das am häufigsten unterschätzte Detail. Hypixels Events-Team über den
Spooky-Festival-Kollaps, öffentlich:

> „It only ever breaks for a few minutes? I think that's to be expected for 10,000
> players all warping to the hub." … „I don't know if there's any cheap solution
> to thousands of players all connecting to different servers."

Ein Event, das für alle gleichzeitig endet, erzeugt einen synchronen
Massen-Transfer. **Sie haben das nie gelöst.** Bei eurer Größenordnung ist das
beherrschbar, aber nur wenn man es einplant: Ende staffeln, oder die Event-Lobby
nach Eventende weiterlaufen lassen und nur den Zustrom umlenken.

**Ergänzend, das Belohnungs-Ende:** Nicht ausgegebene Event-Währung nach
Fristende **automatisch** in reguläre Währung umwandeln (Overwatch-2-Muster,
Abschnitt 6) — keine Abholfrist, keine Verfallsregel. MultiVersus gab nach
Eventende nur zwei Tage zum Einlösen bereits verdienter Belohnungen; das ist die
Sorte Detail, die Support-Tickets erzeugt.

### Schritt 3d — Wenn ein Event wiederkommt: Smoketest

Hypixel, Holidays 2025, aus dem Forum:

> „It looks like a bug from last year is back, where Santa Says wins don't count
> towards your /myposition or the wins leaderboard."

Kein Staff-Reply. **Code, der elf Monate dunkel liegt, wird von nichts getestet** —
er kompiliert, aber niemand führt ihn aus. Zum Reaktivieren einer Saison gehört
deshalb ein Durchlauf der Kernpfade, bevor das Event live geht. Das ist genau der
Grund für die `deactivate()`-Pflicht und den Time-Bomb-Test aus Abschnitt 5.

### Schritt 4 — Messung (ein Nachmittag)

Ohne das ist nach dem Event nichts aussagbar.

**Wichtig vorweg:** *Plan (Player Analytics)* listet Minestom nicht und ist
unbrauchbar; *bStats* misst Plugin-Verbreitung für Autoren, nicht Serverbetrieb.
Beides scheidet aus. Ihr braucht ein eigenes Join-Log — und habt die
Einstiegspunkte mit `PlayerSpawnListener` und `PlayerConfigurationListener`
bereits.

Eine Zeile `timestamp;uuid` pro Join. Daraus fallen mit `sort`/`uniq` heraus:

| Kennzahl | Aussage |
|---|---|
| **Reaktivierte Spieler** — UUIDs im Event, die 30 Tage davor nicht da waren | der eigentliche Zweck eines Events |
| **Post-Event-Retention** — von den Neuzugängen nach 14 und 28 Tagen noch da | ob etwas hängen bleibt |
| Unique Spieler pro Tag | Grundlinie |
| Neue Spieler (erstmals gesehen) | Wachstum |

Zwei Disziplinen, ohne die die Zahlen wertlos sind:

- **Baseline 14 Tage vor dem Event erheben**, auf Wochentage ausgerichtet.
- **Personenstunden mitzählen.** „Hat sich gelohnt" ist ein Bruch; ohne Nenner
  habt ihr nur den Zähler.

**Falls Prometheus:** keine Pro-Spieler-Metriken — das erzeugt eine Zeitreihe je
gesehenem Spieler und ist eine Kardinalitätsbombe. Uniques gehören in eine
Tabelle. *(Der Grafana-MCP-Zugang dieser Session war defekt — `mcp-grafana` nicht
im PATH. Falls die Auswertung dort landen soll, muss das repariert werden.)*

**Confounder, der ins Auswertungsdokument gehört:** Weil die Herbstferien 2026
über den 05.10.–06.11. streuen und das Event mitten drin liegt, lässt sich der
Event-Effekt im ersten Jahr **nicht** vom Ferieneffekt trennen. Erst der
Jahresvergleich ab 2027 hilft.

---

## 4a. Der eigentliche Engpass: der Weg vom Build in den Server

Der Engpass ist erfahrungsgemäß **nicht das Bauen, sondern die Integration.** Der
einzige direkte Beleg dazu stammt von einem Hypixel-Builder: seine abgenommene Map
*„wasn't added to the game until I fully joined the build team months later"* —
Build fertig, Auslieferung wartete auf Entwicklerkapazität. Wynncraft nennt
Bewerbern drei Monate Review-Latenz.

**Handoff-Format: Anvil-Weltordner, nicht Schematic.** Schematics verlieren zu
viel: `.schem` speichert kein Licht und keine Heightmaps, und **Entities sind auf
beiden Seiten opt-in** (`//copy -e` *und* `//paste -e`). Vergisst ein Builder das,
fehlen Item Frames, Paintings, Armor Stands und Display Entities vollständig.
`.litematic` ist client-only und wird serverseitig von nichts gelesen.

**Metadaten neben die Welt.** Das einzige gut dokumentierte Schema kommt von
BlueDragonMC, einem quelloffenen Minestom-Netzwerk: eine `config.yml` **im
Weltordner neben `region/`**, alles unter `world:` genamespaced, mit `name`,
`description`, `author`, `spawnpoints`, `additionalLocations`. Direkt übernehmbar.
Für alles, was in situ gesetzt werden muss, ist euer Guira-Setup-Flow der richtige
Weg — er schreibt das JSON, statt dass jemand Koordinaten abtippt.

**Entschieden: Falco statt Polar.** Eine frühere Fassung dieses Dokuments empfahl
einen Wechsel auf das Polar-Format. Diese Empfehlung ist **hinfällig** — Titan
setzt auf die eigene Engine **Falco** (`falco-anvil`, `falco-light`), die das
Anvil-Format behält.

Das ist die bessere Entscheidung, und zwar aus einem Grund, der im
Polar-Vergleich untergegangen wäre: Polar speichert **keine Entities**, was NPCs,
Item Frames und Display Entities aus der Welt in Konfiguration verschoben hätte.
Falco liest weiter Anvil und braucht diesen Umbau nicht. Dazu kommt, dass ein
Lesefehler bei Falco eine Ausnahme wirft, statt den Chunk als „nicht vorhanden"
zu melden — bei einer gebauten Lobby ist genau das der Unterschied zwischen einem
Fehler und einem stillschweigend neu generierten, leeren Stück Welt.

Anforderungen und Abnahmekriterien dazu stehen in
[`spec-lobby-saison-events.md`](spec-lobby-saison-events.md), Stufe 1.

**Zu klären bleibt die Lizenzfrage:** Falco steht unter AGPL-3.0 ohne
Linking-Ausnahme, Titan veröffentlicht unter Apache-2.0. Siehe Spec, Abschnitt 7.

## 4b. Resource Packs, falls saisonal gewünscht

Aves liefert `ResourcePackHandler` und `ResourcePackCondition`, Titan nutzt beides
nicht. Falls saisonale Optik über Packs laufen soll, ist das Modell seit 1.20.3:
**großes Base-Pack mit stabiler UUID + kleines Season-Delta mit eigener UUID.**
`resource_pack_pop` entfernt gezielt eine UUID, ohne die anderen anzufassen — der
große Base-Pack bleibt im Client-Cache.

Vier Fallen, alle im Minestom-Quelltext bzw. Protokoll belegt:

1. **Nie `replace(true)`** für den Saisonwechsel. Minestom implementiert das als
   Pop-all-plus-Push — nicht atomar, der Client lädt zweimal.
2. **`ConnectionManager.doConfiguration()` macht `packFuture.join()` ohne
   Timeout.** Ein Client, der nie antwortet, parkt seinen Config-Thread unbegrenzt.
   Eigenen Timeout einbauen.
3. **CustomModelData merged nicht.** Ein Season-Pack, das ein Item anfasst, das
   das Base-Pack schon customized, ersetzt dessen Datei ganz — alle Basis-Varianten
   verschwinden still. Item-IDs zwischen Base und Season **disjunkt** halten.
   (Atlanten und Sprachdateien mergen dagegen problemlos.)
4. **Geyser lügt.** Bedrock-Spieler melden über Geyser `SUCCESSFULLY_LOADED`, ohne
   je etwas erhalten zu haben. Am Pack-Status ist das nicht erkennbar — explizit
   auf Bedrock prüfen.

Der Client cached außerdem **nach URL, nicht nach Hash** — also
inhaltsadressierte Dateinamen (`pack-<sha1>.zip`) verwenden und SHA-1 immer
mitsenden.

## 5. Damit es im dritten Jahr nicht kippt

Der eigentliche Vorteil von Packs gegenüber Flags: Aufräumen ist ein
`settings.gradle.kts`-Diff statt eines Refactorings.

1. **Jahreszahl im Namen ist Pflicht** — `season-halloween-2026`, nie `halloween`.
   Erzwingt die bewusste Entscheidung „wiederverwenden oder neu bauen?" statt
   stillem Verhaltensdrift.
2. **`until` im Manifest ist Pflichtfeld.** Kein Pack ohne Enddatum.
3. **Time-Bomb-Test:** ein JUnit-Test, der fehlschlägt, wenn ein registriertes Pack
   sein `until` + 12 Monate überschritten hat. Passt in dieselbe ArchUnit-Schicht,
   die der OLF-Standard ohnehin vorsieht.
4. **Obergrenze auf `TitanFeatures`** — heute 5 Konstanten, Limit bei 12, CI bricht
   darüber. Zwingt zu „welches alte Flag räume ich weg, damit ich das neue anlegen
   darf".
5. **Archivieren statt löschen:** Pack nach `seasons/archive/` und aus
   `settings.gradle.kts`. Nächstes Jahr wieder aufnehmbar, aber solange nicht
   kompiliert, nicht getestet, nicht mitmigriert. Tragekosten null.

---

## 6. Kommunikation

**Discord ist Hub, nicht Reichweite.** Die JIM-Studie 2025 (n = 1.200,
repräsentativ, 12–19 Jahre) misst Discord bei **20 % regelmäßiger Nutzung** —
32 % der Jungen, aber nur **7 % der Mädchen**. Zum Vergleich: Snapchat 56 %,
TikTok 53 %, Twitch 11 %.

Der ermutigende Teil derselben Studie: **Minecraft ist Lieblingsspiel Nr. 1**, mit
rund 25 % bei *offener* Frage ohne Antwortvorgaben — „in allen Befragungsgruppen
an erster Stelle", unabhängig von Alter, Geschlecht und Schulform. Die Zielgruppe
ist da; sie ist nur nicht auf Discord.

Reichweite kommt aus **Serverlisten** (serverliste.net führt mit Abstand das
höchste Volumen) und aus kurzen Clips. **X/Twitter lohnt nicht** — 0,12 %
Engagement gegen 3,73 % bei TikTok.

**Beteiligung realistisch ansetzen** (Nielsens 90-9-1): Ein Build-Contest liegt im
1-%-Tier — bei 200 Aktiven sind das 2–10 Einreichungen. Screenshot-Aktionen und
Abstimmungen liegen im 9-%-Tier, also 20–60. Wer mit 50 Build-Einreichungen
plant, plant falsch.

### Zwei Fehlerbilder, die ihr umgehen könnt

**Halo Infinite, „Winter Contingency" 2021:** zehn Belohnungsstufen, **eine pro
Tag**, über 14 Tage zwischen Weihnachten und Neujahr. Wer alles wollte, musste an
10 von 14 Feiertagen spielen. Schlimmer: Teilnahme brachte keine kleineren
Belohnungen, sondern **kaputte** — ein Schulterpolster ohne Gegenstück.

Ein klassischer 24-Türchen-Adventskalender ist strukturell derselbe Mechanismus.
Drei Änderungen machen ihn sicher:

1. **Kumulativ statt tagesgebunden** — 15 von 24 Tagen da gewesen = 15 Türchen,
   nachholbar.
2. **Teilbelohnungen müssen für sich funktionieren.**
3. **Beim Start sagen, ob es das nächstes Jahr wieder gibt.**

**Overwatch 2, Winter Wonderland 2023** — das Muster fürs Eventende, direkt
kopierbar: nicht ausgegebene Event-Währung wurde nach Fristende **automatisch in
reguläre Währung umgewandelt**, vorab im Ankündigungsposten angekündigt. Kein
Löschen, keine Verfallsfrist, keine Abholpflicht.

---

## 7. Was ausdrücklich nicht getan werden sollte

1. **Ein Flag pro saisonalem Sub-Feature.** Ein Flag pro *Saison*. Die Sub-Features
   leben und sterben gemeinsam.
2. **`if (HALLOWEEN.isActive())` verstreut im Kerncode.** Die Entscheidung fällt
   einmal im Composition Root; danach wird ein Objekt injiziert, kein Boolean
   geprüft.
3. **Vererbung für Saison-Varianten** (`HalloweenLobby extends Lobby`).
   Funktioniert für die erste Saison, kollabiert bei zwei gleichzeitigen.
4. **Ein fettes `Theme`-Interface** mit Farben, Items, Sounds, Mobs und Nachrichten.
   Jedes neue Event zwingt alle alten Themes zu einer neuen Methode. Stattdessen
   kleine, unabhängig registrierbare Beiträge.
5. **Content-Pack ohne `deactivate()`.**
6. **Pack, das ein anderes Pack direkt referenziert.** Verstößt gegen OLF-L2-01,
   funktioniert im Test, bricht im Deployment.
7. **Pack bündelt, was der Host liefert** (Minestom, `:common`, Togglz-Core).
   Zwei Kopien in zwei Classloadern → `NoSuchMethodError`, teuer zu finden.
8. **Wochenlanges Deko-Bauen.** Siehe Abschnitt 0 — der Hebel liegt beim Inhalt.
9. **Ein Einmal-Spektakel als erstes Event.** Höchste Kosten, kürzeste Wirkung —
   die Kategorie, an der Epic das Format eingestellt hat (7b).
10. **Mit hoher Frequenz starten.** Sie lässt sich nicht mehr senken (7b).
11. **Weiter als drei Monate durchplanen.** Überlebt den Kontakt mit der Realität
    nicht (7b).
12. **Am Vortag fertig werden.** Liveops gehört Wochen vorher fertig und durch
    einen Test — Winter heißt Mitte November, nicht 30.11.

---

## 7a. Ein Saison-Primitiv statt jährlich neuer Mechaniken

**Das ist die Antwort auf „wie schiebe ich am schnellsten weitere Funktionen
nach".** Und sie ist ungewöhnlich gut belegt.

Die schärfste Formulierung stammt von InnoGames über ihr Saison-Event „Forge of
Vulcan" in der sechsten Iteration:

> „The team doesn't rebuild it every quarter… **The event currency and 21-day
> runtime have been constant throughout — the stability and habit players need,
> leaving everything else open to change.**"

Und im selben Bericht: Vergleicht man die erste Fassung mit der heutigen, findet
man *„very few similarities between them"*.

**Daraus die Architekturregel: Währung, Laufzeit und Kalenderslot sind das
Produkt. Alles andere ist die Saison.** Genau diese drei Dinge gehören in
stabilen Code; alles darüber gehört in die austauschbaren Saison-Daten.

Drei unabhängige Bestätigungen:

- **Fortnite Winterfest** ist bis in die Werbetexte hinein derselbe Event: die
  Lodge als fester Ort, 14 Geschenke pro Tag unverändert von 2019 bis 2023, der
  Kamin-Loop identisch — und 2019 und 2021 wörtlich dieselbe Werbezeile. Neue
  Inhalte kommen per Unvaulting alter Items, nicht durch Neubau.
- **Genshin Impact** hat Wiederverwendung sogar kategorisiert: eine Wiki-Kategorie
  „Recurring Events" mit rund 65 Einträgen. Ley Line Overflow lief **21×**,
  Marvelous Merchandise 9×, Windtrace 5× — letzteres 2025 als „Snowtrace"
  umgeskinnt. Das Template bleibt, das Balancing wird versioniert.
- **Belka Games** (Clockmaker, neun Jahre alt), unverblümt: *„Don't be shy about
  simply reusing successful old events. In just 2–3 months, your player base will
  refresh significantly."*

**Die Grenze** — Javier Barnes (ehemals Socialpoint/Tilting Point): Templates sind
der Schlüssel zu planbarem Aufwand, aber *„a single kind shouldn't run too often
with the same exact experience and rewards."* Gleiche Mechanik ja, gleiche
Belohnungen nein.

CubeCraft macht in der Minecraft-Welt exakt dasselbe: seit Jahren dieselbe
Hub-Suche, nur umgethemt — 20 Kürbisse, 30 Trick-or-Treats, 20 Blumen,
Strandbälle, 20 Schneemann-Teile. Eine Mechanik, fünf Saisons.

## 7b. Wie viele Events pro Jahr — und die Falle dahinter

**Empfehlung: zwei große plus zwei kleine im ersten Jahr.** Das ist jetzt belegt
statt geschätzt:

- **InnoGames** hat die Kehrtwende dokumentiert: *„I used to firmly believe that
  you needed an event every day, but… synergy and rhythm are more important than
  sheer coverage."* Heutige Zielgröße für große Beats: **drei bis vier pro Jahr**,
  je 4–8 Wochen — mit einem Vollzeitteam.
- **Fortnite** fährt seit neun Jahren exakt **zwei** unstrittige Jahresanker:
  Fortnitemares und Winterfest, lückenlos 2017–2025.
- **Genshin** hält seit 2020 exakt 42 Tage pro Version mit einem Flagship-Event
  pro Version.

Die einzige harte Zahl zu Teamgröße kommt von der GDC 2017: Space Ape fuhr
*wöchentliche* Events mit einem Vierer-Team — allerdings mit fertiger
Live-Ops-Tooling-Infrastruktur in einem 100-Personen-Studio. **Vier Leute schaffen
wöchentliche Events erst, nachdem das Tooling steht.** Genau deshalb steht in
diesem Plan das Tooling (Schritte 1–3) vor der Frequenz.

### Drei Fallstricke, die teuer sind

**Kadenz ist ein Einwegversprechen.** Epic stellte Fortnite-Updates von
wöchentlich auf zweiwöchentlich um, um Crunch zu lindern — *„But the situation did
not improve."* Aus zwölf Interviews bei Polygon: *„Crunch never ends in a live
service game like that."* Frequenz lässt sich leicht erhöhen und praktisch nicht
mehr senken, ohne dass es als Rückschritt gelesen wird. **Fangt niedrig an.**

**Das Einmal-Spektakel tötet das Format.** Donald Mustard zur Einstellung der
Fortnite-Live-Events, gefragt nach dem Grund: *„It was resources."* Und: *„the
event team was starting to be needed to work on other stuff."* Der Kontrast ist
messbar — Galactus war **zehn Minuten Inhalt** mit rund sechs Monaten Vorlauf,
gegen 2–4 Wochen Laufzeit bei jedem wiederkehrenden Feiertagsevent. Für ein Team
eurer Größe ist das Einmal-Spektakel die teuerste denkbare Kategorie.

**Drei Monate Planungshorizont, nicht mehr.** Grant Shonkwiler, ehemals
Fortnite-Producer bei Epic: *„a feature level schedule for the next 3 months. Why
3 months? Because I have found anything more than that is a waste of time, it
won't survive contact with players OR devs."* Ergänzend Barnes: *„Liveops should
be finished weeks before they go live, and should go through QA. The cost of being
late on a liveops or having bugs is huge."*

Auf eure Termine übersetzt: Winter geht am 01.12. live, ist also **Mitte November
fertig** — nicht am 30.11.

## 8. Offene Punkte

1. **Lizenzfrage Falco ↔ Titan.** Falco ist AGPL-3.0 ohne Linking-Ausnahme,
   Titan veröffentlicht unter Apache-2.0. Ein Minecraft-Server ist ein
   Netzwerkdienst — genau der AGPL-§13-Fall. Zu entscheiden, **bevor** der
   Falco-Umstieg beginnt. Siehe Spec, Abschnitt 7.
2. **Saisonale Resource Packs — ja oder nein?** Aves bringt die Bausteine mit.
   Die Entscheidung betrifft auch den Umgang mit Spielern ohne Pack und mit
   Bedrock-Spielern (siehe 4b) und gehört vor Schritt 3 getroffen.
3. **Wird `titan-api` publiziert?** Der `SeasonalContent`-Contract gehört ins
   `:api`-Modul. Das trägt derzeit kein `maven-publish` (siehe
   `olf-minestom-project-standard.md`, offener Punkt 3).
4. **Wann läuft die Extension-Migration?** Siehe Abschnitt 3a — die Antwort
   entscheidet, ob Schritt 3 dieses Plans für 2026 stattfindet oder auf 2027
   verschoben wird.

### Wo die Recherche nichts hergab

Ehrlichkeitshalber, damit niemand diese Fragen für beantwortet hält: Kein Netzwerk
hat je CDN-Wahl, Packgrößen, Downloadzeiten oder Spieler-Abbruchquoten
veröffentlicht. Zu Mineplex-Postmortems und zur Technik von Hive und GommeHD gibt
es nichts Verifizierbares — die entsprechenden Seiten blockieren automatisierte
Abrufe. Und es existiert **kein** öffentliches Datenbankschema eines großen
Netzwerks; die Cosmetics-Empfehlungen stammen aus quelloffenen Plugins und einem
Minestom-Backend, nicht von einem Netzwerk eurer Zielgröße.

---

## Quellen

**Gemessene Telemetrie** (Monatswerte durchschnittlicher gleichzeitiger Spieler):
[Deep Rock Galactic](https://steamcharts.com/app/548430) · [Dead by Daylight](https://steamcharts.com/app/381210) · [Destiny 2](https://steamcharts.com/app/1085660) · [Team Fortress 2](https://steamcharts.com/app/440) · [Warframe](https://steamcharts.com/app/230410) · [Sea of Thieves](https://steamcharts.com/app/1172620) · [Rust](https://steamcharts.com/app/252490)

**Zielgruppe:** [JIM-Studie 2025, mpfs](https://mpfs.de/studie/jim-studie-2025/) ([PDF](https://mpfs.de/app/uploads/2025/11/JIM_2025_PDF_barrierearm.pdf))

**Ferien:** [schulferien.org 2026/27](https://www.schulferien.org/deutschland/ferien/2026-2027/) · [KMK](https://www.kmk.org/service/ferien.html)

**Fehlerbilder:** [Kotaku zu Halo Winter Contingency](https://kotaku.com/halo-infinite-s-winter-event-unfortunately-demands-a-bi-1848254025) · [Blizzard zu Winter Wonderland 2023](https://overwatch.blizzard.com/en-us/news/24033785/a-flurry-of-fun-returns-to-overwatch-2-winter-wonderland-begins-december-19/)

**Beteiligung:** [Nielsen, 90-9-1](https://www.nngroup.com/articles/participation-inequality/)

**Architektur:** [Martin Fowler, Feature Toggles](https://martinfowler.com/articles/feature-toggles.html) · [Togglz Activation Strategies](https://www.togglz.org/documentation/activation-strategies) · [Togglz State Repositories](https://www.togglz.org/documentation/repositories)

**Verknappung (Kaufabsicht, nicht Bindung):** [Scarcity tactics in marketing, J. Retailing 2022](https://doi.org/10.1016/j.jretai.2022.06.003) · [Meta-analysis on product scarcity, Psych. & Marketing 2023](https://doi.org/10.1002/mar.21816) — letztere findet, dass **rein zeitliche** Verknappung der *schwächste* Hebel ist; soziale Sichtbarkeit wirkt stärker.

**Im Code verifiziert:** `MapPool.peekMap()`, `TitanFeatures`, `SingletonFeatureManagerProvider`, sowie `togglz-core-4.6.2.jar` per `javap` (`ReleaseDateActivationStrategy` ohne End-/Zeitzonenparameter; `FileBasedStateRepository(File)` delegiert mit 1000 ms — der Reload funktioniert bereits).
