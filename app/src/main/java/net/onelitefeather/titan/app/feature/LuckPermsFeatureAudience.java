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
package net.onelitefeather.titan.app.feature;

import net.kyori.adventure.permission.PermissionChecker;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.onelitefeather.titan.app.player.TitanPlayer;
import net.onelitefeather.titan.common.feature.FeatureAudience;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Answers the gate's questions through LuckPerms, the permission system Titan already embeds.
 *
 * <p><b>One answer per question.</b> A permission is answered by the online player's
 * {@link PermissionChecker} - on a Titan lobby that is {@link TitanPlayer}, the very object
 * {@code /season}'s command condition consults through {@link PermissionChecker#POINTER}. Reusing
 * it rather than repeating the lookup here (OLF-L2-04) is what keeps the two answers identical: a
 * team member who holds
 * {@value net.onelitefeather.titan.common.feature.ReleaseStage#INTERNAL_PERMISSION}
 * in a server-scoped context is either inside the internal audience for both the command and every
 * feature, or outside both. The earlier version of this class read
 * {@link User#getQueryOptions()} - the holder's <em>stored</em> options, which carry no server,
 * world or dimension context - and so put the same person inside the command's internal audience
 * and outside the gate's.
 *
 * <p>Group membership has no equivalent on the player object, so it stays on LuckPerms; it is
 * resolved against {@link ContextManager#getQueryOptions(User)}, the same contextual options
 * {@link TitanPlayer} evaluates permissions with. An offline player has no context to resolve, so
 * both paths fall back to {@link ContextManager#getStaticQueryOptions()}.
 *
 * <p><b>When LuckPerms is not there.</b> {@link LuckPermsProvider#get()} throws while LuckPerms is
 * loading or after it failed to load, and the gate is asked on every navigator open. Rather than
 * let that escape into a listener, every answer is caught and turned into {@code false} -
 * {@link FeatureAudience#denyAll()}'s behaviour, which that method documents as the safe default.
 * Failing closed is the right direction here because the stages only ever <em>widen</em>: it costs
 * a team member the sight of an unreleased feature until the backend is back, whereas failing open
 * would promote every {@code internal} and {@code lite} feature to the whole server at the one
 * moment nobody can revoke it. {@code ga} features are unaffected either way -
 * {@link net.onelitefeather.titan.common.feature.ReleaseStage#GA} admits everyone without asking
 * an audience - so an outage hides work in progress and never hides the lobby.
 *
 * @author TheMeinerLP
 * @version 2.0.0
 * @since 1.15.0
 */
public final class LuckPermsFeatureAudience implements FeatureAudience {

    private static final Logger LOGGER = LoggerFactory.getLogger(LuckPermsFeatureAudience.class);

    private final Supplier<LuckPerms> luckPerms;

    /** Resolves the permission checker of an online player, or {@code null} when none is online. */
    private final Function<UUID, PermissionChecker> onlineChecker;

    /** Guards the log so an outage costs one warning, not one per navigator entry per open. */
    private final AtomicBoolean unavailable = new AtomicBoolean();

    private LuckPermsFeatureAudience(Supplier<LuckPerms> luckPerms, Function<UUID, PermissionChecker> onlineChecker) {
        this.luckPerms = luckPerms;
        this.onlineChecker = onlineChecker;
    }

    /**
     * Creates an audience reading from the running LuckPerms instance and the online players.
     *
     * @return an audience backed by {@link LuckPermsProvider}
     */
    public static LuckPermsFeatureAudience create() {
        return new LuckPermsFeatureAudience(LuckPermsProvider::get, LuckPermsFeatureAudience::onlineChecker);
    }

    /**
     * Creates an audience reading from an explicitly supplied LuckPerms instance.
     *
     * @param luckPerms supplies the LuckPerms instance to ask
     * @return an audience backed by that instance
     */
    public static LuckPermsFeatureAudience of(Supplier<LuckPerms> luckPerms) {
        return new LuckPermsFeatureAudience(luckPerms, LuckPermsFeatureAudience::onlineChecker);
    }

    /**
     * Creates an audience with an explicit player lookup, so a test can stand in for the running
     * server without booting one.
     *
     * @param luckPerms     supplies the LuckPerms instance to ask
     * @param onlineChecker resolves the permission checker of an online player, {@code null} when
     *                      that player is not online
     * @return an audience backed by both
     */
    static LuckPermsFeatureAudience of(Supplier<LuckPerms> luckPerms, Function<UUID, PermissionChecker> onlineChecker) {
        return new LuckPermsFeatureAudience(luckPerms, onlineChecker);
    }

    @Override
    public boolean hasPermission(UUID playerId, String permission) {
        return answer(() -> {
            PermissionChecker checker = this.onlineChecker.apply(playerId);
            if (checker != null) {
                return checker.test(permission);
            }
            User user = user(playerId);
            return user != null && user.getCachedData().getPermissionData(queryOptions(user)).checkPermission(permission).asBoolean();
        });
    }

    @Override
    public boolean inGroup(UUID playerId, String group) {
        return answer(() -> {
            User user = user(playerId);
            if (user == null) {
                return false;
            }
            for (Group inherited : user.getInheritedGroups(queryOptions(user))) {
                if (inherited.getName().equalsIgnoreCase(group)) {
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Resolves the permission checker of the player behind the id, or {@code null} when nobody by
     * that id is online. On a Titan lobby the checker is the {@link TitanPlayer} itself.
     */
    private static @Nullable PermissionChecker onlineChecker(UUID playerId) {
        Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(playerId);
        return player == null ? null : player.get(PermissionChecker.POINTER).orElse(null);
    }

    /**
     * Returns the query options a permission or group question is answered with: the player's
     * current context while they are online, the static context otherwise. This is the same
     * resolution {@link TitanPlayer} performs, which is what keeps the two answers in step.
     */
    private QueryOptions queryOptions(User user) {
        ContextManager contexts = this.luckPerms.get().getContextManager();
        return contexts.getQueryOptions(user).orElseGet(contexts::getStaticQueryOptions);
    }

    private @Nullable User user(UUID playerId) {
        return this.luckPerms.get().getUserManager().getUser(playerId);
    }

    /**
     * Evaluates one question, denying rather than propagating when the permission backend is not
     * available. See the class javadoc for why the failure direction is closed.
     */
    private boolean answer(BooleanSupplier question) {
        try {
            boolean answer = question.getAsBoolean();
            if (this.unavailable.compareAndSet(true, false)) {
                LOGGER.info("The permission backend answers again; release stages are enforced normally.");
            }
            return answer;
        } catch (RuntimeException exception) {
            if (this.unavailable.compareAndSet(false, true)) {
                LOGGER.warn("The permission backend did not answer; every feature below stage 'ga' stays hidden until it does.", exception);
            }
            return false;
        }
    }
}
