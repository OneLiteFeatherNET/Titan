/**
 * The shared database of the lobby: one Hikari pool, Flyway migrations per {@link
 * net.onelitefeather.titan.persistence.PersistenceUnit} and one Hibernate {@code SessionFactory},
 * all absent unless {@code titan.database.url} is configured. A module contributes a unit as a
 * bean and lists {@code provides = PersistenceUnit.class} so Avaje wires it before this module.
 */
@InjectModule(name = "persistence", requires = PersistenceUnit.class)
package net.onelitefeather.titan.persistence;

import io.avaje.inject.InjectModule;
