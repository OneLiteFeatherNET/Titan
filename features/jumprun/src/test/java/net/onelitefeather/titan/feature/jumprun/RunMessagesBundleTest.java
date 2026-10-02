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
package net.onelitefeather.titan.feature.jumprun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class RunMessagesBundleTest {

    private static Properties load(String file) throws IOException {
        try (InputStream in = RunMessagesBundleTest.class.getResourceAsStream("/titan/jumprun/" + file)) {
            assertNotNull(in, file + " must be on the classpath");
            Properties properties = new Properties();
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return properties;
        }
    }

    @Test
    void fallbackBundleContainsEveryKeyUsedByRunMessages() throws IOException {
        assertTrue(
                load("messages_en.properties").stringPropertyNames().containsAll(RunMessages.KEYS), "messages_en.properties must define all of " + RunMessages.KEYS);
    }

    @Test
    void germanBundleHasExactlyTheKeysOfTheFallbackBundle() throws IOException {
        assertEquals(
                load("messages_en.properties").stringPropertyNames(), load("messages_de.properties").stringPropertyNames(), "de and en bundles must have identical keys");
    }
}
