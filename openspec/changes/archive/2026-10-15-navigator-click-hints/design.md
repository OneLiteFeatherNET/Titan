# Design

## Kontext

Der Navigator zeigt je Öffnung eine geteilte Aves-Inventory (`SharedNavigator`, öffentlich und Team). Ihre Items, also auch die Lore, gelten für alle Spieler, die das Menü öffnen. Eine übersetzte Zeile braucht aber die Sprache des Betrachters, und Minestom 26.1 rendert ein translatable Component nur über `GlobalTranslator` (sonst leer).

## Entscheidungen

- **Eine Inventory je Menüart und Sprache.** Der Navigator hält je Menüart (öffentlich/Team) eine Inventory für Deutsch und eine für Englisch. Öffnen wählt die Sprache des Spielers, unbekannte Sprachen fallen auf Englisch zurück.
- **Warum nicht `GlobalTranslatedInventoryBuilder`.** Die Lobbyswitcher-Liste (`SwitcherInventory.java`) nutzt diesen Aves-Builder bereits für eine übersetzte Liste, er ist also kein neues Muster im Projekt. Für den Navigator sprechen dagegen: Aves' eigene Klick- und Schließ-Listener feuern für übersetzte Builder nicht, der Switcher muss Klicks und Schließen deshalb selbst weiterleiten. Der Navigator behält den nativen `GlobalInventoryBuilder` je Inventory, weil seine Klick-Listener, der Listener-Leak-Test und das Aktualisieren der Stacks an Ort und Stelle (`SharedNavigator.showItems`) darauf aufbauen. Dazu kommt: Nur die Hinweiszeile ist übersetzt, die übrigen Texte nicht. Verworfen sind außerdem Lore für alle in Englisch (verletzt de) und Übersetzung durch den Client (Titan-Schlüssel sind dem Client unbekannt).
- **Übersetzungen in Bundles.** `NavigatorMessages` wie `SpawnMessages`: `MiniMessageTranslationStore` mit `titan.navigator.messages`, bei `@PostConstruct` in den `GlobalTranslator` eingehängt, bei `@PreDestroy` entfernt.
- **Vier Textvarianten, eine Auswahl.** Linksklick: „Server wählen“, wenn `ServerPicker.opensFor` für die gelesenen laufenden Dienste wahr ist, sonst „verbinden“. Rechtsklick nur bei vorhandenem Portal. Die Auswahl steht in `NavigatorMessages` und ist eine Tabelle aus Schlüsseln.
- **Serverauswahl folgt dem Aktualisierungszyklus.** Der Navigator liest je Ziel die laufenden Dienste einmal pro Zyklus (`TaskPlayers.services`). Die Spielerzahl wird daraus summiert und der Hinweis mit `ServerPicker.opensFor` berechnet, damit beide Zeilen aus derselben Lesung stammen. Ändert sich die Anzahl der Dienste, zeigt der Hinweis das bis zum nächsten Zyklus; der Klick selbst liest die Dienste frisch.
- **Portal beim Öffnen frisch.** `itemOf` wird beim Öffnen des Menüs für dessen Sprache neu ausgeführt, bevor die Inventory gezeigt wird. Ein seit dem letzten Zyklus hinzugekommenes oder entferntes Portal erscheint damit sofort. Der Rechtsklick nutzt weiterhin `portalFor` zum Klickzeitpunkt, damit Anzeige und Verhalten gleich sind.
- **Spawn ohne Hinweis.** Spawn hat keine Maustasten-Abhängigkeit, daher keine Zeile.
- **Lore-Reihenfolge.** Spielerzahl zuerst (unverändert), dann Klickhinweis.

## Nicht-Ziele

- Die übrigen Navigator-Texte (Titel, Ziel-Namen, „Server wählen“-Liste von #424) werden nicht übersetzt.
- Keine neue Konfiguration; Texte liegen in den Bundles.
