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

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.onelitefeather.titan.app.bootstrap.reload.ReloadResult;
import net.onelitefeather.titan.app.i18n.TitanTranslations;

/**
 * A pure mapper from a {@link ReloadResult} to the translatable {@link Component}s
 * {@link ReloadCommand} sends as its reply - kept separate from the command itself so it can be
 * unit-tested without a {@link net.minestom.server.command.CommandSender} at all. See
 * {@code openspec/changes/config-reload-feature-flags/tasks.md}, task 5.1.
 *
 * <p>A module in a {@link ReloadResult.Applied} result appears in exactly one of three places:
 * named in the single "applied" line's module list (restarted), its own "rejected" line (kept its
 * previous values), or its own "disabled" line (left stopped) - mirroring
 * {@link ReloadResult.Applied}'s own javadoc. If none of the three lists names a module - e.g. a
 * reload that only changed a {@code features.*} flag or another non-module key - that "applied"
 * line would otherwise read as an empty module list; instead this maps the whole result to a
 * single {@link TitanTranslations#CONFIG_RELOAD_APPLIED_NO_MODULES} line.
 */
public final class ReloadResultMessages {

    private ReloadResultMessages() {
    }

    /**
     * @param result the outcome of one {@code ConfigReloader#reload()} run
     * @return one or more translatable components to send as the reply, in the order they should
     *         be sent
     */
    public static List<Component> toComponents(ReloadResult result) {
        return switch (result) {
            case ReloadResult.Unchanged unused ->
                List.of(Component.translatable(TitanTranslations.CONFIG_RELOAD_UNCHANGED));
            case ReloadResult.Applied applied -> appliedComponents(applied);
            case ReloadResult.Failed(String file, String detail) ->
                List.of(Component.translatable(TitanTranslations.CONFIG_RELOAD_FAILED, Component.text(file), Component.text(detail)));
        };
    }

    private static List<Component> appliedComponents(ReloadResult.Applied applied) {
        if (applied.restartedModules().isEmpty() && applied.rejected().isEmpty() && applied.disabledModules().isEmpty()) {
            return List.of(Component.translatable(TitanTranslations.CONFIG_RELOAD_APPLIED_NO_MODULES));
        }

        List<Component> components = new ArrayList<>();
        components.add(Component.translatable(TitanTranslations.CONFIG_RELOAD_APPLIED, Component.text(String.join(", ", applied.restartedModules()))));
        for (ReloadResult.RejectedModule rejected : applied.rejected()) {
            components.add(Component.translatable(
                    TitanTranslations.CONFIG_RELOAD_REJECTED, Component.text(String.join(", ", rejected.keys())), Component.text(rejected.reason())));
        }
        for (String moduleId : applied.disabledModules()) {
            components.add(Component.translatable(TitanTranslations.CONFIG_RELOAD_DISABLED, Component.text(moduleId)));
        }
        return List.copyOf(components);
    }
}
