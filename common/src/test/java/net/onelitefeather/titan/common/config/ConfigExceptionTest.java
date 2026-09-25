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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Covers {@link ConfigException}'s message shape ({@code <file>: <section>.<field> - <reason>})
 * and the immutable, cause-chaining behaviour of {@link ConfigException#withSection(String)}.
 */
class ConfigExceptionTest {

    @Test
    @DisplayName("A section and field together have the message shape <section>.<field> - <reason>")
    void sectionAndFieldMessageShape() {
        ConfigException exception = ConfigException.invalid("cooldownMillis", "must not be negative").withSection("tickle");

        assertEquals("tickle.cooldownMillis - must not be negative", exception.getMessage());
        assertNull(exception.file(), "invalid()/withSection() never set a file - only malformed() does");
        assertEquals("tickle", exception.section());
        assertEquals("cooldownMillis", exception.field());
        assertEquals("must not be negative", exception.reason());
    }

    @Test
    @DisplayName("invalid() alone carries no file or section, in either the accessors or the message")
    void invalidAloneHasNoFileOrSection() {
        ConfigException exception = ConfigException.invalid("cooldownMillis", "must not be negative");

        assertNull(exception.file());
        assertNull(exception.section());
        assertEquals("cooldownMillis - must not be negative", exception.getMessage());
    }

    @Test
    @DisplayName("malformed() carries a file and a reason but no section or field")
    void malformedHasNoSectionOrField() {
        ConfigException exception = ConfigException.malformed("app.json", "unexpected character at line 3 column 5");

        assertEquals("app.json", exception.file());
        assertNull(exception.section());
        assertNull(exception.field());
        assertEquals("app.json: unexpected character at line 3 column 5", exception.getMessage());
    }

    @Test
    @DisplayName("malformed() with a cause keeps it, so the original failure's stack trace is not lost")
    void malformedWithCauseKeepsIt() {
        RuntimeException original = new RuntimeException("mapping values are not allowed here");

        ConfigException exception = ConfigException.malformed("application.yaml", "mapping values are not allowed here", original);

        assertSame(original, exception.getCause(), "the original failure must be reachable as the cause, for the ERROR log/Sentry");
    }

    @Test
    @DisplayName("malformed() without a cause has none, exactly like the two-argument overload")
    void malformedWithoutCauseHasNoCause() {
        ConfigException exception = ConfigException.malformed("application.yaml", "mapping values are not allowed here", null);

        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("withSection() returns a new instance, keeping the previous one as the cause")
    void withSectionIsImmutableAndChainsCauses() {
        ConfigException original = ConfigException.invalid("cooldownMillis", "must not be negative");

        ConfigException withSection = original.withSection("tickle");

        assertNotSame(original, withSection, "withSection() must not mutate the original instance");
        assertNull(original.section(), "the original instance must stay unchanged");
        assertNull(original.file());
        assertEquals("tickle", withSection.section());
        assertNull(withSection.file(), "withSection() must not set a file");
        assertSame(original, withSection.getCause(), "withSection() keeps the previous instance as its cause");
    }
}
