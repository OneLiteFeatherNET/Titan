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
package net.onelitefeather.titan.app.feature.navigator;

import java.util.ArrayList;
import java.util.List;
import net.minestom.server.entity.Player;
import net.onelitefeather.deliver.DeliverComponent;
import net.onelitefeather.titan.api.deliver.Deliver;

/**
 * A test-only {@link Deliver} that records every delivery instead of sending one, so a test can
 * assert which player was sent to which destination.
 *
 * <p>Mirrors the idea of {@code net.onelitefeather.titan.app.testutils.DummyDeliver} - a no-op
 * {@link Deliver} for tests - but kept local to this package and extended to record calls, per
 * task 6.4's rule that wave C only adds files under {@code app/feature/navigator} and its test
 * package, never touching existing {@code testutils} classes.
 */
final class RecordingDeliver implements Deliver {

    /** One recorded call to {@link #sendPlayer}. */
    record Delivery(Player player, String taskName) {
    }

    private final List<Delivery> deliveries = new ArrayList<>();

    @Override
    public void sendPlayer(Player player, DeliverComponent component) {
        String taskName = component instanceof DeliverComponent.TaskComponent task ? task.taskName() : null;
        this.deliveries.add(new Delivery(player, taskName));
    }

    /**
     * @return every delivery recorded so far, oldest first
     */
    List<Delivery> deliveries() {
        return List.copyOf(this.deliveries);
    }
}
