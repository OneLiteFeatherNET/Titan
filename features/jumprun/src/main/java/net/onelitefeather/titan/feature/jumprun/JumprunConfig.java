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

import io.avaje.config.Configuration;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

/** The live settings of the module: what a run reads when it starts. */
final class JumprunConfig {

    private final LiveSetting<Palettes> palettes;
    private final LiveSetting<Integer> rainbowRerollTicks;
    private final LiveSetting<Integer> ultraRerollTicks;
    private final LiveSetting<List<UUID>> headProfiles;
    private final TeamHeads teamHeads;

    /** Without team heads: every head is a plain one of the palette. */
    JumprunConfig(Configuration config) {
        this(config, new TeamHeads(HeadSkins.NONE, Runnable::run, Clock.systemUTC()));
    }

    JumprunConfig(Configuration config, TeamHeads teamHeads) {
        this.teamHeads = teamHeads;
        this.palettes = new LiveSetting<>("palettes", () -> JumprunSettings.palettes(config));
        this.rainbowRerollTicks = new LiveSetting<>("rainbow.rerollTicks", () -> JumprunSettings.rerollTicks(config, JumprunSettings.RAINBOW_REROLL_TICKS_KEY));
        this.ultraRerollTicks = new LiveSetting<>("ultra.rerollTicks", () -> JumprunSettings.rerollTicks(config, JumprunSettings.ULTRA_REROLL_TICKS_KEY));
        this.headProfiles = new LiveSetting<>("heads.profiles", () -> JumprunSettings.headProfiles(config), List.of());
    }

    /**
     * @throws IllegalArgumentException naming the first invalid key and the reason, which aborts
     *                                  the start
     */
    void readAtStartup() {
        this.palettes.readAtStartup();
        this.rainbowRerollTicks.readAtStartup();
        this.ultraRerollTicks.readAtStartup();
        // Never aborts: a wrong entry only means plain heads until it is fixed. Reading starts the lookups early.
        this.teamHeads.of(this.headProfiles.current());
    }

    /** The palettes of the run to come, with the team heads whose skins are known by now. */
    Palettes palettes() {
        return this.palettes.current().withHeads(this.teamHeads.of(this.headProfiles.current()));
    }

    /**
     * Standing ticks between two rerolls of a run in {@code mode}; 0 for a mode that never rerolls.
     */
    int rerollTicks(Mode mode) {
        return switch (mode.reroll()) {
            case NONE -> 0;
            case MATERIAL -> this.rainbowRerollTicks.current();
            case COURSE -> this.ultraRerollTicks.current();
        };
    }
}
