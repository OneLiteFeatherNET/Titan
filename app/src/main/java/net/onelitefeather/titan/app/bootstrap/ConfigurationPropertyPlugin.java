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

import io.avaje.config.Configuration;
import io.avaje.inject.spi.ConfigPropertyPlugin;
import java.util.Optional;

/**
 * Avaje Inject's {@link ConfigPropertyPlugin} - used internally to evaluate
 * {@code @RequiresProperty}
 * and similar bean conditions while {@link io.avaje.inject.BeanScope#builder()} builds the scope -
 * backed by the lobby's own {@link Configuration} instance instead of Avaje Inject's default
 * plugin.
 *
 * <p>Avaje Inject's default plugin, {@code io.avaje.inject.DConfigProps}, reads the <em>static</em>
 * {@code io.avaje.config.Config} facade directly, the first time anything touches it, with no
 * caught translation of a syntactically broken {@code application.yaml}: that first touch runs
 * inside {@code Config}'s own static initializer, so a broken file would surface there as an
 * uncaught {@link ExceptionInInitializerError} instead of the clean, caught
 * {@link net.onelitefeather.titan.common.config.ConfigException} a single, explicit
 * {@code net.onelitefeather.titan.common.config.ConfigurationFactory#initialise()} call produces.
 * {@link net.onelitefeather.titan.app.Titan} therefore calls {@code initialise()} itself, at a
 * known point before the {@link io.avaje.inject.BeanScope} is built, and hands the resulting
 * {@code Config.asConfiguration()} instance both to this plugin (via
 * {@link io.avaje.inject.BeanScopeBuilder#configPlugin(ConfigPropertyPlugin)}) and to the scope
 * itself (as a supplied bean) - so the default plugin's own, untranslated first touch of the
 * facade never happens at all.
 */
public final class ConfigurationPropertyPlugin implements ConfigPropertyPlugin {

    private final Configuration configuration;

    public ConfigurationPropertyPlugin(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Optional<String> get(String key) {
        return configuration.getOptional(key);
    }

    @Override
    public boolean contains(String key) {
        return configuration.getNullable(key) != null;
    }

    @Override
    public boolean equalTo(String key, String value) {
        return value.equals(configuration.getNullable(key));
    }
}
