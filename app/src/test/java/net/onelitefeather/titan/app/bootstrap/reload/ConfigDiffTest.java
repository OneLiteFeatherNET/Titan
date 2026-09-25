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

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link ConfigDiff#between(Map, Map)} as a pure function - no {@code Config}
 * call,
 * no I/O, only plain {@link Map} fixtures (see
 * {@code openspec/changes/config-reload-feature-flags/tasks.md}, task 3.1).
 */
class ConfigDiffTest {

    @DisplayName("An unchanged configuration produces an empty diff")
    @Test
    void unchangedProducesEmptyDiff() {
        Map<String, String> state = Map.of("sit.offset.y", "0.5", "tickle.cooldownMillis", "4000");

        ConfigDiff diff = ConfigDiff.between(state, state);

        Assertions.assertTrue(diff.isEmpty(), "an identical snapshot must be an empty diff");
        Assertions.assertTrue(diff.affectedModuleIds().isEmpty(), "no module can be affected by an empty diff");
        Assertions.assertFalse(diff.featureFlagsChanged(), "no flag changed");
    }

    @DisplayName("A changed key is reported as changed and names its module")
    @Test
    void changedKeyNamesItsModule() {
        Map<String, String> old = Map.of("sit.offset.y", "0.5");
        Map<String, String> updated = Map.of("sit.offset.y", "0.8");

        ConfigDiff diff = ConfigDiff.between(old, updated);

        Assertions.assertFalse(diff.isEmpty(), "a changed value is not an empty diff");
        Assertions.assertEquals(Set.of("sit.offset.y"), diff.changedKeys(), "must report the changed key");
        Assertions.assertTrue(diff.addedKeys().isEmpty(), "no key was added");
        Assertions.assertTrue(diff.removedKeys().isEmpty(), "no key was removed");
        Assertions.assertEquals(Set.of("sit"), diff.affectedModuleIds(), "the module id is the prefix up to the first dot");
        Assertions.assertEquals("0.5", diff.oldValues().get("sit.offset.y"), "must keep the old value for a revert");
        Assertions.assertEquals("0.8", diff.newValues().get("sit.offset.y"), "must keep the new value to apply");
    }

    @DisplayName("A newly added key is reported as added and names its module")
    @Test
    void addedKeyNamesItsModule() {
        Map<String, String> old = Map.of();
        Map<String, String> updated = Map.of("navigator.entries.parkour.slot", "3");

        ConfigDiff diff = ConfigDiff.between(old, updated);

        Assertions.assertEquals(Set.of("navigator.entries.parkour.slot"), diff.addedKeys());
        Assertions.assertEquals(Set.of("navigator"), diff.affectedModuleIds());
    }

    @DisplayName("A removed key is reported as removed, and its old value is kept for a revert")
    @Test
    void removedKeyKeepsItsOldValue() {
        Map<String, String> old = Map.of("elytra.burnDurationTicks", "200");
        Map<String, String> updated = Map.of();

        ConfigDiff diff = ConfigDiff.between(old, updated);

        Assertions.assertEquals(Set.of("elytra.burnDurationTicks"), diff.removedKeys());
        Assertions.assertEquals(Set.of("elytra"), diff.affectedModuleIds());
        Assertions.assertEquals("200", diff.oldValues().get("elytra.burnDurationTicks"));
    }

    @DisplayName("titan.* and config.* keys change but name no module")
    @Test
    void titanAndConfigKeysNameNoModule() {
        Map<String, String> old = Map.of("titan.command.reload", "false", "config.watch.enabled", "true");
        Map<String, String> updated = Map.of("titan.command.reload", "true", "config.watch.enabled", "false");

        ConfigDiff diff = ConfigDiff.between(old, updated);

        Assertions.assertEquals(Set.of("titan.command.reload", "config.watch.enabled"), diff.changedKeys());
        Assertions.assertTrue(diff.affectedModuleIds().isEmpty(), "titan.* and config.* must never name a module");
        Assertions.assertFalse(diff.featureFlagsChanged());
    }

    @DisplayName("features.* keys flip the feature-flags-changed flag and name no module")
    @Test
    void featureKeysFlipTheFlagWithoutNamingAModule() {
        Map<String, String> old = Map.of("features.NAVIGATOR_SLENDER", "false");
        Map<String, String> updated = Map.of("features.NAVIGATOR_SLENDER", "true");

        ConfigDiff diff = ConfigDiff.between(old, updated);

        Assertions.assertTrue(diff.featureFlagsChanged(), "a features.* change must flip the flag");
        Assertions.assertTrue(diff.affectedModuleIds().isEmpty(), "features.* must never name a module");
    }

    @DisplayName("puts() carries every changed and added key with its new value")
    @Test
    void putsCarriesChangedAndAddedKeysWithNewValues() {
        Map<String, String> old = Map.of("sit.offset.y", "0.5", "tickle.cooldownMillis", "4000");
        Map<String, String> updated = Map.of("sit.offset.y", "0.8", "tickle.cooldownMillis", "4000", "sit.offset.x", "1.0");

        ConfigDiff diff = ConfigDiff.between(old, updated);

        Assertions.assertEquals(Map.of("sit.offset.y", "0.8", "sit.offset.x", "1.0"), diff.puts());
    }

    @DisplayName("removals() carries every removed key")
    @Test
    void removalsCarriesRemovedKeys() {
        Map<String, String> old = Map.of("sit.offset.y", "0.5", "tickle.cooldownMillis", "4000");
        Map<String, String> updated = Map.of("sit.offset.y", "0.5");

        ConfigDiff diff = ConfigDiff.between(old, updated);

        Assertions.assertEquals(Set.of("tickle.cooldownMillis"), diff.removals());
    }

    @DisplayName("keysForModule scopes changed, added and removed keys to one module")
    @Test
    void keysForModuleScopesToOneModule() {
        Map<String, String> old = Map.of("sit.offset.y", "0.5", "tickle.cooldownMillis", "4000");
        Map<String, String> updated = Map.of("sit.offset.y", "0.8", "sit.offset.x", "1.0");

        ConfigDiff diff = ConfigDiff.between(old, updated);

        Assertions.assertEquals(Set.of("sit.offset.y", "sit.offset.x"), diff.keysForModule("sit"));
        Assertions.assertEquals(Set.of("tickle.cooldownMillis"), diff.keysForModule("tickle"));
        Assertions.assertTrue(diff.keysForModule("elytra").isEmpty(), "a module with no touched key gets an empty set");
    }

    @DisplayName("revertFor puts back a changed key's old value and undoes an added key")
    @Test
    void revertForRestoresChangedAndUndoesAdded() {
        Map<String, String> old = Map.of("sit.offset.y", "0.5");
        Map<String, String> updated = Map.of("sit.offset.y", "0.8", "sit.offset.x", "1.0");

        ConfigDiff diff = ConfigDiff.between(old, updated);
        ConfigDiff.ModuleRevert revert = diff.revertFor("sit");

        Assertions.assertEquals(Map.of("sit.offset.y", "0.5"), revert.puts(), "a changed key reverts to its old value");
        Assertions.assertEquals(Set.of("sit.offset.x"), revert.removals(), "an added key must be removed again on revert");
    }

    @DisplayName("revertFor puts back a removed key's old value")
    @Test
    void revertForRestoresRemovedKey() {
        Map<String, String> old = Map.of("elytra.burnDurationTicks", "200");
        Map<String, String> updated = Map.of();

        ConfigDiff diff = ConfigDiff.between(old, updated);
        ConfigDiff.ModuleRevert revert = diff.revertFor("elytra");

        Assertions.assertEquals(Map.of("elytra.burnDurationTicks", "200"), revert.puts());
        Assertions.assertTrue(revert.removals().isEmpty());
    }

    @DisplayName("Several modules changed at once are all reported as affected")
    @Test
    void severalModulesAreAllAffected() {
        Map<String, String> old = Map.of("sit.offset.y", "0.5", "tickle.cooldownMillis", "4000");
        Map<String, String> updated = Map.of("sit.offset.y", "0.8", "tickle.cooldownMillis", "-5");

        ConfigDiff diff = ConfigDiff.between(old, updated);

        Assertions.assertEquals(Set.of("sit", "tickle"), diff.affectedModuleIds());
    }
}
