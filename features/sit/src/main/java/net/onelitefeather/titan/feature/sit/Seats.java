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
package net.onelitefeather.titan.feature.sit;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;

/**
 * Sits players down on an invisible, silent arrow entity and stands them back up.
 *
 * <p>The offset is read fresh on every {@link #sit(Player, Point, Vec)} call, not fixed at
 * construction, so a seated player keeps their seat until the next call changes it.
 */
final class Seats {

    private static final Tag<UUID> ARROW = Tag.UUID("titan:sit/arrow");
    private static final Tag<Pos> ORIGIN = Tag.Structure("titan:sit/origin", Pos.class);

    // Stands the player up first if already sitting, so re-clicking a seat moves them instead of
    // stacking arrows.
    void sit(Player player, Point sitLocation, Vec offset) {
        Objects.requireNonNull(offset, "offset");
        Instance instance = player.getInstance();
        if (instance == null) {
            return;
        }
        if (isSitting(player)) {
            standUp(player);
        }
        Pos playerLocation = player.getPosition();
        SeatEntity arrow = new SeatEntity();
        arrow.setInstance(instance, sitLocation.add(offset));
        arrow.setInvisible(true);
        arrow.setSilent(true);

        player.setTag(ORIGIN, playerLocation);
        arrow.addPassenger(player);
        player.setTag(ARROW, arrow.getUuid());
    }

    void standUp(Player player) {
        Optional.ofNullable(player.getTag(ARROW)).map(player.getInstance()::getEntityByUuid).ifPresent(arrow -> {
            player.removeTag(ARROW);
            Optional.ofNullable(player.getTag(ORIGIN)).ifPresent(player::teleport);
            arrow.removePassenger(player);
            if (arrow.getPassengers().isEmpty()) {
                arrow.remove();
            }
        });
    }

    boolean isSitting(Player player) {
        return player.hasTag(ARROW);
    }

    /** An invisible, silent seat entity that removes itself once its passenger leaves. */
    private static final class SeatEntity extends Entity {

        SeatEntity() {
            super(EntityType.ARROW);
        }

        @Override
        public void update(long time) {
            super.update(time);
            if (this.getPassengers().isEmpty()) {
                this.remove();
            }
        }
    }
}
