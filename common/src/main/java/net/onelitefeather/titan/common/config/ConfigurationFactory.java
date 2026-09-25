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

import io.avaje.config.Config;
import io.avaje.config.Configuration;
import org.jetbrains.annotations.Nullable;

/**
 * Triggers the one-time build of the {@code avaje-config} static {@link Config} facade's
 * {@link Configuration} - {@code application.yaml}, its active profiles, an external file,
 * environment variables and system properties, in that rank order (see
 * {@code openspec/changes/avaje-config-facade/specs/lobby-module-config/spec.md}, "Overrides have
 * a fixed rank order") - and translates a broken load into the same {@link ConfigException} shape
 * both Titan processes already use.
 *
 * <p>{@code Config} builds its {@link Configuration} lazily, in its own static initializer, the
 * first time anything touches the class (see design.md, decision 1's fact check of
 * {@code avaje-config:5.2}). A load failure therefore does not surface as this class's own
 * exception but as {@link ExceptionInInitializerError}, and every further touch of {@code Config}
 * in the same JVM then fails again with {@link NoClassDefFoundError}. {@link #initialise()} is
 * meant to be the very first thing either process does, at a single, known place, so that first
 * touch - and any translation a broken file needs - happens here, before anything else in the
 * start sequence can hit the raw error instead.
 *
 * <p>The lobby (module {@code app}) and the setup server (module {@code setup}) both call
 * {@link #initialise()} through this one factory, rather than duplicating the
 * {@link ExceptionInInitializerError}-unwrapping logic in each process, so a broken {@code
 * application.yaml} is translated into the same {@link ConfigException} shape for both, and the
 * message-parsing logic in {@link #fileNameFrom(RuntimeException)}/
 * {@link #detailFrom(RuntimeException)} is exercised and tested exactly once (DRY).
 *
 * <p>An instance, not a static method: depending on this factory type - rather than calling
 * {@code Config} directly - keeps a caller depending on an abstraction instead of global state,
 * even though {@link #initialise()} itself ultimately has to touch the facade once (see design.md,
 * decision 1's SOLID note: this is a deliberate, bounded exception to the project's "no static
 * singletons" rule).
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
     * Triggers the static {@link Config} facade's one-time initialization - the first touch of
     * {@link Config} in the JVM, wherever this method is called from - so that {@code
     * application.yaml}, its active profiles, an external file, environment variables and system
     * properties are resolved against the JVM's actual process working directory (see the class
     * Javadoc). Every subsequent call, in this JVM, is a no-op: the class is already initialized.
     *
     * @throws ConfigException if a resolved file (e.g. a syntactically broken {@code
     *                         application.yaml}) cannot be parsed, or an unsupported {@code
     *                         CONFIG_FILE}/{@code config.file} extension is configured; see the
     *                         {@code lobby-module-config} spec scenario "Syntaktisch kaputte
     *                         Datei". Wraps the original failure, keeping it as this exception's
     *                         cause so the ERROR log/Sentry still shows where the failure actually
     *                         happened.
     */
    public void initialise() {
        try {
            Config.asConfiguration();
        } catch (ExceptionInInitializerError error) {
            throw translate(error);
        }
    }

    /**
     * Unwraps an {@link ExceptionInInitializerError} thrown by {@link Config}'s static initializer
     * and translates its cause into a {@link ConfigException}, via the same
     * {@link #fileNameFrom(RuntimeException)}/{@link #detailFrom(RuntimeException)} logic
     * {@link #initialise()} always used.
     *
     * <p>Package-private, rather than {@code private}, so {@code ConfigurationFactoryTest} can
     * exercise this translation directly against a hand-built {@link ExceptionInInitializerError},
     * without ever touching the real {@link Config} facade (design.md, decision 5: no unit test
     * calls the facade).
     */
    static ConfigException translate(ExceptionInInitializerError error) {
        if (error.getCause() instanceof RuntimeException cause) {
            return ConfigException.malformed(fileNameFrom(cause), detailFrom(cause), cause);
        }
        return ConfigException.malformed(null, error.getMessage(), error);
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
