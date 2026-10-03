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
package net.onelitefeather.titan.feature.spawn;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SpawnMessagesBundleTest {

    private static Properties load(String file) throws IOException {
        try (InputStream in = SpawnMessagesBundleTest.class.getResourceAsStream("/titan/spawn/" + file)) {
            Assertions.assertNotNull(in, file + " must be on the classpath");
            Properties properties = new Properties();
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return properties;
        }
    }

    @DisplayName("The fallback bundle defines every key SpawnMessages renders")
    @Test
    void fallbackBundleContainsEveryUsedKey() throws IOException {
        Assertions.assertTrue(load("messages_en.properties").stringPropertyNames().containsAll(SpawnMessages.KEYS), "messages_en.properties must define all of " + SpawnMessages.KEYS);
    }

    @DisplayName("The German bundle has exactly the keys of the fallback bundle")
    @Test
    void germanBundleHasTheKeysOfTheFallbackBundle() throws IOException {
        Assertions.assertEquals(load("messages_en.properties").stringPropertyNames(), load("messages_de.properties").stringPropertyNames(), "de and en bundles must have identical keys");
    }
}
