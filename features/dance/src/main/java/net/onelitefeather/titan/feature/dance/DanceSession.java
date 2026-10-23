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
package net.onelitefeather.titan.feature.dance;

import java.util.List;
import net.minestom.server.coordinate.Pos;

/**
 * The tick counter of one dance, free of any server object: it says which actions are due now and
 * when the dance is over. {@link DanceService} drives it once per tick.
 */
final class DanceSession {

    /**
     * Horizontal distance, in blocks, a dancer may drift from where the dance started before it
     * counts as movement. Vertical movement (a jump, a fall) never counts.
     */
    static final double MOVE_THRESHOLD = 0.25;

    /**
     * What one tick brings.
     *
     * @param finished {@code true} once the dance is over; {@code actions} is then empty
     * @param actions  the actions due on this tick
     */
    record Frame(boolean finished, List<DanceAction> actions) {

        static final Frame FINISHED = new Frame(true, List.of());
    }

    private final Dance dance;
    private int tick;

    DanceSession(Dance dance) {
        this.dance = dance;
    }

    Dance dance() {
        return this.dance;
    }

    /**
     * @return the actions of the next tick, or {@link Frame#FINISHED} once the length is reached
     */
    Frame advance() {
        DanceSequence sequence = this.dance.sequence();
        if (sequence.isOver(this.tick)) {
            return Frame.FINISHED;
        }
        List<DanceAction> actions = sequence.actionsAt(this.tick);
        this.tick++;
        return new Frame(false, actions);
    }

    /**
     * @return {@code true} if {@code current} lies more than {@link #MOVE_THRESHOLD} blocks away
     *         from {@code anchor}, measured in the horizontal plane
     */
    static boolean movedBeyond(Pos anchor, Pos current) {
        double dx = current.x() - anchor.x();
        double dz = current.z() - anchor.z();
        return dx * dx + dz * dz > MOVE_THRESHOLD * MOVE_THRESHOLD;
    }
}
