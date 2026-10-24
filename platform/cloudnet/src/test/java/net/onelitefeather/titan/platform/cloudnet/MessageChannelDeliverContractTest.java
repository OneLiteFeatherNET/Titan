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
package net.onelitefeather.titan.platform.cloudnet;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minestom.server.entity.Player;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.common.deliver.ServerConnector;
import net.onelitefeather.titan.common.deliver.TitanServerConnector;
import net.onelitefeather.titan.core.testfixtures.DeliverContract;
import net.onelitefeather.titan.core.testfixtures.RecordingDeliver.Delivery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

/** The real deliver, with a connector installed in the holder, against the shared contract. */
@ExtendWith(MicrotusExtension.class)
class MessageChannelDeliverContractTest implements DeliverContract {

    private final MessageChannelDeliver deliver = new MessageChannelDeliver();
    private final List<Delivery> delivered = new ArrayList<>();
    private Player player;

    @BeforeEach
    void connectAndInstallConnector(Env env) {
        this.player = env.createConnection().connect(env.createFlatInstance());
        TitanServerConnector.setConnector(new ServerConnector() {
            @Override
            public void connectToTask(UUID playerId, String taskName) {
                MessageChannelDeliverContractTest.this.delivered.add(Delivery.toTask(playerOf(playerId), taskName));
            }

            @Override
            public void connectToServer(UUID playerId, String serviceName) {
                MessageChannelDeliverContractTest.this.delivered.add(Delivery.toServer(playerOf(playerId), serviceName));
            }
        });
    }

    // The holder is static by nature; every test leaves it empty again.
    @AfterEach
    void uninstallConnector() {
        TitanServerConnector.setConnector(null);
    }

    private Player playerOf(UUID playerId) {
        if (!this.player.getUuid().equals(playerId)) {
            throw new AssertionError("the connector was asked for a player that was never sent: " + playerId);
        }
        return this.player;
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
        return List.copyOf(this.delivered);
    }
}
