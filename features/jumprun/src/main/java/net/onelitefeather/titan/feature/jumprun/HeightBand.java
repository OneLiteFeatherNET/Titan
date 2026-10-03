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
package net.onelitefeather.titan.feature.jumprun;

import net.onelitefeather.titan.core.module.LobbyHeightBounds;

/**
 * The part of the lobby's height limits a run may use. The bounds are read on every check, so a
 * changed limit applies to the next new block.
 */
record HeightBand(LobbyHeightBounds bounds) {

    /**
     * The most a player falls in one tick (terminal velocity is about 3.92). The spawn column
     * handles the same move event before jumprun, so a fall that crosses the fall threshold and
     * the lower limit in one tick would end in a spawn teleport instead of a reset.
     */
    static final int MAX_FALL_PER_TICK = 4;

    /** One block of slack under the upper limit, on top of the jump apex. */
    private static final int HEAD_ROOM = 1;

    /** Whether a block with this walkable top keeps clear of both limits. */
    boolean allows(Placement block) {
        double top = block.topY();
        boolean aboveFloor = top - Course.FALL_DISTANCE - MAX_FALL_PER_TICK > bounds.minHeight();
        boolean belowCeiling = top + Surface.JUMP_HEIGHT + HEAD_ROOM <= bounds.maxHeight();
        return aboveFloor && belowCeiling;
    }
}
