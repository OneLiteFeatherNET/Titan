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

/** Why a run ended, and what the player is owed for it. */
enum EndReason {
    ABORT(Owed.everything()), FALL(Owed.everything()), EXHAUSTED(Owed.everything()), ELYTRA(Owed.everything()), DEATH(Owed.everything()),
    /** The player is gone: nothing to show or tell, but the score stands. */
    DISCONNECT(Owed.scoreOnly()),
    /**
     * The player went to another instance: the client changes world on its own, the score stands.
     */
    LEFT_INSTANCE(Owed.scoreOnly()),
    /**
     * The lobby stops: the blocks go back, but an interrupted run is neither scored nor reported.
     */
    SHUTDOWN(Owed.blocksOnly());

    private final Owed owed;

    EndReason(Owed owed) {
        this.owed = owed;
    }

    boolean restoresBlocks() {
        return owed.restoresBlocks;
    }

    boolean submitsScore() {
        return owed.submitsScore;
    }

    /**
     * Only a fall and the elytra are a failure worth a sound; a death in the lobby is not the run's
     * doing.
     */
    boolean failed() {
        return this == FALL || this == ELYTRA;
    }

    boolean announcesScore() {
        return owed.announces;
    }

    private record Owed(boolean restoresBlocks, boolean submitsScore, boolean announces) {

        static Owed everything() {
            return new Owed(true, true, true);
        }

        static Owed scoreOnly() {
            return new Owed(false, true, false);
        }

        static Owed blocksOnly() {
            return new Owed(true, false, false);
        }
    }
}
