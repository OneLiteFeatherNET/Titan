# Spec Delta

## ADDED Requirements

### Requirement: Standardwerte und Betreiberdatei werden mit dem Parser des Betriebs gelesen
Die mitgelieferten Standardwerte einer Variante und eine Betreiber-`application.yaml` MÜSSEN mit demselben YAML-Parser gelesen werden, der im Betrieb läuft; jeder Abschnitt jeder Column der Variante MUSS mit dem erwarteten Typ und Wert ankommen, ohne dass ein Wert stillschweigend fehlt. Dieselbe Bedingung MUSS für die Tests jeder Column gelten, die Konfiguration aus YAML liest.

#### Scenario: Alle Abschnitte der Variante sind lesbar
- **WHEN** die zusammengeführten Standardwerte der Variante `cloudnet` geladen werden
- **THEN** ist der Abschnitt jeder Column der Variante vorhanden, und jeder Schlüssel liefert den Typ, den seine Column erwartet

#### Scenario: Betreiberdatei überschreibt einzelne Werte
- **WHEN** eine Betreiber-`application.yaml` mit Listen, verschachtelten Abschnitten und einem Zahlenwert einzelne Standardwerte überschreibt
- **THEN** gelten die überschriebenen Werte mit ihrem Typ, und alle übrigen Schlüssel behalten ihren Standardwert

#### Scenario: Column-Test liest YAML wie der Betrieb
- **WHEN** ein Test einer Column Konfiguration aus einer YAML-Datei über die `Config`-Fassade liest
- **THEN** ist der YAML-Parser des Betriebs auf der Klassenpfad-Laufzeit des Tests, ohne dass die Column ihn selbst einbinden muss
