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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minestom.server.entity.Player;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.api.deliver.DeliverComponent;
import net.onelitefeather.titan.core.testfixtures.RecordingDeliver.Delivery;
import org.junit.jupiter.api.Test;

/**
 * What every {@link Deliver} must do. The recording fake the module tests lean on and the real
 * deliver run the same cases, so the fake cannot drift from the real one.
 */
public interface DeliverContract {

    /** The deliver under test; the same instance on every call within one test. */
    Deliver deliver();

    /** A connected player to send. */
    Player player();

    /** What the deliver under test has passed on so far, oldest first. */
    List<Delivery> delivered();

    private void send(DeliverComponent component) {
        deliver().sendPlayer(player(), component);
    }

    @Test
    default void freshDeliver_hasDeliveredNothing() {
        assertTrue(delivered().isEmpty(), "nothing is delivered before a player is sent");
    }

    @Test
    default void sendingToATask_passesThePlayerAndTheTaskOn() {
        send(DeliverComponent.taskBuilder().taskName("Survival").player(player()).build());

        assertEquals(List.of(Delivery.toTask(player(), "Survival")), delivered(), "the player goes to the task that was asked for");
    }

    @Test
    default void sendingToAServer_passesThePlayerAndTheServerOn() {
        send(DeliverComponent.serverBuilder().serverName("Survival-1").player(player()).build());

        assertEquals(List.of(Delivery.toServer(player(), "Survival-1")), delivered(), "the player goes to the service that was asked for");
    }

    @Test
    default void deliveries_keepTheOrderTheyWereSentIn() {
        send(DeliverComponent.serverBuilder().serverName("Lobby-1").player(player()).build());
        send(DeliverComponent.taskBuilder().taskName("Lobby").player(player()).build());

        assertEquals(List.of(Delivery.toServer(player(), "Lobby-1"), Delivery.toTask(player(), "Lobby")), delivered(), "oldest delivery first");
    }

    @Test
    default void aTaskAndAServerOfTheSameName_stayApart() {
        send(DeliverComponent.taskBuilder().taskName("Lobby").player(player()).build());
        send(DeliverComponent.serverBuilder().serverName("Lobby").player(player()).build());

        assertEquals(List.of(Delivery.toTask(player(), "Lobby"), Delivery.toServer(player(), "Lobby")), delivered(), "a task is not a server of that name");
    }
}
