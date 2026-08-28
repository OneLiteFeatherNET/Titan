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
package net.onelitefeather.titan.common.feature;

import net.onelitefeather.titan.common.utils.ThreadHelper;
import org.togglz.core.Feature;
import org.togglz.core.context.FeatureContext;

/**
 * The feature flags Titan knows. The enum is the single source of truth for the flag names, and
 * {@link SingletonFeatureManagerProvider} builds the ambient
 * {@link org.togglz.core.manager.FeatureManager} from exactly this enum.
 *
 * <p>Release stages and time windows are configuration on an existing flag, not new flags
 * (NFR-009); {@link FeatureGate} reads them from the feature state.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public enum TitanFeatures implements Feature, ThreadHelper {
    NAVIGATOR_CREATIVE, NAVIGATOR_SLENDER, NAVIGATOR_MANIS, NAVIGATOR_SURVIVAL, NAVIGATOR_ELYTRA,;

    @Override
    public boolean isActive() {
        return syncThreadForServiceLoader(() -> FeatureContext.getFeatureManager().isActive(this));
    }
}
