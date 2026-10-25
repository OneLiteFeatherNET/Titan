# Spec Delta

## ADDED Requirements

### Requirement: Fangen ist ein Opt-in über die Einstellung
_Fangen is off by default and follows the player's setting from lobby-settings._

Die Lobby MUSS Fangen über die Einstellung `titan:tag.enabled` aus `lobby-settings` steuern, Standard „aus“. Ein Spieler MUSS nur aktiv sein, wenn die Einstellung „an“ ist. Die Lobby MUSS keinen eigenen Schalter und kein Hotbar-Item für Fangen anbieten.

#### Scenario: Standard
- **WHEN** ein Spieler ohne gesetzte Einstellung die Lobby betritt
- **THEN** ist er nicht aktiv, und seine Treffer und sein Schutz bleiben unverändert

#### Scenario: Einschalten über das Einstellungsmenü
- **WHEN** ein Spieler die Einstellung auf „an“ setzt
- **THEN** wird er aktiv und kann am Spiel teilnehmen, ohne die Lobby neu zu betreten

### Requirement: Der Befehl schaltet dieselbe Einstellung um
_/tag is a shortcut for the same setting, not a second source of truth._

Der Befehl `/tag` MUSS dieselbe Einstellung umschalten, die das Einstellungsmenü schreibt. Ein Befehl aus der Konsole MUSS eine Meldung erhalten, dass nur Spieler mitspielen können, und nichts ändern. Ist die Einstellungs-Schnittstelle nicht erreichbar, MUSS der Befehl eine Meldung zeigen und den Zustand unverändert lassen.

#### Scenario: Befehl im Spiel
- **WHEN** ein Spieler mit ausgeschaltetem Fangen `/tag` eingibt
- **THEN** ist die Einstellung danach „an“ und er ist aktiv

#### Scenario: Befehl aus der Konsole
- **WHEN** die Konsole `tag` ausführt
- **THEN** ändert sich keine Einstellung, und die Konsole erhält eine Meldung

#### Scenario: Schnittstelle nicht erreichbar
- **WHEN** ein Spieler `/tag` eingibt und die Einstellungen nicht lesbar sind
- **THEN** bleibt er inaktiv, und er erhält eine Meldung

### Requirement: Genau ein Fänger pro Instanz
_From two active players on, exactly one player is the tagger; the role moves at random when it is lost._

Die Lobby MUSS in jeder Instanz bei zwei oder mehr aktiven Spielern genau einen Fänger führen. Verliert der Fänger seine Aktivität, MUSS die Rolle zufällig an einen anderen aktiven Spieler gehen. Bei weniger als zwei aktiven Spielern MUSS es keinen Fänger geben. Der allein wartende Spieler MUSS den Hinweis auf einen zweiten Spieler genau einmal kurz in der Actionbar sehen, beim Einschalten, beim Beitritt zu einer Lobby mit Fangen oder wenn der andere Spieler die Instanz verlässt. Der Hinweis DARF danach nicht dauerhaft wiederholt werden.

#### Scenario: Zweiter aktiver Spieler
- **WHEN** der zweite Spieler einer Instanz aktiv wird und keiner Fänger ist
- **THEN** wird einer der beiden zufällig Fänger

#### Scenario: Fänger schaltet ab
- **WHEN** der Fänger seine Einstellung auf „aus“ setzt und mindestens zwei andere aktiv sind
- **THEN** wird zufällig einer der verbleibenden aktiven Spieler Fänger

#### Scenario: Nur ein aktiver Spieler
- **WHEN** nur noch ein aktiver Spieler in der Instanz ist
- **THEN** gibt es keinen Fänger, und dieser Spieler sieht den Hinweis, dass er auf einen zweiten Spieler wartet, einmal kurz und danach nicht mehr

### Requirement: Der Fänger ist ohne Ressourcenpaket erkennbar
_Visibility uses vanilla means only: glowing, red particles and the actionbar, no own scoreboard team._

Die Lobby MUSS den Fänger für alle Zuschauer der Instanz hervorheben, nur mit Vanilla-Mitteln: Leuchten (Standardfarbe) und rote Partikel. Die Lobby MUSS kein eigenes Scoreboard-Team für den Fänger anlegen, damit es nicht mit Namenspräfixen anderer Module kollidiert. Die Actionbar aller aktiven Spieler MUSS anzeigen, wer Fänger ist.

#### Scenario: Fänger sichtbar
- **WHEN** ein Spieler Fänger wird
- **THEN** leuchtet er, hat rote Partikel, und die Actionbar aller aktiven Spieler nennt ihn

### Requirement: Ein Treffer des Fängers übergibt die Rolle
_A melee hit by the tagger on another active player passes the role, with no damage and no knockback._

Ein Nahkampftreffer des Fängers auf einen anderen aktiven Spieler MUSS die Rolle an diesen übergeben. Der Treffer MUSS ohne Schaden und ohne Rückstoß bleiben. Beide Spieler MUSS einen kurzen Ton hören, der neue Fänger MUSS einen Titel sehen.

#### Scenario: Erfolgreicher Treffer
- **WHEN** der Fänger einen anderen aktiven Spieler trifft, der nicht der letzte Fänger gegen ihn war
- **THEN** wird das Ziel Fänger, erleidet keinen Schaden und keinen Rückstoß, und beide hören einen Ton

#### Scenario: Rücktausch innerhalb der Sperre
- **WHEN** der neue Fänger innerhalb von 5 Sekunden seinen Vorgänger trifft
- **THEN** bleibt die Rolle beim Fänger, und der Treffer wird ohne Schaden abgebrochen

#### Scenario: Treffer ohne Fänger
- **WHEN** zwei aktive Spieler einander treffen und keiner von beiden Fänger ist
- **THEN** erleiden sie keinen Schaden und keinen Rückstoß, und die Rolle ändert sich nicht

### Requirement: Schutz für inaktive Spieler bleibt unverändert
_Players who are not active are never affected by tag rules._

Die Lobby MUSS für Treffer, bei denen Angreifer oder Ziel nicht aktiv sind, keine Fangen-Regel anwenden. Der Schutz dieser Spieler MUSS so bleiben, wie er ohne Fangen wäre.

#### Scenario: Treffer auf inaktiven Spieler
- **WHEN** ein aktiver Spieler einen inaktiven Spieler trifft
- **THEN** greift keine Fangen-Regel, und die Lobby verhält sich wie ohne Fangen

### Requirement: Inaktivität des Fängers gibt die Rolle weiter
_An idle tagger must not block the game for everyone else._

Hat der Fänger 120 Sekunden lang weder sich bewegt, getroffen noch eine Eingabe gemacht, MUSS die Rolle an einen anderen aktiven Spieler gehen. Die Zeit MUSS aus der injizierten Uhr kommen.

#### Scenario: Fänger bleibt still
- **WHEN** der Fänger 120 Sekunden ohne Bewegung, Treffer und Eingabe ist
- **THEN** wird ein anderer aktiver Spieler Fänger, nicht derselbe Spieler

#### Scenario: Inaktiver Nicht-Fänger
- **WHEN** ein aktiver Spieler ohne Rolle 120 Sekunden still ist
- **THEN** bleibt er im Spiel, und er kann getroffen werden

### Requirement: Tanz, Sitzen, Elytra, Sprungtest und Serverwechsel
_Each feature has a fixed rule for the tag game; none of them is ignored._

Die Lobby MUSS folgende Regeln anwenden:
- Tanzt der Fänger und trifft, endet sein Tanz, und die Rolle wird trotzdem weitergegeben.
- Wird ein tanzender Spieler getroffen, läuft sein Tanz weiter.
- Sitzende Spieler bleiben aktiv; Sitzen ändert die Teilnahme nicht.
- Während eines Gleitflugs ist die Rolle eingefroren: Treffer zählen nicht.
- Ein Spieler im Sprungtest-Lauf oder in einer Übergabe zu einem anderen Server ist nicht aktiv und verliert die Rolle, falls er Fänger ist.

#### Scenario: Fänger tanzt und trifft
- **WHEN** der tanzende Fänger einen aktiven Spieler trifft
- **THEN** endet sein Tanz, und das Ziel wird Fänger

#### Scenario: Getroffener Tänzer
- **WHEN** ein aktiver Spieler im Tanz getroffen wird
- **THEN** läuft sein Tanz weiter, und die Rolle geht über den Treffer wie üblich

#### Scenario: Gleitflug
- **WHEN** der Fänger oder das Ziel gerade mit der Elytra fliegt
- **THEN** zählt der Treffer nicht, und die Rolle bleibt, wie sie ist

#### Scenario: Sprungtest-Lauf startet
- **WHEN** der Fänger einen Sprungtest-Lauf startet
- **THEN** verliert er die Rolle, und ein anderer aktiver Spieler wird Fänger, falls zwei übrig sind

#### Scenario: Serverwechsel
- **WHEN** ein aktiver Spieler eine Übergabe zu einem anderen Server beginnt oder die Verbindung verliert
- **THEN** ist er nicht mehr aktiv, und verlor er die Rolle, wird sie neu vergeben

### Requirement: Telemetrie und Meldungen
_Counters carry no player ids; messages exist in German and English._

Die Lobby MUSS den Zähler `titan.tag.tags{result}` (`tagged`, `cooldown`) und den Zähler `titan.tag.toggles{state}` (`on`, `off`) führen, ohne Spieler-Id. Alle Nutzertexte MUSS es in Deutsch und Englisch geben, mit denselben Schlüsseln.

#### Scenario: Treffer gezählt
- **WHEN** eine Rollenübergabe stattfindet
- **THEN** erhöht sich `titan.tag.tags` mit dem Ergebnis `tagged`, ohne Spieler-Id

#### Scenario: Schalter gezählt
- **WHEN** ein Spieler Fangen einschaltet
- **THEN** erhöht sich `titan.tag.toggles` mit dem Zustand `on`
