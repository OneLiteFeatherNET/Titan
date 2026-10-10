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
package net.onelitefeather.titan.runtime.bootstrap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BootstrapSettingsTest {

    @TempDir
    Path workingDirectory;

    private Path secretFile() {
        return this.workingDirectory.resolve("forwarding.secret");
    }

    private void writeSecret(String content) throws IOException {
        Files.writeString(secretFile(), content);
    }

    @DisplayName("A non-blank forwarding.secret file is the Velocity secret")
    @Test
    void secretFileIsRead() throws IOException {
        writeSecret("from-file\n");

        Assertions.assertEquals(Optional.of("from-file"), BootstrapSettings.velocitySecret(secretFile(), Map.of()));
    }

    @DisplayName("The forwarding.secret file wins over the minestom.velocity.secret property")
    @Test
    void secretFileWinsOverProperty() throws IOException {
        writeSecret("from-file");

        Assertions.assertEquals(Optional.of("from-file"), BootstrapSettings.velocitySecret(secretFile(), Map.of("minestom.velocity.secret", "from-property")));
    }

    @DisplayName("A blank forwarding.secret file falls back to the property")
    @Test
    void blankSecretFileFallsBackToProperty() throws IOException {
        writeSecret("   \n");

        Assertions.assertEquals(Optional.of("from-property"), BootstrapSettings.velocitySecret(secretFile(), Map.of("minestom.velocity.secret", "from-property")));
    }

    @DisplayName("A missing forwarding.secret file uses the property")
    @Test
    void missingSecretFileUsesProperty() {
        Assertions.assertEquals(Optional.of("from-property"), BootstrapSettings.velocitySecret(secretFile(), Map.of("minestom.velocity.secret", "from-property")));
    }

    @DisplayName("Without a file or a property there is no Velocity secret")
    @Test
    void noSecretMeansNoForwarding() {
        Assertions.assertEquals(Optional.empty(), BootstrapSettings.velocitySecret(secretFile(), Map.of()));
    }

    @DisplayName("The bind host defaults to localhost")
    @Test
    void bindHostDefaultsToLocalhost() {
        Assertions.assertEquals("localhost", BootstrapSettings.bindHost(Map.of()));
    }

    @DisplayName("The bind host comes from service.bind.host")
    @Test
    void bindHostComesFromProperty() {
        Assertions.assertEquals("0.0.0.0", BootstrapSettings.bindHost(Map.of("service.bind.host", "0.0.0.0")));
    }

    @DisplayName("The bind port defaults to 25565")
    @Test
    void bindPortDefaultsTo25565() {
        Assertions.assertEquals(25565, BootstrapSettings.bindPort(Map.of()));
    }

    @DisplayName("The bind port comes from service.bind.port")
    @Test
    void bindPortComesFromProperty() {
        Assertions.assertEquals(40001, BootstrapSettings.bindPort(Map.of("service.bind.port", "40001")));
    }

    @DisplayName("A non-numeric bind port falls back to 25565, as Integer.getInteger does")
    @Test
    void nonNumericBindPortFallsBackToDefault() {
        Assertions.assertEquals(25565, BootstrapSettings.bindPort(Map.of("service.bind.port", "lobby")));
    }
}
