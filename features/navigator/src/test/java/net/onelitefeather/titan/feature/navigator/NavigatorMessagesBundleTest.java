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
package net.onelitefeather.titan.feature.navigator;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NavigatorMessagesBundleTest {

    /** Every key the navigator renders, as declared by NavigatorMessages. */
    private static Set<String> renderedKeys() {
        return new HashSet<>(NavigatorMessages.KEYS);
    }

    private static Properties load(String file) throws IOException {
        try (InputStream in = NavigatorMessagesBundleTest.class.getResourceAsStream("/titan/navigator/" + file)) {
            Assertions.assertNotNull(in, file + " must be on the classpath");
            Properties properties = new Properties();
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return properties;
        }
    }

    @DisplayName("The fallback bundle defines every key NavigatorMessages renders")
    @Test
    void fallbackBundleContainsEveryUsedKey() throws IOException {
        Assertions.assertTrue(load("messages_en.properties").stringPropertyNames().containsAll(renderedKeys()), "messages_en.properties must define all of " + renderedKeys());
    }

    @DisplayName("The fallback bundle has no key that NavigatorMessages does not render")
    @Test
    void fallbackBundleHasNoUnusedKey() throws IOException {
        Assertions.assertEquals(renderedKeys(), load("messages_en.properties").stringPropertyNames(), "every key in messages_en.properties must be rendered, and every rendered key must be defined");
    }

    @DisplayName("The server list's sort, page and back texts are rendered and defined in both bundles")
    @Test
    void serverListTextsAreRenderedAndDefined() throws IOException {
        List<String> pickerKeys = List.of(NavigatorMessages.SORT_ITEM, NavigatorMessages.SORT_NAME, NavigatorMessages.SORT_MOST_PLAYERS, NavigatorMessages.SORT_MOST_FREE, NavigatorMessages.SORT_STATUS, NavigatorMessages.SORT_HINT, NavigatorMessages.PAGE_PREVIOUS, NavigatorMessages.PAGE_NEXT, NavigatorMessages.PAGE_INDICATOR, NavigatorMessages.PICKER_BACK);

        Assertions.assertTrue(renderedKeys().containsAll(pickerKeys), "NavigatorMessages.KEYS must list every sort and page text");
        Assertions.assertTrue(load("messages_de.properties").stringPropertyNames().containsAll(pickerKeys), "messages_de.properties must define every sort and page text");
    }

    @DisplayName("The staging menu, its start and timeout texts, the action line and the on-demand hint are defined in both bundles")
    @Test
    void stagingTextsAreRenderedAndDefined() throws IOException {
        List<String> stagingKeys = List.of("titan.navigator.staging.menu.title", "titan.navigator.staging.starting", "titan.navigator.staging.timeout", "titan.navigator.action.start", "titan.navigator.hint.staging.ondemand");

        Assertions.assertTrue(renderedKeys().containsAll(stagingKeys), "NavigatorMessages.KEYS must list every staging text");
        Assertions.assertTrue(load("messages_de.properties").stringPropertyNames().containsAll(stagingKeys), "messages_de.properties must define every staging text");
        Assertions.assertTrue(load("messages_en.properties").stringPropertyNames().containsAll(stagingKeys), "messages_en.properties must define every staging text");
    }

    @DisplayName("The German bundle has exactly the keys of the fallback bundle")
    @Test
    void germanBundleHasTheKeysOfTheFallbackBundle() throws IOException {
        Assertions.assertEquals(load("messages_en.properties").stringPropertyNames(), load("messages_de.properties").stringPropertyNames(), "de and en bundles must have identical keys");
    }
}
