package net.onelitefeather.titan.utils;

import org.togglz.core.activation.DefaultActivationStrategyProvider;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.manager.FeatureManagerBuilder;
import org.togglz.core.repository.file.FileBasedStateRepository;
import org.togglz.core.spi.FeatureManagerProvider;
import org.togglz.core.user.NoOpUserProvider;

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
                    .userProvider(new NoOpUserProvider())
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
