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
package net.onelitefeather.titan.common.resourcepack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FloodgateBedrockDetectorTest {

    private final BedrockDetector detector = BedrockDetector.floodgate(ResourcePackSettings.DEFAULT_BEDROCK_NAME_PREFIX);

    @Test
    @DisplayName("A floodgate id has an all zero upper half")
    void testFloodgateId() {
        UUID floodgateId = new UUID(0L, 2535423272292734L);

        assertTrue(this.detector.isBedrock(floodgateId, "Steve"));
    }

    @Test
    @DisplayName("An online mode id is not bedrock")
    void testOnlineModeId() {
        UUID online = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");

        assertFalse(this.detector.isBedrock(online, "Notch"));
    }

    @Test
    @DisplayName("An offline mode id is not bedrock either, it keeps its version nibble")
    void testOfflineModeId() {
        UUID offline = UUID.nameUUIDFromBytes("OfflinePlayer:Steve".getBytes(StandardCharsets.UTF_8));

        assertFalse(this.detector.isBedrock(offline, "Steve"));
    }

    @Test
    @DisplayName("The floodgate name prefix is recognised when the proxy already resolved the id")
    void testNamePrefix() {
        assertTrue(this.detector.isBedrock(UUID.randomUUID(), ".BedrockPlayer"));
        assertFalse(this.detector.isBedrock(UUID.randomUUID(), "JavaPlayer"));
    }

    @Test
    @DisplayName("An empty prefix leaves only the id check")
    void testEmptyPrefix() {
        BedrockDetector idOnly = BedrockDetector.floodgate("");

        assertFalse(idOnly.isBedrock(UUID.randomUUID(), ".BedrockPlayer"));
        assertTrue(idOnly.isBedrock(new UUID(0L, 42L), ".BedrockPlayer"));
    }

    @Test
    @DisplayName("The never detector treats everyone as java")
    void testNeverDetector() {
        assertFalse(BedrockDetector.never().isBedrock(new UUID(0L, 42L), ".BedrockPlayer"));
    }
}
