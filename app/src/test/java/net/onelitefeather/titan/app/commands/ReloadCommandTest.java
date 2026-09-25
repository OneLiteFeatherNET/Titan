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
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.util.TriState;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.tag.TagHandler;
import net.onelitefeather.titan.app.bootstrap.reload.ReloadResult;
import net.onelitefeather.titan.app.i18n.TitanTranslations;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link ReloadCommand}: the {@code titan.command.reload} permission gate (fake
 * {@link PermissionChecker}, no real {@link net.minestom.server.entity.Player}) and the reply it
 * sends once its (fake) reloader's future completes - see
 * {@code openspec/changes/config-reload-feature-flags/tasks.md}, task 5.1.
 */
class ReloadCommandTest {

    @DisplayName("A sender the command treats as console may always run it, regardless of any permission checker")
    @Test
    void consoleMayAlwaysRun() {
        FakeSender console = new FakeSender(PermissionChecker.always(TriState.FALSE));
        ReloadCommand command = new ReloadCommand(() -> CompletableFuture.completedFuture(new ReloadResult.Unchanged()), sender -> false);

        Assertions.assertTrue(condition(command).canUse(console, "titanreload"));
    }

    @DisplayName("A sender the command treats as a player without the permission cannot run it and it is hidden")
    @Test
    void playerWithoutPermissionCannotRunIt() {
        FakeSender player = new FakeSender(PermissionChecker.always(TriState.FALSE));
        ReloadCommand command = new ReloadCommand(() -> CompletableFuture.completedFuture(new ReloadResult.Unchanged()), sender -> true);

        Assertions.assertFalse(condition(command).canUse(player, "titanreload"));
    }

    @DisplayName("A sender the command treats as a player with the permission can run it")
    @Test
    void playerWithPermissionCanRunIt() {
        FakeSender player = new FakeSender(PermissionChecker.always(TriState.TRUE));
        ReloadCommand command = new ReloadCommand(() -> CompletableFuture.completedFuture(new ReloadResult.Unchanged()), sender -> true);

        Assertions.assertTrue(condition(command).canUse(player, "titanreload"));
    }

    @DisplayName("Executing it sends the translated reply once the reloader's future completes")
    @Test
    void executingSendsTheTranslatedReplyOnceTheFutureCompletes() {
        CompletableFuture<ReloadResult> future = new CompletableFuture<>();
        FakeSender console = new FakeSender(PermissionChecker.always(TriState.FALSE));
        ReloadCommand command = new ReloadCommand(() -> future, sender -> false);

        executor(command).apply(console, null);
        Assertions.assertTrue(console.sent.isEmpty(), "nothing must be sent before the future completes");

        future.complete(new ReloadResult.Unchanged());

        Assertions.assertEquals(1, console.sent.size());
        TranslatableComponent component = Assertions.assertInstanceOf(TranslatableComponent.class, console.sent.get(0));
        Assertions.assertEquals(TitanTranslations.CONFIG_RELOAD_UNCHANGED, component.key());
    }

    @DisplayName("Executing it triggers exactly one reloader call per invocation")
    @Test
    void executingTriggersExactlyOneReloaderCallPerInvocation() {
        List<CompletableFuture<ReloadResult>> calls = new ArrayList<>();
        FakeSender console = new FakeSender(PermissionChecker.always(TriState.FALSE));
        ReloadCommand command = new ReloadCommand(() -> {
            CompletableFuture<ReloadResult> call = CompletableFuture.completedFuture(new ReloadResult.Unchanged());
            calls.add(call);
            return call;
        }, sender -> false);

        executor(command).apply(console, null);

        Assertions.assertEquals(1, calls.size());
    }

    private static CommandCondition condition(ReloadCommand command) {
        return command.getCondition();
    }

    private static net.minestom.server.command.builder.CommandExecutor executor(ReloadCommand command) {
        return command.getDefaultExecutor();
    }

    /**
     * A minimal, test-only {@link CommandSender} carrying a fixed {@link PermissionChecker} under
     * {@link PermissionChecker#POINTER} and recording every component sent to it - never a real
     * {@link net.minestom.server.entity.Player}, so this test never needs a Minestom {@code Env}.
     */
    private static final class FakeSender implements CommandSender {

        private final PermissionChecker permissionChecker;
        private final TagHandler tagHandler = TagHandler.newHandler();
        final List<Component> sent = new ArrayList<>();

        FakeSender(PermissionChecker permissionChecker) {
            this.permissionChecker = permissionChecker;
        }

        @Override
        public void sendMessage(Component message) {
            this.sent.add(message);
        }

        @Override
        public TagHandler tagHandler() {
            return this.tagHandler;
        }

        @Override
        public Identity identity() {
            return Identity.nil();
        }

        @Override
        public Pointers pointers() {
            return Pointers.builder().withStatic(PermissionChecker.POINTER, this.permissionChecker).build();
        }
    }
}
