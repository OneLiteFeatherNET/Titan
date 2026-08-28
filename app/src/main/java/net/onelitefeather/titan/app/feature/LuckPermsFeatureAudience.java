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

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.onelitefeather.titan.common.feature.FeatureAudience;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Answers the gate's questions through LuckPerms, the permission system Titan already embeds.
 *
 * <p>Permissions are read from the user's cached permission data, group membership from the
 * inherited groups of the user's query options, so a group that {@code lite} itself inherits from
 * counts as well. A player LuckPerms has not loaded yet is treated as holding nothing, which keeps
 * an unfinished login on the narrow side of every release stage.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class LuckPermsFeatureAudience implements FeatureAudience {

    private final Supplier<LuckPerms> luckPerms;

    private LuckPermsFeatureAudience(Supplier<LuckPerms> luckPerms) {
        this.luckPerms = luckPerms;
    }

    /**
     * Creates an audience reading from the running LuckPerms instance.
     *
     * @return an audience backed by {@link LuckPermsProvider}
     */
    public static LuckPermsFeatureAudience create() {
        return new LuckPermsFeatureAudience(LuckPermsProvider::get);
    }

    /**
     * Creates an audience reading from an explicitly supplied LuckPerms instance.
     *
     * @param luckPerms supplies the LuckPerms instance to ask
     * @return an audience backed by that instance
     */
    public static LuckPermsFeatureAudience of(Supplier<LuckPerms> luckPerms) {
        return new LuckPermsFeatureAudience(luckPerms);
    }

    @Override
    public boolean hasPermission(UUID playerId, String permission) {
        User user = user(playerId);
        return user != null && user.getCachedData().getPermissionData(user.getQueryOptions()).checkPermission(permission).asBoolean();
    }

    @Override
    public boolean inGroup(UUID playerId, String group) {
        User user = user(playerId);
        if (user == null) {
            return false;
        }
        for (Group inherited : user.getInheritedGroups(user.getQueryOptions())) {
            if (inherited.getName().equalsIgnoreCase(group)) {
                return true;
            }
        }
        return false;
    }

    private @Nullable User user(UUID playerId) {
        return this.luckPerms.get().getUserManager().getUser(playerId);
    }
}
