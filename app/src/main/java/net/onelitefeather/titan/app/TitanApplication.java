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
package net.onelitefeather.titan.app;

import net.hollowcube.minestom.extensions.ExtensionBootstrap;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.onelitefeather.titan.common.permission.TitanPermissionBridge;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;


public class TitanApplication {

    /** Velocity modern-forwarding secret file, read like Velocity's own forwarding.secret. */
    private static final Path VELOCITY_SECRET_FILE = Path.of("forwarding.secret");

    public static void main(String[] args) {
        // minestom-ce-extensions loads platform extensions (the CloudNet bridge among
        // them) from the extensions/ folder; running standalone simply loads none.
        // This replaces the manual MinestomBridgeExtension wiring + .wrapper guard.
        ExtensionBootstrap bootstrap = bootstrap();

        me.lucko.luckperms.minestom.loader.MinestomLoader.get().load().registerShutdownHook().start();

        // Let the CloudNet bridge (running in a separate extension classloader, see the
        // :bridge module) resolve permissions through LuckPerms. Only JDK types cross the
        // classloader boundary via TitanPermissionBridge.
        TitanPermissionBridge.setResolver((playerId, permission) -> {
            User user = LuckPermsProvider.get().getUserManager().getUser(playerId);
            if (user == null) {
                return false;
            }
            return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
        });

        Titan titan = new Titan();
        titan.initialize();

        // CloudNet passes the bind address/port via -Dservice.bind.host /
        // -Dservice.bind.port; fall back to the standalone defaults otherwise.
        String bindHost = System.getProperty("service.bind.host", "localhost");
        int bindPort = Integer.getInteger("service.bind.port", 25565);
        bootstrap.start(bindHost, bindPort);

        // Read console input so commands typed locally - and the "stop" command CloudNet writes
        // to the service's stdin on shutdown - reach the server. Without this the node can only
        // kill the process after a timeout instead of stopping it cleanly. See StopCommand.
        startConsole();

        // AOT training aid: when -Dtitan.aot.trainSeconds=<n> is set, shut down
        // cleanly after the server has started so the JVM exit writes the AOT
        // configuration/cache. No effect in normal operation.
        Long aotTrainSeconds = Long.getLong("titan.aot.trainSeconds");
        if (aotTrainSeconds != null) {
            Thread.ofVirtual().name("titan-aot-trainer").start(() -> {
                try {
                    Thread.sleep(aotTrainSeconds * 1000L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                System.exit(0);
            });
        }
    }

    /**
     * Starts a daemon thread that forwards console input to the command manager's console sender,
     * so locally typed commands and CloudNet's {@code stop} (written to stdin) are executed.
     */
    private static void startConsole() {
        Supplier<String> lineReader = switch (System.console()) {
            case Console console -> console::readLine;
            case null -> {
                var reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
                yield () -> {
                    try {
                        return reader.readLine();
                    } catch (IOException exception) {
                        throw new IOError(exception);
                    }
                };
            }
        };

        CommandManager commandManager = MinecraftServer.getCommandManager();
        Thread.ofPlatform().name("titan-console").daemon(true).start(() -> {
            while (MinecraftServer.isStarted()) {
                String line = lineReader.get();
                if (line == null) {
                    break;
                }
                if (!line.isBlank()) {
                    commandManager.execute(commandManager.getConsoleSender(), line.trim());
                }
            }
        });
    }

    private static ExtensionBootstrap bootstrap() {
        String secret = velocitySecret();
        if (secret == null || secret.isBlank()) {
            // No proxy secret: ExtensionBootstrap initialises Minestom with default auth.
            return ExtensionBootstrap.init();
        }
        // Velocity modern forwarding: initialise Minestom with the secret, then wrap it
        // in the extension bootstrap (whose static init() cannot accept an Auth).
        MinecraftServer server = MinecraftServer.init(new Auth.Velocity(secret));
        try {
            var constructor = ExtensionBootstrap.class.getDeclaredConstructor(MinecraftServer.class);
            constructor.setAccessible(true);
            return constructor.newInstance(server);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to initialise the extension bootstrap", exception);
        }
    }

    /**
     * Resolves the Velocity modern-forwarding secret, preferring a {@code forwarding.secret}
     * file (as Velocity does) and falling back to the {@code -Dminestom.velocity.secret}
     * system property that CloudNet passes.
     */
    private static String velocitySecret() {
        if (Files.isRegularFile(VELOCITY_SECRET_FILE)) {
            try {
                String fromFile = Files.readString(VELOCITY_SECRET_FILE).trim();
                if (!fromFile.isBlank()) {
                    return fromFile;
                }
            } catch (IOException ignored) {
                // fall through to the system property
            }
        }
        return System.getProperty("minestom.velocity.secret");
    }
}
