# Design

## Kontext

Der Navigator zeigt je Öffnung eine geteilte Aves-Inventory (`SharedNavigator`, öffentlich und Team). Ihre Items, also auch die Lore, gelten für alle Spieler, die das Menü öffnen. Eine übersetzte Zeile braucht aber die Sprache des Betrachters, und Minestom 26.1 rendert ein translatable Component nur über `GlobalTranslator` (sonst leer).

## Entscheidungen

- **Eine Inventory je Sprache.** Der Navigator hält je Menüart (öffentlich/Team) eine Inventory für Deutsch und eine für Englisch. Öffnen wählt die Sprache des Spielers, unbekannte Sprachen fallen auf Englisch zurück. Verworfen: Lore für alle in Englisch (verletzt de), ein Umbau auf `GlobalTranslatedInventoryBuilder` (größere Änderung an Layout und Zählerupdates), Übersetzung durch den Client (Titan-Schlüssel sind dem Client unbekannt).
- **Übersetzungen in Bundles.** `NavigatorMessages` wie `SpawnMessages`: `MiniMessageTranslationStore` mit `titan.navigator.messages`, bei `@PostConstruct` in den `GlobalTranslator` eingehängt, bei `@PreDestroy` entfernt.
- **Vier Textvarianten, eine Auswahl.** Linksklick: „Server wählen“ bei `ServerPicker.DESTINATIONS`, sonst „verbinden“. Rechtsklick nur bei vorhandenem Portal. Die Auswahl steht in `NavigatorMessages` und ist eine Tabelle aus Schlüsseln.
- **Portal zur Laufzeit.** Der Hinweis wird bei jedem Aktualisierungszyklus neu berechnet (`itemOf`), so wie die Spielerzahl. Der Rechtsklick nutzt weiterhin `portalFor`, damit Anzeige und Verhalten gleich sind.
- **Spawn ohne Hinweis.** Spawn hat keine Maustasten-Abhängigkeit, daher keine Zeile.
- **Lore-Reihenfolge.** Spielerzahl zuerst (unverändert), dann Klickhinweis.

## Nicht-Ziele

- Die übrigen Navigator-Texte (Titel, Ziel-Namen, „Server wählen“-Liste von #424) werden nicht übersetzt.
- Keine neue Konfiguration; Texte liegen in den Bundles.
