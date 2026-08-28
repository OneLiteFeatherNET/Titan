# Anforderungen: Titan-Lobby — Saison, Zeit und gestufte Auslieferung

**Verantwortlichkeiten:** Konzept: @TheMeinerLP · Anforderungen gepflegt von: @TheMeinerLP
**Stand:** 21.08.2026
**Projekttyp:** Technisches Projekt (Rolle „Als Entwickler/Betreiber", ergänzt um Spielersicht wo das Verhalten sichtbar ist)
**Baut auf:** [`olf-minestom-project-standard.md`](olf-minestom-project-standard.md) · [`event-modi-plan.md`](event-modi-plan.md)

---

## 1. Kontext & Ausgangslage

Titan ist der Lobby-Server des OneLiteFeather-Netzwerks auf Minestom. Er lädt
Welten heute über Minestoms `AnvilLoader` und wählt die aktive Lobby-Welt über
die System-Property `TITAN_LOBBY_MAP`. Ein Wert `halloween` ist im `setup`-Modul
bereits als Default verdrahtet — der Mechanismus für saisonale Lobbys existiert
also im Ansatz, aber ohne Zeitplan, ohne Rückfallebene und ohne Möglichkeit,
etwas zuerst nur einem Teil der Spieler zu zeigen.

Gleichzeitig ist mit **Falco** eine eigene Engine entstanden, die Minestoms
Chunk-Loader und Lichtsystem ersetzt und deren `falco-instance` einen eigenen
Entladepfad mitbringt. Titan soll darauf wechseln — sowohl weil die eigene Engine
gepflegt wird, als auch weil die Lichtsteuerung Voraussetzung für die geplante
Echtzeit-Tageszeit ist.

Das Vorhaben bündelt sechs Anforderungen, die technisch dieselbe Grundlage
brauchen:

1. Falco als Chunk-Loader und Lichtsystem statt Minestoms Standardimplementierung
2. Saisonale Lobbys für Halloween und Winter, vorbereitet für weitere Spielmodi
3. Jahreszeiten und eine an Berlin gekoppelte Echtzeit-Tageszeit
4. Gestufte Auslieferung: interne Tests → Lite-Spieler → allgemeine Freigabe
5. Anzeige der Build-Server für berechtigte Teammitglieder
6. Portale zum Serverwechsel (optional, spätere Stufe)

Resource Packs sind ausdrücklich **später** vorgesehen und in dieser Fassung nur
als Ausbaustufe umrissen, nicht ausspezifiziert.

### Was der Research dazu beigetragen hat

Zwei Befunde aus [`event-modi-plan.md`](event-modi-plan.md) prägen diese
Anforderungen und sind der Grund für einige Zuschnitte, die sonst willkürlich
wirken würden:

- **Deko allein bewegt keine Spielerzahlen** (Deep Rock Galactic: kosmetische
  Oktober-Events −0,37 % / −12,16 % / −4,92 %; inhaltliche Seasons +154 % /
  +59 % / +142 %). Deshalb ist der Aufwand für saisonale Optik in dieser Spec
  strikt auf „Daten statt Code" gedeckelt.
- **Wiederverwendung ist die Architektur, nicht die Sparmaßnahme.** InnoGames zu
  ihrem Saison-Event in der sechsten Iteration: Währung und Laufzeit blieben
  konstant, „leaving everything else open to change". Übertragen: Zeitfenster,
  Zielgruppen-Stufen und Welt-Auswahl sind stabiler Code; alles Saisonale sind
  austauschbare Daten.

---

## 2. Ziele & Nicht-Ziele

**Ziele:**

* Titan lädt Welten über Falco statt über Minestoms `AnvilLoader` und steuert
  Licht über `falco-light`.
* Eine saisonale Lobby lässt sich ohne Codeänderung ausrollen — neue Welt, neue
  Konfiguration, fertig.
* Die Lobby zeigt Jahreszeit und Tageszeit passend zu Berlin, ohne dass jemand
  etwas von Hand umstellt.
* Jedes Feature kann zuerst intern, dann für Lite-Spieler, dann für alle
  freigegeben werden — über **einen** Mechanismus, nicht drei.
* Berechtigte Teammitglieder sehen die Build-Server im Navigator; alle anderen
  sehen sie nicht.
* Der jeweils erreichte Rollout-Stand ist dokumentiert und nachvollziehbar.

**Nicht-Ziele:**

* **Kein** Resource-Pack-System in dieser Stufe (Stufe 6 umreißt es, mehr nicht).
* **Kein** Cosmetics- oder Belohnungssystem.
* **Keine** neuen Minispiele — die Lobby wird darauf *vorbereitet*, sie bekommt
  sie nicht.
* **Keine** eigene Wetter-Simulation. Jahreszeit steuert Optik und Daten, nicht
  Niederschlagslogik.
* **Kein** A/B-Testing. Die Rollout-Stufen sind sequenziell, nicht parallel.

---

## 3. Stakeholder & Rollen

| Rolle | Person | Interesse |
|---|---|---|
| Maintainer Titan | @TheMeinerLP | Architektur, Reviews, Deployment |
| Maintainer Falco | @TheMeinerLP | API-Stabilität der Engine, AGPL-Frage |
| Buildteam | (offen) | Liefert Saison-Welten, braucht klaren Übergabeweg |
| Betreiber | @TheMeinerLP | CloudNet-Templates, Rollout, Notausschalter |
| Lite-Spieler | — | Frühzugang als Gegenleistung für den Rang |
| Spieler | — | Funktionierende Lobby, keine halben Features |

---

## 4. Ausbaustufen-Übersicht

| Stufe | Kurzbeschreibung | Priorität |
|---|---|---|
| **Stufe 1** | Fundament: Falco-Umstieg, Welt-Auswahl absichern | Must |
| **Stufe 2** | Zeit: Jahreszeiten + Echtzeit-Tageszeit Berlin | Must |
| **Stufe 3** | Freigabe-Stufen: intern → Lite → GA | Must |
| **Stufe 4** | Saison-Pakete: Halloween und Winter als Daten | Should |
| **Stufe 5** | Build-Server im Navigator | Should |
| **Stufe 6** | Resource Packs pro Saison | Could |
| **Stufe 7** | Portale zum Serverwechsel | Could |

Die Reihenfolge ist keine Empfehlung, sondern eine Abhängigkeitskette: Stufe 3
braucht Stufe 1 (Gate greift auf die Welt-Auswahl zu), Stufe 4 braucht Stufe 2
und 3.

---

## 5. Nicht-funktionale Anforderungen

| ID | Kategorie | Anforderung (EARS) | Priorität |
|---|---|---|---|
| NFR-001 | Kompatibilität | The Lobby shall mit der Minestom-Version kompatibel bleiben, die der `aonyx-bom` vorgibt. | Must |
| NFR-002 | Betrieb | If eine konfigurierte Saison-Welt beim Start nicht vorhanden ist, then shall die Lobby mit der Standardwelt starten und eine Warnung mit dem gesuchten Weltnamen protokollieren. | Must |
| NFR-003 | Betrieb | The Lobby shall ohne installiertes Saison-Paket vollständig funktionsfähig starten. | Must |
| NFR-004 | Betrieb | The Ausrollen einer Saison shall keinen Neustart der CloudNet-Node oder der Bridge erfordern. | Must |
| NFR-005 | Sicherheit | If ein Spieler ohne die erforderliche Berechtigung ein Build-Server-Ziel anwählt, then shall die Lobby den Wechsel verweigern und das Ziel gar nicht erst anzeigen. | Must |
| NFR-006 | Korrektheit | The Zeitsteuerung shall Zeitzonen- und Sommerzeitwechsel für `Europe/Berlin` korrekt behandeln, ohne dass ein Neustart nötig wird. | Must |
| NFR-007 | Testbarkeit | The Zeitsteuerung shall über eine injizierte `Clock` testbar sein, sodass Tests kein Warten auf reale Zeit benötigen. | Must |
| NFR-008 | Performance | While die Lobby läuft, shall das Setzen der Tageszeit keine spürbare Tick-Verzögerung verursachen (Aktualisierung höchstens einmal pro Sekunde, nicht pro Tick). | Should |
| NFR-009 | Wartbarkeit | The Anzahl der Konstanten in `TitanFeatures` shall zwölf nicht überschreiten; der Build shall bei Überschreitung fehlschlagen. | Should |
| NFR-010 | Wartbarkeit | If ein Saison-Paket sein hinterlegtes Enddatum um mehr als zwölf Monate überschritten hat, then shall der Build fehlschlagen. | Should |
| NFR-011 | Dokumentation | The Rollout-Stand jedes Features shall in einem versionierten Dokument nachvollziehbar sein (Stufe, Datum, Verantwortlicher). | Must |
| NFR-012 | Lizenz | The Lobby shall keine Abhängigkeit einbinden, deren Lizenz mit der veröffentlichten Lizenz von Titan unvereinbar ist. | Must |
| NFR-013 | Betrieb | When ein Saison-Zeitfenster endet, shall der Übergang gestaffelt erfolgen, sodass nicht alle Spieler gleichzeitig transferiert werden. | Should |

---

## 6. User Stories

### Stufe 1 — Fundament

| ID | Story | Akzeptanzkriterium (EARS) | Schnittstelle | Priorität | Status |
|---|---|---|---|---|---|
| US-1.01 | Als Betreiber möchte ich Welten über Falco laden, damit wir unsere eigene Engine nutzen und Ladefehler nicht als „Chunk fehlt" durchgehen. | When eine Instanz erzeugt wird, shall die Lobby einen `FalcoAnvilLoader` als `ChunkLoader` setzen statt Minestoms `AnvilLoader`. | `net.onelitefeather.falco.anvil.FalcoAnvilLoader(Path, Key)` | Must | offen |
| US-1.02 | Als Betreiber möchte ich, dass ein Lesefehler den Chunk nicht stillschweigend neu generiert, damit gebaute Welten nicht überschrieben werden. | If ein Chunk nicht gelesen werden kann, then shall der Ladevorgang eine Ausnahme werfen und der Chunk shall nicht neu generiert werden. | `AnvilFault`, `ChunkDataException` | Must | offen |
| US-1.03 | Als Betreiber möchte ich Licht über `falco-light` steuern, damit die Lobby vollständig ausgeleuchtet ist und die Tageszeit später korrekt wirkt. | When ein Chunk geladen wird, shall die Lobby dessen Licht über `ChunkLightService` berechnen. | `ChunkLightService`, `ChunkLightScheduler` | Must | offen |
| US-1.04 | Als Betreiber möchte ich bei falsch gesetzter Welt-Property eine verständliche Meldung, damit ein Tippfehler kein Rätsel ist. | If die über `TITAN_LOBBY_MAP` benannte Welt nicht existiert, then shall die Lobby den gesuchten Namen und die gefundenen Welten protokollieren und mit der Standardwelt starten. | `MapPool.peekMap()` | Must | offen |
| US-1.05 | Als Entwickler möchte ich, dass die Welt-Property immer gilt, damit sich lokal und in Produktion nichts unterschiedlich verhält. | The Welt-Auswahl shall die Property unabhängig von der Anzahl vorhandener Welten auswerten. | `MapPool.peekMap()` | Must | offen |
| US-1.06 | Als Betreiber möchte ich Welten pro Saison als eigenes Verzeichnis ablegen, damit der Wechsel ohne Codeänderung möglich ist. | The Lobby shall die aktive Welt aus einem Verzeichnis unter `worlds/` laden, dessen Name konfigurierbar ist. | `worlds/<name>/` | Must | offen |

### Stufe 2 — Jahreszeiten und Echtzeit-Tageszeit

**Entwurfsentscheidung: beide Abbildungen sind Strategien.** Sowohl die
Uhrzeit-Abbildung als auch die Jahreszeitgrenzen werden als
Strategy-Pattern gebaut. Das erlaubt, Variante A und B gegeneinander zu testen
und B später nachzuliefern, ohne den Aufrufcode anzufassen. Details in
Abschnitt 6a.

| ID | Story | Akzeptanzkriterium (EARS) | Schnittstelle | Priorität | Status |
|---|---|---|---|---|---|
| US-2.01 | Als Spieler möchte ich, dass die Lobby-Tageszeit meiner echten Tageszeit entspricht, damit sich die Welt lebendig anfühlt. | While die Lobby läuft, shall die Tageszeit der Instanz der aktuellen Uhrzeit in `Europe/Berlin` entsprechen. | `Instance#setTime`, `setTimeRate(0)` | Must | offen |
| US-2.02 | Als Entwickler möchte ich, dass der eingebaute Tageszyklus abgeschaltet ist, damit unsere Zeitsteuerung nicht gegen Minestom arbeitet. | The Lobby shall die Zeitrate der Instanz auf 0 setzen und die Zeit ausschließlich selbst setzen. | `Instance#setTimeRate` | Must | offen |
| US-2.03 | Als Entwickler möchte ich die Zeitquelle austauschen können, damit Tests deterministisch sind. | The Zeitsteuerung shall ihre Zeit aus einer injizierten `java.time.Clock` beziehen und nicht aus `Instant.now()`. | `java.time.Clock` | Must | offen |
| US-2.04 | Als Betreiber möchte ich, dass Sommerzeit korrekt behandelt wird, damit im Oktober nichts um eine Stunde verrutscht. | When die Sommerzeitumstellung in `Europe/Berlin` stattfindet, shall die Lobby-Tageszeit ohne Neustart korrekt weiterlaufen. | `ZoneId.of("Europe/Berlin")` | Must | offen |
| US-2.05 | Als Entwickler möchte ich die Uhrzeit-Abbildung austauschen können, damit wir lineare und astronomische Variante vergleichen können, ohne den Aufrufcode zu ändern. | The Zeitsteuerung shall die Abbildung von Realzeit auf Spielzeit über eine austauschbare Strategie beziehen. | `DayTimeStrategy` | Must | offen |
| US-2.06 | Als Betreiber möchte ich die lineare Abbildung als Standard, damit die Stufe ohne astronomische Berechnung nutzbar ist. | The Lobby shall ohne abweichende Konfiguration die lineare Abbildung verwenden. | `LinearDayTimeStrategy` | Must | offen |
| US-2.07 | Als Betreiber möchte ich später auf die astronomische Abbildung wechseln können, damit im Dezember spät hell wird. | Where die astronomische Strategie konfiguriert ist, shall die Lobby Sonnenauf- und -untergang für Berlin auf die Spielzeit abbilden. | `SolarDayTimeStrategy` | Could | offen |
| US-2.08 | Als Entwickler möchte ich beide Abbildungen gegen dieselben Testfälle prüfen, damit der Vergleich belastbar ist. | The Testsuite shall beide Strategien gegen denselben Satz fester Zeitpunkte prüfen. | Testfall je Strategie | Should | offen |
| US-2.09 | Als Spieler möchte ich, dass die Lobby die aktuelle Jahreszeit widerspiegelt, damit sie sich über das Jahr verändert. | The Lobby shall die aktuelle Jahreszeit aus dem Datum in `Europe/Berlin` ableiten und als Zustand bereitstellen. | `Season`-Enum | Must | offen |
| US-2.10 | Als Entwickler möchte ich die Jahreszeitgrenzen austauschen können, damit wir meteorologische und astronomische Grenzen vergleichen können. | The Jahreszeit-Ermittlung shall über eine austauschbare Strategie erfolgen. | `SeasonBoundaryStrategy` | Must | offen |
| US-2.11 | Als Betreiber möchte ich meteorologische Grenzen als Standard, weil sie auf feste Monatsanfänge fallen und keine Berechnung brauchen. | The Lobby shall ohne abweichende Konfiguration meteorologische Jahreszeitgrenzen verwenden (1.3., 1.6., 1.9., 1.12.). | `MeteorologicalSeasonStrategy` | Must | offen |
| US-2.12 | Als Betreiber möchte ich auf astronomische Grenzen wechseln können, damit die Jahreszeit zu den Sonnenwenden passt. | Where die astronomische Strategie konfiguriert ist, shall die Lobby die Jahreszeit anhand von Tagundnachtgleichen und Sonnenwenden bestimmen. | `AstronomicalSeasonStrategy` | Could | offen |
| US-2.13 | Als Betreiber möchte ich eine Jahreszeit fest vorgeben können, damit ein Event unabhängig vom Kalender laufen kann. | Where eine Jahreszeit fest konfiguriert ist, shall die Lobby diese verwenden und keine Strategie befragen. | `FixedSeasonStrategy` | Should | offen |
| US-2.14 | Als Betreiber möchte ich, dass die Zeitaktualisierung günstig ist, damit sie den Tick nicht belastet. | The Zeitsteuerung shall die Tageszeit höchstens einmal pro Sekunde aktualisieren. | Scheduler | Should | offen |

### Stufe 3 — Freigabe-Stufen

| ID | Story | Akzeptanzkriterium (EARS) | Schnittstelle | Priorität | Status |
|---|---|---|---|---|---|
| US-3.01 | Als Entwickler möchte ich ein Feature zuerst nur intern sehen, damit wir es prüfen können, bevor es jemand anders sieht. | Where ein Feature auf Stufe „intern" steht, shall die Lobby es ausschließlich Spielern mit der Berechtigung `titan.feature.internal` zeigen. | `FeatureGate`, LuckPerms | Must | umgesetzt (Navigator) |
| US-3.02 | Als Betreiber möchte ich ein Feature auf Lite-Spieler ausweiten, damit wir es unter Last prüfen und Lite einen Vorteil hat. | Where ein Feature auf Stufe „lite" steht, shall die Lobby es Spielern der LuckPerms-Gruppe `lite` **und** Spielern mit `titan.feature.internal` zeigen. | `FeatureGate`, LuckPerms-Gruppe `lite` | Must | umgesetzt (Navigator) |
| US-3.03 | Als Betreiber möchte ich ein Feature allgemein freigeben, damit alle es sehen. | Where ein Feature auf Stufe „ga" steht, shall die Lobby es allen Spielern zeigen. | `FeatureGate` | Must | umgesetzt (Navigator) |
| US-3.04 | Als Betreiber möchte ich ein Feature sofort abschalten können, damit ein Fehler nicht bis zum nächsten Deployment sichtbar bleibt. | If der Notausschalter eines Features gesetzt ist, then shall die Lobby es unabhängig von Stufe und Zeitfenster niemandem zeigen. | Togglz-Flag | Must | umgesetzt (Navigator) |
| US-3.05 | Als Betreiber möchte ich, dass die Abschaltung ohne Neustart wirkt, damit die Reaktionszeit kurz ist. | When die Flag-Datei geändert wird, shall die Änderung innerhalb von zwei Sekunden wirksam sein. | `FileBasedStateRepository` | Must | umgesetzt |
| US-3.06 | Als Betreiber möchte ich Freigaben zeitlich planen, damit ein Event ohne Nachtschicht startet. | Where für ein Feature ein Zeitfenster konfiguriert ist, shall die Lobby es nur innerhalb dieses Fensters aktivieren. | eigene `ActivationStrategy` | Must | umgesetzt (Navigator) |
| US-3.07 | Als Entwickler möchte ich, dass die Prüfreihenfolge festgelegt ist, damit das Verhalten vorhersagbar bleibt. | The Freigabeprüfung shall in dieser Reihenfolge auswerten: Notausschalter, dann Berechtigungsstufe, dann Zeitfenster. | `FeatureGate` | Must | umgesetzt |
| US-3.08 | Als Betreiber möchte ich den aktuellen Stand im Spiel abfragen, damit ich nicht ins Log schauen muss. | When ein berechtigtes Teammitglied `/season status` ausführt, shall die Lobby je Feature Stufe, Zeitfenster und Notausschalter-Zustand ausgeben. | Command | Should | umgesetzt |
| US-3.09 | Als Betreiber möchte ich jeden Stufenwechsel dokumentiert haben, damit nachvollziehbar ist, wann was freigegeben wurde. | When ein Feature die Stufe wechselt, shall der Wechsel mit Zeitpunkt, alter und neuer Stufe protokolliert werden. | Log + `docs/rollout-log.md` | Must | umgesetzt |

### Stufe 4 — Saison-Pakete

| ID | Story | Akzeptanzkriterium (EARS) | Schnittstelle | Priorität | Status |
|---|---|---|---|---|---|
| US-4.01 | Als Betreiber möchte ich eine Saison als eigenes Paket ausliefern, damit sie ohne Kernänderung kommt und geht. | The Lobby shall Saison-Inhalte aus einem separat deploybaren Paket laden. | `SeasonalContent`-Contract | Should | offen |
| US-4.02 | Als Entwickler möchte ich, dass ein Paket sich vollständig zurückbaut, damit nach Saisonende keine Reste bleiben. | When ein Saison-Paket deaktiviert wird, shall es alle von ihm gesetzten Blöcke, Anzeigen und geplanten Aufgaben entfernen. | `SeasonalContent#deactivate` | Must | offen |
| US-4.03 | Als Betreiber möchte ich saisonale Werte ohne Neubau ändern, damit Textänderungen kein Deployment brauchen. | The Saison-Inhalte shall Materialien, Texte, Positionen und Zeitfenster aus einer Konfigurationsdatei beziehen. | JSON im Paket | Should | offen |
| US-4.04 | Als Entwickler möchte ich, dass ein unbekannter Effekt-Typ beim Übersetzen auffällt, nicht im Betrieb. | If eine Saison-Konfiguration einen unbekannten Effekt-Typ enthält, then shall das Laden mit einer benannten Fehlermeldung fehlschlagen. | `sealed interface SeasonEffect` | Should | offen |
| US-4.05 | Als Betreiber möchte ich bei zwei gleichzeitigen Paketen eine feste Reihenfolge, damit das Ergebnis nicht von der Ladereihenfolge abhängt. | Where mehrere Saison-Pakete gleichzeitig aktiv sind, shall die Lobby sie nach einem im Paket hinterlegten Prioritätswert anwenden. | Paket-Manifest | Should | offen |
| US-4.06 | Als Betreiber möchte ich, dass ein Paket nicht ein anderes voraussetzt, damit Deployment-Reihenfolgen egal sind. | The Saison-Pakete shall einander nicht direkt referenzieren. | ArchUnit-Regel | Must | offen |
| US-4.07 | Als Betreiber möchte ich vor dem Livegang sehen, wie es aussieht, ohne dass Spieler es sehen. | Where ein Spieler die Berechtigung `titan.season.preview` hat, shall die Lobby ihm Saison-Inhalte auch außerhalb des Zeitfensters zeigen. | `FeatureGate` | Should | offen |
| US-4.08 | Als Betreiber möchte ich beim Reaktivieren einer alten Saison Gewissheit, dass sie noch funktioniert. | Before eine Saison erneut aktiviert wird, shall ein Testlauf ihrer Kernpfade erfolgreich durchlaufen sein. | Testfall je Paket | Should | offen |

### Stufe 5 — Build-Server im Navigator

| ID | Story | Akzeptanzkriterium (EARS) | Schnittstelle | Priorität | Status |
|---|---|---|---|---|---|
| US-5.01 | Als Teammitglied möchte ich die Build-Server im Navigator sehen, damit ich ohne Befehl dorthin komme. | Where ein Spieler die Berechtigung `titan.navigator.buildserver` hat, shall der Navigator die verfügbaren Build-Server als Ziele anzeigen. | `NavigationHelper` | Should | offen |
| US-5.02 | Als Betreiber möchte ich, dass Spieler ohne Berechtigung diese Ziele gar nicht sehen, damit ihre Existenz nicht verrät, dass es sie gibt. | If ein Spieler die Berechtigung nicht hat, then shall der Navigator die Build-Server-Einträge weder anzeigen noch ihren Platz freihalten. | `NavigationHelper` | Must | offen |
| US-5.03 | Als Betreiber möchte ich, dass die Berechtigung auch beim Wechsel geprüft wird, damit ein manipulierter Klick nichts bewirkt. | When ein Wechsel zu einem Build-Server angefordert wird, shall die Lobby die Berechtigung erneut prüfen, bevor sie den Spieler weiterleitet. | `Deliver` | Must | offen |
| US-5.04 | Als Teammitglied möchte ich sehen, welche Build-Server gerade laufen, damit ich nicht auf einen gestoppten klicke. | The Navigator shall nur Build-Server anzeigen, die zum Zeitpunkt des Öffnens als erreichbar gemeldet sind. | CloudNet-Dienstliste | Should | offen |

### Stufe 6 — Resource Packs (später)

Bewusst grob gehalten. Ausspezifizierung erst, wenn Stufe 1–4 stehen.

| ID | Story | Akzeptanzkriterium (EARS) | Schnittstelle | Priorität | Status |
|---|---|---|---|---|---|
| US-6.01 | Als Spieler möchte ich saisonale Texturen sehen, ohne bei jedem Serverwechsel neu zu laden. | The Lobby shall ein Basis-Paket und ein Saison-Paket mit getrennten Kennungen ausliefern. | `resource_pack_push` | Could | offen |
| US-6.02 | Als Betreiber möchte ich beim Saisonwechsel nur das Saison-Paket tauschen. | When die Saison wechselt, shall die Lobby ausschließlich das Saison-Paket entfernen und ersetzen, nicht alle Pakete. | `resource_pack_pop(uuid)` | Could | offen |
| US-6.03 | Als Betreiber möchte ich, dass ein hängender Client den Verbindungsaufbau nicht blockiert. | If ein Client nicht innerhalb einer konfigurierten Frist auf die Paketanfrage antwortet, then shall die Lobby fortfahren statt zu warten. | eigener Timeout | Could | offen |
| US-6.04 | Als Betreiber möchte ich Bedrock-Spieler korrekt behandeln, weil deren gemeldeter Status nicht zutrifft. | If ein Spieler über Geyser verbunden ist, then shall die Lobby ihn nicht anhand des gemeldeten Paketstatus bewerten. | Bedrock-Erkennung | Could | offen |

### Stufe 7 — Portale (optional)

| ID | Story | Akzeptanzkriterium (EARS) | Schnittstelle | Priorität | Status |
|---|---|---|---|---|---|
| US-7.01 | Als Spieler möchte ich durch ein Portal auf einen anderen Server wechseln, statt einen Navigator zu öffnen. | When ein Spieler einen als Portal definierten Bereich betritt, shall die Lobby ihn an den hinterlegten Zielserver weiterleiten. | `Deliver`, Bereichsprüfung | Could | offen |
| US-7.02 | Als Betreiber möchte ich Portale ohne Codeänderung definieren. | The Portale shall aus einer Konfigurationsdatei mit Bereich und Zielserver geladen werden. | JSON | Could | offen |
| US-7.03 | Als Betreiber möchte ich, dass ein Portal mit unerreichbarem Ziel den Spieler nicht ins Leere schickt. | If der Zielserver eines Portals nicht erreichbar ist, then shall die Lobby den Spieler an Ort und Stelle lassen und ihm eine Meldung anzeigen. | `Deliver` | Could | offen |
| US-7.04 | Als Betreiber möchte ich, dass Portale denselben Berechtigungsregeln folgen wie der Navigator. | Where ein Portal ein berechtigungspflichtiges Ziel hat, shall dieselbe Prüfung gelten wie für das entsprechende Navigator-Ziel. | `FeatureGate` | Could | offen |

---

## 7. Offene Fragen / Risiken

| Frage/Risiko | Auswirkung | Verantwortlich | Status |
|---|---|---|---|
| ~~Lizenzkonflikt Falco ↔ Titan~~ — **entschieden am 21.08.2026: Titan wechselt auf AGPL-3.0.** `LICENSE`, `header.java`, alle 97 getrackten Java-Header sowie die POM-Angaben in `app`, `setup` und `bridge` sind umgestellt. | Erledigt. Der Blocker für Stufe 1 ist damit weg. | @TheMeinerLP | **erledigt** |
| **Zustimmung der Mitautoren zum Lizenzwechsel.** Titan hat neben @TheMeinerLP weitere Urheber: theEvilReaper (~70 Commits), Joltras (~18), Yannick Lamprecht (2), dazu OLF-Organisationsaccounts. Ein Wechsel von Apache-2.0 auf AGPL-3.0 ändert die Bedingungen, unter denen deren Beiträge weitergegeben werden. | Ohne dokumentierte Zustimmung bleibt der Wechsel rechtlich angreifbar. Praktisch lösbar durch eine kurze schriftliche Bestätigung der Mitautoren, im Repository abgelegt. | @TheMeinerLP | **offen** |
| Falco ist als `@ApiStatus.Experimental` markiert; Signaturen können sich in einer Minor-Version ändern. | Umstellungsaufwand bei Falco-Updates | @TheMeinerLP | offen |
| `falco-instance` kann kein `SharedInstance` tragen. | Falls die Lobby mehrere Instanzen auf denselben Chunks braucht, entfällt dieses Modul (`falco-anvil` und `falco-light` sind davon nicht betroffen). | @TheMeinerLP | offen |
| Zeitpunkt der Extension-Umstellung aus dem OLF-Standard. | Fällt sie in Oktober/November, kollidiert sie mit dem Event — siehe `event-modi-plan.md`, Abschnitt 3a | @TheMeinerLP | offen |

---

## 6a. Entwurf: zwei Strategien für Zeit und Jahreszeit

**Entschieden:** Uhrzeit-Abbildung und Jahreszeitgrenzen werden beide als
Strategy-Pattern gebaut. Der Aufrufcode kennt nur die Schnittstelle; welche
Ausprägung läuft, entscheidet die Konfiguration. Damit lassen sich beide Varianten
gegeneinander testen, und die aufwendigere kommt später — ohne Umbau.

### Tageszeit

```java
/**
 * Bildet einen Zeitpunkt der Realwelt auf die Tageszeit einer Minecraft-Welt ab.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.11.0
 */
public interface DayTimeStrategy {

    /** Ein voller Minecraft-Tag in Ticks. */
    int TICKS_PER_DAY = 24_000;

    /**
     * @param instant der Zeitpunkt, für den die Tageszeit gilt
     * @param zone    die Zeitzone, gegen die gerechnet wird
     * @return die Tageszeit in Ticks, im Bereich [0, {@value #TICKS_PER_DAY})
     */
    @Contract(pure = true)
    long ticksAt(Instant instant, ZoneId zone);
}
```

| Ausprägung | Verhalten | Stufe |
|---|---|---|
| `LinearDayTimeStrategy` | 24 reale Stunden gleichmäßig auf 24 000 Ticks; 12:00 Uhr ergibt Mittag | **Standard**, Stufe 2 |
| `SolarDayTimeStrategy` | Sonnenauf- und -untergang für Berlin auf die Spielzeit gelegt; im Dezember spät hell | Could, später |

Die lineare Variante ist bewusst der Standard: Sie liefert den Nutzen fast
vollständig und hat keinen Berechnungsfehler, den man übersehen könnte. Die
astronomische ist die Verfeinerung — in Berlin schwankt der Sonnenaufgang
zwischen etwa 4:45 Uhr im Juni und 8:15 Uhr im Dezember, und genau das macht sie
sichtbar.

**Was der Vergleich prüfen muss** (US-2.08): Beide Ausprägungen werden gegen
denselben Satz fester Zeitpunkte getestet — Sonnenwenden, Tagundnachtgleichen,
beide Sommerzeitumstellungen und ein gewöhnlicher Tag. Da die Zeitquelle eine
injizierte `Clock` ist, braucht kein Test reale Zeit.

### Jahreszeit

```java
/**
 * Bestimmt, welche Jahreszeit an einem Datum gilt.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.11.0
 */
public interface SeasonBoundaryStrategy {

    /**
     * @param date das Datum in der Redaktionszeitzone
     * @return die an diesem Tag geltende Jahreszeit
     */
    @Contract(pure = true)
    Season seasonAt(LocalDate date);
}
```

| Ausprägung | Grenzen | Stufe |
|---|---|---|
| `MeteorologicalSeasonStrategy` | feste Monatsanfänge: 1.3., 1.6., 1.9., 1.12. | **Standard**, Stufe 2 |
| `AstronomicalSeasonStrategy` | Tagundnachtgleichen und Sonnenwenden (um den 20./21.) | Could, später |
| `FixedSeasonStrategy` | gibt immer dieselbe Jahreszeit zurück | Should — Tests, Vorschau, Events außerhalb des Kalenders |

Meteorologisch ist der Standard, weil die Grenzen auf feste Kalendertage fallen
und keine Berechnung brauchen. Der Unterschied zur astronomischen Variante
beträgt rund drei Wochen — spürbar, aber kein Grund, die Berechnung in Stufe 2 zu
ziehen.

`FixedSeasonStrategy` ist nicht nur ein Testhilfsmittel: Sie ist der Weg, ein
Winter-Event im August vorzuführen, ohne an der Systemuhr zu drehen.

### Warum Strategy und nicht Konfigurationsschalter

Ein `if (astronomisch) … else …` an der Abbildungsstelle hätte denselben Effekt
und wäre kürzer. Drei Gründe sprechen dagegen:

1. **Der Vergleich ist der Zweck.** Beide Varianten sollen gegeneinander getestet
   werden. Als eigene Typen sind sie einzeln instanziierbar und einzeln testbar;
   als Zweig einer Bedingung nicht.
2. **Die astronomische Variante bringt eigene Abhängigkeiten mit** (Sonnenstand,
   geografische Position). Die gehören in ihre Klasse, nicht in den gemeinsamen
   Pfad.
3. **`FixedSeasonStrategy` fällt gratis ab.** Bei einer Bedingung wäre die
   Vorschaufunktion ein dritter Zweig; als Strategie ist sie eine zehnzeilige
   Klasse.

Beide Schnittstellen sind bewusst zustandslos und `@Contract(pure = true)` — sie
bekommen den Zeitpunkt übergeben, statt selbst auf die Uhr zu sehen. Die
`Clock` sitzt in der aufrufenden Zeitsteuerung (US-2.03), nicht in den Strategien.

---

## 8. Abnahmekriterien

- [x] Die Lizenzfrage Falco ↔ Titan ist entschieden: Titan steht unter AGPL-3.0 (21.08.2026).
- [ ] Die Zustimmung der Mitautoren zum Lizenzwechsel liegt schriftlich vor und ist im Repository abgelegt.
- [ ] Die Lobby lädt Welten über `FalcoAnvilLoader`; Minestoms `AnvilLoader` wird nicht mehr verwendet.
- [ ] Ein Lesefehler an einem Chunk führt zu einer Ausnahme, nicht zu einem neu generierten Chunk.
- [ ] Ein falsch gesetztes `TITAN_LOBBY_MAP` startet die Lobby mit der Standardwelt und protokolliert den gesuchten Namen.
- [ ] Die Tageszeit der Lobby entspricht der Uhrzeit in Berlin, auch über eine Sommerzeitumstellung hinweg.
- [ ] Die Zeitsteuerung ist mit einer festen `Clock` testbar; ein Test prüft Winter im Sommer.
- [x] Ein Feature lässt sich nacheinander auf intern, lite und ga stellen, ohne dass Code geändert wird.
- [x] Der Notausschalter wirkt innerhalb von zwei Sekunden und schlägt Stufe und Zeitfenster. — *Einschränkung: die Prüfung erfolgt beim Zeichnen des Menüs. Wer den Navigator bereits offen hat, sieht das alte Bild bis zum nächsten Öffnen. Ein abgelehnter Eintrag bekommt keinen Klick-Handler, und `InventoryPreClickEvent` wird global abgebrochen — das Fenster ist also eng, aber vorhanden. Eine Prüfung zur Klickzeit gehört zu `NavigatorEntry` aus Stufe 5.*
- [ ] Ein Spieler ohne `titan.navigator.buildserver` sieht die Build-Server nicht und kann sie auch durch einen manipulierten Klick nicht erreichen.
- [ ] Die Lobby startet ohne Saison-Paket vollständig funktionsfähig.
- [ ] Ein Saison-Paket lässt sich entfernen, ohne dass Reste in der Welt zurückbleiben.
- [ ] Der Rollout-Stand jedes Features ist in `docs/rollout-log.md` nachvollziehbar.

---

## 9. Was diese Spec bewusst offen lässt

Damit niemand diese Punkte für vergessen hält:

- **Der Inhalt der Saisons.** Diese Spec beschreibt das Gerüst, nicht das
  Halloween-Event selbst. Was Spieler tun können, ist ein eigenes Konzept — und
  laut Research der Teil, der tatsächlich Wirkung hat.
- **Das Übergabeformat vom Buildteam.** Die Empfehlung aus dem Research (eine
  `config.yml` im Weltordner, wie BlueDragonMC es macht) ist noch nicht als
  Anforderung formuliert, weil das Buildteam dazu gehört werden sollte.
- **Cosmetics und Belohnungen.** Ausdrücklich Nicht-Ziel.
- **Die Frage, ob `titan-api` publiziert wird.** Betrifft den
  `SeasonalContent`-Contract aus Stufe 4; siehe
  [`olf-minestom-project-standard.md`](olf-minestom-project-standard.md).
