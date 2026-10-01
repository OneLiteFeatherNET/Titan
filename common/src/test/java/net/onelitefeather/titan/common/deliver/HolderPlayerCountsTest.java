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
package net.onelitefeather.titan.common.deliver;

import net.onelitefeather.titan.core.portal.PlayerCount;
import net.onelitefeather.titan.core.portal.SourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HolderPlayerCountsTest {

    private final HolderPlayerCounts counts = new HolderPlayerCounts();

    // The holder is static by nature; every test leaves it empty again.
    @AfterEach
    void emptyHolder() {
        TitanPlayerCountLookup.setLookup(null);
    }

    private static void install(boolean supportsGroups, int[] answer) {
        TitanPlayerCountLookup.setLookup(new PlayerCountLookup() {
            @Override
            public boolean supports(String type) {
                return supportsGroups || !type.equals("group");
            }

            @Override
            public int[] lookup(String type, String name) {
                return type.equals("task") && name.equals("Survival") ? answer : null;
            }
        });
    }

    @DisplayName("An empty holder reports every source as not running")
    @Test
    void emptyHolderIsNotRunning() {
        assertEquals(PlayerCount.NOT_RUNNING, this.counts.count(SourceType.TASK, "Survival"), "no bridge installed");
        assertTrue(this.counts.supports(SourceType.TASK), "an empty holder must not look like a misconfigured source");
    }

    @DisplayName("An installed lookup is translated into the provider-neutral count")
    @Test
    void installedLookupAnswers() {
        install(true, new int[]{3, 20});

        assertEquals(new PlayerCount(3, 20, true), this.counts.count(SourceType.TASK, "Survival"), "lookup values");
    }

    @DisplayName("A lookup that finds nothing running yields not running")
    @Test
    void nullMeansNotRunning() {
        install(true, new int[]{3, 20});

        assertEquals(PlayerCount.NOT_RUNNING, this.counts.count(SourceType.SERVICE, "Survival"), "null from the lookup");
    }

    @DisplayName("Support is asked from the installed lookup by type name")
    @Test
    void supportsDelegates() {
        install(false, null);

        assertFalse(this.counts.supports(SourceType.GROUP), "group not supported");
        assertTrue(this.counts.supports(SourceType.TASK), "task supported");
    }

    @DisplayName("A failing lookup is not swallowed, so the caller can report it")
    @Test
    void failingLookupPropagates() {
        TitanPlayerCountLookup.setLookup(new PlayerCountLookup() {
            @Override
            public boolean supports(String type) {
                return true;
            }

            @Override
            public int[] lookup(String type, String name) {
                throw new IllegalStateException("cloud unreachable");
            }
        });

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> this.counts.count(SourceType.TASK, "Survival"), "the exception crosses the holder");
        assertEquals("cloud unreachable", thrown.getMessage(), "the original failure");
    }
}
