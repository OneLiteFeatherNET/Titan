/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.onelitefeather.titan.common.season;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * US-4.02, against a running world rather than against a mock.
 *
 * <p>Every assertion here is about what the world looks like, before and after. That is the point:
 * a test that checked {@code deactivate()} had been called would pass just as happily against a
 * season that removes nothing, which is the failure this requirement exists to prevent.
 */
@ExtendWith(MicrotusExtension.class)
class SeasonWorldTest {

    private static final Pos DECORATION = new Pos(1.5, 45, 2.5);

    private final SeasonLoader loader = SeasonLoader.create(SeasonFixtures.BERLIN);

    @Test
    @DisplayName("a season puts its block into the world and puts the old one back when it ends")
    void decorationIsPlacedAndTakenBack(Env env) {
        Instance instance = env.createFlatInstance();
        instance.setBlock(DECORATION, Block.SANDSTONE);
        assertTrue(instance.getBlock(DECORATION).compare(Block.SANDSTONE), "the fixture has to start from a known block");

        ConfiguredSeason season = ConfiguredSeason.of(decorationSeason("lanterns", 1, "minecraft:jack_o_lantern"));
        SeasonCanvas canvas = MinestomSeasonCanvas.of(instance);

        season.activate(canvas);
        assertTrue(instance.getBlock(DECORATION).compare(Block.JACK_O_LANTERN), "the decoration must actually be in the world");

        season.deactivate();
        assertTrue(instance.getBlock(DECORATION).compare(Block.SANDSTONE), "the block that was there before must be back, not air");
    }

    @Test
    @DisplayName("the block that comes back is the one that was there, not the one the author assumed")
    void theRestoredBlockIsTheOneThatWasRead(Env env) {
        Instance instance = env.createFlatInstance();
        // Somebody re-built this corner of the lobby since the season was written.
        instance.setBlock(DECORATION, Block.OAK_PLANKS);

        ConfiguredSeason season = ConfiguredSeason.of(decorationSeason("lanterns", 1, "minecraft:jack_o_lantern"));
        season.activate(MinestomSeasonCanvas.of(instance));
        season.deactivate();

        assertTrue(instance.getBlock(DECORATION).compare(Block.OAK_PLANKS), "restoring a hard-coded block would have left a hole in the build");
    }

    @Test
    @DisplayName("two overlapping seasons unwind to the block that was underneath both")
    void overlappingSeasonsUnwindToTheOriginalBlock(Env env) {
        Instance instance = env.createFlatInstance();
        instance.setBlock(DECORATION, Block.SANDSTONE);
        SeasonCanvas canvas = MinestomSeasonCanvas.of(instance);
        SeasonDirector director = SeasonDirector.of(SeasonFixtures.gate(new SeasonFixtures.MutableAudience()), List.of(decorationSeason("low", 1, "minecraft:jack_o_lantern"), decorationSeason("high", 9, "minecraft:sea_lantern")));

        director.synchronize(canvas);
        assertTrue(instance.getBlock(DECORATION).compare(Block.SEA_LANTERN), "the higher priority is applied last and wins");

        director.deactivateAll();
        assertTrue(instance.getBlock(DECORATION).compare(Block.SANDSTONE), "unwinding both must reach the block that was there before either");
    }

    @Test
    @DisplayName("a display is spawned and removed again")
    void displayIsSpawnedAndRemoved(Env env) {
        Instance instance = env.createFlatInstance();
        long before = displays(instance);
        ConfiguredSeason season = ConfiguredSeason.of(this.loader.parse("displayed", """
                {
                  "id": "displayed",
                  "effects": [ { "type": "place_display", "position": { "x": 1.5, "y": 47, "z": 2.5 }, "text": "<gold>Lantern Nights" } ]
                }
                """));
        RecordingSeasonCanvas canvas = new RecordingSeasonCanvas(MinestomSeasonCanvas.of(instance));

        season.activate(canvas);
        env.tick();
        assertEquals(before + 1, displays(instance), "the display must be in the world");
        UUID displayId = canvas.displays().getFirst();
        assertNotNull(instance.getEntityByUuid(displayId));

        season.deactivate();
        env.tick();
        assertEquals(before, displays(instance), "no leftover display when the season ends");
        assertNull(instance.getEntityByUuid(displayId));
    }

    @Test
    @DisplayName("a scheduled ambient sound is cancelled when the season ends")
    void scheduledTaskIsCancelled(Env env) {
        Instance instance = env.createFlatInstance();
        ConfiguredSeason season = ConfiguredSeason.of(this.loader.parse("noisy", """
                {
                  "id": "noisy",
                  "effects": [ { "type": "ambient_sound", "position": { "x": 1.5, "y": 45, "z": 2.5 }, "sound": "minecraft:block.campfire.crackle", "periodSeconds": 1 } ]
                }
                """));
        RecordingSeasonCanvas canvas = new RecordingSeasonCanvas(MinestomSeasonCanvas.of(instance));

        season.activate(canvas);
        assertEquals(1, canvas.handles().size());
        assertTrue(canvas.handles().getFirst().alive(), "the sound loop is a real Minestom task and it is running");

        season.deactivate();
        assertFalse(canvas.handles().getFirst().alive(), "a season that ends must stop making noise");
    }

    @Test
    @DisplayName("the message prefix is replaced and the old one comes back")
    void messagePrefixIsReplacedAndRestored(Env env) {
        Instance instance = env.createFlatInstance();
        SeasonCanvas canvas = MinestomSeasonCanvas.of(instance);
        Component before = canvas.prefix();
        ConfiguredSeason season = ConfiguredSeason.of(this.loader.parse("prefixed", """
                { "id": "prefixed", "effects": [ { "type": "message_prefix", "prefix": "<gold>[Lantern Nights]" } ] }
                """));

        season.activate(canvas);
        assertEquals(Component.text("[Lantern Nights]", NamedTextColor.GOLD), canvas.prefix());
        assertEquals(canvas.prefix(), SeasonPrefix.current(), "the prefix the <prefix> tag reads is the one the season set");

        season.deactivate();
        assertEquals(before, canvas.prefix(), "the lobby's own prefix must come back");
        assertEquals(SeasonPrefix.DEFAULT, SeasonPrefix.current());
    }

    @Test
    @DisplayName("deactivating twice is deactivating once, and deactivating what never ran does nothing")
    void deactivationIsIdempotent(Env env) {
        Instance instance = env.createFlatInstance();
        instance.setBlock(DECORATION, Block.SANDSTONE);
        ConfiguredSeason season = ConfiguredSeason.of(decorationSeason("lanterns", 1, "minecraft:jack_o_lantern"));

        season.deactivate();
        assertFalse(season.active());
        assertTrue(instance.getBlock(DECORATION).compare(Block.SANDSTONE));

        season.activate(MinestomSeasonCanvas.of(instance));
        season.deactivate();
        // A second, later block change must survive the second deactivate: the undo stack is empty
        // and must stay empty rather than replaying the first restore.
        instance.setBlock(DECORATION, Block.GOLD_BLOCK);
        season.deactivate();

        assertTrue(instance.getBlock(DECORATION).compare(Block.GOLD_BLOCK), "a spent undo stack must not be replayed");
    }

    @Test
    @DisplayName("activating twice does not stack a second copy of the same decoration")
    void activationIsIdempotent(Env env) {
        Instance instance = env.createFlatInstance();
        instance.setBlock(DECORATION, Block.SANDSTONE);
        ConfiguredSeason season = ConfiguredSeason.of(decorationSeason("lanterns", 1, "minecraft:jack_o_lantern"));
        SeasonCanvas canvas = MinestomSeasonCanvas.of(instance);

        season.activate(canvas);
        // Without the guard the second activation would record "restore jack_o_lantern" as the undo
        // of the first, and the sandstone would never come back.
        season.activate(canvas);
        season.deactivate();

        assertTrue(instance.getBlock(DECORATION).compare(Block.SANDSTONE));
    }

    @Test
    @DisplayName("when the window shuts, the next synchronise takes the decoration back out")
    void aClosingWindowRemovesTheDecoration(Env env) {
        Instance instance = env.createFlatInstance();
        instance.setBlock(DECORATION, Block.SANDSTONE);
        SeasonCanvas canvas = MinestomSeasonCanvas.of(instance);
        // Open at the fixture's "now", and shut once the kill switch is thrown - which is what an
        // operator reaches for and what NFR-004 says must not need a restart.
        SeasonDefinition open = decorationSeason("lanterns", 1, "minecraft:jack_o_lantern");
        SeasonDirector running = SeasonDirector.of(SeasonFixtures.gate(new SeasonFixtures.MutableAudience()), List.of(open));

        assertTrue(running.synchronize(canvas));
        assertTrue(instance.getBlock(DECORATION).compare(Block.JACK_O_LANTERN));
        assertFalse(running.synchronize(canvas), "nothing changed, so nothing is touched");

        running.deactivateAll();
        assertTrue(instance.getBlock(DECORATION).compare(Block.SANDSTONE));
    }

    private SeasonDefinition decorationSeason(String id, int priority, String block) {
        return this.loader.parse(id, """
                {
                  "id": "%s",
                  "priority": %d,
                  "stage": "ga",
                  "effects": [ { "type": "place_decoration", "position": { "x": 1.5, "y": 45, "z": 2.5 }, "block": "%s" } ]
                }
                """.formatted(id, priority, block));
    }

    private static long displays(Instance instance) {
        return instance.getEntities().stream().filter(entity -> entity.getEntityType() == EntityType.TEXT_DISPLAY).count();
    }
}
