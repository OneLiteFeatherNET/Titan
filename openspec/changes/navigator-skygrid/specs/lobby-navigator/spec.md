# Spec Delta

## ADDED Requirements

### Requirement: Das Ziel SkyGrid erscheint nur mit Recht

Der Navigator MUSS das Ziel SkyGrid in der Serverreihe zeigen, wenn die Rechteprüfung für `titan.navigator.skygrid` beim Öffnen „erteilt“ ergibt; SkyGrid steht dort nach Build. Ergibt sie „verboten“ oder „nicht gesetzt“, MUSS der Navigator genau das Menü ohne SkyGrid zeigen, ohne Lücke und ohne Hinweis. Ein Klick auf SkyGrid MUSS das Recht erneut prüfen und nur bei Erteilung an das Weiterleitungsziel „SkyGrid“ übergeben.

#### Scenario: Spieler mit Recht sieht SkyGrid

- **WHEN** einem Spieler `titan.navigator.skygrid` erteilt ist und er den Navigator öffnet
- **THEN** zeigt die Serverreihe SkyGrid hinter den übrigen sichtbaren Servern, und die Reihe bleibt zentriert

#### Scenario: Spieler ohne Recht sieht kein SkyGrid

- **WHEN** `titan.navigator.skygrid` einem Spieler nicht gesetzt oder verboten ist und er den Navigator öffnet
- **THEN** enthält die Serverreihe kein SkyGrid und zeigt das Menü, das er ohne dieses Recht sähe

#### Scenario: Spieler mit Recht wählt SkyGrid

- **WHEN** einem Spieler `titan.navigator.skygrid` erteilt ist und er im Navigator auf SkyGrid klickt
- **THEN** wird eine Weiterleitung zum Ziel „SkyGrid“ angestoßen, und der Navigator schließt

#### Scenario: Recht wird zwischen Öffnen und Klick entzogen

- **WHEN** ein Spieler mit Recht den Navigator öffnet, ihm danach `titan.navigator.skygrid` entzogen wird und er auf SkyGrid klickt
- **THEN** wird keine Weiterleitung angestoßen, der Navigator schließt, und der Spieler erhält keine Chat-Nachricht
