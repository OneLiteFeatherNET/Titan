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

import org.jetbrains.annotations.Nullable;

/**
 * Signals a problem with configuration read through the {@code io.avaje.config.Config} facade:
 * either a syntactically broken file ({@link #malformed(String, String)}), or a value that failed
 * a module's own validation ({@link #invalid(String, String)}).
 * <p>
 * A module's validation function knows only the full, dotted key it read (e.g.
 * {@code tickle.cooldownMillis}) and the reason a value is rejected - it is thrown as the result
 * of {@link #invalid(String, String)}, with that key passed as {@code field} and no section, since
 * the key already names the module (see
 * {@code openspec/changes/avaje-config-facade/design.md}, decision 3). A caller with a section id
 * of its own to attach - {@code NavigatorEntries}, naming which module contributed a navigator
 * entry - completes it via {@link #withSection(String)} before rethrowing it.
 * <p>
 * The resulting message has the shape {@code <file>: <section>.<field> - <reason>}, for example
 * {@code application.yaml: navigator.entries - entry 'x' uses unknown feature flag 'y'}.
 */
public final class ConfigException extends RuntimeException {

    private final @Nullable String file;
    private final @Nullable String section;
    private final @Nullable String field;
    private final @Nullable String reason;

    private ConfigException(@Nullable String file, @Nullable String section, @Nullable String field, @Nullable String reason, @Nullable Throwable cause) {
        super(buildMessage(file, section, field, reason), cause);
        this.file = file;
        this.section = section;
        this.field = field;
        this.reason = reason;
    }

    /**
     * Creates an exception for a value a module's own validation function rejected. {@code field}
     * is usually the full, dotted configuration key (e.g. {@code tickle.cooldownMillis}), which
     * already names the module, so no section is attached here. A caller with a section id of its
     * own to attach can still complete it via {@link #withSection(String)}.
     *
     * @param field  the name (or full key) of the offending value
     * @param reason a human-readable explanation, e.g. {@code "must not be negative"}
     * @return a new {@link ConfigException} without a section or file
     */
    public static ConfigException invalid(String field, String reason) {
        return new ConfigException(null, null, field, reason, null);
    }

    /**
     * Creates an exception describing a document that could not be parsed as JSON at all.
     *
     * @param file   the file name the broken document was read from, or {@code null} if the
     *               section did not come from a single named file
     * @param detail the underlying parser message; Gson's messages already include the line and
     *               column of the syntax error
     * @return a new {@link ConfigException} describing the broken document
     */
    public static ConfigException malformed(@Nullable String file, String detail) {
        return malformed(file, detail, null);
    }

    /**
     * Creates an exception describing a document that could not be parsed at all, keeping the
     * original failure as this exception's cause so the stack trace that reaches an ERROR log (or
     * Sentry) still shows where the parser actually failed, not just this rethrow.
     *
     * @param file   the file name the broken document was read from, or {@code null} if the
     *               section did not come from a single named file
     * @param detail the underlying parser message; a parser's own message often already includes
     *               the line and column of the syntax error
     * @param cause  the original failure this exception replaces, or {@code null} if there is none
     * @return a new {@link ConfigException} describing the broken document
     */
    public static ConfigException malformed(@Nullable String file, String detail, @Nullable Throwable cause) {
        return new ConfigException(file, null, null, detail, cause);
    }

    /**
     * Returns a copy of this exception with the section id set, keeping this instance as the
     * cause.
     *
     * @param section the id of the section that produced the failing value
     * @return a new {@link ConfigException} carrying the section
     */
    public ConfigException withSection(String section) {
        return new ConfigException(this.file, section, this.field, this.reason, this);
    }

    /**
     * The name of the configuration file the failure was found in, or {@code null} if not yet
     * known.
     */
    public @Nullable String file() {
        return file;
    }

    /**
     * The id of the section the failure was found in, or {@code null} for a document-level
     * failure such as broken JSON.
     */
    public @Nullable String section() {
        return section;
    }

    /**
     * The name of the offending record component, or {@code null} for a document-level failure.
     */
    public @Nullable String field() {
        return field;
    }

    /**
     * A human-readable explanation of the failure.
     */
    public @Nullable String reason() {
        return reason;
    }

    private static String buildMessage(@Nullable String file, @Nullable String section, @Nullable String field, @Nullable String reason) {
        StringBuilder message = new StringBuilder();
        if (file != null) {
            message.append(file).append(": ");
        }
        boolean hasLocation = section != null || field != null;
        if (section != null) {
            message.append(section);
            if (field != null) {
                message.append('.').append(field);
            }
        } else if (field != null) {
            message.append(field);
        }
        if (reason != null) {
            if (hasLocation) {
                message.append(" - ");
            }
            message.append(reason);
        }
        return message.toString();
    }
}
