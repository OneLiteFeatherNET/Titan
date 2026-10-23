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
package net.onelitefeather.titan.feature.npc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.onelitefeather.titan.core.ambience.AmbienceZone;
import net.onelitefeather.titan.core.ambience.VanillaAmbience;
import net.onelitefeather.titan.core.portal.Box;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The moving parts of one ambience zone: its particles, its ambient sound and its silhouettes. The
 * module drives it once per takt with the players of the lobby; it sends only to players in or near
 * the zone and keeps one silhouette per player and point.
 */
final class ZoneRuntime {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZoneRuntime.class);
    private static final String VANILLA = "minecraft";
    private static final String STARE = "entity.enderman.stare";
    private static final float AMBIENT_VOLUME = 0.3f;
    private static final float STARE_VOLUME = 0.5f;
    private static final Vec PARTICLE_OFFSET = new Vec(0.5, 0.5, 0.5);
    private static final float PARTICLE_SPEED = 0.02f;
    // Takts are 5 ticks, so a second is 4 takts.
    private static final int TAKTS_PER_SECOND = 4;

    private record Shown(UUID player, int point) {
    }

    private final AmbienceZone zone;
    private final Box area;
    private final Instance lobby;
    private final NpcTelemetry telemetry;
    private final Random random;
    private final @Nullable Particle particle;
    private final @Nullable SoundSchedule sounds;
    private final @Nullable Sound ambientSound;
    private final ApparitionRules rules;
    private final Sound stareSound = Sound.sound(Key.key(VANILLA, STARE), Sound.Source.HOSTILE, STARE_VOLUME, 1f);
    private final Map<Shown, Entity> figures = new HashMap<>();

    ZoneRuntime(AmbienceZone zone, Instance lobby, NpcTelemetry telemetry, Random random) {
        this.zone = zone;
        this.area = zone.area();
        this.lobby = lobby;
        this.telemetry = telemetry;
        this.random = random;
        this.particle = particle(zone);
        AmbienceZone.Sounds sound = zone.sounds();
        this.sounds = sound == null ? null : new SoundSchedule((int) Math.ceil(sound.minSeconds() * TAKTS_PER_SECOND), (int) Math.ceil(sound.maxSeconds() * TAKTS_PER_SECOND), random);
        this.ambientSound = sound == null ? null : Sound.sound(Key.key(VANILLA, VanillaAmbience.bare(sound.key())), Sound.Source.AMBIENT, AMBIENT_VOLUME, 1f);
        this.rules = new ApparitionRules(new ArrayList<Point>(zone.apparitions()));
    }

    /** One check at takt {@code takt} over all players in the lobby. */
    void tick(long takt, List<Player> players) {
        if (this.particle != null && this.zone.particles() != null) {
            emitParticles(players);
        }
        if (this.sounds != null && this.ambientSound != null) {
            playAmbientSounds(takt, players);
        }
        if (!this.zone.apparitions().isEmpty()) {
            applyChanges(this.rules.update(presence(players)));
        }
    }

    /** Hides every silhouette of a player who left; called on disconnect. */
    void forget(UUID player) {
        applyChanges(this.rules.forget(player));
        if (this.sounds != null) {
            this.sounds.forget(player);
        }
    }

    /** Removes every silhouette still shown; called when the module stops. */
    void close() {
        for (Map.Entry<Shown, Entity> figure : this.figures.entrySet()) {
            figure.getValue().remove();
        }
        this.figures.clear();
    }

    private void emitParticles(List<Player> players) {
        List<Player> near = players.stream().filter(player -> ZoneArea.near(this.area, player.getPosition(), AmbienceModule.NEAR_MARGIN)).toList();
        if (near.isEmpty()) {
            return;
        }
        ParticlePacket packet = new ParticlePacket(this.particle, false, false, ZoneArea.randomPoint(this.area, this.random), PARTICLE_OFFSET, PARTICLE_SPEED, this.zone.particles().count());
        near.forEach(player -> player.sendPacket(packet));
    }

    private void playAmbientSounds(long takt, List<Player> players) {
        for (Player player : players) {
            if (ZoneArea.inside(this.area, player.getPosition()) && this.sounds.due(player.getUuid(), takt)) {
                player.playSound(this.ambientSound, player.getPosition());
            }
        }
    }

    private List<ApparitionRules.Presence> presence(List<Player> players) {
        return players.stream().map(player -> new ApparitionRules.Presence(player.getUuid(), player.getPosition(), ZoneArea.inside(this.area, player.getPosition()))).toList();
    }

    private void applyChanges(List<ApparitionRules.Change> changes) {
        for (ApparitionRules.Change change : changes) {
            Shown key = new Shown(change.player(), change.point());
            if (change.show()) {
                show(key);
            } else {
                hide(key);
            }
        }
    }

    // A silhouette is a figure only its player sees: not auto-viewable, shown to exactly that player.
    private void show(Shown key) {
        Player player = this.lobby.getPlayerByUuid(key.player());
        if (player == null) {
            return;
        }
        Vec point = this.zone.apparitions().get(key.point());
        Entity figure = Mannequins.create(this.zone.apparitionSkin());
        figure.setAutoViewable(false);
        figure.setInstance(this.lobby, new Pos(point.x(), point.y(), point.z())).join();
        figure.addViewer(player);
        this.figures.put(key, figure);
        player.playSound(this.stareSound, point);
        this.telemetry.apparition(this.zone.id());
    }

    private void hide(Shown key) {
        Entity figure = this.figures.remove(key);
        if (figure == null) {
            return;
        }
        Player player = this.lobby.getPlayerByUuid(key.player());
        if (player != null) {
            figure.removeViewer(player);
        }
        figure.remove();
    }

    private static @Nullable Particle particle(AmbienceZone zone) {
        if (zone.particles() == null) {
            return null;
        }
        Particle particle = Particle.fromKey(Key.key(VANILLA, VanillaAmbience.bare(zone.particles().type())));
        if (particle == null) {
            LOGGER.warn("Ambience zone '{}' has no particle '{}' in this client, its particles stay off", zone.id(), zone.particles().type());
        }
        return particle;
    }
}
