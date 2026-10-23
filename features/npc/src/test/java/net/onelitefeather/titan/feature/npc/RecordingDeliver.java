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
package net.onelitefeather.titan.feature.npc;

import java.util.ArrayList;
import java.util.List;
import net.minestom.server.entity.Player;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.api.deliver.DeliverComponent;

/** Records every hand-over as {@code task:<name>} or {@code server:<name>}. */
final class RecordingDeliver implements Deliver {

    record Delivery(Player player, String target) {
    }

    private final List<Delivery> deliveries = new ArrayList<>();

    @Override
    public void sendPlayer(Player player, DeliverComponent component) {
        String target = "unknown";
        if (component instanceof DeliverComponent.TaskComponent task) {
            target = "task:" + task.taskName();
        } else if (component instanceof DeliverComponent.ServerDeliverComponent server) {
            target = "server:" + server.gameServer();
        }
        this.deliveries.add(new Delivery(player, target));
    }

    List<Delivery> deliveries() {
        return List.copyOf(this.deliveries);
    }
}
