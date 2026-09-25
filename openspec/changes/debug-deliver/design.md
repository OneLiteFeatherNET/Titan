# Design

## Context

Siehe `proposal.md` (Why). Maßgeblich für den Ansatz:

- `DeliverProvider.create()` entscheidet anhand von `CloudNetEnvironment.isPresent()` (prüft, ob im Arbeitsverzeichnis ein `.wrapper`-Ordner liegt - eine statische, abhängigkeitsfreie Prüfung des echten Dateisystems) zwischen `MessageChannelDeliver` (mit CloudNet) und, bisher, `NoopDeliver` (ohne CloudNet).
- `Deliver#sendPlayer(Player, DeliverComponent)` ist die einzige Schnittstelle, über die Navigator-Klicks eine Weiterleitung anstoßen; `DeliverComponent` ist ein Sealed Interface mit `TaskComponent` und `ServerDeliverComponent`.
- Das Projekt hat keine i18n-Bundles. Nutzertext steht als Inline-MiniMessage direkt im Code, mit dem global registrierten `<prefix>`-Tag aus `TitanMiniMessageImpl` (ServiceLoader-Provider für `MiniMessage.miniMessage()`).
- `DeliverProvider.create()` läuft auch beim AOT-Training (JDK 25) und in Tests ohne echten CloudNet-Wrapper. Beide Fälle haben keine verbundenen Spieler, `sendPlayer` wird dort also nie aufgerufen.

## Goals / Non-Goals

**Goals:**
- Ein Klick auf ein Navigator-Ziel ohne CloudNet ist beim manuellen Testen sichtbar unterscheidbar von einem stillen Fehler.
- Keine Verhaltensänderung mit CloudNet.

**Non-Goals:**
- Kein Schalter/Property, um zwischen „debug“ und „still“ zu wählen - es gibt nur den einen Fall ohne CloudNet.
- Keine echte Weiterleitung ohne CloudNet; das bleibt technisch unmöglich (keine CloudNet-Bridge auf dem Classpath).
- Keine i18n-Bundles für diese eine Debug-Zeile; das wäre eine eigene, größere Change für das ganze Projekt.

## Decisions

### 1. `DebugDeliver` ersetzt `NoopDeliver` ohne CloudNet, ohne Schalter
`DeliverProvider.create()` liefert ohne CloudNet direkt `DebugDeliver` statt `NoopDeliver`. Kein Property, kein Flag: Der einzige Fall, in dem `DebugDeliver` läuft, ist bereits „kein CloudNet“, ein zusätzlicher Schalter hätte keinen zweiten Zustand zu unterscheiden.

**Auswirkung auf AOT-Training und Tests:** Beide laufen ohne CloudNet-Wrapper und damit mit `DebugDeliver`, aber ohne verbundene Spieler - `sendPlayer` wird dabei nicht aufgerufen, das Verhalten ändert sich für sie nicht.

**Alternative:** ein `-Dtitan.debugDeliver=true`-Flag, das `NoopDeliver` als Standard behält. Verworfen: doppelter Zustand ohne Nutzen, und genau das lokale Testen - der Auslöser dieser Change - bräuchte dann wieder ein zusätzliches manuelles Setzen.

**SOLID:** Open/Closed - `Deliver` bleibt die Erweiterungsstelle, `DebugDeliver` ist eine neue Implementierung, keine bestehende Klasse wird geändert außer der einen Zeile in `DeliverProvider`, die die Implementierung auswählt.

**Test:** Unit-Test in `DebugDeliverTest` (siehe Decision 4). Die Auswahl in `DeliverProvider` selbst ist nicht unit-testbar, siehe Risks.

### 2. Inline-MiniMessage statt Übersetzungsschlüssel
Die Chatzeile wird direkt als MiniMessage-Template im Code gehalten (`<prefix> <gray>No CloudNet running here - you would be sent to task|server <white><target>`), nicht über `Component.translatable(...)` und ein Bundle.

**Built-in geprüft:** Adventures `TranslationStore`/`GlobalTranslator` ist im Stack vorhanden und wäre der richtige Weg für i18n - aber es setzt Bundle-Dateien voraus, die es im Projekt nirgends gibt. Jeder andere Nutzertext in Titan ist bereits Inline-MiniMessage (siehe `TitanMiniMessageImpl`).

**Warum trotzdem akzeptabel:** Diese Zeile erscheint nur, wenn kein CloudNet läuft, also nie in Produktion. Sie richtet sich an die Person, die gerade lokal testet, nicht an Endspieler.

**Alternative:** eine neue `TranslationStore`-Bundle-Infrastruktur für diese eine Zeile einführen. Verworfen: unverhältnismäßiger Aufwand für einen Debug-Hinweis, der nie produktiv sichtbar wird; eine projektweite i18n-Einführung ist eine eigene Change.

**SOLID:** Single Responsibility - `DebugDeliver` bleibt für „melden statt weiterleiten“ zuständig, nicht für Lokalisierung.

**Test:** `DebugDeliverTest` prüft den gerenderten Text über `PlainTextComponentSerializer`.

### 3. `Placeholder.unparsed("target", …)` für das konfigurierte Ziel
Task- und Servernamen kommen aus `app.json` (Betreiber-Konfiguration), nicht aus Spielereingabe. Trotzdem wird `Placeholder.unparsed` statt String-Konkatenation verwendet: Ein Ziel wie `<red>evil` in der Config erscheint dann wortwörtlich im Chat statt als MiniMessage interpretiert zu werden.

**Built-in geprüft:** Adventures `TagResolver`/`Placeholder`-API ist genau für „Wert einsetzen, nicht als Markup interpretieren“ gebaut - keine eigene Escaping-Logik nötig.

**Alternative:** direkte String-Konkatenation (`messageTemplate.replace("<target>", target)`) vor dem Deserialisieren. Verworfen: Ein Ziel mit spitzen Klammern würde dann als MiniMessage-Tag interpretiert, ungewollt Formatierung oder einen Deserialisierungsfehler auslösen.

**Test:** `DebugDeliverTest#sendPlayerWithMiniMessageLikeTargetShowsItLiterally` - ein Zielname mit `<red>` erscheint unverändert im Chat.

### 4. Eine `INFO`-Zeile pro Klick
`DebugDeliver` loggt genau eine `INFO`-Zeile pro Aufruf (`Debug deliver: would send {} ({}) to {} {}`), nicht `DEBUG`.

**Warum `INFO` und nicht `DEBUG`:** Nach den Logging-Regeln ist `INFO` für Zustands-/Lifecycle-ähnliche Ereignisse vorgesehen, `DEBUG` für Pro-Operation-Diagnostik auf häufigen Pfaden. Ein Navigator-Klick ist eine explizite, seltene Spieleraktion (nicht pro Tick, nicht pro Paket), deren Ergebnis - „hier wäre etwas passiert“ - für die manuelle Abnahme ohne Log-Level-Umschalten sichtbar sein soll.

**Test:** `DebugDeliverTest` erfasst die Log-Zeile über `CapturingLoggerFactory` (bestehende SLF4J-Testanbindung des `common`-Moduls) und prüft Text und Anzahl (genau eine Zeile pro Klick).

## Risks / Trade-offs

- **[Risiko]** Die Auswahl in `DeliverProvider.create()` selbst (CloudNet vorhanden → `MessageChannelDeliver`, sonst → `DebugDeliver`) ist nicht durch einen Unit-Test abgedeckt. `CloudNetEnvironment.isPresent()` prüft das echte Arbeitsverzeichnis des JVM-Prozesses (`Path.of(".wrapper")`) statisch, ohne Dependency-Injection-Nahtstelle - ein Test müsste das reale Arbeitsverzeichnis der Testausführung manipulieren, was Testeigenständigkeit und Wiederholbarkeit (F.I.R.S.T.) verletzen würde. **Bekannte Lücke**, keine Behebung in dieser Change: Wer eine testbare `DeliverProvider`-Auswahl will, muss `CloudNetEnvironment` zuerst hinter eine injizierbare Abstraktion legen - das ist ein eigener Schnitt, nicht Teil dieses kleinen Deliver-Changes. Manuell geprüft (siehe `tasks.md`): lokale Läufe ohne CloudNet zeigen tatsächlich `DebugDeliver`-Verhalten.
- **[Trade-off]** Inline-MiniMessage statt i18n-Bundle (Decision 2) bedeutet: Diese eine Zeile ist nicht lokalisierbar. Akzeptiert, weil sie nur außerhalb von Produktion erscheint und das Projekt an keiner Stelle i18n-Bundles hat.
