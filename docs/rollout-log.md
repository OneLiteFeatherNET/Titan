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

**Nicht jedes Feature hängt an einem Togglz-Flag.** Die Resource-Pack-Auslieferung
(Stufe 6) wird über die Datei `resource-packs.json` neben dem Server geschaltet:
ohne Datei ist sie `aus` und registriert nicht einmal ihre Listener, mit Datei ist
sie `ga` — eine Zwischenstufe für einzelne Spielergruppen gibt es dort nicht, weil
ein Paket an den Client geht oder nicht. Für den Nachweis nach NFR-011 zählt
derselbe Eintrag wie bei einem Flag: Datum, Übergang, Grund, verantwortliche
Person.

**Und nicht jedes Flag hat eine Stufe.** `RESOURCE_PACK_REQUIRED_KICK` (Stufe 6,
US-6.05) ist ein reiner Notausschalter: er entscheidet, ob die Lobby einen Spieler
trennt, der ein als `required` konfiguriertes Paket nicht lädt — sei es, weil er
ablehnt, sei es, weil er gar nicht antwortet und die Timeout-Wache
stellvertretend antwortet. Gelesen wird nur der An/Aus-Zustand. Eine Stufe hätte
zur Folge, dass derselbe stumme Client je nach Gruppe getrennt oder eingelassen
wird, und das Team wäre die unbrauchbarste Stichprobe für die eigentliche Frage,
nämlich ob normale Clients das Paket rechtzeitig laden. In der Ausgabe von
`/season status` erscheint das Flag trotzdem mit den Spalten Stufe und
Zeitfenster — **bei diesem Flag sind beide bedeutungslos**, nur „an" oder „aus"
zählt. Die Voreinstellung ist **an** (`@EnabledByDefault`): ohne
`flags.properties`, oder wenn die Datei nicht lesbar ist, bleibt es beim
bisherigen Verhalten und beim Versprechen, das `"required": true` gibt. Für
diesen Schalter gilt derselbe Nachweis wie für jede Stufe: eine Zeile im Verlauf.

## Verlauf

| Datum | Feature | von → nach | Grund | Verantwortlich |
|---|---|---|---|---|
| 2026-08-28 | Resource Packs (Stufe 6) | — → `aus` | Auslieferung, Saisonwechsel, Timeout-Wache und Bedrock-Erkennung sind implementiert und getestet (`:common` und `:app`). Auf keiner Lobby liegt eine `resource-packs.json`, das Feature ist damit überall abgeschaltet und hat noch keinen Spieler erreicht. | @TheMeinerLP |
| 2026-08-29 | `RESOURCE_PACK_REQUIRED_KICK` (Stufe 6) | — → `an` | Neuer Notausschalter für das Trennen wegen eines Pflichtpakets, siehe US-6.05. Er startet an, weil das genau dem Verhalten entspricht, das bereits im Code stand: ein Flag einzuführen darf für sich genommen nichts ändern. Erreicht bisher ohnehin keinen Spieler, da nirgends eine `resource-packs.json` liegt. | @TheMeinerLP |

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
