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
package net.onelitefeather.titan.common.utils;

import org.togglz.core.activation.DefaultActivationStrategyProvider;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.manager.FeatureManagerBuilder;
import org.togglz.core.repository.file.FileBasedStateRepository;
import org.togglz.core.spi.FeatureManagerProvider;
import org.togglz.core.user.thread.ThreadLocalUserProvider;

import java.io.File;

public final class SingletonFeatureManagerProvider implements FeatureManagerProvider {

    private static FeatureManager featureManager;
    private static final File FLAGS = new File("flags.properties");

    @Override
    public FeatureManager getFeatureManager() {
        if (featureManager == null) {
            featureManager = new FeatureManagerBuilder().featureEnum(TitanFeatures.class).stateRepository(new FileBasedStateRepository(FLAGS)).userProvider(new ThreadLocalUserProvider()).activationStrategyProvider(new DefaultActivationStrategyProvider()).build();
        }

        return featureManager;
    }

    @Override
    public int priority() {
        return 30;
    }
}
