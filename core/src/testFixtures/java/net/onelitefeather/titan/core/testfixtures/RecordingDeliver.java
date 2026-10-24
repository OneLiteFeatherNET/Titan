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

import java.util.ArrayList;
import java.util.List;
import net.minestom.server.entity.Player;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.api.deliver.DeliverComponent;

/**
 * A test-only {@link Deliver} that records every delivery instead of sending one, so a test can
 * assert which player was sent to which task or to which service.
 */
public final class RecordingDeliver implements Deliver {

    /** One recorded call to {@link #sendPlayer}; at most one of the two names is set. */
    public record Delivery(Player player, String taskName, String serverName) {

        public static Delivery toTask(Player player, String taskName) {
            return new Delivery(player, taskName, null);
        }

        public static Delivery toServer(Player player, String serverName) {
            return new Delivery(player, null, serverName);
        }
    }

    private final List<Delivery> deliveries = new ArrayList<>();

    @Override
    public void sendPlayer(Player player, DeliverComponent component) {
        String taskName = component instanceof DeliverComponent.TaskComponent task ? task.taskName() : null;
        String serverName = component instanceof DeliverComponent.ServerDeliverComponent server ? server.gameServer() : null;
        this.deliveries.add(new Delivery(player, taskName, serverName));
    }

    public List<Delivery> deliveries() {
        return List.copyOf(this.deliveries);
    }
}
