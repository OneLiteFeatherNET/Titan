package net.onelitefeather.titan.core.architecture.fixture.cycle.second;

import net.onelitefeather.titan.core.architecture.fixture.cycle.first.First;

/** Rule fixture: the other half of the package cycle. */
public final class Second {

    public First first() {
        return new First();
    }
}
