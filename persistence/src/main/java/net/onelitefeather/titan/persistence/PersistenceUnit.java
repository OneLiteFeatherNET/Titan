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

import java.util.List;
import java.util.regex.Pattern;

/**
 * What a module contributes to the one shared database: its JPA entities and, under {@code
 * classpath:db/migration/<name>/}, the Flyway migrations that create their tables. The migration
 * history lives in {@code flyway_<name>_history}, so the version numbers of two units never clash.
 *
 * @param name     lower-case identifier; becomes part of a SQL table name
 * @param entities every entity class the unit maps
 */
public record PersistenceUnit(String name, List<Class<?>> entities) {

    private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9_]*");

    public PersistenceUnit {
        if (!NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Persistence unit name must match " + NAME.pattern() + " but was '" + name + "'");
        }
        entities = List.copyOf(entities);
    }

    String migrationLocation() {
        return "classpath:db/migration/" + this.name;
    }

    String historyTable() {
        return "flyway_" + this.name + "_history";
    }
}
