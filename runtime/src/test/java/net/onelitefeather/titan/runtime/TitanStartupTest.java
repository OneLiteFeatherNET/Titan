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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.ArrayList;
import java.util.List;
import net.minestom.server.Auth;
import net.onelitefeather.titan.core.bootstrap.ServerBootstrap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class TitanStartupTest {

    private static ServerBootstrap recordingBootstrap(List<String> calls) {
        return new ServerBootstrap() {
            @Override
            public String name() {
                return "extensions";
            }

            @Override
            public void init(Auth auth) {
                calls.add("init");
            }

            @Override
            public void start(String host, int port) {
                calls.add("start " + host + ":" + port);
            }

            @Override
            public List<String> loadedExtensions() {
                return List.of();
            }
        };
    }

    @DisplayName("The server is initialised, the exception handler installed, the scope built and only then the server started")
    @Test
    void stepsRunInInitHandlerScopeStartOrder() {
        List<String> calls = new ArrayList<>();
        ServerBootstrap bootstrap = recordingBootstrap(calls);

        TitanStartup.run(bootstrap, null, "localhost", 25565, () -> calls.add("handler"), ignored -> calls.add("scope"));

        Assertions.assertEquals(List.of("init", "handler", "scope", "start localhost:25565"), calls);
    }

    @DisplayName("A failing scope build never starts the server")
    @Test
    void failingScopeSkipsStart() {
        List<String> calls = new ArrayList<>();
        ServerBootstrap bootstrap = recordingBootstrap(calls);

        Assertions.assertThrows(IllegalStateException.class, () -> TitanStartup.run(bootstrap, null, "localhost", 25565, () -> {
        }, ignored -> {
            throw new IllegalStateException("column missing");
        }));

        Assertions.assertFalse(calls.stream().anyMatch(call -> call.startsWith("start")), "the server must not start, was: " + calls);
    }

    @DisplayName("The start log names the active server bootstrap")
    @Test
    void startLogNamesTheActiveBootstrap() {
        Logger logger = (Logger) LoggerFactory.getLogger(TitanStartup.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            TitanStartup.run(recordingBootstrap(new ArrayList<>()), null, "localhost", 25565, () -> {
            }, ignored -> {
            });
        } finally {
            logger.detachAppender(appender);
        }

        Assertions.assertEquals(1, appender.list.size(), "must log exactly one line");
        ILoggingEvent event = appender.list.get(0);
        Assertions.assertEquals("Server bootstrap: {}", event.getMessage());
        Assertions.assertArrayEquals(new Object[]{"extensions"}, event.getArgumentArray());
        Assertions.assertEquals(Level.INFO, event.getLevel());
    }
}
