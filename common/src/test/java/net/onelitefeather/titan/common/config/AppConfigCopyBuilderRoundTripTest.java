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
package net.onelitefeather.titan.common.config;

import net.minestom.server.coordinate.Vec;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Proves the copy-builder bug described in
 * {@code openspec/changes/lobby-feature-modules/design.md} (Context, "Monolithische Config"):
 * {@link AppConfig#builder(AppConfig)} does not carry over {@code minHeightBeforeTeleport} and
 * {@code maxHeightBeforeTeleport}. The setup server's {@code AppCommand} builds every field
 * change on top of the existing {@link AppConfig} through that method, so saving any single
 * value (for example the sit offset) silently resets both height bounds to {@code 0}.
 *
 * <p>This test round-trips the repository's real {@code app.json} (mirrored as a test fixture
 * at {@code common/src/test/resources/config/app.json}) through {@link AppConfigProvider}:
 * load, change one unrelated field via the copy-builder, save, reload - and expects the height
 * bounds to have survived.
 *
 * <p>Disabled until {@code ConfigStore} (task 7.1 of lobby-feature-modules) replaces the
 * copy-builder with an abschnittsweises, non-lossy save.
 */
@Disabled("Copy-builder drops heights - replaced by ConfigStore in lobby-feature-modules (task 7.1)")
class AppConfigCopyBuilderRoundTripTest {

    @Test
    @DisplayName("min/maxHeightBeforeTeleport survive AppConfig.builder(existing) round trip through app.json")
    void heightsSurviveCopyBuilderRoundTrip(@TempDir Path tempDir) throws IOException {
        try (InputStream fixture = getClass().getResourceAsStream("/config/app.json")) {
            assertNotNull(fixture, "test fixture /config/app.json is missing from common/src/test/resources");
            Files.copy(fixture, tempDir.resolve("app.json"), StandardCopyOption.REPLACE_EXISTING);
        }

        AppConfigProvider provider = AppConfigProvider.create(tempDir);
        AppConfig loaded = provider.getAppConfig();

        int expectedMinHeight = loaded.minHeightBeforeTeleport();
        int expectedMaxHeight = loaded.maxHeightBeforeTeleport();

        // An unrelated change, exactly like the setup server's AppCommand does for a single field.
        AppConfig changed = AppConfig.builder(loaded).sitOffset(new Vec(0.1, 0.75, 0.9)).build();
        provider.saveConfig(changed);

        AppConfig reloaded = provider.getAppConfig();

        assertEquals(expectedMinHeight, reloaded.minHeightBeforeTeleport(), "minHeightBeforeTeleport must survive an AppConfig.builder(existing) round trip");
        assertEquals(expectedMaxHeight, reloaded.maxHeightBeforeTeleport(), "maxHeightBeforeTeleport must survive an AppConfig.builder(existing) round trip");
    }
}
