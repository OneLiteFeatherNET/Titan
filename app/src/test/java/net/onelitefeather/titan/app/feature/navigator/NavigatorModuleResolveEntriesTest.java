/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.onelitefeather.titan.app.feature.navigator;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.avaje.config.Configuration;
import java.util.List;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntry;
import net.onelitefeather.titan.common.config.RuntimeConfigFallback;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;

/**
 * Unit coverage for {@link NavigatorModule#resolveEntries}: the pure (in the sense of never
 * touching the static {@code io.avaje.config.Config} facade) runtime fallback behind the
 * navigator's on-open re-read (see {@code openspec/changes/config-reload-feature-flags/
 * design.md}, decision 2, and {@code tasks.md}, task 3.6). Every test builds its own
 * {@link Configuration} instances directly and its own {@link RuntimeConfigFallback} (F.I.R.S.T. -
 * Independent); none of this mutates {@code io.avaje.config.Config}.
 *
 * <p>{@link MicrotusExtension} is needed because {@link NavigatorModule#toNavigatorEntry} builds a
 * real {@code ItemStack} for each entry's icon (unlike {@link NavigatorEntryValidation#buildEntry},
 * whose own {@link Material#fromKey(String)} check needs no booted registry at all - see that
 * class's Javadoc) - which needs Minestom's registry bound, not merely {@link Material#fromKey}
 * resolving the key. {@link MicrotusExtension} only binds that registry once its {@code Env}
 * parameter is actually resolved (see {@link net.minestom.testing.extension.MicrotusExtension}),
 * so every test method below declares one, exactly like {@link NavigatorModuleTest} and
 * {@link NavigatorFeatureFlagTest} do - unused otherwise, since
 * {@link NavigatorModule#resolveEntries}
 * itself needs no {@code Env} at all.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorModuleResolveEntriesTest {

    private static Configuration oneValidEntry(String name, int slot, String feature) {
        Configuration.Builder builder = Configuration.builder().put("navigator.entries." + name + ".slot", Integer.toString(slot)).put("navigator.entries." + name + ".icon", "minecraft:grass_block").put("navigator.entries." + name + ".displayName", "<green>" + name).put("navigator.entries." + name + ".destination", name);
        if (feature != null) {
            builder.put("navigator.entries." + name + ".feature", feature);
        }
        return builder.build();
    }

    private static RuntimeConfigFallback freshFallback() {
        return new RuntimeConfigFallback(Configuration.builder().build());
    }

    @DisplayName("A valid live configuration with only known features passes through unchanged, without warning")
    @Test
    void validLiveConfigurationPassesThroughUnchanged(Env env) {
        Configuration live = oneValidEntry("survival", 4, null);
        Configuration shipped = oneValidEntry("shipped-survival", 4, null);
        Logger logger = (Logger) LoggerFactory.getLogger(RuntimeConfigFallback.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            List<NavigatorEntry> result = NavigatorModule.resolveEntries(live, shipped, new FakeFeatureFlags(), freshFallback());

            Assertions.assertEquals(1, result.size());
            Assertions.assertEquals("survival", result.getFirst().destination(), "the live entry must be used, not the shipped one");
            Assertions.assertTrue(appender.list.isEmpty(), "a valid configuration must never warn");
        } finally {
            logger.detachAppender(appender);
        }
    }

    @DisplayName("An unknown feature name falls the whole set back to the shipped entries and warns once")
    @Test
    void unknownFeatureNameFallsBackToShippedEntriesAndWarnsOnce(Env env) {
        Configuration live = oneValidEntry("survival", 4, "GIBT_ES_NICHT");
        Configuration shipped = oneValidEntry("shipped-survival", 4, null);
        FakeFeatureFlags featureFlags = new FakeFeatureFlags().declare("NAVIGATOR_SLENDER", true);
        Logger logger = (Logger) LoggerFactory.getLogger(RuntimeConfigFallback.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            List<NavigatorEntry> result = NavigatorModule.resolveEntries(live, shipped, featureFlags, freshFallback());

            Assertions.assertEquals(1, result.size());
            Assertions.assertEquals("shipped-survival", result.getFirst().destination(), "an unknown feature name must fall the whole set back to the shipped entries");
            Assertions.assertEquals(1, appender.list.size(), "exactly one WARN must be logged for a new invalid configuration");
            Assertions.assertEquals("navigator.entries", appender.list.getFirst().getArgumentArray()[0], "the WARN must name navigator.entries");
        } finally {
            logger.detachAppender(appender);
        }
    }

    @DisplayName("An out-of-range slot falls the whole set back to the shipped entries")
    @Test
    void outOfRangeSlotFallsBackToShippedEntries(Env env) {
        Configuration live = oneValidEntry("survival", 9, null);
        Configuration shipped = oneValidEntry("shipped-survival", 4, null);

        List<NavigatorEntry> result = NavigatorModule.resolveEntries(live, shipped, new FakeFeatureFlags(), freshFallback());

        Assertions.assertEquals("shipped-survival", result.getFirst().destination());
    }

    @DisplayName("The same invalid configuration seen again does not warn a second time")
    @Test
    void sameInvalidConfigurationDoesNotWarnAgain(Env env) {
        Configuration live = oneValidEntry("survival", 4, "GIBT_ES_NICHT");
        Configuration shipped = oneValidEntry("shipped-survival", 4, null);
        RuntimeConfigFallback fallback = freshFallback();
        Logger logger = (Logger) LoggerFactory.getLogger(RuntimeConfigFallback.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            NavigatorModule.resolveEntries(live, shipped, new FakeFeatureFlags(), fallback);
            NavigatorModule.resolveEntries(live, shipped, new FakeFeatureFlags(), fallback);

            Assertions.assertEquals(1, appender.list.size(), "a repeated, unchanged invalid configuration must warn only once");
        } finally {
            logger.detachAppender(appender);
        }
    }
}
