package net.onelitefeather.titan.core.architecture.fixture.sleep;

/** Rule fixture: waits on the wall clock, which the no-sleep rule for tests must report. */
public final class SleepsInTest {

    public void waitBriefly() throws InterruptedException {
        Thread.sleep(10L);
    }
}
