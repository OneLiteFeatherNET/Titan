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
package net.onelitefeather.titan.apps.cloudnet.bootstrap;

import io.avaje.config.Configuration;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

/**
 * Checks that the merged shipped {@code application.yaml} and an operator file reach avaje-config
 * intact, using the same YAML parser production runs with.
 *
 * <p>The shipped file is read raw with SnakeYAML and compared leaf by leaf with what a separate
 * {@link Configuration} instance returns, so a value that avaje-config drops or mistypes shows up
 * as a named key. Never the static {@code Config} facade: it is shared JVM state.
 */
class ApplicationYamlParityTest {

    private static final String SHIPPED = "application.yaml";

    @DisplayName("every key of the shipped application.yaml is readable through avaje-config with the value written in the file")
    @Test
    void everyShippedKeyIsReadWithItsValue() throws IOException {
        Map<String, Object> leaves = new LinkedHashMap<>();
        flatten("", readShipped(), leaves);
        Configuration configuration = Configuration.builder().load(SHIPPED).build();

        List<String> mismatches = new ArrayList<>();
        leaves.forEach((key, expected) -> mismatch(configuration, key, expected).ifPresent(mismatches::add));

        Assertions.assertTrue(mismatches.isEmpty(), "avaje-config drops or changes these shipped values:\n" + String.join("\n", mismatches));
    }

    @DisplayName("an operator file overrides a number, a list and a nested value with their types, and keeps every other default")
    @Test
    void operatorFileOverridesWithTypedValuesAndKeepsOtherDefaults(@TempDir Path workingDir) throws IOException {
        Path operatorFile = workingDir.resolve("application.yaml");
        Files.writeString(operatorFile, """
                spawn:
                  simulationDistance: 3
                sit:
                  allowedBlocks:
                    - minecraft:oak_stairs
                jumprun:
                  rainbow:
                    rerollTicks: 12
                """);

        Configuration configuration = Configuration.builder().load(SHIPPED).load(operatorFile.toFile()).build();

        Assertions.assertEquals(3, configuration.getInt("spawn.simulationDistance"), "the operator's number must arrive as an int");
        Assertions.assertEquals(List.of("minecraft:oak_stairs"), configuration.list().of("sit.allowedBlocks"), "the operator's list must arrive as the list it sets");
        Assertions.assertEquals(12, configuration.getInt("jumprun.rainbow.rerollTicks"), "the operator's nested value must arrive at its full path");
        Assertions.assertEquals(-64, configuration.getInt("spawn.minHeight"), "a key the operator file does not set keeps its shipped default");
    }

    private static Map<String, Object> readShipped() throws IOException {
        try (InputStream stream = ApplicationYamlParityTest.class.getClassLoader().getResourceAsStream(SHIPPED)) {
            Assertions.assertNotNull(stream, "the merged " + SHIPPED + " must be on the test classpath");
            return new Yaml().load(stream);
        }
    }

    private static void flatten(String prefix, Object node, Map<String, Object> leaves) {
        if (node instanceof Map<?, ?> map) {
            map.forEach((key, value) -> flatten(prefix.isEmpty() ? key.toString() : prefix + "." + key, value, leaves));
        } else {
            leaves.put(prefix, node);
        }
    }

    private static Optional<String> mismatch(Configuration configuration, String key, Object expected) {
        if (expected == null) {
            // A YAML null carries no value, so there is nothing a parser could drop.
            return Optional.empty();
        }
        if (expected instanceof List<?> items) {
            List<String> actual = configuration.list().of(key);
            List<String> wanted = items.stream().map(String::valueOf).toList();
            // avaje-config reads an empty YAML list as one blank entry; both mean "no entries".
            boolean emptyAsBlank = wanted.isEmpty() && actual.equals(List.of(""));
            return actual.equals(wanted) || emptyAsBlank ? Optional.empty() : Optional.of(key + ": expected " + wanted + " but was " + actual);
        }
        String actual = configuration.getOptional(key).orElse(null);
        return String.valueOf(expected).equals(actual) ? Optional.empty() : Optional.of(key + ": expected " + expected + " but was " + actual);
    }
}
