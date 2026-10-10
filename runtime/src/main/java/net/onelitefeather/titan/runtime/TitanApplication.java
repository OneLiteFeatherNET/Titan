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

import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.onelitefeather.titan.common.deliver.ConnectorStartupCheck;
import net.onelitefeather.titan.common.observability.TitanObservability;
import net.onelitefeather.titan.core.bootstrap.ServerBootstrap;
import net.onelitefeather.titan.runtime.bootstrap.BootstrapSettings;
import net.onelitefeather.titan.runtime.bootstrap.ServerBootstraps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Supplier;


public class TitanApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(TitanApplication.class);

    /** Velocity modern-forwarding secret file, read like Velocity's own forwarding.secret. */
    private static final Path VELOCITY_SECRET_FILE = Path.of("forwarding.secret");

    public static void main(String[] args) {
        // Anything logged before this reaches the console but not Sentry.
        TitanObservability.bootstrap();

        // Also catch Error: a broken application.yaml throws ExceptionInInitializerError;
        // rethrowing would hang on a permission platform's non-daemon threads.
        try {
            Map<String, String> properties = startupProperties();
            List<ServerBootstrap> candidates = ServiceLoader.load(ServerBootstrap.class).stream().map(ServiceLoader.Provider::get).toList();
            TitanStartup.run(ServerBootstraps.select(candidates), velocityAuth(properties), BootstrapSettings.bindHost(properties), BootstrapSettings.bindPort(properties), TitanObservability::installExceptionHandler, active -> new Titan(active).initialize());
        } catch (RuntimeException | Error throwable) {
            LOGGER.error("Titan failed to start: {}", throwable.toString(), throwable);
            System.exit(1);
            return;
        }
        // Extensions install their connector only once the server is started; checking earlier would always see none.
        ConnectorStartupCheck.verify();

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

    /** The system properties the start-up settings read, copied so those settings stay pure. */
    private static Map<String, String> startupProperties() {
        Map<String, String> properties = new HashMap<>();
        for (String key : List.of(BootstrapSettings.SECRET_PROPERTY, BootstrapSettings.HOST_PROPERTY, BootstrapSettings.PORT_PROPERTY)) {
            String value = System.getProperty(key);
            if (value != null) {
                properties.put(key, value);
            }
        }
        return properties;
    }

    /** Velocity modern forwarding is on only with a secret; null keeps Minestom's default auth. */
    private static Auth velocityAuth(Map<String, String> properties) {
        return BootstrapSettings.velocitySecret(VELOCITY_SECRET_FILE, properties).map(Auth.Velocity::new).orElse(null);
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
}
