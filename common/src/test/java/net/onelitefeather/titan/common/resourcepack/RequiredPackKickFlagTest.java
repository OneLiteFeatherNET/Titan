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

import net.onelitefeather.titan.common.utils.TitanFeatures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.manager.FeatureManagerBuilder;
import org.togglz.core.repository.FeatureState;
import org.togglz.core.repository.mem.InMemoryStateRepository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the default of {@link TitanFeatures#RESOURCE_PACK_REQUIRED_KICK}. A lobby without a
 * {@code flags.properties} - or with one that cannot be read - must enforce a pack the operator
 * configured {@code required}, because the absence of a file is not a decision to stop
 * enforcing.
 */
class RequiredPackKickFlagTest {

    private static FeatureManager emptyRepository() {
        return new FeatureManagerBuilder().featureEnum(TitanFeatures.class).stateRepository(new InMemoryStateRepository()).build();
    }

    @Test
    @DisplayName("A missing flag file leaves required packs enforced")
    void testDefaultsToEnforcing() {
        assertTrue(emptyRepository().isActive(TitanFeatures.RESOURCE_PACK_REQUIRED_KICK), "Without a stored state the kick must stay on");
    }

    @Test
    @DisplayName("The flag is a plain kill switch an operator can turn off")
    void testCanBeSwitchedOff() {
        FeatureManager manager = emptyRepository();
        manager.setFeatureState(new FeatureState(TitanFeatures.RESOURCE_PACK_REQUIRED_KICK, false));
        assertFalse(manager.isActive(TitanFeatures.RESOURCE_PACK_REQUIRED_KICK));
    }
}
