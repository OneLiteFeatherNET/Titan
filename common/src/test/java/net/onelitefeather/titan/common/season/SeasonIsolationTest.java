/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.onelitefeather.titan.common.season;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * US-4.06: no season may require another, so no deployment order can be wrong.
 *
 * <p>The spec suggests an ArchUnit rule for this, and under a design where each season were its own
 * jar of Java that would be exactly right. It is not what was built. A season here is a JSON file;
 * there are no per-season classes for ArchUnit to constrain, and a rule over the classes that do
 * exist would pass forever without ever having been able to fail — the worst kind of green test.
 *
 * <p>What is checked instead is the property the rule was standing in for, at the level where a
 * season actually lives:
 *
 * <ul>
 * <li>the data model offers no field in which one season could name another, checked by reflection
 * so that adding such a field breaks this test;</li>
 * <li>removing any one season leaves the rest loading and running, which is what "no season
 * requires another" means to an operator holding a directory of files.</li>
 * </ul>
 */
class SeasonIsolationTest {

    /**
     * The types a season's own data is allowed to be made of. A season type appearing here that is
     * not on this list would be a way for one season to point at another.
     */
    private static final Set<Class<?>> ALLOWED_SEASON_TYPES = Set.of(SeasonWindow.class, SeasonEffect.class);

    private final SeasonLoader loader = SeasonLoader.create(SeasonFixtures.BERLIN);

    @Test
    @DisplayName("a season has no field in which it could name another season")
    void aSeasonCannotNameAnotherSeason() {
        List<String> offenders = new ArrayList<>();
        for (RecordComponent component : SeasonDefinition.class.getRecordComponents()) {
            Class<?> type = componentType(component);
            if (isSeasonType(type) && !ALLOWED_SEASON_TYPES.contains(type)) {
                offenders.add("SeasonDefinition." + component.getName() + " is a " + type.getSimpleName());
            }
        }
        assertEquals(List.of(), offenders, "a season that can reference a season has a deployment order");
    }

    @Test
    @DisplayName("no effect can name a season either")
    void noEffectCanNameASeason() {
        List<String> offenders = new ArrayList<>();
        for (Class<?> permitted : SeasonEffect.class.getPermittedSubclasses()) {
            for (RecordComponent component : permitted.getRecordComponents()) {
                Class<?> type = componentType(component);
                if (isSeasonType(type)) {
                    offenders.add(permitted.getSimpleName() + "." + component.getName() + " is a " + type.getSimpleName());
                }
            }
        }
        assertEquals(List.of(), offenders, "an effect that can reference a season has a deployment order");
    }

    @Test
    @DisplayName("every effect type has a record, and every record a type - the two lists cannot drift")
    void everyEffectTypeIsReachableFromTheSealedHierarchy() {
        Set<Class<?>> permitted = Set.of(SeasonEffect.class.getPermittedSubclasses());
        for (SeasonEffect.Type type : SeasonEffect.Type.values()) {
            assertTrue(permitted.contains(type.effectClass()), type.id() + " maps to a class outside the sealed hierarchy");
        }
        assertEquals(permitted.size(), SeasonEffect.Type.values().length, "a record with no type id cannot be written in a file; a type id with no record cannot be read");
    }

    @Test
    @DisplayName("removing one season leaves the others loading and running")
    void removingOneSeasonLeavesTheOthers(@TempDir Path directory) throws IOException {
        Path first = SeasonFixtures.write(directory, "first", SeasonFixtures.seasonJson("first", 1, null, null, "minecraft:stone"));
        SeasonFixtures.write(directory, "second", SeasonFixtures.seasonJson("second", 2, null, null, "minecraft:dirt"));

        assertEquals(List.of("first", "second"), ids(this.loader.loadAll(directory)));

        Files.delete(first);

        assertEquals(List.of("second"), ids(this.loader.loadAll(directory)), "the season that is left must still load on its own");
    }

    private static List<String> ids(List<SeasonDefinition> definitions) {
        return definitions.stream().map(SeasonDefinition::id).toList();
    }

    private static Class<?> componentType(RecordComponent component) {
        // Unwrap List<X> so a "seasons" field would be caught rather than seen as a plain List.
        if (component.getGenericType() instanceof java.lang.reflect.ParameterizedType parameterized && parameterized.getActualTypeArguments().length == 1 && parameterized.getActualTypeArguments()[0] instanceof Class<?> argument) {
            return argument;
        }
        return component.getType();
    }

    private static boolean isSeasonType(Class<?> type) {
        return type.getPackageName().equals(SeasonDefinition.class.getPackageName());
    }
}
