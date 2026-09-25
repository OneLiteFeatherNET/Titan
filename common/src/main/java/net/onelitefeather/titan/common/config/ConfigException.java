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
 * Signals that a value a module's own validation function read through the
 * {@code io.avaje.config.Config} facade failed that validation ({@link #invalid(String, String)}).
 * <p>
 * A syntactically broken {@code application.yaml} is not this exception's concern any more: the
 * facade's own static initializer fails first touch with {@link ExceptionInInitializerError},
 * whose cause chain already names the file and the line/column (built-in first - see
 * {@code openspec/changes/avaje-config-facade/design.md}, decision 1).
 * <p>
 * A module's validation function knows only the full, dotted key it read (e.g.
 * {@code tickle.cooldownMillis}) and the reason a value is rejected - it is thrown as the result
 * of {@link #invalid(String, String)}, with that key passed as {@code field} and no section, since
 * the key already names the module (see
 * {@code openspec/changes/avaje-config-facade/design.md}, decision 3). A caller with a section id
 * of its own to attach - {@code NavigatorEntries}, naming which module contributed a navigator
 * entry - completes it via {@link #withSection(String)} before rethrowing it.
 * <p>
 * The resulting message has the shape {@code <section>.<field> - <reason>}, for example
 * {@code navigator.entries - entry 'x' uses unknown feature flag 'y'}.
 */
public final class ConfigException extends RuntimeException {

    private final @Nullable String section;
    private final @Nullable String field;
    private final @Nullable String reason;

    private ConfigException(@Nullable String section, @Nullable String field, @Nullable String reason, @Nullable Throwable cause) {
        super(buildMessage(section, field, reason), cause);
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
     * @return a new {@link ConfigException} without a section
     */
    public static ConfigException invalid(String field, String reason) {
        return new ConfigException(null, field, reason, null);
    }

    /**
     * Returns a copy of this exception with the section id set, keeping this instance as the
     * cause.
     *
     * @param section the id of the section that produced the failing value
     * @return a new {@link ConfigException} carrying the section
     */
    public ConfigException withSection(String section) {
        return new ConfigException(section, this.field, this.reason, this);
    }

    /**
     * The id of the section the failure was found in, or {@code null} for a
     * {@link #invalid(String, String)} exception without a {@link #withSection(String)}.
     */
    public @Nullable String section() {
        return section;
    }

    /**
     * The name of the offending record component.
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

    private static String buildMessage(@Nullable String section, @Nullable String field, @Nullable String reason) {
        StringBuilder message = new StringBuilder();
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
