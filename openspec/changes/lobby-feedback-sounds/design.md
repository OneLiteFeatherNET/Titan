# Design

## Context

Eine Column ist ein `@Singleton` unter `features/<name>` und hängt nur von `core` ab. Tickle läuft auf `EntityAttackEvent` (Priorität 600), Portal auf `PlayerMoveEvent` (900). Die Töne sind Vanilla-Sounds, die über Minestom-Pakete an Clients gehen. Kein Resource Pack, keine eigenen Sound-Events.

Bestehende Töne in der Lobby: `jumprun` spielt Töne nur an den Läufer (`Sound.Emitter.self()`), `dance` spielt positional an alle außer dem Tänzer (`playSoundExcept`). Beide Muster sind bekannt; die Entscheidungen unten folgen ihnen.

## Goals / Non-Goals

**Goals**

- Ein hörbarer Vanilla-Ton für Kitzeln, Portal-Durchgang (Box und Ring) und Abweisung.
- Ein Ton pro Ereignis; kein Ton bei Spam.
- Entscheidungen (Begrenzung, Auswahl des Tons) als reine Logik, ohne Server testbar; Zeit und Zufall von außen.

**Non-Goals**

- Eigene Sounds, Resource Packs, Konfiguration der Werte.
- Töne bei Übergabe-Fehlern, weil das Signal fehlt (siehe D5).

## Decisions

### D1 Kitzel-Ton: `entity.allay.item_given`, Lautstärke 0,6, Tonhöhe 0,9 bis 1,2

Der Allay-Ton klingt kurz, hell und verspielt, und er ist in Vanilla eindeutig ein Gluckser-Ton, kein Alarm. Kandidaten waren `entity.fox.sniff` (zu kurz und zu tierisch), `entity.villager.yes` (zu erwachsen, Zustimmung statt Lachen) und `block.amethyst_block.chime` (klingt nach Ereignis, nicht nach Kitzeln). Die Tonhöhe wird zufällig gewählt, damit mehrere Kitzel nicht gleich klingen. Der Zufall kommt aus einem injizierten `RandomGenerator`; Tests setzen einen festen Seed.

Lautstärke 0,6: Vanilla-Lautstärke 1,0 ist auf etwa 16 Blöcke hörbar; 0,6 bleibt nah am Spieler, ohne die Nachbarschaft zu stören.

### D2 Hörweite: positional am Ziel, an Spieler im Umkreis von 16 Blöcken

Der Ton hängt an der Position des gekitzelten Spielers, nicht am Kitzelnden, weil das Ziel das ist, was gekitzelt wurde. Gesendet wird nur an Spieler, deren Position im 16-Blöcke-Umkreis des Ziels liegt. Das ist die Hörweite eines Tons mit Lautstärke 1,0. Der Client dämpft ohnehin nach Abstand; die Begrenzung auf dem Server spart nur Pakete an Spieler, die den Ton ohnehin nicht hören. Ein Ton an alle Spieler der Instanz (`Instance.playSound`) wäre dagegen zu laut in einer vollen Lobby.

Der Kitzelnde steht beim Angriff in Reichweite (etwa drei Blöcke) und hört den Ton damit wie das Ziel.

### D3 Begrenzung pro Paar: 2 Sekunden, geordnet, injizierte Uhr

`TicklePairLimiter` merkt sich je geordnetem Paar (Kitzelnder, Ziel) den Zeitpunkt des letzten Tons. Innerhalb von 2 Sekunden gibt es keinen Ton. Das Paar ist geordnet, weil „A kitzelt B“ und „B kitzelt A“ getrennte Ereignisse sind.

Ehrlicher Hinweis: Die Abklingzeit des Kitzelns je Angreifer (Standard 4 Sekunden) begrenzt einen Angreifer schon heute. Die Paar-Begrenzung greift erst bei einer kürzeren konfigurierten Abklingzeit, und sie begrenzt nicht die Summe mehrerer Angreifer auf ein Ziel. Das ist gewollt: Der Ton ist kein Teil der Regel, sondern nur die Lautstärke-Bremse pro Paar. Eine Begrenzung pro Ziel wäre eine Regel mit anderer Bedeutung und bleibt offen.

Der Zustand ist eine `ConcurrentHashMap`, die je Aufruf abgelaufene Einträge entfernt. Die Lobby hat wenige Spieler, daher bleibt das billig, und es braucht keinen Disconnect-Listener.

### D4 Portal-Durchgang: genau ein Ton, nur für den Spieler

Der Ton wird im Move-Listener genau dann gespielt, wenn `PortalTrigger` ein Portal zur Übergabe liefert. Die Abklingzeit des Triggers garantiert, dass ein Durchgang genau einen Ton auslöst. Der Ton geht vor `deliver.sendPlayer`, weil danach der Spieler die Lobby verlässt.

- Box: `entity.enderman.teleport`, Lautstärke 0,5, Tonhöhe 1,0. Ein kurzes Wischen, kein langer Portal-Ton (`block.portal.travel` wäre zu lang).
- Ring (`Disc`): `entity.experience_orb.pickup`, Lautstärke 0,6, Tonhöhe 1,3. Eine helle, kurze Glocke, die sich von der Box unterscheidet. Der Ring-Ton ersetzt den Teleport-Ton, damit ein Durchgang genau einen Ton hat.

Der Ton geht nur an den Spieler (`Sound.Emitter.self()`), nicht an Umstehende. Ein Teleport-Geräusch für alle in der Nähe wäre in der Lobby Lärm ohne Information.

### D5 Abweisung: `block.note_block.bass`, nur für den Spieler, einmal

`PortalTrigger` meldet eine Abweisung je Portal und Betritt, und zwar nicht während der Abklingzeit (bestehende Regel). Der Callback wandert vom Konstruktor in den Aufruf von `onMove`. Der Move-Listener übergibt dabei eine Closure, die den bewegten Spieler kennt und so Telemetrie und Ton an den richtigen Spieler bringt. Der Konstruktor-Callback kannte keinen Spieler.

Ein Abweisungston bei offline oder vollen Zielen wird nicht gebaut. `Deliver.sendPlayer` ist `void`, und die Lobby erfährt weder, ob der Task existiert, noch ob er voll ist. Das Signal dafür müsste erst in den Deliver-Pfad; das ist eine eigene Änderung.

Das Requirement „Ein Recht am Portal schützt die Weiterleitung“ war bisher „nichts passiert“. Der Ton ist eine Rückmeldung, keine Weiterleitung und keine Chat-Nachricht. Die Spec sagt das ausdrücklich.

### D6 Konstanten im Code, eine Hilfsklasse je Column

Jede Column hat eine kleine Hilfsklasse (`TickleSounds`, `PortalSounds`) mit den Konstanten. Gemeinsame Logik gibt es nicht: Die beiden Columns spielen unterschiedliche Töne an unterschiedliche Kreise, und das gemeinsame Stück wäre nur ein `Sound.sound(...)`-Aufruf. Die Regel „gemeinsame Logik nur, wenn sie dieselbe Bedeutung hat“ greift hier.

## Risks / Trade-offs

- Ein Ton, der vor dem Verbindungsabbau gesendet wird, kann beim Client verloren gehen, wenn der Transfer sofort greift. Der Ton soll den Durchgang begleiten; wenn er nicht ankommt, verliert der Spieler nur den Ton, keine Übergabe.
- Positional an alle Nahen bedeutet bei großem Gedränge viele Pakete je Kitzel. Die Begrenzung pro Paar bremst das nicht gegen viele Angreifer; die Reichweite ist klein genug, dass es in der Praxis selten ist.
