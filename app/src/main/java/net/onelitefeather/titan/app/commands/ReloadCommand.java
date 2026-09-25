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

import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.util.TriState;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;
import net.onelitefeather.titan.app.bootstrap.reload.ReloadResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reloads the lobby's configuration at runtime: a global command, {@code /titanreload}, with the
 * {@code titan.command.reload} permission - see
 * {@code openspec/changes/config-reload-feature-flags/design.md}, decision 2.
 *
 * <p>Permission is checked the same way {@link net.onelitefeather.titan.app.commands.StopCommand}
 * checks {@code titan.command.stop}: any non-player sender (the console, i.e. CloudNet) may always
 * run it; a {@link Player} needs the permission, resolved via
 * {@link CommandSender#getOrDefault(net.kyori.adventure.pointer.Pointer, Object)} with
 * {@link PermissionChecker#POINTER}. {@link #setCondition} both hides the command from a player
 * without the permission (Minestom never sends it in {@code DeclareCommandsPacket}) and rejects an
 * attempt to run it anyway.
 *
 * <p><b>Testability.</b> Whether a sender counts as "console" (always allowed, permission never
 * checked) is injected as {@code requiresPermission} rather than hardcoded to
 * {@code instanceof Player} inline, so a unit test can force either branch against a plain fake
 * {@link CommandSender} - never a real {@link Player}, which needs a live Minestom server to
 * construct. Production always uses the package-private constructor's default,
 * {@code sender instanceof Player}.
 *
 * <p><b>Triggering a reload.</b> {@code reloader} is a small abstraction - a
 * {@code Supplier<CompletableFuture<ReloadResult>>}, production wiring passes
 * {@code ConfigReloader::reload} - so a test can hand in a fake that never touches the real
 * reload machinery. The reply ({@link ReloadResultMessages#toComponents(ReloadResult)}) is sent
 * once that future completes, on whichever thread completes it (production: the reload's own
 * worker or tick thread, per {@code ConfigReloader}'s own javadoc) - never by blocking the calling
 * thread on {@code .join()}/{@code .get()}.
 *
 * <p><b>The console always gets the English reply.</b> Minestom renders a {@code
 * Component.translatable(...)} per receiver locale only for an actual {@link Player} - and only
 * when {@code ServerFlag.AUTOMATIC_COMPONENT_TRANSLATION} is on (see
 * {@code ComponentTranslationBootstrap}). The console has no receiver locale of its own; instead of
 * leaving it to whatever the host JVM's default locale happens to be, {@code consoleRenderer}
 * renders the reply to {@link Locale#ENGLISH} explicitly, right here, before it is sent - see
 * {@code openspec/changes/config-reload-feature-flags/design.md}, decision 5. A {@link Player}'s
 * reply is left untouched, so Minestom's own per-receiver translation still applies to it.
 */
public final class ReloadCommand extends Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReloadCommand.class);
    private static final String PERMISSION = "titan.command.reload";

    private final Supplier<CompletableFuture<ReloadResult>> reloader;
    private final Predicate<CommandSender> requiresPermission;
    private final UnaryOperator<Component> consoleRenderer;

    public ReloadCommand(Supplier<CompletableFuture<ReloadResult>> reloader) {
        this(reloader, sender -> sender instanceof Player, component -> GlobalTranslator.render(component, Locale.ENGLISH));
    }

    ReloadCommand(Supplier<CompletableFuture<ReloadResult>> reloader, Predicate<CommandSender> requiresPermission) {
        this(reloader, requiresPermission, component -> GlobalTranslator.render(component, Locale.ENGLISH));
    }

    ReloadCommand(
                  Supplier<CompletableFuture<ReloadResult>> reloader, Predicate<CommandSender> requiresPermission, UnaryOperator<Component> consoleRenderer) {
        super("titanreload");
        this.reloader = Objects.requireNonNull(reloader, "reloader");
        this.requiresPermission = Objects.requireNonNull(requiresPermission, "requiresPermission");
        this.consoleRenderer = Objects.requireNonNull(consoleRenderer, "consoleRenderer");
        setCondition(this::canReload);
        setDefaultExecutor((sender, context) -> triggerReload(sender));
    }

    private boolean canReload(@NotNull CommandSender sender, @Nullable String commandString) {
        if (!this.requiresPermission.test(sender)) {
            return true;
        }
        return sender.getOrDefault(PermissionChecker.POINTER, PermissionChecker.always(TriState.FALSE)).test(PERMISSION);
    }

    private void triggerReload(CommandSender sender) {
        // The same predicate canReload() uses to decide whether the permission check applies:
        // it is false for exactly the senders treated as "console" here too - production wires
        // both from the same sender instanceof Player check.
        boolean isConsole = !this.requiresPermission.test(sender);
        this.reloader.get().thenAccept(result -> {
            for (Component component : ReloadResultMessages.toComponents(result)) {
                sender.sendMessage(isConsole ? this.consoleRenderer.apply(component) : component);
            }
        }).exceptionally(error -> {
            LOGGER.error("Configuration reload triggered by {} failed unexpectedly", sender, error);
            return null;
        });
    }
}
