# Design

## Kontext

Der Navigator ist eine Reihe mit neun Plätzen und einem Titel „Navigator“. Die Lore zeigt je Ziel die Spielerzahl und die Klickhinweise. Ein Inventar gilt für alle Spieler, die es öffnen (Aves `GlobalInventoryBuilder`), deshalb wird die Sichtbarkeit über die Wahl des Inventars gesteuert und nicht über Items je Spieler. Die Serverauswahl (`SharedServerPicker`) ist bisher englisch und in der Größe an die Zahl der Dienste gebunden.

## Entscheidungen

### Layout: vier Reihen mit Rahmen

Der Navigator nutzt `InventoryType.CHEST_4_ROW` (36 Plätze). Reihen und Spalten zählen ab 0.

| Reihe | Plätze | Inhalt |
| --- | --- | --- |
| 0 | 0–8 | Rahmen |
| 1 | 9–17 | Minispiele: ElytraRace 11, Slender 15 |
| 2 | 18–26 | Server: Survival 20, Creative 22, Build 24 |
| 3 | 27–35 | Rahmen, Spawn 31 in der Mitte |

Der Rahmen besteht aus Reihe 0 und 3 sowie Spalte 0 und 8. Die Glasscheiben wechseln nach Parität von Reihe plus Spalte: gerade `LIGHT_BLUE_STAINED_GLASS_PANE`, ungerade `CYAN_STAINED_GLASS_PANE`, mit leerem Namen. Die übrigen Plätze im Inneren sind `GRAY_STAINED_GLASS_PANE`. Ein Eintrag ohne Recht lässt seinen Platz grau. Spawn liegt auf Platz 31 und ersetzt dort die Rahmenscheibe, die Mitte der unteren Reihe ist also der Spawn-Eintrag.

Die Plätze sind in `NavigatorLayout` als reine Funktionen festgelegt, damit der Test ohne Server prüfen kann, welche Plätze Rahmen und welche Ziele sind.

### Farben und Lore

Jeder Modus hat eine Verlaufsfarbe, die für Namen, Trennlinie und die Zeilen gilt. Die Lore eines Eintrags ist in dieser Reihenfolge:

1. Trennlinie `━━━━━━━━━━━━` in der Verlaufsfarbe des Modus.
2. Spielerzeile, sofern die Spielerzahl bekannt ist: `Spieler  13 / 40`. Das Maximum steht nur dort, wo es bekannt ist (siehe unten), sonst nur `Spieler  13`. Ohne bekannte Spielerzahl entfällt die Zeile.
3. Leerzeile.
4. Klickzeilen, eine je Klick: `▸ KEY  AKTION`. Der Schlüssel steht in Weiß, die Aktion in Grau.

Die Texte stehen in den Bundles, gerendert über den `GlobalTranslator` (Minestom 26.1 zeigt übersetzbare Komponenten sonst leer). Der Name nutzt `<!i>` und den Verlauf des Modus. Der Spawn-Eintrag hat nur den Namen.

Das Maximum summiert die Limits aller laufenden Dienste eines Ziels, aber nur, wenn jeder Dienst ein Limit gemeldet hat. Sonst ist es unbekannt, und die Zeile zeigt nur die Zahl. So entsteht kein irreführendes „13 / 20“, wenn ein Dienst noch kein Limit gemeldet hat.

### Klickmodell

`ClickAction.of(minigame, click, portal)` ordnet jeden Klick einer Aktion zu und liefert dafür ein `Optional`. Benannt sind nur Links- und Rechtsklick, mit oder ohne Shift (`ClickAction.isNamed`). Jeder andere Klicktyp (Zahlentasten, Q, F, Mittelklick, Doppelklick, Ziehen) liefert nichts; der Klick wird abgebrochen und tut nichts. Dasselbe gilt für die Einträge der Serverauswahl. Shift hat nur dann eine Wirkung, wenn ein Portal existiert. Sonst gilt der Klick wie ohne Shift.

| Klick | Minispiel (ElytraRace, Slender) | Server (Survival, Creative, Build) |
| --- | --- | --- |
| Links | Schnellbeitritt | Verbinden |
| Rechts | Serverauswahl (immer) | Verbinden (kein Hinweis) |
| Shift-Links oder Shift-Rechts, mit Portal | Zum Portal laufen | Zum Portal laufen |
| Shift-Links oder Shift-Rechts, ohne Portal | wie Links bzw. Rechts | Verbinden |

Die Klickzeilen folgen derselben Funktion: `ClickAction.hints(minigame, portal)` liefert für Minispiele Schnellbeitritt, Serverauswahl und ggf. Portal, für Server Verbinden und ggf. Portal. Eine Zeile für den Rechtsklick auf einem Server gibt es nicht.

### Schnellbeitritt

`QuickJoin.pick(running)` wählt aus den laufenden Diensten eines Ziels den Dienst, der bereit ist und nicht voll ist, mit den meisten Spielern zuerst. Bei Gleichstand gewinnt der kleinste Name. „Bereit und nicht voll“ ist dasselbe Kriterium wie das Klicken in der Serverauswahl (`ServerPicker.joinable`: Limit gemeldet und nicht voll). Gibt es keinen solchen Dienst, öffnet sich die Serverauswahl, die den Grund zeigt.

Die Dienste werden beim Klick neu gelesen, wie bei der Serverauswahl. Die Lesung läuft auf dem Executor, die Entscheidung auf dem nächsten Tick.

### Lesung der Dienste und Lesefehler

`TaskPlayers.services(task)` liefert `Optional<List<ServiceCount>>` und unterscheidet zwei Fälle, die vorher beide als leere Liste aussahen:

- Eine leere Liste heißt: das Ziel läuft nirgends. Die Serverauswahl öffnet sich dann mit „Kein Server läuft gerade.“ in der Mitte, und der Klick tut sonst nichts.
- Ein leeres Optional heißt: die Dienste sind nicht lesbar, weil kein Zähl-Anbieter da ist oder die Abfrage eine Ausnahme wirft. Schnellbeitritt, Rechtsklick und Auswahl öffnen dann nichts und übergeben niemanden. Der Spieler erhält eine Chat-Ablehnung „Servers » Die Serverliste ist gerade nicht verfügbar.“, und der Navigator bleibt offen.

Die Anzeige (Zahl und Klickzeilen) nutzt weiter die leere Liste als „unbekannt“; dort ändert sich nichts.

### Frische Lesung beim Auswählen

Eine Auswahl aus der Serverauswahl prüft den Dienst mit einer neuen Lesung, nicht mit dem Stand vom Öffnen. Dieselbe Lesung mit Executor und nächstem Tick wie beim Schnellbeitritt. Ist der Dienst inzwischen voll oder nicht bereit, gilt die bestehende Ablehnung; ist er nicht mehr gelistet, wird er wie ein nicht bereiter Dienst abgelehnt, weil es keinen eigenen Text für „nicht mehr da“ gibt. Ein Doppelklick während einer laufenden Lesung wird wie bisher ignoriert.

### Menü-Varianten

Die Entscheidung ist: die acht geteilten Inventare bleiben. Jede Kombination aus Rechten und Sprache hat ihr eigenes Inventar, und der Titel ist für die Sprache fest. Das Inventar wird beim Start mit Titel und Layout eingerichtet, und Spieler sehen nur das geteilte Inventar.

Verworfen wurden zwei Alternativen:

- **`GlobalTranslatedInventoryBuilder`** (Aves rendert je Betrachter-Sprache): Aves' Klick- und Schließ-Listener feuern für übersetzte Builder nicht. `SwitcherInventory` in der Lobbyswitcher-Funktion muss deshalb selbst weiterleiten und die Schließ-Logik nachbauen. Der Navigator müsste dann die Listener-Pflege selbst übernehmen, die heute Aves erledigt.
- **Ein Inventar je Öffnung** (per-Spieler-Rendering): Jede Öffnung würde ein neues Inventar mit Listener anlegen. Der Leak-Test würde dann Öffnungen zählen statt Registrierungen, und die Lebensdauer der Inventare müsste verwaltet werden.

Die Serverauswahl folgt demselben Muster: je Ziel und Sprache ein `SharedServerPicker`, je Zeilenzahl ein `GlobalInventoryBuilder`, das erst bei Bedarf registriert wird. Das sind höchstens 2 Ziele × 2 Sprachen × 6 Zeilen Builder.

### Serverauswahl im Stil des Navigators

Titel „Server wählen“ im Verlauf, übersetzt. Jeder Eintrag hat den Verlauf seines Ziels für den Namen, dieselbe Spielerzeile (mit Maximum, wenn bekannt), eine Leerzeile und eine Zustandszeile `▸ Zustand` in Rot (Voll), Gelb (Noch nicht bereit) oder Grün (Klicken zum Beitreten). Die Anzahl der Zeilen richtet sich weiter nach der Zahl der Dienste, mit Grau für Leerplätze. Ein Rahmen wäre bei variabler Zeilenzahl nicht einfach, deshalb entfällt er hier.

Eine Ablehnung (voll, nicht bereit oder nicht lesbar) bleibt eine Chat-Nachricht mit dem Präfix „Servers“, der Grund ist übersetzt. Läuft das Ziel nirgends, zeigt die Serverauswahl mittig den Eintrag „Kein Server läuft gerade.“, der beim Klick nichts tut.

## Risiken

- Der Rechtsklick auf ein Minispiel ohne laufenden Dienst öffnet die Serverauswahl mit dem Hinweis „Kein Server läuft gerade.“. Das folgt aus der Vorgabe, dass der Rechtsklick immer die Auswahl öffnet. Ein Lesefehler wird dagegen im Chat abgelehnt und öffnet keine Auswahl.
- Spawn auf Platz 31 liegt in der unteren Rahmenreihe. Die Mitte ist dann eine Lücke im Rahmen, was beabsichtigt ist.
- Die Klickhinweise zeigen den Stand der letzten Aktualisierung, wie bisher. Der Klick selbst liest die Dienste neu.
