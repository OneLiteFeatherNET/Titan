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

import io.avaje.config.Configuration;
import net.onelitefeather.titan.common.config.ConfigException;
import org.jetbrains.annotations.Nullable;

/**
 * Builds the lobby's {@code avaje-config} {@link Configuration} - {@code application.yaml}, its
 * active profiles, an external file, environment variables and system properties, in that rank
 * order (see
 * {@code openspec/changes/standardized-config-profiles/specs/lobby-module-config/spec.md},
 * "Overrides have a fixed rank order"). This is the single place both {@link PlatformBeans} (in
 * production) and the configuration precedence test build a {@link Configuration} from, so a test
 * exercises exactly the code path production runs, never a re-implementation of it.
 *
 * <p>An instance, not a static method: depending on this factory type - rather than calling
 * {@code Configuration.builder()} directly, or the static {@code io.avaje.config.Config} facade -
 * is what lets a test substitute its own factory later if the need ever arises, and keeps
 * {@link PlatformBeans} depending on an abstraction instead of global state.
 *
 * <p>{@link #load()} takes no working directory: {@code avaje-config} always resolves {@code
 * application.yaml} and friends against the JVM's actual process working directory, never against
 * a path a caller hands it (see {@code design.md}, decision 1's spike result), so a parameter here
 * could only ever be ignored or misleadingly suggest otherwise. A caller that needs {@code
 * application.yaml} read from a particular directory must therefore start the whole JVM in that
 * directory (e.g. via {@link ProcessBuilder#directory(java.io.File)}, the way the configuration
 * precedence test does), not pass it to this method.
 */
public final class ConfigurationFactory {

    /**
     * The prefix {@code avaje-config}'s own {@code InitialLoader#loadCustomExtension} puts in
     * front of the resource name when a resolved file (e.g. a syntactically broken {@code
     * application.yaml}) fails to load: {@code new IllegalStateException("Error loading properties
     * - " + resourceName, cause)} (confirmed by decompiling {@code avaje-config:5.2}).
     */
    private static final String RESOURCE_LOAD_FAILURE_PREFIX = "Error loading properties - ";

    /**
     * @return a {@link Configuration} built from {@code application.yaml}, its active profiles, an
     *         external file (via {@code CONFIG_FILE}/{@code config.file}), environment variables
     *         and system properties - all resolved against the JVM's actual process working
     *         directory (see the class Javadoc)
     * @throws ConfigException if a resolved file (e.g. a syntactically broken {@code
     *                         application.yaml}) cannot be parsed, or an unsupported {@code
     *                         CONFIG_FILE}/{@code config.file} extension is configured; see the
     *                         {@code lobby-module-config} spec scenario "Syntaktisch kaputte
     *                         Datei". Wraps {@code avaje-config}'s own {@link RuntimeException},
     *                         keeping it as this exception's cause so the ERROR log/Sentry still
     *                         shows where the failure actually happened, into the same
     *                         {@link ConfigException} shape
     *                         {@link net.onelitefeather.titan.common.config.AppJsonMigration} uses
     *                         for a broken {@code app.json}, so both failure paths surface the same
     *                         way to an operator.
     */
    public Configuration load() {
        try {
            return Configuration.builder().includeResourceLoading().build();
        } catch (RuntimeException e) {
            throw ConfigException.malformed(fileNameFrom(e), detailFrom(e), e);
        }
    }

    /**
     * Extracts the resource name from an {@code avaje-config} loading failure shaped like
     * {@link #RESOURCE_LOAD_FAILURE_PREFIX}. {@code avaje-config} does not only throw that shape,
     * though: an unsupported {@code CONFIG_FILE}/{@code config.file} extension surfaces as {@code
     * InitialLoader#loadViaSystemProperty}'s own {@code IllegalArgumentException("Expecting only
     * properties or ... file extensions but got [" + file + "]")}, whose whole sentence is not a
     * file name at all. Returning that whole sentence here would make {@link #detailFrom} - which
     * falls back to the same message when there is no cause - repeat it a second time in the final
     * {@link ConfigException} message. So a file name is extracted only when the message actually
     * has the expected shape; any other message yields {@code null} here, leaving
     * {@link #detailFrom(RuntimeException)} to carry the whole sentence exactly once.
     *
     * <p>Package-private, rather than {@code private}, purely so {@code ConfigurationFactoryTest}
     * can exercise this pure parsing logic directly against a fabricated exception, without
     * needing a real broken file on disk (see that test's Javadoc for why).
     */
    static @Nullable String fileNameFrom(RuntimeException e) {
        String message = e.getMessage();
        if (message == null || !message.startsWith(RESOURCE_LOAD_FAILURE_PREFIX)) {
            return null;
        }
        return message.substring(RESOURCE_LOAD_FAILURE_PREFIX.length()).trim();
    }

    /**
     * For the {@link #RESOURCE_LOAD_FAILURE_PREFIX} message shape, prefers the cause's message -
     * for a broken {@code application.yaml} that is SnakeYAML's own message, which already names
     * the line and column - falling back to the outer exception's message if there is no cause.
     * For any other message shape (see {@link #fileNameFrom(RuntimeException)}), returns the outer
     * message as-is: that shape's whole sentence already is the detail, and {@link
     * #fileNameFrom(RuntimeException)} returns {@code null} for it, so returning it here too does
     * not duplicate anything in the final {@link ConfigException} message.
     *
     * <p>Package-private for the same testability reason as
     * {@link #fileNameFrom(RuntimeException)}.
     */
    static @Nullable String detailFrom(RuntimeException e) {
        String message = e.getMessage();
        if (message == null || !message.startsWith(RESOURCE_LOAD_FAILURE_PREFIX)) {
            return message;
        }
        Throwable cause = e.getCause();
        String detail = cause != null ? cause.getMessage() : null;
        return detail != null ? detail : message;
    }
}
