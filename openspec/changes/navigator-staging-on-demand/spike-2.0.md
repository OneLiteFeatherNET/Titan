# Spike 2.0: CloudNet-Dienste starten und stoppen

Gegen `driver-api-4.0.0-RC18-SNAPSHOT` (Gradle-Cache, `javap` und Quellcode-Jar).

## (a) Konfiguration für einen Task

- `ServiceTaskProvider.serviceTask(String)` liefert `ServiceTask` oder `null`; `serviceTaskAsync(String)` liefert `CompletableFuture<ServiceTask>`.
- `ServiceConfiguration.builder(ServiceTask)` übernimmt Taskname, Runtime, Groups, Templates, Deployments, JVM-Optionen, `autoDeleteOnStop` und `staticService` aus dem Task.
- `.build()` liefert die `ServiceConfiguration`. `null` als Task wirft eine NPE.
- Der Adapter setzt `autoDeleteOnStop(true)` explizit. Sonst bliebe ein gestoppter Staging-Dienst mit `autoDeleteOnStop=false` als `PREPARED` (also „STARTING“) in der Liste, und der Doppelstart-Schutz würde nie wieder freigeben.

## (b) Erzeugen und Starten

- `CloudServiceFactory.createCloudService(ServiceConfiguration)` liefert `ServiceCreateResult` und startet den Dienst nicht. Der Javadoc sagt „Creates and prepares“. Der Dienst steht danach im Zustand `PREPARED`.
- Zum Starten braucht es `SpecificCloudServiceProvider.start()` (Default auf `updateLifecycle(RUNNING)`) oder `startAsync()` (`CompletableFuture<Void>`). Den Dienst spricht man über `CloudServiceProvider.serviceProvider(UUID)` an.
- `createCloudServiceAsync(...)` liefert `CompletableFuture<ServiceCreateResult>`.
- `ServiceConfiguration.createNewServiceAsync()` ruft `createCloudServiceAsync` über `InjectionLayer.boot()` auf. Der Adapter nutzt diesen Weg, weil CloudNet ihn selbst nutzt und so keine zweite Injektionsstelle nötig ist.
- `ServiceCreateResult.State`: `CREATED` (`serviceInfo()` gesetzt), `DEFERRED` (nur mit Retry-Konfiguration, die der Adapter nicht setzt), `FAILED`.

## (c) Lebenszyklus (`ServiceLifeCycle`)

- `PREPARED`: nach dem Erzeugen. Start, Stopp und Löschen sind erlaubt. Auch der Zustand nach einem Stopp, wenn `autoDeleteOnStop=false` ist.
- `RUNNING`: der Prozess wurde gestartet. Das heißt nicht, dass der Dienst verbunden ist oder Spieler annimmt. Das Spielerlimit kommt erst später (`TitanPlayerLimit`).
- `STOPPED`: Übergangszustand. Danach folgt `PREPARED` (ohne Auto-Delete) oder `DELETED`.
- `DELETED`: entfernt.
- Erlaubte Wechsel: `PREPARED` → `RUNNING`, `STOPPED`, `DELETED`; `RUNNING` → `STOPPED`, `DELETED`; `STOPPED` → `PREPARED`, `DELETED`.
- Mapping auf `Phase`: `PREPARED` → `STARTING`, `RUNNING` → `RUNNING`, `STOPPED` und `DELETED` → `STOPPED`, unbekannt oder fehlend → `STARTING` (D5).

## Konsequenzen für den Adapter

- Start: `serviceTaskAsync` → `builder(task).autoDeleteOnStop(true).build().createNewServiceAsync()` → bei `CREATED` `serviceProvider(uuid).startAsync()`. Der Rückgabewert `true` heißt „Anfrage abgesendet“. Das Ergebnis kommt asynchron und wird geloggt.
- Stopp: `serviceByNameAsync(name)` → `serviceProvider(uuid).stopAsync()`. Ein zweiter Stopp desselben Dienstes ist in CloudNet ein No-op (`STOPPED` → `STOPPED` ist kein erlaubter Wechsel). Der Adapter kann das synchron nicht als „abgelehnt“ melden. Der Rückgabewert `true` heißt auch hier „abgesendet“.
- Lesen: `servicesByTask(String)` ist synchron. Es läuft nur im Lesethread des Navigators (D6), wie die bestehenden Spielerzahlen.
- Nicht geprüft (Abschnitt 7.2, manuell): ob `ServiceTaskProvider` und `CloudServiceProvider` in `InjectionLayer.ext()` gebunden sind. Die bestehende Bridge holt den Provider aus `ext()`, für den Task-Provider ist das eine Annahme.
