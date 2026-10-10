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

/**
 * The start-up settings read from the working directory and the system properties. Both sources
 * are passed in, so the rules are testable without the real file system or properties.
 */
public final class BootstrapSettings {

    public static final String SECRET_PROPERTY = "minestom.velocity.secret";
    public static final String HOST_PROPERTY = "service.bind.host";
    public static final String PORT_PROPERTY = "service.bind.port";
    static final String DEFAULT_HOST = "localhost";
    static final int DEFAULT_PORT = 25565;

    private BootstrapSettings() {
    }

    /**
     * The Velocity modern-forwarding secret: a non-blank {@code forwarding.secret} file wins, then
     * the {@code minestom.velocity.secret} property.
     */
    public static Optional<String> velocitySecret(Path secretFile, Map<String, String> properties) {
        return readSecretFile(secretFile).or(() -> Optional.ofNullable(properties.get(SECRET_PROPERTY)).filter(secret -> !secret.isBlank()));
    }

    public static String bindHost(Map<String, String> properties) {
        return properties.getOrDefault(HOST_PROPERTY, DEFAULT_HOST);
    }

    /** A missing or non-numeric value yields the default, as {@link Integer#getInteger} does. */
    public static int bindPort(Map<String, String> properties) {
        String value = properties.get(PORT_PROPERTY);
        if (value == null) {
            return DEFAULT_PORT;
        }
        try {
            return Integer.decode(value);
        } catch (NumberFormatException notANumber) {
            return DEFAULT_PORT;
        }
    }

    private static Optional<String> readSecretFile(Path secretFile) {
        if (!Files.isRegularFile(secretFile)) {
            return Optional.empty();
        }
        try {
            String secret = Files.readString(secretFile).trim();
            return secret.isBlank() ? Optional.empty() : Optional.of(secret);
        } catch (IOException unreadable) {
            return Optional.empty();
        }
    }
}
