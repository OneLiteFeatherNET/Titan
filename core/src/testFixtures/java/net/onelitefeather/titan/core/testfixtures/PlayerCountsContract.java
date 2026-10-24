/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.onelitefeather.titan.core.testfixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.onelitefeather.titan.core.portal.PlayerCount;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.portal.ServiceCount;
import net.onelitefeather.titan.core.portal.SourceType;
import org.junit.jupiter.api.Test;

/**
 * What every {@link PlayerCounts} must do. The fake the module tests lean on and the provider
 * backed by the CloudNet bridge run the same cases, so the fake cannot drift from the real one.
 */
public interface PlayerCountsContract {

    SourceType TYPE = SourceType.TASK;
    String NAME = "Slender";
    ServiceCount FIRST = new ServiceCount("Slender-1", 7, 20);
    ServiceCount SECOND = new ServiceCount("Slender-2", 3, 25);

    /**
     * A fresh provider that reports exactly {@code running} for the source {@code type}/{@code
     * name} and nothing for any other source. Called once per test.
     */
    PlayerCounts countsOf(SourceType type, String name, ServiceCount... running);

    @Test
    default void running_listsTheServicesOfTheSourceInOrder() {
        PlayerCounts counts = countsOf(TYPE, NAME, FIRST, SECOND);

        assertEquals(List.of(FIRST, SECOND), counts.running(TYPE, NAME), "every running service of the source, in the order the provider has them");
    }

    @Test
    default void running_ofAnotherSource_isEmpty() {
        PlayerCounts counts = countsOf(TYPE, NAME, FIRST);

        assertTrue(counts.running(TYPE, "Bounce").isEmpty(), "a source nobody runs lists no services");
    }

    @Test
    default void count_sumsOnlineAndMaxOverTheRunningServices() {
        PlayerCounts counts = countsOf(TYPE, NAME, FIRST, SECOND);

        assertEquals(new PlayerCount(10, 45, true), counts.count(TYPE, NAME), "the count of a source is the sum over its services");
    }

    @Test
    default void count_ofASourceWithoutServices_isNotRunning() {
        PlayerCounts counts = countsOf(TYPE, NAME);

        assertEquals(PlayerCount.NOT_RUNNING, counts.count(TYPE, NAME), "a source without services is not running");
    }

    @Test
    default void count_ofAnotherSource_isNotRunning() {
        PlayerCounts counts = countsOf(TYPE, NAME, FIRST);

        assertEquals(PlayerCount.NOT_RUNNING, counts.count(TYPE, "Bounce"), "the services of one source are not counted for another");
    }

    @Test
    default void supports_theSourceTypeItWasAskedAbout() {
        PlayerCounts counts = countsOf(TYPE, NAME, FIRST);

        assertTrue(counts.supports(TYPE), "a provider that answers for a type supports it");
    }
}
