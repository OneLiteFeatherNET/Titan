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
package net.onelitefeather.titan.app.bootstrap.reload;

import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link ModuleKeys} - the pure key-to-module-id mapping
 * {@link ConfigChangeHandler} uses to decide which modules a
 * {@code io.avaje.config.ModificationEvent}'s changed keys affect. See
 * {@code openspec/changes/config-reload-feature-flags/design.md}, decision 1, and tasks.md, task
 * 3.2.
 */
class ModuleKeysTest {

    @DisplayName("A key's module id is its prefix up to (not including) the first dot")
    @Test
    void moduleIdIsThePrefixUpToTheFirstDot() {
        Assertions.assertEquals("sit", ModuleKeys.moduleIdOf("sit.offset.y"));
        Assertions.assertEquals("tickle", ModuleKeys.moduleIdOf("tickle.cooldownMillis"));
    }

    @DisplayName("features./titan./config. keys never name a module")
    @Test
    void featuresTitanAndConfigPrefixesNeverNameAModule() {
        Assertions.assertNull(ModuleKeys.moduleIdOf("features.NAVIGATOR_SLENDER"));
        Assertions.assertNull(ModuleKeys.moduleIdOf("titan.config.reload.intervalSeconds"));
        Assertions.assertNull(ModuleKeys.moduleIdOf("config.watch.enabled"));
    }

    @DisplayName("A key with no dot at all is its own module id")
    @Test
    void aKeyWithNoDotIsItsOwnModuleId() {
        Assertions.assertEquals("sit", ModuleKeys.moduleIdOf("sit"));
    }

    @DisplayName("affectedModuleIds collapses several keys of the same module into one id")
    @Test
    void affectedModuleIdsCollapsesSeveralKeysOfTheSameModule() {
        Set<String> keys = Set.of("sit.offset.x", "sit.offset.y", "sit.offset.z");

        Assertions.assertEquals(Set.of("sit"), ModuleKeys.affectedModuleIds(keys));
    }

    @DisplayName("affectedModuleIds names every affected module, but never features/titan/config")
    @Test
    void affectedModuleIdsNamesEveryAffectedModuleButNeverNonModuleKeys() {
        Set<String> keys = Set.of("sit.offset.y", "tickle.cooldownMillis", "features.NAVIGATOR_SLENDER", "titan.config.reload.intervalSeconds", "config.watch.enabled");

        Assertions.assertEquals(Set.of("sit", "tickle"), ModuleKeys.affectedModuleIds(keys));
    }

    @DisplayName("affectedModuleIds is empty for an empty key set")
    @Test
    void affectedModuleIdsIsEmptyForAnEmptyKeySet() {
        Assertions.assertTrue(ModuleKeys.affectedModuleIds(Set.of()).isEmpty());
    }

    @DisplayName("keysForModule returns only the keys belonging to that module")
    @Test
    void keysForModuleReturnsOnlyThatModulesKeys() {
        Set<String> keys = Set.of("sit.offset.y", "tickle.cooldownMillis", "features.NAVIGATOR_SLENDER");

        Assertions.assertEquals(Set.of("sit.offset.y"), ModuleKeys.keysForModule("sit", keys));
        Assertions.assertEquals(Set.of("tickle.cooldownMillis"), ModuleKeys.keysForModule("tickle", keys));
    }

    @DisplayName("keysForModule is empty for a module with no changed key")
    @Test
    void keysForModuleIsEmptyForAModuleWithNoChangedKey() {
        Set<String> keys = Set.of("sit.offset.y");

        Assertions.assertTrue(ModuleKeys.keysForModule("elytra", keys).isEmpty());
    }

    @DisplayName("keysForModule does not match a module id that is merely a prefix of another key's module id")
    @Test
    void keysForModuleDoesNotMatchAPrefixOfAnotherModuleId() {
        // "sit" must not accidentally match a hypothetical "sitting.foo" key - the "." after the
        // module id is part of the match, not just the characters.
        Set<String> keys = Set.of("sitting.foo");

        Assertions.assertTrue(ModuleKeys.keysForModule("sit", keys).isEmpty());
    }
}
