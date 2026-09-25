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
 * Covers {@link ConfigException}'s message shape ({@code <section>.<field> - <reason>}) and the
 * immutable, cause-chaining behaviour of {@link ConfigException#withSection(String)}.
 */
class ConfigExceptionTest {

    @Test
    @DisplayName("A section and field together have the message shape <section>.<field> - <reason>")
    void sectionAndFieldMessageShape() {
        ConfigException exception = ConfigException.invalid("cooldownMillis", "must not be negative").withSection("tickle");

        assertEquals("tickle.cooldownMillis - must not be negative", exception.getMessage());
        assertEquals("tickle", exception.section());
        assertEquals("cooldownMillis", exception.field());
        assertEquals("must not be negative", exception.reason());
    }

    @Test
    @DisplayName("invalid() alone carries no section, in either the accessor or the message")
    void invalidAloneHasNoSection() {
        ConfigException exception = ConfigException.invalid("cooldownMillis", "must not be negative");

        assertNull(exception.section());
        assertEquals("cooldownMillis - must not be negative", exception.getMessage());
    }

    @Test
    @DisplayName("withSection() returns a new instance, keeping the previous one as the cause")
    void withSectionIsImmutableAndChainsCauses() {
        ConfigException original = ConfigException.invalid("cooldownMillis", "must not be negative");

        ConfigException withSection = original.withSection("tickle");

        assertNotSame(original, withSection, "withSection() must not mutate the original instance");
        assertNull(original.section(), "the original instance must stay unchanged");
        assertEquals("tickle", withSection.section());
        assertSame(original, withSection.getCause(), "withSection() keeps the previous instance as its cause");
    }
}
