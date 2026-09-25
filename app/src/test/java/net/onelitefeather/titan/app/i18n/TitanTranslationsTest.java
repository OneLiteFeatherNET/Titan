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
package net.onelitefeather.titan.app.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.Translator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link TitanTranslations}. Every test builds its own store with {@link
 * TitanTranslations#createStore()} and renders through that store instance directly (F.I.R.S.T. -
 * Independent: never through the shared, process-wide {@link GlobalTranslator#translator()}
 * singleton, and no test mutates it).
 */
class TitanTranslationsTest {

    private static final List<String> KEY_CONSTANTS = List.of(
            TitanTranslations.CONFIG_RELOAD_UNCHANGED, TitanTranslations.CONFIG_RELOAD_APPLIED, TitanTranslations.CONFIG_RELOAD_REJECTED, TitanTranslations.CONFIG_RELOAD_FAILED
    );

    @DisplayName("Every key constant used in code exists in the English bundle")
    @Test
    void everyKeyConstantExistsInTheEnglishBundle() {
        Properties english = readBundle("titan_en");

        for (String key : KEY_CONSTANTS) {
            Assertions.assertTrue(
                    english.containsKey(key), "the English bundle must define " + key + ", since it is the fallback locale"
            );
        }
    }

    @DisplayName("The English and German bundles define exactly the same keys")
    @Test
    void bothBundlesDefineExactlyTheSameKeys() {
        Set<String> english = readBundle("titan_en").stringPropertyNames();
        Set<String> german = readBundle("titan_de").stringPropertyNames();

        Assertions.assertEquals(
                english, german, "titan_de.properties must neither miss a key from titan_en.properties nor add one of its own"
        );
    }

    @DisplayName("Rendering with Locale.GERMAN yields the German bundle's text")
    @Test
    void renderingWithGermanLocaleYieldsGermanText() {
        MiniMessageTranslationStore store = TitanTranslations.createStore();
        TranslatableComponent component = Component.translatable(TitanTranslations.CONFIG_RELOAD_UNCHANGED);

        Component rendered = store.translate(component, Locale.GERMAN);

        Assertions.assertNotNull(rendered, "a registered key must render for a registered locale");
        String plain = PlainTextComponentSerializer.plainText().serialize(rendered);
        Assertions.assertTrue(
                plain.contains("Änderungen"), "must contain the German bundle's text (with its umlaut), was: " + plain
        );
    }

    @DisplayName("Rendering with an unknown locale falls back to the English bundle's text")
    @Test
    void renderingWithUnknownLocaleFallsBackToEnglish() {
        MiniMessageTranslationStore store = TitanTranslations.createStore();
        TranslatableComponent component = Component.translatable(TitanTranslations.CONFIG_RELOAD_UNCHANGED);

        Component renderedForUnknownLocale = store.translate(component, Locale.JAPANESE);
        Component renderedForEnglish = store.translate(component, Locale.ENGLISH);

        String unknownLocaleText = PlainTextComponentSerializer.plainText().serialize(renderedForUnknownLocale);
        String englishText = PlainTextComponentSerializer.plainText().serialize(renderedForEnglish);
        Assertions.assertEquals(
                englishText, unknownLocaleText, "an unregistered locale (Locale.JAPANESE) must fall back to the store's default locale (English)"
        );
        Assertions.assertTrue(
                unknownLocaleText.contains("no changes"), "must contain the English bundle's text, was: " + unknownLocaleText
        );
    }

    @DisplayName("register(translator) builds a store and adds it as a source, without touching the real GlobalTranslator")
    @Test
    void registerAddsTheStoreAsASourceOnTheGivenTranslator() {
        RecordingTranslator fake = new RecordingTranslator();

        MiniMessageTranslationStore returned = TitanTranslations.register(fake);

        Assertions.assertEquals(1, fake.addedSources.size(), "must add exactly one source");
        Assertions.assertSame(returned, fake.addedSources.get(0), "must add the store it built and returns");
    }

    private static Properties readBundle(String baseName) {
        Properties properties = new Properties();
        String resource = "/lang/" + baseName + ".properties";
        try (InputStream input = TitanTranslationsTest.class.getResourceAsStream(resource)) {
            Assertions.assertNotNull(input, "bundle must be on the classpath: " + resource);
            properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return properties;
    }

    /**
     * A minimal, test-only {@link GlobalTranslator} that only records {@link #addSource}, so this
     * test never reaches for the real {@link GlobalTranslator#translator()} singleton and cannot
     * leak a registered store into other tests.
     */
    private static final class RecordingTranslator implements GlobalTranslator {

        private final List<Translator> addedSources = new ArrayList<>();

        @Override
        public Key name() {
            return Key.key("titan", "test-translator");
        }

        @Override
        public Iterable<? extends Translator> sources() {
            return List.copyOf(addedSources);
        }

        @Override
        public boolean addSource(Translator translator) {
            return addedSources.add(translator);
        }

        @Override
        public boolean removeSource(Translator translator) {
            return addedSources.remove(translator);
        }

        @Override
        public java.text.MessageFormat translate(String key, Locale locale) {
            return null;
        }
    }
}
