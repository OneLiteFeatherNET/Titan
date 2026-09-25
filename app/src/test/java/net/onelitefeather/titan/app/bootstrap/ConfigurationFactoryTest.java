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

import net.onelitefeather.titan.common.config.ConfigException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link ConfigurationFactory#fileNameFrom(RuntimeException)} and {@link
 * ConfigurationFactory#detailFrom(RuntimeException)} - the pure logic {@link
 * ConfigurationFactory#load()} uses to translate an {@code avaje-config} loading failure into a
 * {@link ConfigException} naming the broken file and the parser's position (see the {@code
 * lobby-module-config} spec scenario "Syntaktisch kaputte Datei") - plus coverage that {@link
 * ConfigurationFactory#load()}'s own translation keeps the original failure as the resulting
 * {@link ConfigException}'s cause.
 *
 * <p>{@link ConfigurationFactory#load()} itself is not called here: exercising the real failure
 * needs a genuinely broken {@code application.yaml} in the JVM's actual working directory
 * (design.md decision 1's spike result - {@code avaje-config} ignores {@code user.dir}), which
 * only a child JVM with a {@code @TempDir} working directory can provide without breaking
 * Independent/Repeatable (F.I.R.S.T.) - see
 * {@link ConfigurationPrecedenceTest#brokenApplicationYamlAbortsCleanlyNamingFileAndPosition} for
 * that coverage. What is tested here, hermetically, is the message-parsing logic itself against
 * exceptions shaped exactly like the ones {@code avaje-config} itself throws (confirmed by
 * decompiling {@code avaje-config:5.2}): {@code new IllegalStateException("Error loading
 * properties - " + resourceName, cause)} for a resource that fails to load, and {@code new
 * IllegalArgumentException("Expecting only properties or ... file extensions but got [" + file +
 * "]")} for an unsupported {@code CONFIG_FILE}/{@code config.file} extension.
 */
class ConfigurationFactoryTest {

    @DisplayName("The resource name is read from the tail of avaje-config's own message")
    @Test
    void fileNameIsReadFromTheMessageTail() {
        RuntimeException failure = brokenYamlFailure();

        String fileName = ConfigurationFactory.fileNameFrom(failure);

        Assertions.assertEquals("application.yaml", fileName, "the file name must be read from the message avaje-config itself produces");
    }

    @DisplayName("The detail prefers the cause's message, which carries the parser's line and column")
    @Test
    void detailPrefersTheCausesMessage() {
        RuntimeException failure = brokenYamlFailure();

        String detail = ConfigurationFactory.detailFrom(failure);

        Assertions.assertEquals("mapping values are not allowed here in 'reader', line 3, column 13", detail, "the parser's own message - which names the line and column - must not be lost behind the outer IllegalStateException");
    }

    @DisplayName("Without a cause, the detail falls back to the outer exception's own message")
    @Test
    void detailFallsBackToTheOuterMessageWithoutACause() {
        RuntimeException failure = new IllegalStateException("Error loading properties - application.yaml");

        String detail = ConfigurationFactory.detailFrom(failure);

        Assertions.assertEquals("Error loading properties - application.yaml", detail, "with no cause, the outer message is the only information available");
    }

    @DisplayName("Without a message, the file name cannot be read and is null rather than throwing")
    @Test
    void fileNameIsNullWhenTheExceptionHasNoMessage() {
        RuntimeException failure = new IllegalStateException((String) null);

        Assertions.assertNull(ConfigurationFactory.fileNameFrom(failure), "with no message at all, there is nothing to parse a file name out of");
    }

    @DisplayName("An unsupported CONFIG_FILE extension's message does not have a file name to extract")
    @Test
    void fileNameIsNullForAnUnsupportedExtensionMessage() {
        RuntimeException failure = unsupportedExtensionFailure();

        String fileName = ConfigurationFactory.fileNameFrom(failure);

        Assertions.assertNull(fileName, "this message shape is not \"Error loading properties - <file>\", so no file name must be extracted from it");
    }

    @DisplayName("An unsupported CONFIG_FILE extension's whole message becomes the detail exactly once")
    @Test
    void detailIsTheRawMessageOnceForAnUnsupportedExtensionMessage() {
        RuntimeException failure = unsupportedExtensionFailure();

        String detail = ConfigurationFactory.detailFrom(failure);

        Assertions.assertEquals("Expecting only properties or yaml or json file extensions but got [garbage.txt]", detail, "the whole sentence is the only information available and must appear exactly once");
    }

    @DisplayName("Combined, an unsupported extension's translated exception carries the message once, with no file")
    @Test
    void unsupportedExtensionFailureIsNotDuplicatedInTheFinalMessage() {
        RuntimeException failure = unsupportedExtensionFailure();

        ConfigException translated = ConfigException.malformed(ConfigurationFactory.fileNameFrom(failure), ConfigurationFactory.detailFrom(failure), failure);

        Assertions.assertEquals("Expecting only properties or yaml or json file extensions but got [garbage.txt]", translated.getMessage(), "the raw message must appear exactly once, never duplicated as both the file name and the detail");
        Assertions.assertNull(translated.file(), "this failure names no single file, so file() must stay null");
    }

    @DisplayName("The translated ConfigException keeps the original avaje-config failure as its cause")
    @Test
    void translatedExceptionKeepsTheOriginalFailureAsItsCause() {
        RuntimeException failure = brokenYamlFailure();

        ConfigException translated = ConfigException.malformed(ConfigurationFactory.fileNameFrom(failure), ConfigurationFactory.detailFrom(failure), failure);

        Assertions.assertSame(failure, translated.getCause(), "the original avaje-config failure must stay reachable as the cause, for the ERROR log/Sentry");
    }

    private static RuntimeException brokenYamlFailure() {
        return new IllegalStateException("Error loading properties - application.yaml", new RuntimeException("mapping values are not allowed here in 'reader', line 3, column 13"));
    }

    private static RuntimeException unsupportedExtensionFailure() {
        return new IllegalArgumentException("Expecting only properties or yaml or json file extensions but got [garbage.txt]");
    }
}
