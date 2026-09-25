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
 * Signals a problem with a {@code SectionBinder}-bound configuration section: either a
 * syntactically broken file, or a value that failed the validation performed by a config
 * record's compact constructor.
 * <p>
 * Config records only know their own field and the reason a value is rejected; they do not
 * know which section of the document they were loaded from. A record's compact constructor is
 * therefore expected to throw the result of {@link #invalid(String, String)}, which carries the
 * field and reason only. {@link SectionBinder} catches that exception (Gson wraps constructor
 * failures, so the binder unwraps the cause chain first) and completes it with the section id and
 * file name via {@link #withSection(String)} and {@link #withFile(String)} before rethrowing it.
 * <p>
 * The resulting message has the shape {@code <file>: <section>.<field> - <reason>}, for example
 * {@code app.json: tickle.cooldownMillis - must not be negative}.
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
     * Creates an exception for a value that a config record's compact constructor rejected.
     * The section is not known at this point; {@link SectionBinder} fills it in via
     * {@link #withSection(String)} once it knows which section produced the failing record.
     *
     * @param field  the name of the offending record component
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
        return new ConfigException(file, null, null, detail, null);
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
     * Returns a copy of this exception with the file name set, keeping this instance as the
     * cause.
     *
     * @param file the name of the file the offending document was read from, or {@code null} if
     *             it did not come from a single named file
     * @return a new {@link ConfigException} carrying the file name
     */
    public ConfigException withFile(@Nullable String file) {
        return new ConfigException(file, this.section, this.field, this.reason, this);
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
