package net.onelitefeather.titan.core.architecture.fixture.logger;

/** Rule fixture: a logger from System.getLogger, which the no-System.getLogger rule must report. */
public final class SystemLogger {

    private static final System.Logger LOG = System.getLogger(SystemLogger.class.getName());

    public void log() {
        LOG.log(System.Logger.Level.INFO, "system");
    }
}
