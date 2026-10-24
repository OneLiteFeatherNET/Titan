package net.onelitefeather.titan.core.architecture.fixture.clock;

import java.time.Instant;

/** Rule fixture: reads the system clock directly, which the no-system-clock rule must report. */
public final class ReadsSystemClock {

    public Instant now() {
        return Instant.now();
    }
}
