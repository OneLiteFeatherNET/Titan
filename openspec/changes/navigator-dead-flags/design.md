# Design

Kein Querschnittsthema: ein Modul, keine neue Abhängigkeit, keine Migration. Das Dokument hält nur die zwei Entscheidungen fest, die sonst beim Umsetzen offen blieben.

## Context

Siehe proposal.md. `ConfigFeatureFlags` leitet die „bekannten“ Flags aus dem Abschnitt `features` der gemergten, mitgelieferten `application.yaml` ab (`knownFlagsIn`); `isActive` ist `known && Config.getBool(...)`. Eine entfernte Flag ist damit unbekannt, `isActive` liefert `false`, `exists` ebenfalls; niemand wirft oder warnt. Nichts außer `Destination` liest Flag-Namen (`exists` wird nirgends produktiv aufgerufen). Die in-flight-Änderung `navigator-build-destination` (Build-Ziel) ist bereits in `main` umgesetzt; ihr Delta ändert dieselbe Anforderungsdatei, aber andere Anforderungen (dieser Change nutzt nur ADDED).

## Goals / Non-Goals

**Goals:** Die mitgelieferten Standardwerte spiegeln genau die Flags, die ein Ziel schalten; der Flag-Name existiert im Code einmal.

**Non-Goals:** Ob das `FeatureFlags`-SPI für eine einzige Flag noch lohnt (offene Frage an den Nutzer, eigener Change). Keine Warnung für entfernte Flags (siehe D2). Keine Änderung an `runtime`, `core` oder `ConfigFeatureFlags`.

## Decisions

**D1: Konstante `Destination.SLENDER_FLAG = "NAVIGATOR_SLENDER"`.** `Destination` ist package-privat, alle Navigator-Tests liegen im selben Paket; ein eigener Holder wäre für einen String Ballast. `SLENDER(…, SLENDER_FLAG, null)` und alle Tests (`declare(Destination.SLENDER_FLAG, …)`) nutzen sie. Eine Konstante im Enum-Kopf kann per einfachem Namen „illegal forward reference“ auslösen; dann qualifiziert (`Destination.SLENDER_FLAG`) referenzieren, und nur wenn auch das nicht kompiliert, in eine verschachtelte `Flags`-Klasse verschieben. Alternative Konfigurationsschlüssel-Konstante im `core`-SPI: verworfen, `core` kennt keine Navigator-Flags. Getestet durch die Unit-Tests aus D2 (Unit; SRP: der Name hat einen Ort).

**D2: Standardwerte und Ziele werden gegeneinander geprüft.** `DefaultNavigatorFeatureFlagsTest` prüft heute nur „jede Flag eines Ziels steht in den Standardwerten“. Neu zusätzlich die Gegenrichtung: die Schlüssel von `features` sind genau die Flags der `Destination`-Werte. Das verhindert, dass eine Flag wieder ohne Ziel eingeführt wird, ohne eine Laufzeitprüfung oder Warnung einzubauen. Eine Warnung für vom Betreiber gesetzte, unbekannte Flags wurde erwogen und verworfen: `ConfigFeatureFlags` kennt die Betreiber-Schlüssel nicht (und dürfte sie ohne Ordnungsprüfung des ganzen `features`-Abschnitts nicht auflisten), und ein WARN pro Start für eine wirkungslose Zeile hat keinen Handlungsnutzen (ERROR/WARN heißt „Betreiber muss handeln“). Test: Unit, ohne Server, eigene `Configuration`-Instanz je Test (F.I.R.S.T. wie bisher).

**D3: Verlaufsfarbe als sichtbares Verhalten testen.** Der Test entfaltet `Destination.SLENDER.item()` (`customName`) Buchstabe für Buchstabe und prüft `#616161` am ersten und `#e80000` am letzten Zeichen. Er ist heute rot (letzter Buchstabe `#80000C`). Kein Test auf den Rohstring; das Literal in `Destination` bleibt MiniMessage wie bei den übrigen Zielen. Unit, kein Minestom-Server.

## Risks / Trade-offs

- [Betreiber-Konfig mit entfernten Flags] → wirkungslos und still, siehe proposal.md „Kompatibilität“; README nennt, dass nur `NAVIGATOR_SLENDER` existiert.
- [Konflikt mit `navigator-build-destination`] → beide Deltas nur ADDED bzw. disjunkte Anforderungen; beim Archivieren keine Überschneidung.
