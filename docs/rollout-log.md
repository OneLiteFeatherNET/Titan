# Rollout-Log

Nachweis darüber, welches Feature wann auf welcher Freigabestufe stand. Erfüllt
NFR-011 aus [`spec-lobby-saison-events.md`](spec-lobby-saison-events.md).

**Diese Datei wird bei jedem Stufenwechsel ergänzt, nicht überschrieben.** Alte
Zeilen bleiben stehen — der Verlauf ist der Zweck.

## Die Stufen

| Stufe | Wer sieht es | Berechtigung |
|---|---|---|
| `intern` | nur das Team | `titan.feature.internal` |
| `premium` | Team **und** Premium-Spieler | `titan.feature.premium` |
| `ga` | alle Spieler | — |
| `aus` | niemand (Notausschalter) | — |

Der Notausschalter schlägt jede Stufe und jedes Zeitfenster. Die Prüfreihenfolge
ist in US-3.07 festgelegt: erst Notausschalter, dann Stufe, dann Zeitfenster.

## Verlauf

| Datum | Feature | von → nach | Grund | Verantwortlich |
|---|---|---|---|---|
| — | — | — | noch kein Eintrag | — |

## Wie ein Eintrag entsteht

Ein Stufenwechsel wird an zwei Stellen festgehalten, und beide sind Pflicht:

1. **Hier**, als neue Zeile mit Datum, Feature, Übergang, Grund und
   verantwortlicher Person.
2. **Im Log der Anwendung**, automatisch beim Wechsel (US-3.09).

Der Grund ist das Feld, das später zählt. „Auf premium gehoben" ist keine
Begründung; „interne Prüfung ohne Befund über zwei Wochen" ist eine.

## Rücknahmen

Eine Rücknahme (etwa `ga` → `aus`) bekommt eine eigene Zeile mit dem Grund und,
sobald bekannt, einen Verweis auf die Ursache. Zeilen werden nicht gelöscht,
wenn ein Feature später erneut freigegeben wird — gerade der zweite Anlauf ist
die interessante Information.
