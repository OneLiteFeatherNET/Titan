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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;
import net.minestom.server.event.EventNode;
import net.minestom.server.timer.Scheduler;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.junit.jupiter.api.Test;

class JumprunBoundsTest {

    @Test
    void startingWithoutTheLobbyHeightBoundsFailsClearly() {
        JumprunModule module = new JumprunModule(EventNode.all("unused"), () -> null, List::of, new InMemoryRunRecords(), Optional.empty(), Runnable::run, Scheduler.newScheduler(), RecordingLobbyItems::new, new RunMessages(), () -> JumprunFixture.SEED, TestBlocks.shippedReader(), () -> {
            throw new IllegalStateException("no spawn column");
        }, JumprunFixture.CLOCK, Telemetry.noop());

        IllegalStateException failure = assertThrows(IllegalStateException.class, module::start, "the missing bounds abort the start");

        assertEquals("jumprun needs LobbyHeightBounds from the spawn column", failure.getMessage(), "the message names what is missing");
    }
}
