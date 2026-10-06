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
package net.onelitefeather.titan.feature.jumprun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.avaje.config.Configuration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.event.player.PlayerChunkLoadEvent;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import net.onelitefeather.titan.feature.jumprun.course.Surface;
import net.onelitefeather.titan.feature.jumprun.course.TestBlocks;
import net.onelitefeather.titan.feature.jumprun.persistence.InMemoryRunRecords;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The material drawn for a block is what the player is sent, at the start, on advance and on
 * resend.
 */
@ExtendWith(MicrotusExtension.class)
class JumprunMaterialTest {

    private static final int LANDINGS = 8;

    private static boolean isPaletteMaterial(int stateId) {
        return Arrays.stream(Surface.values()).flatMap(surface -> TestBlocks.shipped().of(surface).blocks().stream()).anyMatch(material -> material.stateId() == stateId);
    }

    private static List<BlockChangePacket> blockChanges(List<ServerPacket> packets) {
        return packets.stream().filter(BlockChangePacket.class::isInstance).map(BlockChangePacket.class::cast).filter(packet -> packet.blockStateId() != Block.AIR.stateId()).toList();
    }

    @Test
    void theBlocksShownAtTheStartAreMaterialsOfTheirShapes(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);

            assertFalse(run.ahead().isEmpty(), "the start shows blocks");
            for (BlockChangePacket packet : run.ahead()) {
                assertTrue(isPaletteMaterial(packet.blockStateId()), "state " + packet.blockStateId() + " is in no palette");
            }
        }
    }

    @Test
    void theBlocksShownWhileAdvancingShowSeveralMaterials(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            Set<Integer> states = new HashSet<>();
            run.ahead().forEach(packet -> states.add(packet.blockStateId()));

            for (int landing = 0; landing < LANDINGS; landing++) {
                blockChanges(run.landOnNext()).forEach(packet -> states.add(packet.blockStateId()));
            }

            assertTrue(states.size() > 1, "more than one material over " + LANDINGS + " landings, got " + states);
            states.forEach(state -> assertTrue(isPaletteMaterial(state), "state " + state + " is in no palette"));
        }
    }

    @Test
    void aResendShowsTheMaterialsThatWereSentFirst(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            Map<Point, Integer> shown = new LinkedHashMap<>();
            run.ahead().forEach(packet -> shown.put(packet.blockPosition(), packet.blockStateId()));
            for (int landing = 0; landing < LANDINGS; landing++) {
                blockChanges(run.landOnNext()).forEach(packet -> shown.put(packet.blockPosition(), packet.blockStateId()));
            }
            Collector<BlockChangePacket> resent = run.connection().trackIncoming(BlockChangePacket.class);

            Point first = shown.keySet().stream().reduce((older, newer) -> newer).orElseThrow();
            env.process().eventHandler().call(new PlayerChunkLoadEvent(run.player(), first.blockX() >> 4, first.blockZ() >> 4));

            List<BlockChangePacket> packets = resent.collect();
            assertFalse(packets.isEmpty(), "the chunk of the newest window block is sent again");
            for (BlockChangePacket packet : packets) {
                assertEquals(shown.get(packet.blockPosition()), packet.blockStateId(), "material at " + packet.blockPosition() + " must not be drawn again");
            }
        }
    }

    @Test
    void anOverriddenPaletteChangesTheMaterialsSent(Env env) {
        Configuration config = TestBlocks.shippedWith(Surface.FULL, Map.of("lime_wool", "1"));
        try (JumprunFixture fixture = JumprunFixture.start(env, config)) {
            StartedRun run = StartedRun.start(env, fixture);
            Set<Integer> states = new HashSet<>();
            run.ahead().forEach(packet -> states.add(packet.blockStateId()));

            for (int landing = 0; landing < LANDINGS; landing++) {
                blockChanges(run.landOnNext()).forEach(packet -> states.add(packet.blockStateId()));
            }

            assertEquals(Set.of(Block.LIME_WOOL.stateId()), states, "only the configured material is sent");
        }
    }

    @Test
    void anInvalidPaletteAbortsTheStartWithItsKey(Env env) {
        Configuration config = TestBlocks.shippedWith(Surface.FULL, Map.of("lime_wool", "-1"));
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            JumprunModule module = new JumprunModule(titan.node(), () -> null, List::of, new InMemoryRunRecords(), RecordingLobbyItems::new, new RunMessages(), () -> JumprunFixture.SEED, new JumprunConfig(config), TestBlocks.BOUNDS, JumprunFixture.CLOCK);

            IllegalArgumentException abort = assertThrows(IllegalArgumentException.class, module::start);

            assertTrue(abort.getMessage().startsWith("jumprun.palettes.full.lime_wool"), "the abort names the key: " + abort.getMessage());
        }
    }

    @Test
    void anInvalidRainbowRerollIntervalAbortsTheStartWithItsKey(Env env) {
        assertInvalidRerollAborts(env, JumprunSettings.RAINBOW_REROLL_TICKS_KEY);
    }

    @Test
    void anInvalidUltraRerollIntervalAbortsTheStartWithItsKey(Env env) {
        assertInvalidRerollAborts(env, JumprunSettings.ULTRA_REROLL_TICKS_KEY);
    }

    private static void assertInvalidRerollAborts(Env env, String key) {
        Configuration config = TestBlocks.shippedConfiguration();
        config.setProperty(key, "0");
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            JumprunModule module = new JumprunModule(titan.node(), () -> null, List::of, new InMemoryRunRecords(), RecordingLobbyItems::new, new RunMessages(), () -> JumprunFixture.SEED, new JumprunConfig(config), TestBlocks.BOUNDS, JumprunFixture.CLOCK);

            IllegalArgumentException abort = assertThrows(IllegalArgumentException.class, module::start);

            assertTrue(abort.getMessage().startsWith(key), "the abort names the key: " + abort.getMessage());
        }
    }
}
