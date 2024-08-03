package net.onelitefeather.titan.utils;

import org.togglz.core.Feature;
import org.togglz.core.context.FeatureContext;

public enum TitanFeatures implements Feature, ThreadHelper {
    WINTER,
    HALLOWEEN,
    SLENDER,
    MANIS,
    ;

    @Override
    public boolean isActive() {
        return syncThreadForServiceLoader(() -> FeatureContext.getFeatureManager().isActive(this));
    }
}
