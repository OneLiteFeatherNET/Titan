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

import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.random.RandomGenerator;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.module.LobbyPlayerBusyEvent;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The radio: a hotbar item that opens the vanilla disc menu, and a switch to the MIDI songs from
 * {@code radio/songs.yaml}. Every title plays for the one player who started it, and the play mode
 * (once, loop, shuffle) lives in that player's session only.
 *
 * <p>The lobby ticks once per server tick: the playbacks step on that tick, and their notes are
 * counted in ticks of this loop, so a stalled loop never sends a burst of notes.
 */
@Singleton
public final class RadioModule {

    /** This feature's position among its sibling {@link FeatureNode}s. */
    static final int EVENT_PRIORITY = 680;

    static final String ID = "radio";

    private static final Logger LOG = LoggerFactory.getLogger(RadioModule.class);
    private static final Path DIRECTORY = Path.of("radio");

    private final EventNode<Event> titan;
    private final Scheduler scheduler;
    private final Telemetry telemetry;
    private final RandomGenerator random;
    private final RadioMessages messages;
    private final Path directory;
    private final RadioTelemetry radioTelemetry;
    private final RadioSessions sessions = new RadioSessions();
    private List<MidiSong> songs = List.of();
    private RadioSelection selection;
    private RadioMenuBuilder menus;
    private FeatureNode node;
    private Task tickTask;
    private long serverTick;
    private boolean stopped;

    @Inject
    public RadioModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, Scheduler scheduler, Telemetry telemetry, RandomGenerator random, RadioMessages messages) {
        this(titan, scheduler, telemetry, random, messages, DIRECTORY);
    }

    /** The directory of the MIDI songs is a parameter, so a test points it at its own files. */
    RadioModule(EventNode<Event> titan, Scheduler scheduler, Telemetry telemetry, RandomGenerator random, RadioMessages messages, Path directory) {
        this.titan = Objects.requireNonNull(titan, "titan must not be null");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler must not be null");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry must not be null");
        this.random = Objects.requireNonNull(random, "random must not be null");
        this.messages = Objects.requireNonNull(messages, "messages must not be null");
        this.directory = Objects.requireNonNull(directory, "directory must not be null");
        this.radioTelemetry = new RadioTelemetry(this.telemetry);
    }

    @PostConstruct
    void start() {
        SongCatalog.Load load = SongCatalog.load(this.directory);
        this.songs = load.songs();
        load.songs().forEach(song -> this.radioTelemetry.songLoaded());
        load.rejections().forEach(rejection -> this.radioTelemetry.songRejected());
        LOG.info("Radio started with {} MIDI songs, {} rejected", this.songs.size(), load.rejections().size());
        this.selection = new RadioSelection(catalog(), this.random);
        this.menus = new RadioMenuBuilder(this.songs);
        this.menus.register();
        this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY, this.telemetry).on(PlayerDisconnectEvent.class, event -> logout(event.getPlayer())).on(LobbyPlayerBusyEvent.class, this::onBusy).onIncludingCancelled(InventoryPreClickEvent.class, this::onClick);
        this.tickTask = this.scheduler.scheduleTask(this::tick, TaskSchedule.tick(1), TaskSchedule.tick(1));
    }

    /**
     * Idempotent. Ends every title with {@code SHUTDOWN} and removes the listeners and inventories.
     */
    @PreDestroy
    void stop() {
        if (this.stopped) {
            return;
        }
        this.stopped = true;
        if (this.tickTask != null) {
            this.tickTask.cancel();
        }
        if (this.node != null) {
            this.node.close();
        }
        for (RadioSession session : this.sessions.all()) {
            if (session.playing()) {
                end(session, StopReason.SHUTDOWN);
            }
        }
        this.sessions.clear();
        if (this.menus != null) {
            this.menus.unregister();
        }
    }

    /** Opens the vanilla disc menu for {@code player}, in the mode of the player's session. */
    void open(Player player) {
        if (this.stopped) {
            return;
        }
        open(player, RadioMenu.VANILLA, this.sessions.of(player).mode());
    }

    /** The inventory of a menu in a mode, as {@code locale} sees it; the tests look at these. */
    Inventory inventory(RadioMenu menu, PlayMode mode, Locale locale) {
        return this.menus.inventory(menu, mode, locale);
    }

    /** The play mode of the player's session. */
    PlayMode modeOf(Player player) {
        return this.sessions.of(player).mode();
    }

    private void open(Player player, RadioMenu menu, PlayMode mode) {
        player.openInventory(this.menus.inventory(menu, mode, localeOf(player)));
    }

    private void onClick(InventoryPreClickEvent event) {
        if (this.stopped || this.menus == null) {
            return;
        }
        Optional<RadioMenuBuilder.Variant> clicked = this.menus.ownerOf(event.getInventory());
        if (clicked.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        click(event.getPlayer(), clicked.get(), event.getSlot());
    }

    private void click(Player player, RadioMenuBuilder.Variant clicked, int slot) {
        RadioSession session = this.sessions.of(player);
        int disc = RadioMenuLayout.discAt(clicked.menu(), slot);
        if (disc >= 0) {
            List<? extends RadioSong> titles = clicked.menu() == RadioMenu.VANILLA ? VanillaDiscs.ALL : this.songs;
            if (disc < titles.size()) {
                start(player, session, titles.get(disc), Trigger.USER);
            }
            return;
        }
        switch (RadioMenuLayout.controlAt(slot)) {
            case STOP -> stop(player, StopReason.USER);
            case NEXT -> next(player, session);
            case MODE -> {
                PlayMode mode = clicked.mode().next();
                session.mode(mode);
                open(player, clicked.menu(), mode);
            }
            case SWITCH -> {
                if (this.menus.switchShown()) {
                    open(player, clicked.menu().other(), clicked.mode());
                }
            }
            case NONE -> {
                // A filler slot or an empty disc slot does nothing.
            }
        }
    }

    private void start(Player player, RadioSession session, RadioSong song, Trigger trigger) {
        stopCurrent(session, StopReason.NEXT);
        RadioPlayback playback = new RadioPlayback(song, new PlayerRadioSink(player));
        session.play(song, playback);
        this.radioTelemetry.started(song.kind(), session.mode(), trigger);
        this.radioTelemetry.notesDropped(playback.start(this.serverTick).droppedNotes());
        player.sendActionBar(RadioMessages.text(RadioMessages.PLAYING, localeOf(player), song.title(), song.artist()));
    }

    private void stop(Player player, StopReason reason) {
        Optional<RadioSession> session = this.sessions.find(player.getUuid());
        if (session.isEmpty() || !session.get().playing()) {
            return;
        }
        stopCurrent(session.get(), reason);
        if (reason == StopReason.USER || reason == StopReason.BUSY) {
            player.sendActionBar(RadioMessages.text(RadioMessages.STOPPED, localeOf(player)));
        }
    }

    // Next takes the title after the one playing, or the first when nothing plays, and ends the current one with NEXT.
    private void next(Player player, RadioSession session) {
        Optional<RadioSong> target = this.selection.next(session.playing() ? session.song() : null, session.view().lastCategory(), session.mode());
        if (target.isEmpty()) {
            return;
        }
        start(player, session, target.get(), Trigger.USER);
    }

    private void logout(Player player) {
        stop(player, StopReason.LOGOUT);
        this.sessions.forget(player.getUuid());
    }

    private void onBusy(LobbyPlayerBusyEvent event) {
        if (this.stopped || !event.busy()) {
            return;
        }
        stop(event.getPlayer(), StopReason.BUSY);
    }

    private void stopCurrent(RadioSession session, StopReason reason) {
        if (session.playing()) {
            end(session, reason);
        }
    }

    private void end(RadioSession session, StopReason reason) {
        RadioSong song = session.song();
        session.playback().stop();
        session.clear();
        this.radioTelemetry.stopped(reason, song.kind());
    }

    /**
     * One step of every playback, on every server tick. A title that ends may start its successor.
     */
    private void tick() {
        if (this.stopped) {
            return;
        }
        this.serverTick++;
        for (RadioSession session : this.sessions.all()) {
            if (!session.playing()) {
                continue;
            }
            PlaybackTick step = session.playback().tick(this.serverTick);
            this.radioTelemetry.notesDropped(step.droppedNotes());
            if (step.ended()) {
                RadioSong ended = session.song();
                session.clear();
                this.radioTelemetry.stopped(StopReason.END, ended.kind());
                Optional<RadioSong> successor = this.selection.afterEnd(ended, session.mode());
                if (successor.isPresent() && session.player().isOnline()) {
                    start(session.player(), session, successor.get(), Trigger.AUTO);
                }
            }
        }
    }

    private List<RadioSong> catalog() {
        List<RadioSong> catalog = new ArrayList<>(VanillaDiscs.ALL);
        catalog.addAll(this.songs);
        return catalog;
    }

    private static Locale localeOf(Player player) {
        return RadioMenuBuilder.languageOf(player.getLocale());
    }
}
