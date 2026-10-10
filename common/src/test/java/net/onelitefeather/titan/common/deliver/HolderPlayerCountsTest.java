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

import java.util.List;
import java.util.Map;
import net.onelitefeather.titan.common.config.testing.CapturingLoggerFactory;
import net.onelitefeather.titan.core.portal.PlayerCount;
import net.onelitefeather.titan.core.portal.ServiceCount;
import net.onelitefeather.titan.core.portal.SourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HolderPlayerCountsTest {

    private final HolderPlayerCounts counts = new HolderPlayerCounts();

    @BeforeEach
    void emptyLog() {
        CapturingLoggerFactory.clear();
    }

    // The holder is static by nature; every test leaves it empty again.
    @AfterEach
    void emptyHolder() {
        TitanPlayerCountLookup.setLookup(null);
    }

    private static long linesAt(String level) {
        return CapturingLoggerFactory.messages().stream().filter(line -> line.startsWith(level + " ")).count();
    }

    private static Map<String, Object> row(String name, int online, int max) {
        return Map.of(PlayerCountLookup.NAME, name, PlayerCountLookup.ONLINE, online, PlayerCountLookup.MAX, max);
    }

    private static void failWith(Error error) {
        TitanPlayerCountLookup.setLookup(new PlayerCountLookup() {
            @Override
            public boolean supports(String type) {
                return true;
            }

            @Override
            public int[] lookup(String type, String name) {
                throw error;
            }

            @Override
            public List<Map<String, Object>> running(String type, String name) {
                throw error;
            }
        });
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

    @DisplayName("An empty holder lists no running services")
    @Test
    void emptyHolderListsNothing() {
        assertTrue(this.counts.running(SourceType.TASK, "Lobby").isEmpty(), "no bridge installed");
    }

    @DisplayName("The installed lookup's JDK rows are mapped to core service counts")
    @Test
    void runningRowsAreMapped() {
        TitanPlayerCountLookup.setLookup(new PlayerCountLookup() {
            @Override
            public boolean supports(String type) {
                return true;
            }

            @Override
            public int[] lookup(String type, String name) {
                return null;
            }

            @Override
            public List<Map<String, Object>> running(String type, String name) {
                return type.equals("task") && name.equals("Lobby") ? List.of(row("Lobby-1", 3, 20), row("Lobby-2", 0, 20)) : List.of();
            }
        });

        assertEquals(List.of(new ServiceCount("Lobby-1", 3, 20), new ServiceCount("Lobby-2", 0, 20)), this.counts.running(SourceType.TASK, "Lobby"), "the lookup's rows for the task");
        assertTrue(this.counts.running(SourceType.TASK, "Survival").isEmpty(), "nothing runs for another task");
    }

    @DisplayName("A linkage error of the bridge side surfaces as a runtime exception with the cause")
    @Test
    void linkageErrorBecomesRuntimeException() {
        NoClassDefFoundError error = new NoClassDefFoundError("eu/cloudnetservice/Gone");
        failWith(error);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> this.counts.running(SourceType.TASK, "Lobby"), "callers only handle runtime exceptions");
        assertEquals(error, thrown.getCause(), "the original error is kept");
    }

    @DisplayName("The first linkage error warns with the throwable, later ones only log DEBUG")
    @Test
    void linkageErrorWarnsOnce() {
        failWith(new NoClassDefFoundError("eu/cloudnetservice/Gone"));

        assertThrows(IllegalStateException.class, () -> this.counts.running(SourceType.TASK, "Lobby"));
        assertThrows(IllegalStateException.class, () -> this.counts.running(SourceType.TASK, "Lobby"));
        assertThrows(IllegalStateException.class, () -> this.counts.count(SourceType.TASK, "Lobby"));

        assertEquals(1, linesAt("WARN"), "one warning per kind of error");
        assertTrue(CapturingLoggerFactory.messages().getFirst().contains("NoClassDefFoundError"), "the warning names the error: " + CapturingLoggerFactory.messages().getFirst());
        assertEquals(2, linesAt("DEBUG"), "the repeats go to DEBUG");
    }

    @DisplayName("A lookup without its own listing yields an empty list")
    @Test
    void lookupWithoutListingIsEmpty() {
        install(true, new int[]{3, 20});

        assertTrue(this.counts.running(SourceType.TASK, "Survival").isEmpty(), "the default listing is empty");
    }
}
