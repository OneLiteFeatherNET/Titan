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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.inventory.InventoryCloseEvent;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.module.LobbyPlayerBusyEvent;
import net.onelitefeather.titan.core.settings.PlayerSettings;
import net.onelitefeather.titan.core.settings.SettingsRegistry;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.theevilreaper.aves.i18n.TextData;
import net.theevilreaper.aves.inventory.layout.InventoryLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The radio: a hotbar item that opens the category overview, and one window per category with its
 * titles. Every title plays for the one player who started it, and the play mode (once, loop,
 * shuffle) and the volume belong to that player: the mode lives in the session, the volume in the
 * player's settings.
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
    private final PlayerSettings settings;
    private final SettingsRegistry registry;
    private final Path directory;
    private final RadioTelemetry radioTelemetry;
    private final Executor io;
    private final SongNotesCache notes;
    private final RadioSessions sessions;
    private final RadioViews views = new RadioViews();
    private List<MidiSong> songs = List.of();
    private List<RadioCategory> declared = List.of();
    private RadioSelection selection;
    private FeatureNode node;
    private Task tickTask;
    private long serverTick;
    private boolean stopped;

    @Inject
    public RadioModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, Scheduler scheduler, Telemetry telemetry, RandomGenerator random, RadioMessages messages, PlayerSettings settings, SettingsRegistry registry) {
        this(titan, scheduler, telemetry, random, messages, settings, registry, DIRECTORY, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * The directory of the MIDI songs and the executor of the note reads are parameters, so a test
     * points them at its own files and runs the reads inline. An executor that is an
     * {@link ExecutorService} is shut down with the module.
     */
    RadioModule(EventNode<Event> titan, Scheduler scheduler, Telemetry telemetry, RandomGenerator random, RadioMessages messages, PlayerSettings settings, SettingsRegistry registry, Path directory, Executor io) {
        this.titan = Objects.requireNonNull(titan, "titan must not be null");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler must not be null");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry must not be null");
        this.random = Objects.requireNonNull(random, "random must not be null");
        this.messages = Objects.requireNonNull(messages, "messages must not be null");
        this.settings = Objects.requireNonNull(settings, "settings must not be null");
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.directory = Objects.requireNonNull(directory, "directory must not be null");
        this.io = Objects.requireNonNull(io, "io must not be null");
        this.radioTelemetry = new RadioTelemetry(this.telemetry);
        this.notes = new SongNotesCache(this.io, RadioModule::parseFile);
        this.sessions = new RadioSessions(() -> new RadioLoad(this.notes, this.scheduler::scheduleNextTick));
    }

    private static List<MidiNote> parseFile(MidiSong song) throws SongFormatException, IOException {
        return MidiSongParser.notes(Files.readAllBytes(song.file()));
    }

    @PostConstruct
    void start() {
        SongCatalog.Load load = SongCatalog.load(this.directory);
        this.songs = load.songs();
        this.declared = load.categories();
        this.registry.registerLevel(ID, Volume.SETTING);
        load.songs().forEach(song -> this.radioTelemetry.songLoaded());
        load.rejections().forEach(rejection -> this.radioTelemetry.songRejected());
        LOG.info("Radio started with {} categories, {} MIDI songs and {} rejected entries", this.declared.size(), this.songs.size(), load.rejections().size());
        this.selection = new RadioSelection(catalog(), this.random);
        this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY, this.telemetry).on(PlayerDisconnectEvent.class, event -> logout(event.getPlayer())).on(LobbyPlayerBusyEvent.class, this::onBusy).onIncludingCancelled(InventoryPreClickEvent.class, this::onClick).on(InventoryCloseEvent.class, this::onClose);
        this.tickTask = this.scheduler.scheduleTask(this::tick, TaskSchedule.tick(1), TaskSchedule.tick(1));
    }

    /**
     * Idempotent. Ends every title with {@code SHUTDOWN} and closes the open windows.
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
            if (session.playing() || session.loading()) {
                end(session, StopReason.SHUTDOWN);
            }
        }
        this.sessions.clear();
        for (var entry : this.views.entries()) {
            entry.getValue().unregister();
        }
        this.views.clear();
        if (this.io instanceof ExecutorService service) {
            service.shutdown();
        }
    }

    /** Opens the category overview for {@code player}; the play mode of the session stays. */
    void open(Player player) {
        if (this.stopped) {
            return;
        }
        RadioSession session = this.sessions.of(player);
        session.view().showOverview();
        show(player, session);
    }

    /** The play mode of the player's session. */
    PlayMode modeOf(Player player) {
        return this.sessions.of(player).mode();
    }

    /** The window the player has open, if the radio opened one; the tests look at it. */
    Optional<RadioWindow> windowOf(Player player) {
        return this.views.find(player.getUuid());
    }

    // Builds a new window for the state of the session. The title is fixed in Aves, so a page or
    // category change is a new window; the old one is replaced first, so its close changes nothing.
    private void show(Player player, RadioSession session) {
        Locale locale = localeOf(player);
        RadioWindow window = new RadioWindow(player, locale);
        window.setTitleData(titleOf(session, locale));
        window.setLayout(InventoryLayout.fromType(RadioWindow.TYPE));
        window.setDataLayoutFunction(previous -> RadioMenuBuilder.build(menuView(player, session), locale));
        window.register();
        RadioWindow replaced = this.views.put(player.getUuid(), window);
        if (replaced != null) {
            replaced.unregister();
        }
        window.open(locale);
    }

    // The content of the window is redrawn in place; only the title needs show().
    private void refresh(Player player) {
        this.views.find(player.getUuid()).ifPresent(RadioWindow::invalidateDataLayout);
    }

    private RadioMenuBuilder.MenuView menuView(Player player, RadioSession session) {
        RadioSessionState state = session.view();
        List<RadioCategory> categories = state.inOverview() ? RadioCategories.visible(this.declared, idsWithSongs()) : List.of();
        List<RadioSong> titles = state.inOverview() ? List.of() : titlesOf(state.openCategory());
        RadioSong playing = session.playing() ? session.song() : null;
        int volume = this.settings.level(player.getUuid(), Volume.SETTING);
        boolean volumeLater = playing != null && playing.kind() == RadioKind.VANILLA && session.startVolume() != volume;
        boolean canNext = session.playing() || state.lastCategory() != null;
        return new RadioMenuBuilder.MenuView(state, categories, titles, session.mode(), volume, playing, volumeLater, canNext);
    }

    private TextData titleOf(RadioSession session, Locale locale) {
        RadioSessionState state = session.view();
        if (state.inOverview()) {
            return new TextData(RadioMessages.WINDOW_OVERVIEW);
        }
        int pages = PageLayout.pageCount(titlesOf(state.openCategory()).size());
        return new TextData(RadioMessages.WINDOW_CATEGORY, state.openCategory().name(locale), String.valueOf(PageLayout.clampPage(state.page(), titlesOf(state.openCategory()).size())), String.valueOf(pages));
    }

    private void onClick(InventoryPreClickEvent event) {
        if (this.stopped || this.views.ownerOf(event.getInventory()).isEmpty()) {
            return;
        }
        event.setCancelled(true);
        click(event.getPlayer(), event.getSlot(), event.getClick() instanceof Click.Right);
    }

    private void click(Player player, int slot, boolean right) {
        RadioSession session = this.sessions.of(player);
        RadioSessionState state = session.view();
        if (slot >= RadioMenuLayout.CONTENT) {
            control(player, session, RadioMenuLayout.controlAt(slot), right);
            return;
        }
        if (state.inOverview()) {
            List<RadioCategory> categories = RadioCategories.visible(this.declared, idsWithSongs());
            if (slot < categories.size()) {
                state.openCategory(categories.get(slot));
                show(player, session);
            }
            return;
        }
        List<RadioSong> titles = titlesOf(state.openCategory());
        int first = (PageLayout.clampPage(state.page(), titles.size()) - 1) * RadioMenuLayout.CONTENT;
        if (first + slot < titles.size()) {
            start(player, session, titles.get(first + slot), Trigger.USER);
        }
    }

    private void control(Player player, RadioSession session, RadioMenuLayout.Control control, boolean right) {
        RadioSessionState state = session.view();
        List<RadioSong> titles = state.inOverview() ? List.of() : titlesOf(state.openCategory());
        int pages = PageLayout.pageCount(titles.size());
        int page = PageLayout.clampPage(state.page(), titles.size());
        switch (control) {
            case BACK -> {
                if (!state.inOverview()) {
                    state.showOverview();
                    show(player, session);
                }
            }
            case PREVIOUS -> {
                if (!state.inOverview() && page > 1) {
                    state.page(page - 1);
                    show(player, session);
                }
            }
            case FORWARD -> {
                if (!state.inOverview() && page < pages) {
                    state.page(page + 1);
                    show(player, session);
                }
            }
            case STOP -> stop(player, StopReason.USER);
            case NEXT -> next(player, session);
            case MODE -> {
                session.mode(session.mode().next());
                refresh(player);
            }
            case VOLUME -> {
                int current = this.settings.level(player.getUuid(), Volume.SETTING);
                this.settings.setLevel(player.getUuid(), Volume.SETTING, right ? Volume.down(current) : Volume.up(current));
                refresh(player);
            }
            case PAGE_INFO, NOW_PLAYING, NONE -> {
                // Information only: a click on it does nothing.
            }
        }
    }

    // A cached title starts at once; an uncached one shows the loading text and starts when its notes are read.
    private void start(Player player, RadioSession session, RadioSong song, Trigger trigger) {
        stopCurrent(session, StopReason.NEXT);
        session.load().begin(song, new TitleStart(player, session, song, trigger));
        session.touch();
    }

    private void play(Player player, RadioSession session, RadioSong song, List<MidiNote> notes, Trigger trigger) {
        int volume = this.settings.level(player.getUuid(), Volume.SETTING);
        RadioPlayback playback = new RadioPlayback(song, notes, new PlayerRadioSink(player), () -> this.settings.level(player.getUuid(), Volume.SETTING));
        session.play(song, playback, volume);
        this.radioTelemetry.started(song.kind(), session.mode(), trigger, song.category().id());
        this.radioTelemetry.notesDropped(playback.start(this.serverTick).droppedNotes());
        player.sendActionBar(RadioMessages.text(RadioMessages.PLAYING, localeOf(player), song.title(), song.artist()));
    }

    // A title whose notes cannot be read starts nothing: the player is told, the rejection is counted and logged once.
    private void rejected(Player player, RadioSong song, Throwable cause) {
        this.radioTelemetry.songRejected();
        String name = song instanceof MidiSong midi ? midi.file().toString() : song.title();
        LOG.warn("Radio song {} cannot be read and is not played: {}", name, cause.getMessage());
        if (player.isOnline()) {
            player.sendActionBar(RadioMessages.text(RadioMessages.REJECTED, localeOf(player)));
        }
    }

    private void stop(Player player, StopReason reason) {
        Optional<RadioSession> session = this.sessions.find(player.getUuid());
        if (session.isEmpty() || !(session.get().playing() || session.get().loading())) {
            return;
        }
        stopCurrent(session.get(), reason);
        if (reason == StopReason.USER || reason == StopReason.BUSY) {
            player.sendActionBar(RadioMessages.text(RadioMessages.STOPPED, localeOf(player)));
        }
    }

    // Next takes the title after the one playing, or the first of the last opened category, inside that category.
    private void next(Player player, RadioSession session) {
        Optional<RadioSong> target = this.selection.next(session.playing() ? session.song() : null, session.view().lastCategory(), session.mode());
        if (target.isEmpty()) {
            return;
        }
        start(player, session, target.get(), Trigger.USER);
    }

    private void logout(Player player) {
        stop(player, StopReason.LOGOUT);
        this.views.remove(player.getUuid()).ifPresent(RadioWindow::unregister);
        this.sessions.forget(player.getUuid());
    }

    private void onBusy(LobbyPlayerBusyEvent event) {
        if (this.stopped || !event.busy()) {
            return;
        }
        stop(event.getPlayer(), StopReason.BUSY);
    }

    // The player closed the window: the radio forgets the window, keeps the session (mode, category, page).
    private void onClose(InventoryCloseEvent event) {
        Optional<RadioWindow> owner = this.views.ownerOf(event.getInventory());
        if (owner.isEmpty()) {
            return;
        }
        this.views.remove(event.getPlayer().getUuid());
        owner.get().unregister();
    }

    private void stopCurrent(RadioSession session, StopReason reason) {
        if (session.playing() || session.loading()) {
            end(session, reason);
        }
    }

    // Ends the title or the pending load; the generation advances and the notes lease is released.
    private void end(RadioSession session, StopReason reason) {
        if (session.playing()) {
            RadioSong song = session.song();
            session.playback().stop();
            session.clear();
            this.radioTelemetry.stopped(reason, song.kind());
        }
        session.load().cancel();
        session.touch();
    }

    /**
     * One step of every playback, on every server tick. A title that ends may start its successor.
     * An open window whose state changed is redrawn once here.
     */
    private void tick() {
        if (this.stopped) {
            return;
        }
        this.serverTick++;
        for (RadioSession session : this.sessions.all()) {
            if (session.playing()) {
                PlaybackTick step = session.playback().tick(this.serverTick);
                this.radioTelemetry.notesDropped(step.droppedNotes());
                if (step.ended()) {
                    RadioSong ended = session.song();
                    session.clear();
                    session.load().cancel();
                    this.radioTelemetry.stopped(StopReason.END, ended.kind());
                    Optional<RadioSong> successor = this.selection.afterEnd(ended, session.mode());
                    if (successor.isPresent() && session.player().isOnline()) {
                        start(session.player(), session, successor.get(), Trigger.AUTO);
                    }
                }
            }
            if (session.takeChanged()) {
                refresh(session.player());
            }
        }
    }

    private List<RadioSong> catalog() {
        List<RadioSong> catalog = new ArrayList<>(VanillaDiscs.ALL);
        catalog.addAll(this.songs);
        return catalog;
    }

    /**
     * The titles of one category in order: the vanilla discs for Minecraft, the songs otherwise.
     */
    private List<RadioSong> titlesOf(RadioCategory category) {
        return catalog().stream().filter(song -> song.category().id().equals(category.id())).toList();
    }

    private Set<String> idsWithSongs() {
        return catalog().stream().map(song -> song.category().id()).collect(Collectors.toSet());
    }

    /**
     * One title's start for one player. The outcome arrives on the tick thread, and a result that
     * the load drops never reaches this class; a player who logged out hears nothing.
     */
    private final class TitleStart implements RadioLoad.Listener {

        private final Player player;
        private final RadioSession session;
        private final RadioSong song;
        private final Trigger trigger;

        TitleStart(Player player, RadioSession session, RadioSong song, Trigger trigger) {
            this.player = player;
            this.session = session;
            this.song = song;
            this.trigger = trigger;
        }

        @Override
        public void loading() {
            this.player.sendActionBar(RadioMessages.text(RadioMessages.LOADING, localeOf(this.player)));
        }

        @Override
        public void ready(List<MidiNote> notes) {
            if (!RadioModule.this.stopped && this.player.isOnline()) {
                play(this.player, this.session, this.song, notes, this.trigger);
            }
        }

        @Override
        public void failed(Throwable cause) {
            if (!RadioModule.this.stopped) {
                rejected(this.player, this.song, cause);
            }
        }
    }

    /**
     * The language a window is built for: German for German clients, English for every other one.
     */
    static Locale localeOf(Player player) {
        Locale locale = player.getLocale();
        return locale != null && Locale.GERMAN.getLanguage().equals(locale.getLanguage()) ? Locale.GERMAN : Locale.ENGLISH;
    }

}
