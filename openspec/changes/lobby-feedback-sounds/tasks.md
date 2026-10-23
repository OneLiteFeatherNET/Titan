# Tasks

## 1. Vorlauf

- [x] 1.1 OpenSpec-Artefakte (`proposal.md`, `design.md`, `specs/lobby-tickle/spec.md`, `specs/lobby-portals/spec.md`, `tasks.md`) schreiben und mit `openspec validate --strict lobby-feedback-sounds` prüfen. Commit `docs(openspec): propose lobby-feedback-sounds`.
- [x] 1.2 Vanilla-Töne per `javap` gegen Minestom `2026.10.07-26.2` prüfen (`SoundEvents`, `SoundEffectPacket.pitch`).

## 2. Tickle (`features/tickle`)

- [ ] 2.1 Test zuerst (Unit, `TicklePairLimiterTest`, `AdjustableClock`): erster Ton erlaubt; zweiter innerhalb 2 s verweigert; ab 2 s erlaubt; anderes Paar nicht betroffen; Paar ist geordnet.
- [ ] 2.2 `TicklePairLimiter` implementieren (Clock injiziert, `ConcurrentHashMap`).
- [ ] 2.3 Test zuerst (Unit, `TickleSoundsTest`, `Random` mit festem Seed): Tonhöhe liegt immer in [0,9; 1,2].
- [ ] 2.4 Test zuerst (Env, `TickleSoundTest`, `env.tick()`): Kitzel-Ton für Kitzelnden und Nahe, nicht für fernen Spieler, nicht ohne Feder.
- [ ] 2.5 `TickleSounds` implementieren und in `TickleAttackHandler` nach der Nachricht verdrahten; `TickleModule` bekommt einen Konstruktor mit `RandomGenerator`, den der `@Inject`-Konstruktor mit dem Standardzufall belegt.

## 3. Portal (`features/portal`)

- [ ] 3.1 Test zuerst (Env, `PortalSoundTest`): Box-Durchgang spielt genau einen Teleport-Ton nur für den Spieler; Ring-Durchgang genau einen Ring-Ton und keinen Teleport-Ton; Umstehende hören nichts.
- [ ] 3.2 Test zuerst (Env): Abweisung spielt den Bass-Ton einmal; Abweisung während der Abklingzeit spielt nichts; Bleiben im Bereich spielt nicht erneut.
- [ ] 3.3 `PortalTrigger.onMove` nimmt den Abweisungs-Callback als Parameter (statt im Konstruktor); bestehende Tests in `PortalTriggerTest` anpassen.
- [ ] 3.4 `PortalSounds` implementieren (Box, Ring, Abweisung) und in `PortalModule` verdrahten: Ton vor `deliver.sendPlayer`.

## 4. Dokumentation

- [ ] 4.1 Prüfen, ob `docs/lobby-modules.md` die beiden Module beschreibt; wenn ja, Töne kurz ergänzen.
- [ ] 4.2 Spec-Delta mit der Implementierung abgleichen (Lautstärken, Tonhöhen, Reichweite).

## 5. Prüfung

- [ ] 5.1 `./gradlew build` grün; `openspec validate --strict lobby-feedback-sounds` grün.

## 6. Commits und Pull Request

- [ ] 6.1 Commit `docs(openspec): propose lobby-feedback-sounds`.
- [ ] 6.2 Commit `feat(sounds): play sounds when tickling and passing portals` für Code und Tests.
- [ ] 6.3 Pull Request öffnen mit dem Titel `feat(sounds): play sounds when tickling and passing portals`, Beschreibung auf Englisch, abschließend `https://claude.ai/code/session_01AM6yGQ33vvJqWg39vCAcYt`. Danach einmal `gh pr checks --watch`, Fehler beheben. Nicht mergen.
