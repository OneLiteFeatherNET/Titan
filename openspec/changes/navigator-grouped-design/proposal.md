# Proposal

## Why

Der Navigator verteilt seine Ziele auf feste Plätze, und sie sehen sich alle gleich: gleiche Glasscheiben, gleiche Zeilen, nur Name und Farbe unterscheiden sie. Die leeren Plätze sind mit grauem Glas gefüllt, was das Menü unruhig wirkt. Wer wissen will, wo gerade etwas los ist, muss die Serverauswahl öffnen, weil die Lore keine Zahl zeigt, die man auf einen Blick liest. Die Gruppen Minispiele und Server sind im Raster nicht zu erkennen.

## What Changes

- **Rahmen**: Reihe 0, die untere Reihe und die Randspalten bekommen eine einzige dunkle Glasscheibe ohne Namen statt der abwechselnden hellblauen und cyanen Scheiben. Innere Plätze ohne Ziel bleiben leer statt grauer Füllscheiben.
- **Gruppen**: Reihe 1 zeigt die sichtbaren Minispiele (ElytraRace, Bounce, Tamias, Slender), zentriert in den Plätzen 10 bis 16. Reihe 2 zeigt die sichtbaren Server (Survival, Creative, Build), zentriert in den Plätzen 19 bis 25. Ein Ziel hat damit eine Gruppe statt eines festen Platzes. Die Plätze innerhalb der Gruppe ergeben sich aus der Zahl der Ziele, die der Spieler sehen darf.
- **Unten**: Spawn bleibt auf Platz 31. Der Staging-Hub wandert von Platz 16 auf Platz 35 (untere rechte Ecke) und erscheint weiterhin nur mit einem Staging-Recht. Das Untermenü „Staging“ bleibt unverändert.
- **Einheitliche Lore**: Jedes Ziel des Hauptmenüs zeigt eine Statuszeile „● 12 Spieler“ (grün ab einem Spieler, grau bei null), eine kurze Beschreibungszeile, eine Leerzeile und danach die bestehenden Klickhinweise. Die Trennlinie in der Verlaufsfarbe entfällt. Die Texte liegen in `messages_de.properties` und `messages_en.properties`.
- **Anzahl und Glanz**: Die Stapelanzahl eines Ziels ist seine Spielerzahl, begrenzt auf 1 bis 64 (bei null Spielern 1). Solange mindestens ein Spieler auf dem Ziel ist, glänzt sein Symbol.
- **Titel**: „✦ Navigator ✦“ als Farbverlauf (MiniMessage-Gradient).
- **Aktualisierung**: Zahlen, Anzahl und Glanz folgen dem bestehenden Aktualisierungstakt `navigator.refreshSeconds`. Keine neue Konfiguration.

## Capabilities

### New Capabilities

Keine.

### Modified Capabilities

- `lobby-navigator`: Rahmen, Plätze und Gruppen, Titel, Lore der Modus-Einträge und die Plätze der gebundenen Ziele ändern sich. Siehe `specs/lobby-navigator/spec.md`.

## Impact

- **Code**: `features/navigator`. `Destination` verliert den festen Hauptmenü-Platz und bekommt eine Gruppe (MINIGAME, SERVER, STAGING). Die Platzrechnung und die Rahmenscheibe ziehen in `NavigatorLayout` (reine Logik), die Lore und die Stapelanzahl in `NavigatorMessages` bzw. `NavigatorItems`. `NavigatorModule` und `SharedNavigator` aktualisieren beim bestehenden Takt. Das Untermenü „Staging“ behält seine festen Plätze.
- **Nutzertexte**: neue Schlüssel für die Statuszeile und eine Beschreibung je Ziel in beiden Bundles. Der Titel ändert sich von „Navigator“ zu „✦ Navigator ✦“.
- **Konfiguration**: keine neue. Keine Abhängigkeit, kein Feature-Flag.
- **Abhängigkeit**: Diese Änderung setzt auf `navigator-staging-on-demand` auf, das Tamias, den Staging-Hub auf Platz 16 und das Untermenü einführt. Die Deltas hier ändern deren Anforderungen. Jene Änderung wird vor dieser archiviert.
- **Nicht brechend**: Rechte, Klicks, Weiterleitungen und Schnellbeitritt bleiben gleich. Nur Platzierung, Rahmen, Lore, Stapelanzahl und Glanz ändern sich.

## Delivery

PR-Titel (Conventional Commit, Typ `feat`, Scope `navigator`, nicht brechend):

`feat(navigator): group the navigator menu and show live activity`
