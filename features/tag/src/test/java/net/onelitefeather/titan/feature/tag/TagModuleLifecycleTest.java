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
package net.onelitefeather.titan.feature.tag;

import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
class TagModuleLifecycleTest {

    @DisplayName("Stopping the module removes the glow of the tagger")
    @Test
    void stopRemovesTheGlow(Env env) {
        Instance instance = env.createFlatInstance();
        TagModuleIntegrationTest.Round round = TagModuleIntegrationTest.Round.start(env);
        TagModuleIntegrationTest.Seat first = TagModuleIntegrationTest.enabledSeat(env, round, instance);
        TagModuleIntegrationTest.Seat second = TagModuleIntegrationTest.enabledSeat(env, round, instance);
        TagModuleIntegrationTest.Seat tagger = TagModuleIntegrationTest.taggerOf(first, second);

        round.module.stop();

        Assertions.assertFalse(tagger.player().isGlowing(), "a stopped module must not leave a glow behind");
        round.close();
    }

    @DisplayName("After stop no event moves the role any more")
    @Test
    void stopLeavesNoListenerBehind(Env env) {
        Instance instance = env.createFlatInstance();
        TagModuleIntegrationTest.Round round = TagModuleIntegrationTest.Round.start(env);
        TagModuleIntegrationTest.Seat first = TagModuleIntegrationTest.enabledSeat(env, round, instance);
        TagModuleIntegrationTest.Seat second = TagModuleIntegrationTest.enabledSeat(env, round, instance);
        TagModuleIntegrationTest.Seat tagger = TagModuleIntegrationTest.taggerOf(first, second);
        Player target = TagModuleIntegrationTest.otherOf(tagger, first, second).player();
        round.module.stop();

        env.process().eventHandler().call(new EntityAttackEvent(tagger.player(), target));

        Assertions.assertFalse(target.isGlowing(), "after stop the attack must not pass the role any more");
        round.close();
    }
}
