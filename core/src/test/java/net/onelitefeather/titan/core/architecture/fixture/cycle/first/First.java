package net.onelitefeather.titan.core.architecture.fixture.cycle.first;

import net.onelitefeather.titan.core.architecture.fixture.cycle.second.Second;

/** Rule fixture: one half of a package cycle, which the cycle rule must report. */
public final class First {

    public Second second() {
        return new Second();
    }
}
