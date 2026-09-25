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
 * and the immutable, cause-chaining behaviour of {@link ConfigException#withSection(String)} and
 * {@link ConfigException#withFile(String)}.
 */
class ConfigExceptionTest {

    @Test
    @DisplayName("A fully qualified exception has the message shape <file>: <section>.<field> - <reason>")
    void fullMessageShape() {
        ConfigException exception = ConfigException.invalid("cooldownMillis", "must not be negative").withSection("tickle").withFile("app.json");

        assertEquals("app.json: tickle.cooldownMillis - must not be negative", exception.getMessage());
        assertEquals("app.json", exception.file());
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
    @DisplayName("withSection() and withFile() each return a new instance, keeping the previous one as the cause")
    void withSectionAndWithFileAreImmutableAndChainCauses() {
        ConfigException original = ConfigException.invalid("cooldownMillis", "must not be negative");

        ConfigException withSection = original.withSection("tickle");
        ConfigException withBoth = withSection.withFile("app.json");

        assertNotSame(original, withSection, "withSection() must not mutate the original instance");
        assertNotSame(withSection, withBoth, "withFile() must not mutate the instance it was called on");
        assertNull(original.section(), "the original instance must stay unchanged");
        assertNull(original.file());
        assertEquals("tickle", withSection.section());
        assertNull(withSection.file(), "withSection() alone must not set a file");
        assertEquals("tickle", withBoth.section(), "withFile() must keep the section set by withSection()");
        assertEquals("app.json", withBoth.file());
        assertSame(original, withSection.getCause(), "withSection() keeps the previous instance as its cause");
        assertSame(withSection, withBoth.getCause(), "withFile() keeps the previous instance as its cause");
    }
}
