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
package net.onelitefeather.titan.app.feature.sit;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Covers {@link Seats} directly, one level below the full {@link SitModule} wiring covered by
 * {@link SitModuleIntegrationTest}: sitting places an invisible, silent seat entity at the offset
 * position and mounts the player on it; standing up removes the tag, teleports the player back and
 * removes the now-passenger-less seat entity.
 */
@ExtendWith(MicrotusExtension.class)
class SeatsTest {

    private static final Vec OFFSET = new Vec(0.5, 0.25, 0.5);

    @DisplayName("Sitting mounts the player on an invisible, silent seat entity at the offset position")
    @Test
    void sitPlacesAnInvisibleSilentSeatAtTheOffset(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        player.teleport(new Pos(0, 64, 0));
        Seats seats = new Seats(OFFSET);

        Pos sitLocation = new Pos(1, 65, 1);
        seats.sit(player, sitLocation);

        Assertions.assertTrue(seats.isSitting(player));
        Assertions.assertNotNull(player.getVehicle(), "the player must be riding the seat entity");
        Assertions.assertTrue(player.getVehicle().isInvisible(), "the seat entity must be invisible");
        Assertions.assertTrue(player.getVehicle().isSilent(), "the seat entity must be silent");
        Pos expected = sitLocation.add(OFFSET);
        Assertions.assertEquals(expected.x(), player.getVehicle().getPosition().x(), 0.001);
        Assertions.assertEquals(expected.y(), player.getVehicle().getPosition().y(), 0.001);
        Assertions.assertEquals(expected.z(), player.getVehicle().getPosition().z(), 0.001);
    }

    @DisplayName("Standing up teleports the player back to where they sat down from and removes the seat")
    @Test
    void standUpTeleportsBackAndRemovesTheSeat(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        Pos originalPosition = new Pos(3, 70, 3);
        player.teleport(originalPosition);
        Seats seats = new Seats(OFFSET);
        seats.sit(player, new Pos(5, 65, 5));
        var seat = player.getVehicle();

        seats.standUp(player);

        Assertions.assertFalse(seats.isSitting(player));
        Assertions.assertEquals(originalPosition.x(), player.getPosition().x(), 0.001);
        Assertions.assertEquals(originalPosition.y(), player.getPosition().y(), 0.001);
        Assertions.assertEquals(originalPosition.z(), player.getPosition().z(), 0.001);
        Assertions.assertNull(instance.getEntityByUuid(seat.getUuid()), "the seat entity must be removed once empty");
    }

    @DisplayName("Standing up a player who is not sitting does nothing")
    @Test
    void standUpOnANonSittingPlayerIsANoop(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        Seats seats = new Seats(OFFSET);

        Assertions.assertDoesNotThrow(() -> seats.standUp(player));
        Assertions.assertFalse(seats.isSitting(player));
    }

    @DisplayName("Sitting again while already sitting replaces the old seat with a new one")
    @Test
    void sittingAgainReplacesTheOldSeat(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        player.teleport(new Pos(0, 64, 0));
        Seats seats = new Seats(OFFSET);
        seats.sit(player, new Pos(0, 64, 0));
        var firstSeat = player.getVehicle();

        seats.sit(player, new Pos(10, 64, 10));
        var secondSeat = player.getVehicle();

        Assertions.assertTrue(seats.isSitting(player));
        Assertions.assertNotEquals(firstSeat.getUuid(), secondSeat.getUuid());
        Assertions.assertNull(instance.getEntityByUuid(firstSeat.getUuid()), "the old seat must be removed");
    }
}
