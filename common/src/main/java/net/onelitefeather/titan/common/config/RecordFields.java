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

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;

/**
 * The small record-component lookups shared by every class that has to walk a config record's own
 * shape: {@link ConfigSections} (deciding whether a flat, dotted key belongs to a component
 * actually typed {@code List}/{@code Set}/array) and {@link SectionBinder} (naming the exact
 * nested component a type mismatch or an unknown key was found in). Extracted so both walk a
 * record's components, including a {@code Map<String, Record>} component's value type, exactly
 * one way (DRY).
 */
final class RecordFields {

    private RecordFields() {
    }

    /**
     * Finds the record component named {@code name} in {@code type}, or {@code null} if {@code
     * type} declares no such component.
     */
    static @Nullable RecordComponent component(Class<?> type, String name) {
        for (RecordComponent component : type.getRecordComponents()) {
            if (component.getName().equals(name)) {
                return component;
            }
        }
        return null;
    }

    /**
     * The value type of a {@code Map<String, V>} component, or {@code null} if {@code component}
     * is not a parameterized two-argument map type (e.g. a raw {@code Map}).
     */
    static @Nullable Class<?> mapValueType(RecordComponent component) {
        Type genericType = component.getGenericType();
        if (genericType instanceof ParameterizedType parameterized) {
            Type[] arguments = parameterized.getActualTypeArguments();
            if (arguments.length == 2 && arguments[1] instanceof Class<?> valueClass) {
                return valueClass;
            }
        }
        return null;
    }
}
