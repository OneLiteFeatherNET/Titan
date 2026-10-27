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

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.EntitySoundEffectPacket;
import net.minestom.server.network.packet.server.play.StopSoundPacket;
import net.minestom.server.timer.Scheduler;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.module.LobbyPlayerBusyEvent;
import net.onelitefeather.titan.core.module.item.ItemSlot;
import net.onelitefeather.titan.core.module.item.LobbyItem;
import net.onelitefeather.titan.core.settings.SettingsRegistry;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(MicrotusExtension.class)
class RadioModuleIntegrationTest {

    static final long SEED = 7L;
    /** The overview lists Minecraft first, so its slot is 0; Sonstige follows when it has songs. */
    static final int MINECRAFT_SLOT = 0;
    static final int SONSTIGE_SLOT = 1;
    /** Slot of the disc 13 in the Minecraft list, the first vinyl disc. */
    static final int DISC_13_SLOT = 0;
    /** Slot of the disc 11 in the Minecraft list: a short disc, 71 seconds. */
    static final int ELEVEN_SLOT = 10;
    static final int ELEVEN_END_TICKS = 71 * RadioLimits.TICKS_PER_SECOND + RadioLimits.DISC_END_BUFFER_TICKS;
    /** Past the second note of {@link TestSongFiles#writeSunrise}, which is on game tick 20. */
    static final int MIDI_NOTES_TICKS = 25;
    static final int STOP_SLOT = RadioMenuLayout.STOP;
    static final int MODE_SLOT = RadioMenuLayout.MODE;
    static final int VOLUME_SLOT = RadioMenuLayout.VOLUME;

    @TempDir
    Path radio;

    /** A player with the connection that carries their packets. */
    record Seat(TestConnection connection, Player player) {
    }

    /** One running module with its fixtures; closing it stops the module and the node. */
    static final class Round implements AutoCloseable {

        final RadioModule module;
        final RadioMessages messages = new RadioMessages();
        final TestTelemetry telemetry = TestTelemetry.create();
        final InMemoryRadioSettings settings = new InMemoryRadioSettings();
        private final TestTitanNode titanNode;

        Round(Env env, Path directory) {
            this.titanNode = TestTitanNode.attach(env);
            this.messages.register();
            EventNode<Event> titan = this.titanNode.node();
            Scheduler scheduler = env.process().scheduler();
            this.module = new RadioModule(titan, scheduler, this.telemetry.telemetry(), new Random(SEED), this.messages, this.settings, new SettingsRegistry(), directory, Runnable::run);
            this.module.start();
        }

        Seat connect(Env env, Instance instance) {
            TestConnection connection = env.createConnection();
            return new Seat(connection, connection.connect(instance));
        }

        long counter(String name, Attributes attributes) {
            return this.telemetry.counter(name, attributes);
        }

        @Override
        public void close() {
            this.module.stop();
            this.titanNode.close();
            this.messages.close();
            this.telemetry.close();
        }
    }

    static Attributes attributes(String... keysAndValues) {
        AttributesBuilder builder = Attributes.builder();
        for (int index = 0; index < keysAndValues.length; index += 2) {
            builder.put(keysAndValues[index], keysAndValues[index + 1]);
        }
        return builder.build();
    }

    /** Clicks {@code slot} in the inventory {@code player} has open, as the client would. */
    static void click(Env env, Player player, int slot) {
        click(env, player, slot, new Click.Left(slot));
    }

    /** A right click on {@code slot}. */
    static void clickRight(Env env, Player player, int slot) {
        click(env, player, slot, new Click.Right(slot));
    }

    private static void click(Env env, Player player, int slot, Click click) {
        AbstractInventory open = player.getOpenInventory();
        Assertions.assertNotNull(open, "the player must have a radio window open before a click");
        env.process().eventHandler().call(new InventoryPreClickEvent(open, player, click));
    }

    static void tick(Env env, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            env.tick();
        }
    }

    /** Opens the radio, lets the window fill, and opens the category at {@code overviewSlot}. */
    static void openCategory(Env env, RadioModuleIntegrationTest.Round round, Player player, int overviewSlot) {
        round.module.open(player);
        env.tick();
        click(env, player, overviewSlot);
        env.tick();
    }

    @DisplayName("The radio item sits on hotbar slot 5 and is there without any MIDI file")
    @Test
    void radioItemIsOnHotbarFiveWithoutMidiFiles(Env env) {
        try (Round round = new Round(env, this.radio.resolve("missing"))) {
            LobbyItem item = new RadioItems().radioItem(round.module, round.messages);

            Assertions.assertEquals(ItemSlot.hotbar(5), item.placement(), "the radio must take hotbar slot 5");
            Assertions.assertEquals("titan:radio", item.key().asString(), "the radio item must carry its key");
        }
    }

    @DisplayName("The overview lists Minecraft first, and Sonstige only when there are songs")
    @Test
    void overviewListsMinecraftAndSonstigeWithSongs(Env env) throws Exception {
        TestSongFiles.writeSunrise(this.radio, "sunrise.mid");
        Instance instance = env.createFlatInstance();
        try (Round round = new Round(env, this.radio)) {
            Player player = round.connect(env, instance).player();

            round.module.open(player);
            env.tick();

            AbstractInventory overview = player.getOpenInventory();
            Assertions.assertEquals(Material.MUSIC_DISC_13, overview.getItemStack(MINECRAFT_SLOT).material(), "the first category must be Minecraft, shown as disc 13");
            Assertions.assertEquals(Material.PAPER, overview.getItemStack(SONSTIGE_SLOT).material(), "Sonstige must follow Minecraft when it has songs");
            Assertions.assertEquals(Material.BARRIER, overview.getItemStack(STOP_SLOT).material(), "the stop control must keep its slot in the overview");
        }
    }

    @DisplayName("Without MIDI files the overview shows Minecraft only")
    @Test
    void overviewWithoutSongsShowsMinecraftOnly(Env env) {
        Instance instance = env.createFlatInstance();
        try (Round round = new Round(env, this.radio.resolve("missing"))) {
            Player player = round.connect(env, instance).player();

            round.module.open(player);
            env.tick();

            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, player.getOpenInventory().getItemStack(SONSTIGE_SLOT).material(), "Sonstige must not be listed without songs");
        }
    }

    @DisplayName("A clicked vinyl disc plays its record sound for the clicker only")
    @Test
    void clickedDiscPlaysForTheClickerOnly(Env env) {
        Instance instance = env.createFlatInstance();
        try (Round round = new Round(env, this.radio.resolve("missing"))) {
            Seat clicker = round.connect(env, instance);
            Seat other = round.connect(env, instance);
            Collector<EntitySoundEffectPacket> clickerSounds = clicker.connection().trackIncoming(EntitySoundEffectPacket.class);
            Collector<EntitySoundEffectPacket> otherSounds = other.connection().trackIncoming(EntitySoundEffectPacket.class);
            openCategory(env, round, clicker.player(), MINECRAFT_SLOT);

            click(env, clicker.player(), DISC_13_SLOT);

            EntitySoundEffectPacket sound = clickerSounds.collect().getFirst();
            Assertions.assertEquals(VanillaDiscs.ALL.getFirst().sound(), sound.soundEvent(), "the first disc must play its own record sound");
            Assertions.assertEquals(clicker.player().getEntityId(), sound.entityId(), "the sound must be emitted by the clicker, so it stays with them");
            otherSounds.assertEmpty();
        }
    }

    @DisplayName("Stop sends a named stop for the disc that plays")
    @Test
    void stopEndsTheDiscWithANamedStop(Env env) {
        Instance instance = env.createFlatInstance();
        try (Round round = new Round(env, this.radio.resolve("missing"))) {
            Seat clicker = round.connect(env, instance);
            Collector<StopSoundPacket> stops = clicker.connection().trackIncoming(StopSoundPacket.class);
            openCategory(env, round, clicker.player(), MINECRAFT_SLOT);
            click(env, clicker.player(), DISC_13_SLOT);

            click(env, clicker.player(), STOP_SLOT);

            Assertions.assertEquals(VanillaDiscs.ALL.getFirst().sound().key().asString(), stops.collect().getFirst().sound(), "stop must name the disc's own sound");
            Assertions.assertEquals(1, round.counter("titan.radio.stops", attributes("reason", "user", "kind", "vanilla")), "the user stop must be counted");
        }
    }

    @DisplayName("A MIDI song in Sonstige plays its notes for the clicker only")
    @Test
    void midiSongPlaysNotesForTheClickerOnly(Env env) throws Exception {
        TestSongFiles.writeSunrise(this.radio, "sunrise.mid");
        Instance instance = env.createFlatInstance();
        try (Round round = new Round(env, this.radio)) {
            Seat clicker = round.connect(env, instance);
            Seat other = round.connect(env, instance);
            Collector<EntitySoundEffectPacket> clickerSounds = clicker.connection().trackIncoming(EntitySoundEffectPacket.class);
            Collector<EntitySoundEffectPacket> otherSounds = other.connection().trackIncoming(EntitySoundEffectPacket.class);
            openCategory(env, round, clicker.player(), SONSTIGE_SLOT);

            click(env, clicker.player(), 0);
            tick(env, MIDI_NOTES_TICKS);

            List<EntitySoundEffectPacket> sounds = clickerSounds.collect();
            Assertions.assertFalse(sounds.isEmpty(), "the clicker must hear the notes of the song");
            Assertions.assertTrue(sounds.stream().allMatch(sound -> sound.soundEvent().key().asString().startsWith("minecraft:block.note_block.")), "every sound of a MIDI song must be a note block sound");
            otherSounds.assertEmpty();
        }
    }

    @DisplayName("A MIDI file that cannot be parsed at play time starts nothing and is counted once as rejected")
    @Test
    void unreadableMidiFileIsRejectedAtPlayTime(Env env) throws Exception {
        Files.write(this.radio.resolve("broken.mid"), new byte[]{'M', 'T', 'h', 'd', 1, 2, 3});
        Files.writeString(this.radio.resolve("songs.yaml"), "songs:\n  - id: broken\n    title: Broken\n    artist: Beispiel\n    disc: MUSIC_DISC_CAT\n    file: broken.mid\n");
        Instance instance = env.createFlatInstance();
        try (Round round = new Round(env, this.radio)) {
            Seat clicker = round.connect(env, instance);
            Collector<EntitySoundEffectPacket> sounds = clicker.connection().trackIncoming(EntitySoundEffectPacket.class);
            openCategory(env, round, clicker.player(), SONSTIGE_SLOT);

            click(env, clicker.player(), 0);
            tick(env, MIDI_NOTES_TICKS);

            sounds.assertEmpty();
            Assertions.assertEquals(1, round.counter("titan.radio.songs", attributes("result", "rejected")), "the unreadable file must be counted once as rejected");
            Assertions.assertEquals(0, round.counter("titan.radio.plays", attributes("kind", "midi", "mode", "once", "trigger", "user", "category", "other")), "no title may start for the unreadable file");
        }
    }

    @DisplayName("Logout stops the radio with reason logout and forgets the play mode")
    @Test
    void logoutStopsAndForgetsTheMode(Env env) {
        Instance instance = env.createFlatInstance();
        try (Round round = new Round(env, this.radio.resolve("missing"))) {
            Seat clicker = round.connect(env, instance);
            openCategory(env, round, clicker.player(), MINECRAFT_SLOT);
            click(env, clicker.player(), DISC_13_SLOT);
            click(env, clicker.player(), MODE_SLOT);
            Assertions.assertEquals(PlayMode.LOOP, round.module.modeOf(clicker.player()), "the mode button must switch the mode to loop");

            env.process().eventHandler().call(new PlayerDisconnectEvent(clicker.player()));

            Assertions.assertEquals(1, round.counter("titan.radio.stops", attributes("reason", "logout", "kind", "vanilla")), "a logout must be counted as a logout stop");
            Assertions.assertEquals(PlayMode.ONCE, round.module.modeOf(clicker.player()), "the mode must be forgotten at logout");
        }
    }

    @DisplayName("A busy player's radio stops and does not restart in loop mode")
    @Test
    void busyStopsAndDoesNotRestart(Env env) {
        Instance instance = env.createFlatInstance();
        try (Round round = new Round(env, this.radio.resolve("missing"))) {
            Seat clicker = round.connect(env, instance);
            Collector<StopSoundPacket> stops = clicker.connection().trackIncoming(StopSoundPacket.class);
            openCategory(env, round, clicker.player(), MINECRAFT_SLOT);
            click(env, clicker.player(), MODE_SLOT);
            click(env, clicker.player(), ELEVEN_SLOT);
            // A collector reads once and then stops listening, so it is started after the disc has begun.
            Collector<EntitySoundEffectPacket> sounds = clicker.connection().trackIncoming(EntitySoundEffectPacket.class);

            env.process().eventHandler().call(new LobbyPlayerBusyEvent(clicker.player(), true));
            env.process().eventHandler().call(new LobbyPlayerBusyEvent(clicker.player(), false));
            tick(env, ELEVEN_END_TICKS + 5);

            Assertions.assertEquals(1, stops.collect().size(), "busy must stop the disc once");
            sounds.assertEmpty();
            Assertions.assertEquals(1, round.counter("titan.radio.stops", attributes("reason", "busy", "kind", "vanilla")), "the busy stop must be counted");
        }
    }

    @DisplayName("A disc that ends in loop mode starts again, counted as an end and an automatic start")
    @Test
    void endInLoopStartsTheSameDiscAgain(Env env) {
        Instance instance = env.createFlatInstance();
        try (Round round = new Round(env, this.radio.resolve("missing"))) {
            Seat clicker = round.connect(env, instance);
            openCategory(env, round, clicker.player(), MINECRAFT_SLOT);
            click(env, clicker.player(), MODE_SLOT);
            Collector<EntitySoundEffectPacket> played = clicker.connection().trackIncoming(EntitySoundEffectPacket.class);
            click(env, clicker.player(), ELEVEN_SLOT);

            tick(env, ELEVEN_END_TICKS + 5);

            List<EntitySoundEffectPacket> sounds = played.collect();
            Assertions.assertEquals(2, sounds.size(), "the disc must start a second time after it ended in loop mode");
            Assertions.assertEquals(sounds.get(0).soundEvent(), sounds.get(1).soundEvent(), "the second start must be the same disc");
            Assertions.assertEquals(1, round.counter("titan.radio.stops", attributes("reason", "end", "kind", "vanilla")), "the end must be counted as an end stop");
            Assertions.assertEquals(1, round.counter("titan.radio.plays", attributes("kind", "vanilla", "mode", "loop", "trigger", "auto", "category", "minecraft")), "the restart must be counted as an automatic loop start");
        }
    }

    @DisplayName("The volume button lowers the volume to 75 percent and stores it for the player")
    @Test
    void volumeButtonStoresTheLevel(Env env) {
        Instance instance = env.createFlatInstance();
        try (Round round = new Round(env, this.radio.resolve("missing"))) {
            Player player = round.connect(env, instance).player();
            openCategory(env, round, player, MINECRAFT_SLOT);

            clickRight(env, player, VOLUME_SLOT);

            Assertions.assertEquals(75, round.settings.level(player.getUuid(), Volume.SETTING), "a right click at 100 percent must store 75 percent");
        }
    }

    @DisplayName("The mode button changes the open window in place, the same inventory stays open")
    @Test
    void modeButtonKeepsTheWindow(Env env) {
        Instance instance = env.createFlatInstance();
        try (Round round = new Round(env, this.radio.resolve("missing"))) {
            Player player = round.connect(env, instance).player();
            openCategory(env, round, player, MINECRAFT_SLOT);
            AbstractInventory before = player.getOpenInventory();

            click(env, player, MODE_SLOT);
            env.tick();

            Assertions.assertSame(before, player.getOpenInventory(), "the mode change must not reopen the window");
            Assertions.assertEquals(PlayMode.LOOP, round.module.modeOf(player), "the mode must change to loop");
        }
    }

    @DisplayName("The title that plays for a player glows in that player's window only")
    @Test
    void playingTitleGlows(Env env) {
        Instance instance = env.createFlatInstance();
        try (Round round = new Round(env, this.radio.resolve("missing"))) {
            Player player = round.connect(env, instance).player();
            openCategory(env, round, player, MINECRAFT_SLOT);
            click(env, player, DISC_13_SLOT);
            tick(env, 3);

            AbstractInventory window = player.getOpenInventory();
            Assertions.assertEquals(Boolean.TRUE, window.getItemStack(DISC_13_SLOT).get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE), "the title that plays must glow");
            Assertions.assertNull(window.getItemStack(ELEVEN_SLOT).get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE), "a title that does not play must not glow");
        }
    }
}
