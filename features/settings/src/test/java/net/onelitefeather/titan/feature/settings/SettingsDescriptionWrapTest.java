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
package net.onelitefeather.titan.feature.settings;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.onelitefeather.titan.core.settings.BooleanSetting;
import net.onelitefeather.titan.core.settings.SettingDefault;
import net.onelitefeather.titan.core.settings.SettingKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SettingsDescriptionWrapTest {

    private static final String OPTION_PREFIX = "titan.settings.option.";
    private static final String DESCRIPTION_SUFFIX = ".description";
    private static final int WIDTH = 30;

    private SettingsMessages messages;

    @BeforeEach
    void registerTexts() {
        this.messages = new SettingsMessages();
        this.messages.register();
    }

    @AfterEach
    void unregisterTexts() {
        this.messages.close();
    }

    @DisplayName("Every German and English option description wraps to at most 30 characters per line")
    @Test
    void everyDescriptionFitsTheLoreWidth() throws IOException {
        for (Locale language : List.of(Locale.GERMAN, Locale.ENGLISH)) {
            for (BooleanSetting setting : settingsOf(language)) {
                for (String line : plainLines(SettingsMessages.description(setting, language))) {
                    Assertions.assertTrue(line.length() <= WIDTH, language + " line '" + line + "' of " + setting.key() + " must have at most " + WIDTH + " characters");
                }
            }
        }
    }

    @DisplayName("Every long option description takes more than one lore line")
    @Test
    void longDescriptionsAreWrapped() throws IOException {
        for (Locale language : List.of(Locale.GERMAN, Locale.ENGLISH)) {
            for (BooleanSetting setting : settingsOf(language)) {
                Assertions.assertTrue(SettingsMessages.description(setting, language).size() > 1, language + " description of " + setting.key() + " must wrap onto several lines");
            }
        }
    }

    @DisplayName("Every description line is gray and not italic")
    @Test
    void descriptionLinesAreGrayAndNotItalic() {
        for (Component line : SettingsMessages.description(SettingsFixtures.TAG, Locale.ENGLISH)) {
            Assertions.assertEquals(NamedTextColor.GRAY, line.color(), "a description line must be gray");
            Assertions.assertEquals(TextDecoration.State.FALSE, line.decoration(TextDecoration.ITALIC), "a description line must not be italic");
        }
    }

    /**
     * One setting per description key of the bundle, so a new option is checked without a new test.
     */
    private static List<BooleanSetting> settingsOf(Locale language) throws IOException {
        String file = language == Locale.GERMAN ? "messages_de.properties" : "messages_en.properties";
        List<BooleanSetting> settings = new ArrayList<>();
        for (String key : load(file).stringPropertyNames()) {
            if (key.startsWith(OPTION_PREFIX) && key.endsWith(DESCRIPTION_SUFFIX)) {
                String id = key.substring(OPTION_PREFIX.length(), key.length() - DESCRIPTION_SUFFIX.length());
                int dot = id.indexOf('.');
                settings.add(new BooleanSetting(SettingKey.parse(id.substring(0, dot) + ":" + id.substring(dot + 1)), SettingDefault.OPT_IN));
            }
        }
        Assertions.assertFalse(settings.isEmpty(), file + " must define option descriptions");
        return settings;
    }

    private static List<String> plainLines(List<Component> lines) {
        return lines.stream().map(line -> PlainTextComponentSerializer.plainText().serialize(line)).toList();
    }

    private static Properties load(String file) throws IOException {
        try (InputStream in = SettingsDescriptionWrapTest.class.getResourceAsStream("/titan/settings/" + file)) {
            Assertions.assertNotNull(in, file + " must be on the classpath");
            Properties properties = new Properties();
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return properties;
        }
    }
}
