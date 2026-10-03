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

import io.avaje.inject.PostConstruct;
import io.avaje.inject.RequiresProperty;
import jakarta.inject.Singleton;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Announces that migrations ran and the schema validated; depending on the factory orders it last.
 */
@Singleton
@RequiresProperty(DatabaseProperties.URL)
final class DatabaseReady {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseReady.class);

    private final SchemaMigrations migrations;

    DatabaseReady(SessionFactory sessionFactory, SchemaMigrations migrations) {
        this.migrations = migrations;
    }

    // Deliberately no url, user or password: a JDBC url can carry credentials.
    @PostConstruct
    void announce() {
        LOG.atInfo().addKeyValue("units", this.migrations.units()).addKeyValue("migrations", this.migrations.applied()).log("Database ready");
    }
}
