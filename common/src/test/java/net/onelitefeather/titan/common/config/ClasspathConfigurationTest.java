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

import io.avaje.config.Configuration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link ClasspathConfiguration#load(String, ClassLoader)}: it loads every key
 * a real classpath resource declares, and a missing resource loads no key at all rather than
 * throwing - see {@code io.avaje.config.CoreConfigurationBuilder#load(String)}, which only logs
 * that the resource was not found and leaves the builder's map untouched.
 *
 * <p>{@code classpath-configuration-test.yaml} (this test's fixture, under
 * {@code common/src/test/resources}) is used only here, kept apart from the {@code common}
 * module's own production resources.
 */
class ClasspathConfigurationTest {

    @DisplayName("Every key the classpath resource declares is loaded")
    @Test
    void loadsEveryKeyTheClasspathResourceDeclares() {
        Configuration configuration = ClasspathConfiguration.load("classpath-configuration-test.yaml", ClasspathConfigurationTest.class.getClassLoader());

        Assertions.assertEquals("hello", configuration.get("sample.text"));
        Assertions.assertEquals(42, configuration.getInt("sample.number"));
    }

    @DisplayName("A missing classpath resource loads no key, rather than throwing")
    @Test
    void missingClasspathResourceLoadsNoKey() {
        Configuration configuration = ClasspathConfiguration.load("does-not-exist.yaml", ClasspathConfigurationTest.class.getClassLoader());

        Assertions.assertNull(configuration.getNullable("sample.text"), "a missing resource must load no key");
        Assertions.assertNull(configuration.getNullable("sample.number"), "a missing resource must load no key");
    }

    @DisplayName("Null arguments are rejected")
    @Test
    void nullArgumentsAreRejected() {
        Assertions.assertThrows(NullPointerException.class, () -> ClasspathConfiguration.load(null, ClasspathConfigurationTest.class.getClassLoader()));
        Assertions.assertThrows(NullPointerException.class, () -> ClasspathConfiguration.load("classpath-configuration-test.yaml", null));
    }
}
