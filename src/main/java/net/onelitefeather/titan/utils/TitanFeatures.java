package net.onelitefeather.titan.utils;

import org.togglz.core.Feature;
import org.togglz.core.annotation.EnabledByDefault;
import org.togglz.core.context.FeatureContext;

public enum TitanFeatures implements Feature, ThreadHelper {
    WINTER,
    HALLOWEEN,
    SLENDER,
    MANIS,
    @EnabledByDefault
    ELYTRA_FLY
    ;

    @Override
    public boolean isActive() {
        return syncThreadForServiceLoader(() -> FeatureContext.getFeatureManager().isActive(this));
    }
}
