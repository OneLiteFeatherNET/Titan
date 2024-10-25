package net.onelitefeather.titan.common.utils;

import org.togglz.core.Feature;
import org.togglz.core.annotation.EnabledByDefault;
import org.togglz.core.context.FeatureContext;

public enum TitanFeatures implements Feature, ThreadHelper {
    NAVIGATOR_CREATIVE,
    NAVIGATOR_SLENDER,
    NAVIGATOR_MANIS,
    NAVIGATOR_SURVIVAL,
    NAVIGATOR_ELYTRA,
    @EnabledByDefault
    ELYTRA_FLY,
    ENTITIES,
;
    @Override
    public boolean isActive() {
        return syncThreadForServiceLoader(() -> FeatureContext.getFeatureManager().isActive(this));
    }
}
