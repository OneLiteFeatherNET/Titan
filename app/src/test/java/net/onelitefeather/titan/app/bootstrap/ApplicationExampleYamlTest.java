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
import net.onelitefeather.titan.app.feature.elytra.ElytraConfig;
import net.onelitefeather.titan.app.feature.sit.SitConfig;
import net.onelitefeather.titan.app.feature.spawn.SpawnConfig;
import net.onelitefeather.titan.app.feature.tickle.TickleConfig;
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

            Assertions.assertEquals(SpawnConfig.DEFAULTS, sections.section("spawn", SpawnConfig.class, SpawnConfig.DEFAULTS), "spawn must bind to exactly its own defaults");
            Assertions.assertEquals(SitConfig.DEFAULTS, sections.section("sit", SitConfig.class, SitConfig.DEFAULTS), "sit must bind to exactly its own defaults");
            Assertions.assertEquals(TickleConfig.DEFAULTS, sections.section("tickle", TickleConfig.class, TickleConfig.DEFAULTS), "tickle must bind to exactly its own defaults");
            Assertions.assertEquals(ElytraConfig.DEFAULTS, sections.section("elytra", ElytraConfig.class, ElytraConfig.DEFAULTS), "elytra must bind to exactly its own defaults");

            // navigator no longer has a Config record to bind through ConfigSections (see
            // openspec/changes/avaje-config-facade/design.md, decision 6); its own values are
            // checked directly against the raw Configuration instead, literal expected values in
            // place of the removed record's own defaults.
            Assertions.assertEquals("<yellow>Navigator", configuration.get("navigator.title"), "navigator.title must match its own default");
            Assertions.assertEquals(0, configuration.getInt("navigator.entries.elytrarace.slot"), "navigator.entries.elytrarace.slot");
            Assertions.assertEquals("Survival", configuration.get("navigator.entries.survival.destination"), "navigator.entries.survival.destination");
            Assertions.assertEquals("NAVIGATOR_SLENDER", configuration.getNullable("navigator.entries.slender.feature"), "navigator.entries.slender.feature");
            Assertions.assertEquals("MemberBuild", configuration.get("navigator.entries.creative.destination"), "navigator.entries.creative.destination");
        } finally {
            sectionBinderLogger.detachAppender(appender);
        }

        Assertions.assertTrue(appender.list.isEmpty(), "no section may log an unknown-key warning; log was: " + appender.list);
    }
}
