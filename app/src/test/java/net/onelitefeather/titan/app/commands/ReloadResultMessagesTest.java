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
package net.onelitefeather.titan.app.commands;

import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.onelitefeather.titan.app.bootstrap.reload.ReloadResult;
import net.onelitefeather.titan.app.i18n.TitanTranslations;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for the pure {@link ReloadResultMessages#toComponents(ReloadResult)} mapper: which
 * translatable components (and how many) each {@link ReloadResult} shape maps to - see
 * {@code openspec/changes/config-reload-feature-flags/tasks.md}, task 5.1.
 */
class ReloadResultMessagesTest {

    @DisplayName("Unchanged maps to exactly one 'unchanged' component with no arguments")
    @Test
    void unchangedMapsToASingleUnchangedComponent() {
        List<Component> components = ReloadResultMessages.toComponents(new ReloadResult.Unchanged());

        Assertions.assertEquals(1, components.size());
        TranslatableComponent component = assertTranslatable(components.get(0), TitanTranslations.CONFIG_RELOAD_UNCHANGED);
        Assertions.assertTrue(component.arguments().isEmpty());
    }

    @DisplayName("Applied with restarted modules maps to one 'applied' component naming them")
    @Test
    void appliedMapsToOneAppliedLineNamingTheRestartedModules() {
        ReloadResult.Applied applied = new ReloadResult.Applied(List.of("sit", "tickle"), List.of(), List.of(), false);

        List<Component> components = ReloadResultMessages.toComponents(applied);

        Assertions.assertEquals(1, components.size());
        TranslatableComponent component = assertTranslatable(components.get(0), TitanTranslations.CONFIG_RELOAD_APPLIED);
        Assertions.assertEquals("sit, tickle", argumentPlain(component, 0));
    }

    @DisplayName("Applied with a rejected module adds exactly one rejected line naming its keys and reason")
    @Test
    void appliedWithARejectedModuleAddsOneRejectedLine() {
        ReloadResult.RejectedModule rejected = new ReloadResult.RejectedModule(
                "tickle", Set.of("tickle.cooldownMillis"), "tickle.cooldownMillis must not be negative");
        ReloadResult.Applied applied = new ReloadResult.Applied(List.of(), List.of(rejected), List.of(), false);

        List<Component> components = ReloadResultMessages.toComponents(applied);

        Assertions.assertEquals(2, components.size(), "expected one applied line plus one rejected line");
        assertTranslatable(components.get(0), TitanTranslations.CONFIG_RELOAD_APPLIED);
        TranslatableComponent rejectedComponent = assertTranslatable(components.get(1), TitanTranslations.CONFIG_RELOAD_REJECTED);
        Assertions.assertEquals("tickle.cooldownMillis", argumentPlain(rejectedComponent, 0));
        Assertions.assertEquals("tickle.cooldownMillis must not be negative", argumentPlain(rejectedComponent, 1));
    }

    @DisplayName("Applied with a disabled module adds exactly one disabled line naming it")
    @Test
    void appliedWithADisabledModuleAddsOneDisabledLine() {
        ReloadResult.Applied applied = new ReloadResult.Applied(List.of(), List.of(), List.of("tickle"), false);

        List<Component> components = ReloadResultMessages.toComponents(applied);

        Assertions.assertEquals(2, components.size(), "expected one applied line plus one disabled line");
        assertTranslatable(components.get(0), TitanTranslations.CONFIG_RELOAD_APPLIED);
        TranslatableComponent disabledComponent = assertTranslatable(components.get(1), TitanTranslations.CONFIG_RELOAD_DISABLED);
        Assertions.assertEquals("tickle", argumentPlain(disabledComponent, 0));
    }

    @DisplayName("Applied with rejected and disabled modules adds one line for each, in that order")
    @Test
    void appliedWithRejectedAndDisabledModulesAddsBothLinesInOrder() {
        ReloadResult.RejectedModule rejected = new ReloadResult.RejectedModule("sit", Set.of("sit.offset.y"), "boom");
        ReloadResult.Applied applied = new ReloadResult.Applied(List.of("elytra"), List.of(rejected), List.of("tickle"), false);

        List<Component> components = ReloadResultMessages.toComponents(applied);

        Assertions.assertEquals(3, components.size());
        assertTranslatable(components.get(0), TitanTranslations.CONFIG_RELOAD_APPLIED);
        assertTranslatable(components.get(1), TitanTranslations.CONFIG_RELOAD_REJECTED);
        assertTranslatable(components.get(2), TitanTranslations.CONFIG_RELOAD_DISABLED);
    }

    @DisplayName("Failed maps to exactly one 'failed' component naming the file and the location")
    @Test
    void failedMapsToASingleFailedComponentNamingFileAndDetail() {
        ReloadResult.Failed failed = new ReloadResult.Failed("application.yaml", "line 3, column 4");

        List<Component> components = ReloadResultMessages.toComponents(failed);

        Assertions.assertEquals(1, components.size());
        TranslatableComponent component = assertTranslatable(components.get(0), TitanTranslations.CONFIG_RELOAD_FAILED);
        Assertions.assertEquals("application.yaml", argumentPlain(component, 0));
        Assertions.assertEquals("line 3, column 4", argumentPlain(component, 1));
    }

    private static TranslatableComponent assertTranslatable(Component component, String key) {
        TranslatableComponent translatable = Assertions.assertInstanceOf(TranslatableComponent.class, component, "expected a translatable component for key " + key);
        Assertions.assertEquals(key, translatable.key());
        return translatable;
    }

    private static String argumentPlain(TranslatableComponent component, int index) {
        Object value = component.arguments().get(index).value();
        Component argument = Assertions.assertInstanceOf(Component.class, value, "expected a component argument at index " + index);
        return PlainTextComponentSerializer.plainText().serialize(argument);
    }
}
