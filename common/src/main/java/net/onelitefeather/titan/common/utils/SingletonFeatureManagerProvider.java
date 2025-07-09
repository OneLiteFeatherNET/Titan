/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
            featureManager = new FeatureManagerBuilder()
                    .featureEnum(TitanFeatures.class)
                    .stateRepository(new FileBasedStateRepository(FLAGS))
                    .userProvider(new ThreadLocalUserProvider())
                    .activationStrategyProvider(new DefaultActivationStrategyProvider())
                    .build();
        }

        return featureManager;
    }

    @Override
    public int priority() {
        return 30;
    }
}
