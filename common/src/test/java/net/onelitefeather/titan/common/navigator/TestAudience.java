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
package net.onelitefeather.titan.common.navigator;

import net.onelitefeather.titan.common.feature.FeatureAudience;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Fixture standing in for LuckPerms. Permissions can be taken away again, which is the whole
 * point: a permission held while the menu was drawn may be gone by the time the click arrives.
 */
final class TestAudience implements FeatureAudience {

    private final Set<String> permissions = new HashSet<>();

    TestAudience grant(UUID playerId, String permission) {
        this.permissions.add(key(playerId, permission));
        return this;
    }

    TestAudience revoke(UUID playerId, String permission) {
        this.permissions.remove(key(playerId, permission));
        return this;
    }

    @Override
    public boolean hasPermission(UUID playerId, String permission) {
        return this.permissions.contains(key(playerId, permission));
    }

    @Override
    public boolean inGroup(UUID playerId, String group) {
        return false;
    }

    private static String key(UUID playerId, String value) {
        return playerId + "/" + value;
    }
}
