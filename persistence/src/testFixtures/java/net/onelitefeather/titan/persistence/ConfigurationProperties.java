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
package net.onelitefeather.titan.persistence;

import io.avaje.config.Configuration;
import io.avaje.inject.BeanScope;
import io.avaje.inject.BeanScopeBuilder;
import io.avaje.inject.spi.ConfigPropertyPlugin;
import java.util.Optional;

/**
 * A {@code @RequiresProperty} source backed by one {@link Configuration}, with the semantics of
 * Avaje's own avaje-config plugin, which reads the static global one. A test builds its scope with
 * {@link #scopeBuilder(Configuration)} so no two tests share configuration.
 */
public final class ConfigurationProperties implements ConfigPropertyPlugin {

    private final Configuration config;

    private ConfigurationProperties(Configuration config) {
        this.config = config;
    }

    /** A scope builder whose conditions and injected {@link Configuration} are {@code config}. */
    public static BeanScopeBuilder scopeBuilder(Configuration config) {
        BeanScopeBuilder builder = BeanScope.builder().bean(Configuration.class, config);
        builder.configPlugin(new ConfigurationProperties(config));
        return builder;
    }

    @Override
    public Optional<String> get(String property) {
        return this.config.getOptional(property);
    }

    @Override
    public boolean contains(String property) {
        return this.config.getNullable(property) != null;
    }

    @Override
    public boolean equalTo(String property, String value) {
        return value.equals(this.config.getNullable(property));
    }
}
