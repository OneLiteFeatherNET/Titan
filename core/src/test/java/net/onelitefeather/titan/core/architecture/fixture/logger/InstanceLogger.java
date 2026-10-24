package net.onelitefeather.titan.core.architecture.fixture.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Rule fixture: an instance logger, which the logger rule must report. */
public final class InstanceLogger {

    private final Logger log = LoggerFactory.getLogger(InstanceLogger.class);

    public void log() {
        log.info("instance");
    }
}
