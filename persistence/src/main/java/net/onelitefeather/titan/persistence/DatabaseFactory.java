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

import com.zaxxer.hikari.HikariDataSource;
import io.avaje.config.Configuration;
import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import io.avaje.inject.RequiresProperty;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.jpa.HibernatePersistenceConfiguration;

/**
 * The one pool and the one {@link SessionFactory} of the process, built only when {@code
 * titan.database.url} is configured. Avaje closes beans in reverse creation order, so the
 * {@link DatabaseWriter} is closed before the {@code SessionFactory}, and that before the pool.
 */
@Factory
@RequiresProperty(DatabaseProperties.URL)
final class DatabaseFactory {

    @Bean
    DatabaseSettings databaseSettings(Configuration config) {
        return DatabaseSettings.from(config);
    }

    @Bean(destroyMethod = "close")
    HikariDataSource dataSource(DatabaseSettings settings) {
        return new HikariDataSource(HikariSettings.apply(settings));
    }

    @Bean
    SchemaMigrations schemaMigrations(HikariDataSource dataSource, List<PersistenceUnit> units) {
        return SchemaMigrations.migrate(dataSource, units);
    }

    @Bean(destroyMethod = "close")
    SessionFactory sessionFactory(DatabaseSettings settings, HikariDataSource dataSource, List<PersistenceUnit> units, SchemaMigrations migrations) {
        return (SessionFactory) hibernateConfiguration(settings, dataSource, units).createEntityManagerFactory();
    }

    static HibernatePersistenceConfiguration hibernateConfiguration(DatabaseSettings settings, HikariDataSource dataSource, List<PersistenceUnit> units) {
        Class<?>[] entities = units.stream().flatMap(unit -> unit.entities().stream()).toArray(Class<?>[]::new);
        HibernatePersistenceConfiguration hibernate = new HibernatePersistenceConfiguration("titan").managedClasses(entities);
        settings.hibernateProperties().forEach(hibernate::property);
        // Set last: the pool and validate are not configurable.
        hibernate.property(AvailableSettings.JAKARTA_NON_JTA_DATASOURCE, dataSource).property(SchemaToolingSettings.HBM2DDL_AUTO, "validate");
        return hibernate;
    }

    // Depends on the SessionFactory only for the order: created after it, closed before it.
    @Bean(destroyMethod = "close")
    DatabaseWriter databaseWriter(SessionFactory sessionFactory) {
        return new DatabaseWriter();
    }
}
