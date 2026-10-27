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
package net.onelitefeather.titan.feature.radio;

import java.util.Objects;
import net.minestom.server.entity.Player;

/**
 * One player's radio state for this session: the play mode, and the title that plays now, if any.
 * It exists only while the player is on the lobby, so the mode is forgotten at logout.
 */
final class RadioSession {

    private final Player player;
    private PlayMode mode = PlayMode.DEFAULT;
    private RadioSong song;
    private RadioPlayback playback;
    private final RadioSessionState view = new RadioSessionState();
    private final RadioLoad load;
    private int startVolume = Volume.DEFAULT;
    private boolean changed;

    RadioSession(Player player, RadioLoad load) {
        this.player = Objects.requireNonNull(player, "player must not be null");
        this.load = Objects.requireNonNull(load, "load must not be null");
    }

    Player player() {
        return this.player;
    }

    /**
     * The load of the title this player starts: its notes lease and the generation of its start.
     */
    RadioLoad load() {
        return this.load;
    }

    /** Whether a title of this player waits for its notes. */
    boolean loading() {
        return this.load.pending();
    }

    /** Where the player is in the window: overview or category, page, generation. */
    RadioSessionState view() {
        return this.view;
    }

    PlayMode mode() {
        return this.mode;
    }

    void mode(PlayMode mode) {
        this.mode = Objects.requireNonNull(mode, "mode must not be null");
    }

    /** The title that plays now or was last played; {@code null} before the first start. */
    RadioSong song() {
        return this.song;
    }

    RadioPlayback playback() {
        return this.playback;
    }

    boolean playing() {
        return this.playback != null;
    }

    /** The volume the playing title started at; a vanilla disc keeps it until it ends. */
    int startVolume() {
        return this.startVolume;
    }

    void play(RadioSong song, RadioPlayback playback, int startVolume) {
        this.song = song;
        this.playback = playback;
        this.startVolume = startVolume;
        touch();
    }

    /** The title is over, stopped or ended; the song stays as the last one played. */
    void clear() {
        this.playback = null;
        touch();
    }

    /** Marks the open window as stale, so it is drawn again on the next tick. */
    void touch() {
        this.changed = true;
    }

    /** @return whether the open window is stale; the mark is cleared by this call */
    boolean takeChanged() {
        boolean changed = this.changed;
        this.changed = false;
        return changed;
    }
}
