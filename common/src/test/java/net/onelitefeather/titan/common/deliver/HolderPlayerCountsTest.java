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
import java.util.function.BiFunction;
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

    /** Installs a lookup that supports every type and lists what {@code rows} answers. */
    private static void install(BiFunction<String, String, List<Map<String, Object>>> rows) {
        TitanPlayerCountLookup.setLookup(new PlayerCountLookup() {
            @Override
            public boolean supports(String type) {
                return true;
            }

            @Override
            public List<Map<String, Object>> running(String type, String name) {
                return rows.apply(type, name);
            }
        });
    }

    private static void survival(Map<String, Object>... services) {
        install((type, name) -> type.equals("task") && name.equals("Survival") ? List.of(services) : List.of());
    }

    @DisplayName("An empty holder reports every source as not running")
    @Test
    void emptyHolderIsNotRunning() {
        assertEquals(PlayerCount.NOT_RUNNING, this.counts.count(SourceType.TASK, "Survival"), "no bridge installed");
        assertTrue(this.counts.supports(SourceType.TASK), "an empty holder must not look like a misconfigured source");
    }

    @DisplayName("The running services of a source are summed into the provider-neutral count")
    @Test
    @SuppressWarnings("unchecked")
    void runningServicesAreSummed() {
        survival(row("Survival-1", 3, 20), row("Survival-2", 4, 30));

        assertEquals(new PlayerCount(7, 50, true), this.counts.count(SourceType.TASK, "Survival"), "online and max of both services");
    }

    @DisplayName("A running service without players still counts as running")
    @Test
    @SuppressWarnings("unchecked")
    void emptyServiceIsRunning() {
        survival(row("Survival-1", 0, 20));

        assertEquals(new PlayerCount(0, 20, true), this.counts.count(SourceType.TASK, "Survival"), "0 online of 20");
    }

    @DisplayName("A source without running services yields not running")
    @Test
    @SuppressWarnings("unchecked")
    void nothingListedIsNotRunning() {
        survival(row("Survival-1", 3, 20));

        assertEquals(PlayerCount.NOT_RUNNING, this.counts.count(SourceType.SERVICE, "Survival"), "nothing listed for the service");
    }

    @DisplayName("Support is asked from the installed lookup by type name")
    @Test
    void supportsDelegates() {
        TitanPlayerCountLookup.setLookup(new PlayerCountLookup() {
            @Override
            public boolean supports(String type) {
                return !type.equals("group");
            }

            @Override
            public List<Map<String, Object>> running(String type, String name) {
                return List.of();
            }
        });

        assertFalse(this.counts.supports(SourceType.GROUP), "group not supported");
        assertTrue(this.counts.supports(SourceType.TASK), "task supported");
    }

    @DisplayName("A failing lookup is not swallowed, so the caller can report it")
    @Test
    void failingLookupPropagates() {
        install((type, name) -> {
            throw new IllegalStateException("cloud unreachable");
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
        install((type, name) -> type.equals("task") && name.equals("Lobby") ? List.of(row("Lobby-1", 3, 20), row("Lobby-2", 0, 20)) : List.of());

        assertEquals(List.of(new ServiceCount("Lobby-1", 3, 20), new ServiceCount("Lobby-2", 0, 20)), this.counts.running(SourceType.TASK, "Lobby"), "the lookup's rows for the task");
        assertTrue(this.counts.running(SourceType.TASK, "Survival").isEmpty(), "nothing runs for another task");
    }

    @DisplayName("A linkage error of the bridge side surfaces as a runtime exception with the cause")
    @Test
    void linkageErrorBecomesRuntimeException() {
        NoClassDefFoundError error = new NoClassDefFoundError("eu/cloudnetservice/Gone");
        install((type, name) -> {
            throw error;
        });

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> this.counts.running(SourceType.TASK, "Lobby"), "callers only handle runtime exceptions");
        assertEquals(error, thrown.getCause(), "the original error is kept");
    }

    @DisplayName("The first linkage error warns with the throwable, later ones only log DEBUG")
    @Test
    void linkageErrorWarnsOnce() {
        install((type, name) -> {
            throw new NoClassDefFoundError("eu/cloudnetservice/Gone");
        });

        assertThrows(IllegalStateException.class, () -> this.counts.running(SourceType.TASK, "Lobby"));
        assertThrows(IllegalStateException.class, () -> this.counts.running(SourceType.TASK, "Lobby"));
        assertThrows(IllegalStateException.class, () -> this.counts.count(SourceType.TASK, "Lobby"));

        assertEquals(1, linesAt("WARN"), "one warning per kind of error");
        assertTrue(CapturingLoggerFactory.messages().getFirst().contains("NoClassDefFoundError"), "the warning names the error: " + CapturingLoggerFactory.messages().getFirst());
        assertEquals(2, linesAt("DEBUG"), "the repeats go to DEBUG");
    }
}
