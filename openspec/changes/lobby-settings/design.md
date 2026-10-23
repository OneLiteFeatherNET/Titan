# Design

## Context

Eine Column ist ein `@Singleton` unter `features/<name>`, das nur von `core` abhängt; `start()` hängt seinen `FeatureNode` an den `titan`-Knoten, `stop()` hängt ihn ab. Die Plattform liefert `Scheduler`, `Clock`, `CommandManager` und `Telemetry` als Beans. Spieler verbinden sich erst nach `Titan#finishStartup`, der `BeanScope` steht vorher. Registrierungen in `@PostConstruct` liegen also vor dem ersten Login (siehe `lobby-dances`, D1).

Otis (Stand 1.17.0, Commit `d3499b6` im Repository) stellt diese Schnittstelle bereit:

- `GET /v1/players/{uuid}/settings` mit optionalem `?namespace=` (Liste, unpaginiert), `GET|PUT|DELETE /v1/players/{uuid}/settings/{key}`.
- Ein Eintrag ist `{key, value, version, updatedAt}`. `value` ist beliebiges JSON. Die Version ist ein Zähler, der bei jeder Wertänderung um eins steigt, beginnend bei 1. Es gibt kein `ETag` und kein `If-Match`; letzter Schreiber gewinnt.
- Schlüssel `namespace:value`, Namespace `[a-z0-9_.-]`, Wert `[a-z0-9_./-]`, Obergrenze des Werts 65536 Bytes. Otis kennt keine Standardwerte und keine Schlüsselregistry. Unbekannte, aber gültige Schlüssel werden angenommen.
- `PUT` liefert 201 beim Anlegen, 200 bei Änderung oder unverändertem Wert. `DELETE` liefert 204, auch wenn nichts existierte.
- Fehler sind RFC-9457-Problem-Details. Ein unbekannter Spieler liefert 404 (`player-not-found`).
- Die Tabelle `player_setting` entsteht durch Flyway V2. Die Migration ist im Repository vorhanden, der Stand der Produktion ist nicht verifiziert.
- Keine Authentifizierung.

Titan hat heute keine persistierte Spieleroption. Die einzige persistierte Spielerdaten sind die Jump-and-Run-Bestwerte in `jumprun_run` (eigene Tabelle, nicht Einstellung). Es gibt keine HTTP-Client-Abhängigkeit und keine Test-Bibliothek für HTTP-Server.

## Goals / Non-Goals

**Goals**

- Eine kleine, typisierte Schnittstelle, über die jede Column eine Spieleroption anlegt, ohne Otis, HTTP oder Aves zu kennen.
- Ein Menü, das alle registrierten Optionen zeigt; Umschalten wirkt sofort im Speicher und wird im Hintergrund gespeichert.
- Die Lobby läuft ohne Otis weiter: Standardwerte, zuletzt bekannte Werte, keine Blockade.
- Kein Blockieren von Tick-Thread und Beitritt durch Netzwerk-I/O.

**Non-Goals**

- Nicht-boolesche Optionen, Schemaversionen, Schlüsselmigrationen.
- Lokale Persistenz über Prozessgrenzen hinweg (kein Festplatten-Cache).
- Synchronisation zu Velocity oder zu anderen Lobbys in Echtzeit. Ein Spieler ist immer auf einer Lobby; Änderungen werden beim nächsten Laden gesehen.
- Konfliktauflösung mit Otis-Versionen. Siehe D5.

## Decisions

### D1 Core-Schnittstelle und Registry

Im Paket `net.onelitefeather.titan.core.settings` in `core` liegen:

- `SettingKey` (Record `namespace`, `value`): erzeugt aus einem Schlüssel wie `titan:tag.enabled`; prüft die Zeichen aus Otis (Namespace `[a-z0-9_.-]`, Wert `[a-z0-9_./-]`) beim Erzeugen, damit ein falscher Schlüssel beim Start und nicht erst beim Speichern auffällt.
- `BooleanSetting` (Record `key`, `default`, `label`-Schlüssel): der Standardwert ist `OPT_IN` (aus) oder `OPT_OUT` (an). Ein Enum `SettingDefault` mit `OPT_IN` und `OPT_OUT` macht die Semantik sichtbar; eine freie Boolean-Angabe wäre mehrdeutig.
- `PlayerSettings` (Port): `boolean isEnabled(UUID, BooleanSetting)`, `void set(UUID, BooleanSetting, boolean)`, `void addChangeListener(SettingChangeListener)`.
- `SettingsRegistry`: `register(BooleanSetting)` wirft bei doppeltem Schlüssel; `definitions()` liefert die Liste in Registrierungsreihenfolge.

Columns rufen `register` in ihrem `@PostConstruct` auf und halten den Port per Konstruktor-Injektion. Die Column `settings` liest die Registry erst beim Öffnen des Menüs, daher ist die Reihenfolge der `@PostConstruct`-Aufrufe unerheblich.

`isEnabled` liefert nie einen Fehler. Ist die Option unbekannt oder nicht geladen, liefert es den Standardwert.

### D2 Implementierung in `features/settings` (rein, ohne I/O)

`features/settings` enthält:

- `PlayerSettingsService` (implementiert `PlayerSettings`): hält je Spieler eine `Map<SettingKey, Entry>` mit `value`, `version` (letzte bekannte Otis-Version, `0` = unbekannt) und `dirty` (lokal geändert, noch nicht gespeichert). Thread-sicher über `ConcurrentHashMap`. Ruft Listener nach Änderungen auf.
- `SettingsStore` (Port im Core-Paket, siehe D4): `load(UUID)`, `save(UUID, key, value)`. Implementierungen: `InMemorySettingsStore` (Offline) und `OtisSettingsStore` in `platform/otis`.
- `SettingsMenu` (Aves, siehe D6), `SettingsCommand` (`/settings`), `SettingsItem` (Hotbar-Item in Slot 7), `SettingsModule` (`@PostConstruct`/`@PreDestroy`, Lebenszyklus).
- `SettingsTelemetry`, der Zähler und Histogramme kapselt.

Die Logik ist reine Funktion von Zustand und Eingabe: `toggle`, `applyLoaded`, `markSaved`, `shouldFlush`. Sie bekommt Zeit über `Clock` und Verzögerungen über einen injizierten `ScheduledExecutor`-Port, damit Tests ohne Warten laufen.

### D3 Laden beim Beitritt, ohne zu blockieren

Das Laden startet, sobald Minestom den Spieler in der Konfigurationsphase kennt (`AsyncPlayerConfigurationEvent`). Es läuft auf einem eigenen Executor mit virtuellen Threads, nie auf dem Tick-Thread und nie im Beitritts-Thread als Wartezeit. Beim Beitritt sind alle Optionen auf Standard gesetzt; wenn das Laden fertig ist, werden die Werte über den Scheduler auf dem Tick-Thread übernommen (Minestom 26.1: der Scheduler läuft auf `TickSchedulerThread`, nicht auf dem `TickThread`; Inventar-Änderungen gehören deshalb ausdrücklich auf den Tick-Thread).

Regel bei der Übernahme: Ein lokal geänderter Schlüssel (`dirty`) oder ein Schlüssel, der seit Ladebeginn umgeschaltet wurde, bleibt unverändert. Der Otis-Stand gewinnt nur bei unveränderten Schlüsseln. Hat der Spieler inzwischen den Server verlassen, wird das Ergebnis verworfen.

Das Laden holt die Liste mit `?namespace=titan`, nicht einzelne Schlüssel. Eine Liste ist ein Aufruf je Spieler, unabhängig von der Zahl der Optionen.

### D4 Adapter `platform/otis` und Fehlerpfade

`OtisSettingsStore` nutzt `java.net.http.HttpClient` (eine Instanz je Prozess, HTTP/1.1 reicht). Es gibt:

- Timeouts: Verbindungsaufbau `otis.connectTimeoutMillis` (Standard 1000), Anfrage `otis.requestTimeoutMillis` (Standard 2000).
- Wiederholungen: Laden `otis.loadRetries` (Standard 1, nur bei Netzfehlern und 5xx); Schreiben `otis.writeRetries` (Standard 3) mit festem Abstand 500 ms, nur bei Netzfehlern und 5xx. 4xx wird nicht wiederholt; `404 player-not-found` beim Schreiben wird als „später erneut versuchen“ behandelt, weil Velocity den Spieler anlegt (siehe D11).
- Circuit Breaker, selbst gebaut (keine neue Bibliothek): nach `otis.circuitFailureThreshold` (Standard 3) Fehlern in Folge ist er offen; Anfragen werden für `otis.circuitOpenMillis` (Standard 30000) nicht gesendet. Danach geht er in einen Halb-offen-Zustand, in dem genau eine Anfrage erlaubt ist. Die Zeit kommt aus `Clock`.
- Fallback: Ist der Breaker offen oder schlägt das Laden fehl, bleiben die Standardwerte gesetzt. Bereits geladene Werte im Prozess bleiben erhalten, auch wenn Otis danach ausfällt.

Die JSON-Abbildung ist klein: Anfrage `{"value": …}` für Wert-Schreibzugriffe, Antwort mit `value`, `version`, `updatedAt`. Der Adapter liest nur das, was er braucht; unbekannte Felder werden ignoriert.

### D5 Konflikte und Versionen

Otis hat keine Optimistic Concurrency für Einstellungen. Die Lobby verwendet deshalb Last-Write-Wins je Schlüssel. Das ist vertretbar, weil ein Spieler jeweils nur auf einer Lobby ist und Schalter nur `true`/`false` setzen. Die Version aus Antworten wird gespeichert (`version` im Eintrag) und beim Laden verglichen, damit ein Test den Pfad „Otis hat neuere Version als wir“ abdeckt; ein Konflikt wird nur geloggt, nicht aufgelöst.

Ein `If-Match` in Otis (Voraussetzung 4 in der Proposal) würde einen 409 liefern; der Adapter behandelt 409 schon jetzt wie einen wiederholbaren Fehler, damit der Schritt später ohne Änderung der Lobby kommt.

### D6 Menü mit Aves

Das Menü ist ein Aves-Inventar mit einer Zeile je Option, Zeile 1 mit Schließen. Je Option ein Vanilla-Item: `LIME_DYE` für an, `GRAY_DYE` für aus; Name und Beschreibung aus den Übersetzungsschlüsseln `settings.<key>.name` und `settings.<key>.description`. Ein Klick ruft `toggle` auf und schreibt mit Verzögerung (D7); danach öffnet der Spieler das Inventar der neuen Zustandskombination.

Inventare sind nicht spielerspezifisch. Sie werden je Zustandskombination der registrierten Optionen und je Sprache (`de`, `en`) gebaut: höchstens 2 · 2^n Inventare für n Optionen, dokumentierte Obergrenze etwa 6 Optionen (also höchstens 128). Sie entstehen einmal beim Start und werden wie die geteilten Ansichten des Navigators (`features/navigator`) verwendet. Es gibt keinen Listener pro Spieler; das Öffnen wählt das Inventar anhand des aktuellen Zustands und der Sprache. Ein Builder je Modul, register/unregister im Lebenszyklus. Überschreitet die Zahl der Optionen die Obergrenze, wird die Strategie in einem eigenen Change neu bewertet.

Texte: `titan/settings/messages_de.properties` und `messages_en.properties`, gleiche Schlüssel, Test prüft beide Dateien.

### D7 Schreiben im Hintergrund (Write-behind)

Jede Änderung setzt `dirty` für den Schlüssel und plant ein Speichern nach `otis.writeDebounceMillis` (Standard 2000) ein. Eine neue Änderung desselben Schlüssels verschiebt den Termin. Beim Verlassen des Spielers wird sofort gespeichert, mit einem Limit von 3 Sekunden pro Spieler. Beim Herunterfahren werden alle ausstehenden Schreibvorgänge geflusht, mit gemeinsamem Limit von 5 Sekunden.

Schlägt das Speichern nach allen Wiederholungen fehl, bleibt `dirty` gesetzt, und der Schlüssel wird beim nächsten Änderungsversuch oder beim nächsten Verlassen erneut gespeichert. Ein Absturz vor dem Speichern verliert die letzte Änderung; das wird im Proposal und im Spec festgehalten, weil ein Persistenzpuffer auf der Festplatte nicht Teil dieser Änderung ist.

### D8 Offline-Variante und Konfiguration

Ist `otis.url` leer, wählt die Konfiguration `InMemorySettingsStore`. Der Start loggt eine Warnung, dass Einstellungen nicht persistiert werden, und die Telemetrie setzt das Attribut `store=memory`. Mit gesetztem `otis.url` wird der Otis-Adapter gewählt.

Konfigurationsschlüssel folgen dem Muster `<bereich>.<feld>` aus den Standardwerten der Columns:

| Schlüssel | Standard | Zweck |
|---|---|---|
| `settings.enabled` | `false` | Funktion ein- oder ausschalten |
| `otis.url` | leer | Basis-URL; leer = In-Memory |
| `otis.token` | leer (aus `TITAN_OTIS_TOKEN`) | Bearer-Token, sobald Otis es verlangt |
| `otis.connectTimeoutMillis` | `1000` | Verbindungsaufbau |
| `otis.requestTimeoutMillis` | `2000` | Gesamte Anfrage |
| `otis.loadRetries` | `1` | Wiederholungen beim Laden |
| `otis.writeRetries` | `3` | Wiederholungen beim Schreiben |
| `otis.writeDebounceMillis` | `2000` | Verzögerung bis zum Speichern |
| `otis.circuitFailureThreshold` | `3` | Fehler in Folge bis zum Öffnen |
| `otis.circuitOpenMillis` | `30000` | Dauer der Sperre |

Das Token wird nie geloggt, weder beim Start noch in Fehlermeldungen; die Konfigurationsanzeige maskiert es.

### D9 Telemetrie

- `titan.settings.load` (Histogramm, Millisekunden), Attribut `outcome` mit `ok`, `timeout`, `error`, `circuit_open`, `defaults`.
- `titan.settings.write` (Zähler), Attribut `outcome` mit `ok`, `retry`, `error`.
- `titan.settings.toggled` (Zähler), Attribut `key` (Schlüssel ohne Wert).
- `titan.settings.circuit` (Zähler), Attribut `state` mit `open`, `half_open`, `closed`.

Keine Spieler-UUIDs in Metriken; im Debug-Log ist die UUID erlaubt.

### D10 Sicherheit

Otis hat keine Authentifizierung. Jeder mit Netzwerkzugang zu Otis kann die Einstellungen eines Spielers lesen und schreiben, wenn er die UUID kennt. Die Schranken heute:

- Otis ist nur im internen Netz erreichbar; die Lobby (mc01) erreicht den Dienst über den internen Pfad.
- Die gespeicherten Werte sind Schalter. Es gibt keine personenbezogenen Daten außer der UUID, die ohnehin im Netzwerk bekannt ist.
- Der Adapter erlaubt nur Schlüssel aus der Registry (D1); ein fremder Schlüssel kann nicht über die Lobby gesetzt werden.

Vor dem Einschalten in Produktion muss die Otis-Authentifizierung (Voraussetzung 1) gelandet sein. Die Lobby sendet das Token dann bereits, sobald `otis.token` gesetzt ist. Ohne Token und mit Otis-Authentifizierung schlägt jeder Aufruf mit 401 fehl, was der Breaker als Fehler zählt und der Start als Warnung meldet.

### D11 Otis-Stand in Produktion (verifiziert 2026-10-24)

- Produktion läuft Otis 1.17.0 (`harbor.onelitefeather.dev/onelitefeather/otis:1.17.0`). Die Voraussetzung „Version ausgerollt“ ist erfüllt.
- `GET /v1/players/{uuid}/settings` liefert 200 mit `[]`. Die Migration V2 (`player_setting`) ist damit in Produktion gelaufen.
- Velocity legt den Spieler bei jedem Beitritt an (`/otis/update/{uuid}` in den Produktions-Access-Logs). Ein 404 beim ersten Schreiben ist daher nicht zu erwarten. Die Behandlung von `404 player-not-found` als „später erneut versuchen“ (D4) bleibt nur als defensiver Fallback.
- Einzige blockierende Voraussetzung ist die Dienst-Authentifizierung in Otis. Sie ist ein eigener Change in `OneLiteFeatherNET/Otis`.

### D12 Client: eigene JSON-Abbildung statt `otis-client`

Der Adapter nutzt nur zwei Endpunkte: die Liste mit `?namespace=titan` und `PUT /v1/players/{uuid}/settings/{key}`. Er bildet deren JSON selbst mit Gson ab, das `common` bereits verwendet. Der generierte `otis-client` entfällt: seine Veröffentlichung ist nicht prüfbar (das Repository ist anonym nicht lesbar), und für zwei Endpunkte lohnt keine generierte Abhängigkeit.

### D13 Gradle-Modul `platform/otis`

Der Adapter liegt in einem eigenen Gradle-Modul `platform/otis` (Paket `net.onelitefeather.titan.platform.otis`). So bleiben `core` und die Columns frei von HTTP. Das Modul hängt von `core` und Gson ab; die Abhängigkeit wird explizit deklariert.

### D14 Hotbar-Slot 7

Belegt sind Slot 0 (Jump and Run), 4 (Navigator) und 8 (Lobby-Switcher, nur Profil `cloudnet`). Slot 7 ist frei und wird für das Einstellungs-Item verwendet. Die Konfliktprüfung beim Start bleibt als Absicherung.

### D15 Verzögerung beim Schreiben

Die Verzögerung von 2 Sekunden (`otis.writeDebounceMillis`, D8) ist akzeptiert.

## Failure modes

| Fall | Verhalten |
|---|---|
| Otis nicht erreichbar beim Beitritt | Standardwerte; Laden scheitert nach Timeout; Breaker zählt; Beitritt unverändert |
| Otis antwortet langsam (über Timeout) | Wie oben; kein Warten auf den Tick-Thread |
| Otis liefert 404 für den Spieler | Standardwerte; Schreiben bleibt `dirty` und wird später erneut versucht |
| Otis liefert 401/403 | Breaker öffnet; Warnung im Log; Menü funktioniert im Speicher |
| Otis liefert 5xx oder Netzfehler beim Schreiben | Wiederholung; danach `dirty` bleibt; nächste Gelegenheit |
| Prozess stürzt ab mit ausstehendem Schreiben | Letzte Änderung verloren (akzeptiert, siehe D7) |
| Otis hat neuere Version als die Lobby | Übernahme bei nicht veränderten Schlüsseln; Log auf Debug |
| Spieler verlässt Server während des Ladens | Ergebnis verworfen |
| Option wird in Registry nicht gefunden (z. B. nach Entfernen) | `isEnabled` liefert Standard; Eintrag bleibt in Otis unberührt |

