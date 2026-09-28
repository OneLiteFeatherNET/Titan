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
package net.onelitefeather.titan.runtime;

import net.hollowcube.minestom.extensions.ExtensionBootstrap;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.onelitefeather.titan.common.observability.TitanObservability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(TitanApplication.class);

    /** Velocity modern-forwarding secret file, read like Velocity's own forwarding.secret. */
    private static final Path VELOCITY_SECRET_FILE = Path.of("forwarding.secret");

    public static void main(String[] args) {
        // Anything logged before this reaches the console but not Sentry.
        TitanObservability.bootstrap();

        // minestom-extensions loads platform extensions (the CloudNet bridge among
        // them) from the extensions/ folder; running standalone simply loads none.
        ExtensionBootstrap bootstrap = bootstrap();

        // Needs an initialised MinecraftServer, which the line above provides. Replaces
        // Minestom's Throwable::printStackTrace default with SLF4J logging.
        TitanObservability.installExceptionHandler();

        // Also catch Error: a broken application.yaml throws ExceptionInInitializerError;
        // rethrowing would hang on a permission platform's non-daemon threads.
        try {
            Titan titan = new Titan();
            titan.initialize();
        } catch (RuntimeException | Error throwable) {
            LOGGER.error("Titan failed to start: {}", throwable.toString(), throwable);
            System.exit(1);
            return;
        }

        // CloudNet passes the bind address/port via -Dservice.bind.host /
        // -Dservice.bind.port; fall back to the standalone defaults otherwise.
        String bindHost = System.getProperty("service.bind.host", "localhost");
        int bindPort = Integer.getInteger("service.bind.port", 25565);
        bootstrap.start(bindHost, bindPort);

        // Reads console input so locally typed commands and CloudNet's "stop" (written to stdin)
        // reach the server; without this CloudNet can only kill the process after a timeout.
        startConsole();

        // AOT training aid: with -Dtitan.aot.trainSeconds=<n> set, shuts down after start so JVM
        // exit writes the AOT cache; no effect otherwise.
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
        // Velocity modern forwarding: init(Auth) is the only point it can be turned on - Minestom
        // binds the Auth in MinecraftServer.init with no way to change it afterwards.
        return ExtensionBootstrap.init(new Auth.Velocity(secret));
    }

    private static String velocitySecret() {
        if (Files.isRegularFile(VELOCITY_SECRET_FILE)) {
            try {
                String fromFile = Files.readString(VELOCITY_SECRET_FILE).trim();
                if (!fromFile.isBlank()) {
                    return fromFile;
                }
            } catch (IOException ignored) {
            }
        }
        return System.getProperty("minestom.velocity.secret");
    }
}
