package net.onelitefeather.titan.core.architecture.fixture.acyclic.one;

import net.onelitefeather.titan.core.architecture.fixture.acyclic.two.Two;

/** Rule fixture: depends on the other package only one way, so the cycle rule must accept it. */
public final class One {

    public Two two() {
        return new Two();
    }
}
