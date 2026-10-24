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

import java.util.List;
import net.minestom.server.entity.Player;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.api.deliver.DeliverComponent;
import net.onelitefeather.titan.core.testfixtures.RecordingDeliver.Delivery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
class RecordingDeliverTest implements DeliverContract {

    private final RecordingDeliver deliver = new RecordingDeliver();
    private Player player;

    @BeforeEach
    void connect(Env env) {
        this.player = env.createConnection().connect(env.createFlatInstance());
    }

    @Override
    public Deliver deliver() {
        return this.deliver;
    }

    @Override
    public Player player() {
        return this.player;
    }

    @Override
    public List<Delivery> delivered() {
        return this.deliver.deliveries();
    }

    @Test
    void deliveries_isASnapshot_laterDeliveriesDoNotChangeIt() {
        List<Delivery> before = this.deliver.deliveries();

        this.deliver.sendPlayer(this.player, DeliverComponent.taskBuilder().taskName("Lobby").player(this.player).build());

        assertEquals(List.of(), before, "a list handed out earlier does not grow");
    }
}
