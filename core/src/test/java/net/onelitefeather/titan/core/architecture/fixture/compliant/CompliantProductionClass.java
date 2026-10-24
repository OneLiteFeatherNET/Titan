package net.onelitefeather.titan.core.architecture.fixture.compliant;

import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Rule fixture: follows every rule, so each rule must accept it. */
public final class CompliantProductionClass {

    private static final Logger LOG = LoggerFactory.getLogger(CompliantProductionClass.class);

    private final Clock clock;

    public CompliantProductionClass(Clock clock) {
        this.clock = clock;
    }

    public Instant now() {
        LOG.debug("reading the injected clock");
        return Instant.now(clock);
    }
}
