# Design

## Kontext

Der Navigator ist ein Enum (`Destination`) mit Slot, Icon, Verlauf, Anzeigename, CloudNet-Task und optionalem Recht. `Destination.permissioned()` liefert die gebundenen Ziele, `Destination.visible(granted)` filtert ein Menü. `NavigatorModule` baut je Teilmenge der gebundenen Ziele und Sprache ein geteiltes Inventar (`SharedNavigator`), prüft das Recht beim Öffnen und erneut beim Klick. `ServerPicker.DESTINATIONS` legt fest, welche Modi Schnellbeitritt und Serverauswahl anbieten; die Klickhinweise lesen dieselbe Menge. Slender ist der Vorlage: Recht über `isAllowed`, Slot 15, Verlauf, Eintrag in `DESTINATIONS`. Ein Recht je Ziel ist das bestehende Muster, siehe `proposal.md` und den Spec-Delta in `specs/lobby-navigator/spec.md`.

## Ziele / Nicht-Ziele

**Ziele:**

- Bounce ist ein weiterer Enum-Eintrag mit eigenem Recht; alle Stellen, die Ziele über das Enum oder `permissioned()` finden, sehen ihn ohne weitere Verdrahtung.
- Die Menüvarianten folgen der bestehenden Teilmengen-Schleife, damit das Menü bei jedem Recht-Stand genau das richtige Inventar zeigt.
- Keine neue Klasse, kein neuer Listener, keine neue Konfiguration.

**Nicht-Ziele:**

- Keine Spiellogik für Bounce im Lobby-Code; der Server läuft als CloudNet-Task `Bounce` und wird über den bestehenden Schnellbeitritt erreicht.
- Keine eigene Portal-Logik für Bounce; ein Portal wirkt wie bei Slender, sobald es für das Ziel existiert.
- Keine Änderung an Slender, Build oder am Permission-Service.
- Keine Lokalisierung des Namens „Bounce“; es ist ein Eigenname wie „ElytraRace“ und „Slender“.

## Entscheidungen

- **Bounce als Enum-Eintrag mit Recht.** `BOUNCE(13, Material.SLIME_BALL, "<gradient:#00FA9A:#40E0D0>", "Bounce", "Bounce", "titan.navigator.bounce")`. Das Framework (Aves) bietet dafür keinen Baustein, und die Sichtbarkeit über Rechte ist im Code bereits gelöst; das Enum mit Recht ist das Muster von Slender und Build, also folgt Bounce diesem Weg statt einer neuen Abstraktion. Die Startfarbe `#00FA9A` weicht von `#7CFC00` (Survival) ab, damit die beiden grünen Einträge im Menü unterscheidbar bleiben. Test: `NavigatorDestinationTest` (Slot, Recht, Task, erster und letzter Buchstabe des Verlaufs).
  SOLID: offen für Erweiterung (neuer Eintrag), ohne Logik in Methoden zu ändern (Open/Closed).

- **Slot 13.** Die Ziele liegen auf den Plätzen 11 (ElytraRace), 15 (Slender), 20 (Survival), 22 (Creative) und 24 (Build) sowie Spawn auf 31; der Rahmen belegt die erste und letzte Reihe sowie die Randspalten (9, 17, 18, 26). Platz 13 ist in der zweiten Reihe frei und liegt zwischen ElytraRace und Slender, also stehen die drei Minispiele nebeneinander. Test: `NavigatorLayoutTest` (Platz 13 ist Bounce, nur bei Recht) und `NavigatorRenderedLayoutTest` (gerendertes Menü mit Bounce-Recht).

- **Menüvarianten über die bestehende Teilmengen-Schleife.** `NavigatorModule` bildet für jede Teilmenge von `Destination.permissioned()` und jede Sprache ein geteiltes Inventar. Drei Rechte ergeben acht Teilmengen, also sechzehn Inventare bei zwei Sprachen. Keine Codeänderung an der Schleife nötig; die Zahl ergibt sich aus dem Enum. Verworfen: je Spieler ein Inventar (verletzt „Öffnen häuft nichts an“) und ein Lazy-Cache mit `computeIfAbsent` (zusätzliche Synchronisation und Tests für einen Gewinn, der bei sechzehn kleinen Inventaren nicht nötig ist). Test: `NavigatorPermissionTest` (Anzahl beim Start, Teilmengen-Zuordnung) und `NavigatorModuleLeakTest` (Listener beim Öffnen unverändert).

- **Schnellbeitritt über `ServerPicker.DESTINATIONS`.** `Set.of(ELYTRA_RACE, SLENDER, BOUNCE)`. Die Klickhinweise (`ClickAction.hints`) und die Klickzuordnung lesen dieselbe Menge, daher folgen Linksklick, Rechtsklick und Zeilen ohne Zusatzcode. Verworfen: ein eigenes Feld am Enum („schnell beitreten“), das zwei Quellen für dieselbe Menge schaffen würde. Test: `ServerPickerTest` (Menge enthält Bounce) und `NavigatorClickHintTest` (Zeilen für Bounce).

- **Recht bei Öffnen und Klick.** Die bestehende Prüfung `isAllowed` gilt unverändert für jedes gebundene Ziel. Der Test für „Recht zwischen Öffnen und Klick entzogen“ wird für Bounce gespiegelt; FakePermissionService steuert den Zustand, und Env-Ticks laufen explizit.

- **Telemetrie unverändert.** Das Attribut `kind` hängt nur an Build. Falls das Attribut `destination` den Enum-Namen nutzt, kommt ein bekannter Wert hinzu; die Kardinalität bleibt begrenzt. Test: `NavigatorTelemetryTest` prüft das Attribut für Bounce.

- **Javadoc korrigieren.** Der Kommentar in `Destination` sagt „row one of the grid“ für die Minispiele, obwohl sie in Reihe zwei liegen; beim Eintragen von Bounce wird er auf die korrekte Reihe und das neue Recht angepasst (keine Verhaltensänderung).

- **Keine neuen Texte.** Der Name „Bounce“ wird wie „ElytraRace“ und „Slender“ nicht übersetzt; die Klickhinweise nutzen die vorhandenen Schlüssel für Minispiele. Keine neuen Log-Zeilen, keine neuen Metriken oder Spans.

## Risiken / Abwägungen

- **Menüzahl wächst mit 2^n.** Bei einem vierten gebundenen Ziel wären es 32 Inventare je Sprache. → Mitigation: die Zahl steht im Spec-Requirement „Menü-Varianten bleiben sechzehn geteilte Inventare“; beim vierten Recht wird die Entscheidung (Lazy-Cache oder Menü je Spieler) neu bewertet, bevor das Recht eingeführt wird.
- **CloudNet-Task `Bounce` fehlt.** Der Klick zeigt dann die bestehende Ablehnung („Die Serverliste ist gerade nicht verfügbar.“) bzw. „Gerade läuft kein Server.“, kein Absturz. → Mitigation: Betriebsschritt in `docs/lobby-modules.md` und `README.md`.
- **Verwechslung der beiden grünen Einträge.** → Mitigation: Startfarbe `#00FA9A` statt `#7CFC00`; Sichtprüfung im Smoke-Test.
- **Rendering im Test an festen Slots.** `NavigatorModuleTest` und `NavigatorLayoutTest` listen Slots für Glasscheiben; sie müssen für die Menüs mit Bounce-Recht angepasst werden, nicht für das öffentliche Menü.
