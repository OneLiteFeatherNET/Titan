# Rollout-Log

Nachweis darüber, welches Feature wann auf welcher Freigabestufe stand. Erfüllt
NFR-011 aus [`spec-lobby-saison-events.md`](spec-lobby-saison-events.md).

**Diese Datei wird bei jedem Stufenwechsel ergänzt, nicht überschrieben.** Alte
Zeilen bleiben stehen — der Verlauf ist der Zweck.

## Die Stufen

| Stufe | Wer sieht es | Bedingung |
|---|---|---|
| `internal` | nur das Team | Berechtigung `titan.feature.internal` |
| `lite` | Team **und** Lite-Spieler | LuckPerms-Gruppe `lite`, zusätzlich zu `titan.feature.internal` |
| `ga` | alle Spieler | — |
| — (Notausschalter) | niemand | Flag steht auf `false` |

Der Notausschalter schlägt jede Stufe und jedes Zeitfenster. Die Prüfreihenfolge
ist in US-3.07 festgelegt: erst Notausschalter, dann Stufe, dann Zeitfenster.
Umgesetzt ist sie in `FeatureGate` — der einzigen Klasse, die mit Togglz spricht.

Ein Feature ohne hinterlegte Stufe gilt als `internal`, ein Feature ohne Eintrag
in der Flag-Datei als abgeschaltet. Die enge Auslegung ist Absicht: ein
vergessener Eintrag darf nichts freigeben.

## Wie eine Stufe gesetzt wird

Alles steht in `flags.properties` neben dem Prozess und wird ohne Neustart
innerhalb einer Sekunde übernommen (US-3.05):

```properties
# Notausschalter: false heisst, niemand sieht das Feature
NAVIGATOR_ELYTRA = true
NAVIGATOR_ELYTRA.strategy = season-window
# Stufe: internal | lite | ga
NAVIGATOR_ELYTRA.param.stage = lite
# Zeitfenster, beide Grenzen optional
NAVIGATOR_ELYTRA.param.from = 2026-10-01
NAVIGATOR_ELYTRA.param.to = 2026-11-05
NAVIGATOR_ELYTRA.param.zone = Europe/Berlin
```

Kommentare stehen in einer `.properties`-Datei immer in einer eigenen Zeile: ein
`#` mitten in der Zeile ist Teil des Wertes, kein Kommentar.

`from` (einschließend) und `to` (ausschließend) nehmen ein Datum oder eine
ISO-Zeitangabe (`2026-10-01T18:00`); `zone` ist optional und steht sonst auf
`Europe/Berlin`. Ein unlesbarer Wert schaltet das Feature ab, statt es zu öffnen.

Den aktuellen Stand zeigt `/season status` im Spiel — je Feature Stufe,
Zeitfenster und Notausschalter. Die Togglz-Adminkonsole ist ein Servlet und in
einem Minestom-Prozess nicht verfügbar; der Befehl ersetzt sie.

## Verlauf

| Datum | Feature | von → nach | Grund | Verantwortlich |
|---|---|---|---|---|
| — | — | — | noch kein Eintrag | — |

## Wie ein Eintrag entsteht

Ein Stufenwechsel wird an zwei Stellen festgehalten, und beide sind Pflicht:

1. **Hier**, als neue Zeile mit Datum, Feature, Übergang, Grund und
   verantwortlicher Person.
2. **Im Log der Anwendung**, automatisch beim Wechsel (US-3.09). Die Anwendung
   sieht jede Sekunde nach und schreibt bei einer Änderung eine Zeile der Form:

   ```
   Feature NAVIGATOR_ELYTRA changed release stage at 2026-10-01T00:00:01+02:00[Europe/Berlin]: internal -> lite
   ```

   Der erste Blick nach einem Start ist kein Wechsel und wird nicht
   protokolliert — ein Neustart soll keine Stufenwechsel erfinden.

Der Grund ist das Feld, das später zählt. „Auf lite gehoben" ist keine
Begründung; „interne Prüfung ohne Befund über zwei Wochen" ist eine.

## Rücknahmen

Eine Rücknahme (etwa `ga` → `aus`) bekommt eine eigene Zeile mit dem Grund und,
sobald bekannt, einen Verweis auf die Ursache. Zeilen werden nicht gelöscht,
wenn ein Feature später erneut freigegeben wird — gerade der zweite Anlauf ist
die interessante Information.
