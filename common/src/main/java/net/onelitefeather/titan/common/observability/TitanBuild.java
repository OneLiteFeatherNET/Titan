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
package net.onelitefeather.titan.common.observability;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * The version and short commit of the running build, read from the manifest of the jar it was
 * started from. Every lookup failure falls back to the defaults: a broken lookup must never stop
 * the server from starting.
 */
public record TitanBuild(String version, String commit) {

    private static final String DEV_VERSION = "dev";
    private static final String UNKNOWN_COMMIT = "unknown";
    private static final Attributes.Name GIT_COMMIT = new Attributes.Name("Git-Commit");

    public TitanBuild {
        version = orDefault(version, DEV_VERSION);
        commit = orDefault(commit, UNKNOWN_COMMIT);
    }

    /**
     * Reads the build of the jar {@code type} was loaded from; without a jar (IDE, tests) the
     * fallbacks apply.
     */
    public static TitanBuild of(Class<?> type) {
        try {
            CodeSource source = type.getProtectionDomain().getCodeSource();
            if (source == null || source.getLocation() == null) {
                return fromManifest(new Attributes());
            }
            return fromLocation(Path.of(source.getLocation().toURI()));
        } catch (URISyntaxException | RuntimeException exception) {
            return fromManifest(new Attributes());
        }
    }

    /** Reads the manifest of {@code location}; a class directory has no manifest and falls back. */
    static TitanBuild fromLocation(Path location) {
        try (JarFile jar = new JarFile(location.toFile())) {
            Manifest manifest = jar.getManifest();
            return fromManifest(manifest == null ? new Attributes() : manifest.getMainAttributes());
        } catch (IOException | RuntimeException exception) {
            return fromManifest(new Attributes());
        }
    }

    static TitanBuild fromManifest(Attributes attributes) {
        return new TitanBuild(attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION), attributes.getValue(GIT_COMMIT));
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
