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

import com.zaxxer.hikari.HikariConfig;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * Applies the {@code titan.database.hikari} section to a {@link HikariConfig} by bean setter name.
 * HikariCP's own {@code Properties} constructor logs the failed value (a number parse error, say),
 * so the settings are applied here and a failure names keys only.
 */
final class HikariSettings {

    private static final String DRIVER_PREFIX = "dataSource.";
    private static final Set<Class<?>> SCALARS = Set.of(int.class, long.class, boolean.class, String.class);
    // These would replace the JDBC url as the way to connect; the url always comes from titan.database.url.
    private static final Set<String> CONNECTION_BYPASS = Set.of("dataSourceClassName", "dataSourceJNDI", "dataSource");

    private static final Map<String, Method> SETTERS = setters();

    private HikariSettings() {
    }

    /**
     * @throws IllegalStateException naming every unknown or invalid key, without a cause or any
     *                               value
     */
    static HikariConfig apply(DatabaseSettings settings) {
        HikariConfig config = new HikariConfig();
        Set<String> rejected = new TreeSet<>();
        Properties section = settings.hikari();
        for (String key : section.stringPropertyNames()) {
            if (!apply(config, key, section.getProperty(key))) {
                rejected.add(key);
            }
        }
        if (!rejected.isEmpty()) {
            throw new IllegalStateException("titan.database.hikari has unknown or invalid settings: " + rejected);
        }
        config.setJdbcUrl(settings.url());
        if (settings.user() != null) {
            config.setUsername(settings.user());
        }
        if (settings.password() != null) {
            config.setPassword(settings.password());
        }
        return config;
    }

    private static boolean apply(HikariConfig config, String key, String value) {
        if (key.startsWith(DRIVER_PREFIX) && key.length() > DRIVER_PREFIX.length()) {
            config.addDataSourceProperty(key.substring(DRIVER_PREFIX.length()), value);
            return true;
        }
        Method setter = SETTERS.get(key);
        if (setter == null || CONNECTION_BYPASS.contains(key)) {
            return false;
        }
        try {
            setter.invoke(config, convert(setter.getParameterTypes()[0], value));
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            // Dropped on purpose: the cause may hold the value.
            return false;
        }
    }

    private static Object convert(Class<?> type, String value) {
        String text = value.trim();
        if (type == int.class) {
            return Integer.parseInt(text);
        }
        if (type == long.class) {
            return Long.parseLong(text);
        }
        if (type == boolean.class) {
            if (!text.equalsIgnoreCase("true") && !text.equalsIgnoreCase("false")) {
                throw new IllegalArgumentException("not a boolean");
            }
            return Boolean.parseBoolean(text);
        }
        return value;
    }

    // Only the types a YAML scalar can express; object-typed setters (dataSource, metrics, ...) stay unreachable.
    private static Map<String, Method> setters() {
        Map<String, Method> setters = new HashMap<>();
        try {
            for (PropertyDescriptor property : Introspector.getBeanInfo(HikariConfig.class, Object.class).getPropertyDescriptors()) {
                Method write = property.getWriteMethod();
                if (write != null && SCALARS.contains(write.getParameterTypes()[0])) {
                    setters.put(property.getName(), write);
                }
            }
        } catch (IntrospectionException e) {
            throw new IllegalStateException("HikariConfig cannot be introspected", e);
        }
        return Map.copyOf(setters);
    }
}
