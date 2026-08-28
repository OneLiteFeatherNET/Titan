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

import org.togglz.core.activation.DefaultActivationStrategyProvider;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.manager.FeatureManagerBuilder;
import org.togglz.core.repository.file.FileBasedStateRepository;
import org.togglz.core.spi.FeatureManagerProvider;
import org.togglz.core.user.thread.ThreadLocalUserProvider;

import java.io.File;

/**
 * Supplies the ambient Togglz {@link FeatureManager} for Titan: {@link TitanFeatures} backed by
 * the {@code flags.properties} file next to the running process.
 *
 * <p><b>Why the priority is {@value #PRIORITY}.</b> Togglz collects every
 * {@link FeatureManagerProvider} on the classpath through the {@link java.util.ServiceLoader},
 * sorts them by {@link #priority()} ascending and takes the first one that returns a manager, so
 * the lowest number wins. Titan's fat jar contains a second provider:
 * {@code net.onelitefeather.butterfly.minestom.feature.SingletonFeatureManagerProvider}, which
 * reads the same {@code flags.properties} but builds its manager from {@code ButterflyFeatures}.
 * Both providers used to declare {@code 30}, and a tie is broken by whichever entry the merged
 * service file happens to list first - that is shadow's classpath walk order, which changes when
 * a dependency is reordered in {@code app/build.gradle.kts} or its configuration changes. Losing
 * that coin flip is quiet rather than loud: {@code getFeatureState} still resolves Titan's flags
 * by name, but {@link FeatureGate#statuses()},
 * {@link FeatureGate#pollStageTransitions()} and {@code /season status} would enumerate
 * Butterfly's enum instead of Titan's. {@value #PRIORITY} puts Titan ahead of Butterfly's 30 and
 * of Togglz's own providers (50 to 200) without relying on file order.
 *
 * <p><b>On the static field.</b> The lazily initialised {@code featureManager} is mutable static
 * state, which OLF-L2-05 forbids on principle. It is the deviation that rule names explicitly:
 * the Togglz SPI instantiates this class through the {@link java.util.ServiceLoader}, so there is
 * no instance for the manager to hang off. The consequence the rule draws is that the class
 * belongs in Butterfly rather than in two projects - not that the field should be turned into
 * something else here.
 *
 * @author TheMeinerLP
 * @version 1.1.0
 * @since 1.0.0
 */
public final class SingletonFeatureManagerProvider implements FeatureManagerProvider {

    /**
     * Priority of this provider. Lower wins; Butterfly's rival provider declares {@code 30} and
     * Togglz's own providers declare {@code 50} and above.
     */
    public static final int PRIORITY = 10;

    private static FeatureManager featureManager;
    private static final File FLAGS = new File("flags.properties");

    /**
     * Returns the feature manager, building it on first use.
     *
     * @return the manager over {@link TitanFeatures}
     */
    @Override
    public FeatureManager getFeatureManager() {
        if (featureManager == null) {
            featureManager = new FeatureManagerBuilder().featureEnum(TitanFeatures.class).stateRepository(new FileBasedStateRepository(FLAGS)).userProvider(new ThreadLocalUserProvider()).activationStrategyProvider(new DefaultActivationStrategyProvider()).build();
        }

        return featureManager;
    }

    /**
     * Returns {@value #PRIORITY}, low enough to beat Butterfly's provider deterministically.
     *
     * @return the provider priority, lower wins
     */
    @Override
    public int priority() {
        return PRIORITY;
    }
}
