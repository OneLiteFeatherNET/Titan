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
package net.onelitefeather.titan.common.resourcepack;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Makes a season change take effect on a running lobby.
 *
 * <p>Without this nothing ever calls {@link ResourcePackService#applySeason}: an operator swaps
 * the Halloween entry in {@code resource-packs.json} for Winter and the players who are online
 * keep Halloween until the process is restarted. US-6.02 asks for the opposite - the season pack
 * is replaced when the season turns, not when the lobby next boots.
 *
 * <p>The trigger is the configuration file itself, re-read on a fixed interval. That is
 * deliberate:
 * <ul>
 * <li>The file is already the control surface. An operator changes a season by editing it, and
 * a deployment or a cron job can do the same at midnight without a console, a permission or a
 * network call.</li>
 * <li>It is how this project already treats file-based configuration. Togglz'
 * {@code FileBasedStateRepository} - the state repository behind the lobby's feature flags -
 * polls its file at 1000&nbsp;ms for exactly this reason.</li>
 * <li>It needs no new command, no new permission and no coordination with a proxy, so nothing
 * has to be reachable for a season to turn.</li>
 * </ul>
 *
 * <p>Only the season slot is watched. A changed base pack, timeout or Bedrock setting is
 * ignored, because swapping the large base pack under a lobby full of players is a restart-level
 * decision, not something a file poll should do by itself.
 *
 * <p>The poll runs on Minestom's scheduler, which means it runs on the server tick thread. That
 * matters: {@code Player#sendResourcePacks} writes into the same unsynchronised map that the
 * packet handler reads, and the packet handler runs on the tick thread too.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class ResourcePackSeasonWatcher implements AutoCloseable {

    /** How often the configuration file is re-read when no interval is given. */
    public static final Duration DEFAULT_INTERVAL = Duration.ofSeconds(5);

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcePackSeasonWatcher.class);

    private final Path directory;
    private final ResourcePackService service;
    private final Supplier<Collection<Player>> onlinePlayers;
    private final Duration interval;
    private final Scheduler scheduler;
    private volatile @Nullable Task task;

    /**
     * Creates a watcher with explicit collaborators. Tests use this to point the watcher at a
     * temporary directory and to shorten the interval.
     *
     * @param directory     the directory holding {@code resource-packs.json}
     * @param service       the service whose season is swapped
     * @param onlinePlayers the players a swap has to reach
     * @param interval      how often the file is re-read
     * @param scheduler     the scheduler the poll runs on
     */
    public ResourcePackSeasonWatcher(Path directory, ResourcePackService service, Supplier<Collection<Player>> onlinePlayers, Duration interval, Scheduler scheduler) {
        this.directory = Objects.requireNonNull(directory, "The configuration directory must not be null");
        this.service = Objects.requireNonNull(service, "The resource pack service must not be null");
        this.onlinePlayers = Objects.requireNonNull(onlinePlayers, "The player supplier must not be null");
        this.interval = Objects.requireNonNull(interval, "The poll interval must not be null");
        this.scheduler = Objects.requireNonNull(scheduler, "The scheduler must not be null");
        if (interval.isZero() || interval.isNegative()) {
            throw new IllegalArgumentException("The poll interval must be positive, got: " + interval);
        }
    }

    /**
     * Creates the watcher Titan runs with: the server's own scheduler, the server's online
     * players and {@link #DEFAULT_INTERVAL}.
     *
     * @param directory the directory the server runs in
     * @param service   the service whose season is swapped
     * @return the watcher
     */
    public static ResourcePackSeasonWatcher create(Path directory, ResourcePackService service) {
        return new ResourcePackSeasonWatcher(directory, service, () -> MinecraftServer.getConnectionManager().getOnlinePlayers(), DEFAULT_INTERVAL, MinecraftServer.getSchedulerManager());
    }

    /**
     * Starts polling. Calling this twice has no effect beyond the first call.
     */
    public void start() {
        if (this.task != null) {
            return;
        }
        TaskSchedule schedule = TaskSchedule.duration(this.interval);
        this.task = this.scheduler.scheduleTask(this::poll, schedule, schedule);
        LOGGER.info("Watching {} for season changes every {}", this.directory.resolve(ResourcePackSettingsProvider.FILE_NAME), this.interval);
    }

    /**
     * Re-reads the configuration once and swaps the season pack when it changed.
     *
     * <p>Public so a test can drive one poll without waiting for the timer, and so an operator
     * command could later force one.
     *
     * @return {@code true} when a season change was applied
     */
    public boolean poll() {
        Optional<ResourcePackSettings> loaded = ResourcePackSettingsProvider.reload(this.directory);
        if (loaded.isEmpty()) {
            return false;
        }
        ResourcePackDefinition configured = loaded.get().season();
        ResourcePackDefinition current = this.service.settings().season();
        if (Objects.equals(configured, current)) {
            return false;
        }
        Collection<Player> players = this.onlinePlayers.get();
        LOGGER.info("The season pack changed from {} to {} - swapping it for {} online player(s)", identify(current), identify(configured), players.size());
        this.service.applySeason(configured, players);
        return true;
    }

    /**
     * Stops polling.
     */
    @Override
    public void close() {
        Task running = this.task;
        this.task = null;
        if (running != null) {
            running.cancel();
        }
    }

    /**
     * Names a pack for a log line.
     *
     * @param definition the pack, or {@code null} when no season is served
     * @return the pack identifier, or {@code none}
     */
    private static String identify(@Nullable ResourcePackDefinition definition) {
        return definition == null ? "none" : definition.id().toString();
    }
}
