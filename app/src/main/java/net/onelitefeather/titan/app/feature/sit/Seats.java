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
 * <p>This is the module's own copy of what used to be {@code common.helper.SitHelper} - same
 * behaviour, but non-static (the {@link #offset} a seat is placed at is fixed once, at
 * construction, instead of being threaded through every call) and using this feature's own,
 * namespaced tags instead of the ones {@code common.utils.Tags} used to own. See {@code
 * design.md}, decision 9.
 *
 * <p>Package-private: only {@link SitModule} constructs and uses this.
 */
final class Seats {

    /** The seat entity a sitting player is riding, keyed by its {@link UUID}. */
    private static final Tag<UUID> ARROW = Tag.UUID("titan:sit/arrow");

    /** The position a sitting player is teleported back to when they stand up. */
    private static final Tag<Pos> ORIGIN = Tag.Structure("titan:sit/origin", Pos.class);

    private final Vec offset;

    Seats(Vec offset) {
        this.offset = Objects.requireNonNull(offset, "offset");
    }

    /**
     * Sits {@code player} down at {@code sitLocation} (offset by this instance's {@link #offset}).
     * A player who is already sitting is stood up first, then sat down again at the new location.
     *
     * @param player      the player to sit down
     * @param sitLocation the location - typically a clicked block's position - to sit at
     */
    void sit(Player player, Point sitLocation) {
        Instance instance = player.getInstance();
        if (instance == null) {
            return;
        }
        if (isSitting(player)) {
            standUp(player);
        }
        Pos playerLocation = player.getPosition();
        SeatEntity arrow = new SeatEntity();
        arrow.setInstance(instance, sitLocation.add(this.offset));
        arrow.setInvisible(true);
        arrow.setSilent(true);

        player.setTag(ORIGIN, playerLocation);
        arrow.addPassenger(player);
        player.setTag(ARROW, arrow.getUuid());
    }

    /**
     * Stands {@code player} up again, teleporting them back to the position they sat down from.
     * A player who is not sitting is left untouched.
     *
     * @param player the player to stand up
     */
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

    /**
     * @param player the player to check
     * @return {@code true} if {@code player} is currently sitting
     */
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
