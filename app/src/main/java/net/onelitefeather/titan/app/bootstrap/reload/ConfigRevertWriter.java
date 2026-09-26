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

/**
 * Restores exactly one module's keys to values a {@link ConfigChangeHandler} already knows from
 * its own snapshot, after that module rejected the new values avaje-config's file watcher just
 * applied - see {@code openspec/changes/config-reload-feature-flags/design.md}, decisions 1 and 2.
 *
 * <p>The production implementation ({@link ConfigChangeBootstrap}) publishes a single
 * {@code Config.eventBuilder("reload-revert")}: every key in {@code puts} is put back to its old
 * value, every key in {@code removals} (a key this reload had newly added for the module) is
 * removed, then the event is published. {@link ConfigChangeHandler} itself ignores any event
 * named {@code "reload-revert"} (see its own javadoc), so this revert never triggers another run
 * of the same handler.
 *
 * <p>A test hands in a fake that records the call instead - no {@code Config} mutator belongs in a
 * unit test (see {@code openspec/changes/avaje-config-facade/design.md}, decision 5).
 */
@FunctionalInterface
public interface ConfigRevertWriter {

    /**
     * @param puts     keys to put back, each with the value it had before the rejected change
     * @param removals keys to remove again - the module's own keys this reload had newly added,
     *                 absent from the snapshot before it
     */
    void revert(Map<String, String> puts, Set<String> removals);
}
