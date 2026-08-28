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
package net.onelitefeather.titan.common.portal;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.deliver.DeliverType;
import net.onelitefeather.titan.common.utils.TitanFeatures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalConfigProviderTest {

    @Test
    @DisplayName("a hand-written file is the whole interface: no code change adds a portal")
    void readsHandWrittenFile(@TempDir Path directory) throws IOException {
        Files.writeString(directory.resolve(PortalConfig.PORTAL_FILE_NAME), """
                {
                  "portals": [
                    {
                      "id": "survival",
                      "type": "task",
                      "target": "Survival",
                      "feature": "NAVIGATOR_SURVIVAL",
                      "min": { "x": 10.0, "y": 64.0, "z": 10.0 },
                      "max": { "x": 14.0, "y": 68.0, "z": 14.0 }
                    },
                    {
                      "id": "creative",
                      "target": "MemberBuild",
                      "min": { "x": 20.0, "y": 64.0, "z": 20.0 },
                      "max": { "x": 24.0, "y": 68.0, "z": 24.0 }
                    }
                  ],
                  "retriggerCooldownMillis": 1500,
                  "unreachableMessage": "<red>no <target>",
                  "deniedMessage": "<red>not for you"
                }""");

        PortalConfig config = PortalConfigProvider.create(directory).getPortalConfig();

        assertEquals(2, config.portals().size());
        assertEquals(1500L, config.retriggerCooldownMillis());
        assertEquals("<red>no <target>", config.unreachableMessage());
        assertEquals("<red>not for you", config.deniedMessage());

        Portal survival = config.portals().get(0).resolve().orElseThrow();
        assertEquals("survival", survival.id());
        assertEquals(DeliverType.TASK, survival.type());
        assertEquals("Survival", survival.target());
        assertEquals(TitanFeatures.NAVIGATOR_SURVIVAL, survival.feature());
        assertTrue(survival.contains(new Pos(12.5, 66, 12.5)));

        Portal creative = config.portals().get(1).resolve().orElseThrow();
        assertEquals(DeliverType.TASK, creative.type(), "an omitted type means task");
        assertNull(creative.feature(), "an omitted feature means ungated");
    }

    @Test
    @DisplayName("a missing file leaves a default behind, so the format is discoverable")
    void writesDefaultWhenMissing(@TempDir Path directory) {
        PortalConfig config = PortalConfigProvider.create(directory).getPortalConfig();

        assertTrue(config.portals().isEmpty());
        assertTrue(Files.exists(directory.resolve(PortalConfig.PORTAL_FILE_NAME)));
        assertEquals(PortalConfig.defaultConfig().retriggerCooldownMillis(), config.retriggerCooldownMillis());
    }

    @Test
    @DisplayName("what the provider writes, the provider reads back")
    void roundTripsAPortal(@TempDir Path directory) {
        PortalConfigProvider provider = PortalConfigProvider.create(directory);
        PortalDefinition definition = new PortalDefinition("elytra", "server", "Elytra-1", "NAVIGATOR_ELYTRA", new Vec(1, 64, 1), new Vec(5, 68, 5));

        provider.saveConfig(PortalConfig.of(List.of(definition), 2000L, "<red>gone", "<red>nope"));

        PortalConfig reloaded = PortalConfigProvider.create(directory).getPortalConfig();
        assertEquals(1, reloaded.portals().size());
        assertEquals(2000L, reloaded.retriggerCooldownMillis());
        Portal portal = reloaded.portals().get(0).resolve().orElseThrow();
        assertEquals(DeliverType.SERVER, portal.type());
        assertEquals("Elytra-1", portal.target());
        assertEquals(TitanFeatures.NAVIGATOR_ELYTRA, portal.feature());
        assertTrue(portal.contains(new Pos(3.5, 66, 3.5)));
    }

    @Test
    @DisplayName("missing keys are filled in rather than rejected")
    void repairsPartialFile(@TempDir Path directory) throws IOException {
        Files.writeString(directory.resolve(PortalConfig.PORTAL_FILE_NAME), """
                { "portals": [] }""");

        PortalConfig config = PortalConfigProvider.create(directory).getPortalConfig();

        assertTrue(config.portals().isEmpty());
        assertEquals(0L, config.retriggerCooldownMillis(), "an omitted cooldown means no debounce, not a broken file");
        assertEquals(PortalConfig.defaultConfig().unreachableMessage(), config.unreachableMessage());
        assertEquals(PortalConfig.defaultConfig().deniedMessage(), config.deniedMessage());
    }
}
