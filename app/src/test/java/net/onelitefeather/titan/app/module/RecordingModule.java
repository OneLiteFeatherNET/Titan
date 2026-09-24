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
package net.onelitefeather.titan.app.module;

import java.util.List;
import java.util.function.Consumer;

/**
 * A minimal {@link LobbyModule} for tests: it appends {@code "enable:<id>"} and
 * {@code "disable:<id>"} to a shared log so a test can assert on ordering, and otherwise delegates
 * to whatever behaviour the test supplies.
 */
final class RecordingModule implements LobbyModule {

    private final String id;
    private final List<String> log;
    private final Consumer<ModuleContext> onEnable;
    private final Runnable onDisable;

    RecordingModule(String id, List<String> log) {
        this(id, log, context -> {
        }, () -> {
        });
    }

    RecordingModule(String id, List<String> log, Consumer<ModuleContext> onEnable, Runnable onDisable) {
        this.id = id;
        this.log = log;
        this.onEnable = onEnable;
        this.onDisable = onDisable;
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public void enable(ModuleContext context) {
        this.log.add("enable:" + this.id);
        this.onEnable.accept(context);
    }

    @Override
    public void disable() {
        this.log.add("disable:" + this.id);
        this.onDisable.run();
    }
}
