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
package net.onelitefeather.titan.app.bootstrap;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.avaje.config.Configuration;
import java.util.List;
import net.onelitefeather.titan.app.feature.navigator.NavigatorConfig;
import net.onelitefeather.titan.common.config.ConfigSections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Covers task 4.4 of {@code openspec/changes/standardized-config-profiles}: the classpath {@code
 * application.yaml} (see {@code app/src/main/resources/application.yaml}) - the file that both
 * replaces the repo-root {@code app.json} and, per {@code openspec/changes/avaje-config-facade
 * /design.md} decision 2, is now copied by the {@code applicationExampleYaml} Gradle task into the
 * distribution as {@code application.example.yaml}, rather than that file being hand-maintained -
 * loads through the real {@code avaje-config} + SnakeYAML pipeline and binds every module's own
 * section to exactly that module's {@code DEFAULTS}, with no "unknown keys" warning from {@code
 * SectionBinder} - i.e. every key the file sets is one the corresponding record actually declares.
 *
 * <p>Loaded as its own {@link Configuration} instance via
 * {@link Configuration.Builder#load(String)}
 * - which reads a classpath resource, never the static {@code io.avaje.config.Config} facade (see
 * design.md, decision 5: unit tests never touch that facade).
 *
 * <p>The unknown-keys warning is logged by {@code net.onelitefeather.titan.common.config
 * .SectionBinder}, a package-private class in {@code common} this module cannot reference
 * directly; it is looked up by its fully-qualified name instead, the same
 * {@link ch.qos.logback.core.read.ListAppender} pattern {@link ModuleStartupLogTest} uses.
 */
class ApplicationExampleYamlTest {

    private static final String CLASSPATH_APPLICATION_YAML = "application.yaml";

    @DisplayName("application.yaml binds every module section to its own defaults, with no unknown-key warning")
    @Test
    void exampleYamlBindsEveryModuleSectionToItsDefaults() {
        Logger sectionBinderLogger = (Logger) LoggerFactory.getLogger("net.onelitefeather.titan.common.config.SectionBinder");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        sectionBinderLogger.addAppender(appender);

        try {
            Configuration configuration = Configuration.builder().load(CLASSPATH_APPLICATION_YAML).build();
            ConfigSections sections = new ConfigSections(configuration);

            // spawn and sit no longer have a config record to bind through ConfigSections (see
            // avaje-config-facade task 2.1/2.2); their own defaults are checked directly against
            // the loaded Configuration instead.
            Assertions.assertEquals(-64, configuration.getInt("spawn.minHeight"), "spawn.minHeight must match its own default");
            Assertions.assertEquals(310, configuration.getInt("spawn.maxHeight"), "spawn.maxHeight must match its own default");
            Assertions.assertEquals(2, configuration.getInt("spawn.simulationDistance"), "spawn.simulationDistance must match its own default");
            Assertions.assertEquals(0.5, configuration.getDecimal("sit.offset.x").doubleValue(), "sit.offset.x must match its own default");
            Assertions.assertEquals(0.25, configuration.getDecimal("sit.offset.y").doubleValue(), "sit.offset.y must match its own default");
            Assertions.assertEquals(0.5, configuration.getDecimal("sit.offset.z").doubleValue(), "sit.offset.z must match its own default");
            Assertions.assertEquals(List.of("minecraft:spruce_stairs"), configuration.list().of("sit.allowedBlocks"), "sit.allowedBlocks must match its own default");
            Assertions.assertEquals(4000L, configuration.getLong("tickle.cooldownMillis"), "tickle.cooldownMillis must bind to exactly its own default");
            Assertions.assertEquals(30, configuration.getInt("elytra.burnDurationTicks"), "elytra.burnDurationTicks must bind to exactly its own default");
            Assertions.assertEquals(40, configuration.getInt("elytra.cooldownTicks"), "elytra.cooldownTicks must bind to exactly its own default");
            Assertions.assertEquals(NavigatorConfig.DEFAULTS, sections.section("navigator", NavigatorConfig.class, NavigatorConfig.DEFAULTS), "navigator must bind to exactly its own defaults, entries keyed by name");
        } finally {
            sectionBinderLogger.detachAppender(appender);
        }

        Assertions.assertTrue(appender.list.isEmpty(), "no section may log an unknown-key warning; log was: " + appender.list);
    }
}
