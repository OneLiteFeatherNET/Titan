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

import java.util.ArrayList;
import java.util.List;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.entity.Player;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.module.LobbyPlayerBusyEvent;
import net.onelitefeather.titan.core.module.LobbyReturnToSpawnEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** A jump and run takes the player out of the lobby games while it lasts. */
@ExtendWith(MicrotusExtension.class)
class JumprunBusyEventTest {

    private static List<LobbyPlayerBusyEvent> recordBusyEvents(Env env) {
        List<LobbyPlayerBusyEvent> events = new ArrayList<>();
        env.process().eventHandler().addListener(LobbyPlayerBusyEvent.class, events::add);
        return events;
    }

    @DisplayName("Starting a run tells the lobby that the player is busy")
    @Test
    void startingARunSaysBusy(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            List<LobbyPlayerBusyEvent> events = recordBusyEvents(env);

            StartedRun run = StartedRun.start(env, fixture);

            Assertions.assertEquals(1, events.size(), "exactly one busy event must be fired when the run starts");
            Assertions.assertSame(run.player(), events.get(0).getPlayer(), "the event must name the runner");
            Assertions.assertTrue(events.get(0).busy(), "a started run must be reported as busy");
        }
    }

    @DisplayName("Ending a run tells the lobby that the player is free again")
    @Test
    void endingARunSaysNotBusy(Env env) {
        try (JumprunFixture fixture = JumprunFixture.start(env)) {
            StartedRun run = StartedRun.start(env, fixture);
            List<LobbyPlayerBusyEvent> events = recordBusyEvents(env);
            Player player = run.player();

            EventDispatcher.call(new LobbyReturnToSpawnEvent(player));
            env.tick();

            Assertions.assertEquals(1, events.size(), "exactly one busy event must be fired when the run ends");
            Assertions.assertFalse(events.get(0).busy(), "a finished run must be reported as not busy");
        }
    }
}
