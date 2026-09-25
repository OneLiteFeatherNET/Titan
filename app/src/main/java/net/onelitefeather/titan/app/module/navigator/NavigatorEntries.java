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
package net.onelitefeather.titan.app.module.navigator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import net.onelitefeather.titan.common.config.ConfigException;
import net.onelitefeather.titan.common.feature.FeatureFlags;

/**
 * Platform-wide registry of {@link NavigatorEntry} instances contributed by modules.
 *
 * <p>This class holds data only - see {@code design.md}, decision 8: turning these entries into an
 * inventory and reacting to clicks on it is the navigator feature module's job, not this
 * registry's.
 * A module never talks to this class directly; it goes through the per-module {@link View} handed
 * out by {@code net.onelitefeather.titan.app.module.ModuleContext#navigator()}, which ties every
 * entry it adds to the module's id and removes them automatically once the module is disabled.
 *
 * <p>{@link #version()} increments on every change (an add or an add that actually removes at least
 * one entry), so a renderer holding a shared inventory knows, without re-reading {@link #entries()}
 * on every open, whether it needs to rebuild. {@link #validate()} is meant to run once, after every
 * module has been enabled, and aborts startup by throwing {@link NavigatorConflictException} if two
 * entries - from the configuration or from any combination of modules - share a slot.
 * {@link #validate(FeatureFlags)} runs the same slot check and, in addition, checks every entry's
 * optional {@link NavigatorEntry#feature()} against a {@link FeatureFlags} source - again for every
 * entry regardless of which module contributed it, not only the ones the {@code navigator} module
 * itself reads from configuration.
 */
public final class NavigatorEntries {

    private record Origin(String moduleId, NavigatorEntry entry) {
    }

    private final List<Origin> origins = new ArrayList<>();
    private final AtomicLong version = new AtomicLong();

    /**
     * Adds {@code entry}, attributed to {@code moduleId}, and increments {@link #version()}.
     *
     * <p>Prefer {@link #forModule(String, Consumer)} over calling this directly - it also wires up
     * automatic removal when the module is disabled.
     *
     * @param moduleId the id of the module contributing {@code entry}
     * @param entry    the entry to add
     */
    public synchronized void add(String moduleId, NavigatorEntry entry) {
        Objects.requireNonNull(moduleId, "moduleId must not be null");
        Objects.requireNonNull(entry, "entry must not be null");
        this.origins.add(new Origin(moduleId, entry));
        this.version.incrementAndGet();
    }

    /**
     * Removes every entry contributed by {@code moduleId}. {@link #version()} only increments if at
     * least one entry was actually removed.
     *
     * @param moduleId the module whose entries to remove
     */
    public synchronized void removeAll(String moduleId) {
        Objects.requireNonNull(moduleId, "moduleId must not be null");
        boolean changed = this.origins.removeIf(origin -> origin.moduleId().equals(moduleId));
        if (changed) {
            this.version.incrementAndGet();
        }
    }

    /**
     * @return a counter that increments on every change to this registry; a renderer compares it
     *         against the version it last built its shared inventory from to know whether a rebuild
     *         is needed
     */
    public long version() {
        return this.version.get();
    }

    /**
     * @return an immutable snapshot of every entry currently registered, ordered by
     *         {@link NavigatorEntry#slot()}
     */
    public synchronized List<NavigatorEntry> entries() {
        return this.origins.stream().map(Origin::entry).sorted(Comparator.comparingInt(NavigatorEntry::slot)).toList();
    }

    /**
     * Checks that no two entries occupy the same slot. Meant to run once, after every module has
     * been enabled.
     *
     * @throws NavigatorConflictException if two entries share a slot; the message names the slot
     *                                    and
     *                                    both entries together with the module that contributed
     *                                    each
     */
    public synchronized void validate() {
        Map<Integer, Origin> bySlot = new HashMap<>();
        for (Origin origin : this.origins) {
            Origin existing = bySlot.putIfAbsent(origin.entry().slot(), origin);
            if (existing != null) {
                throw new NavigatorConflictException(origin.entry().slot(), existing.moduleId(), existing.entry(), origin.moduleId(), origin.entry());
            }
        }
    }

    /**
     * Runs {@link #validate()} (the slot-conflict check), then checks every entry's optional
     * {@link NavigatorEntry#feature()} against {@code featureFlags} - for <em>every</em> entry
     * currently registered, regardless of which module contributed it. That covers both an entry
     * the {@code navigator} module itself read from configuration and one any other module added
     * through its own {@link View#add}: neither ever passed {@code featureFlags} anywhere else, so
     * this is the only place either kind of entry's feature name is checked at all.
     *
     * <p>Meant to run once, after every module has been enabled - alongside the item-placement
     * check in {@code ModuleRegistry#enableAll()} - so an operator misspelling a feature name, in
     * {@code application.yaml} or in a module's own hard-coded entry, aborts startup instead of
     * silently hiding (or always showing) a destination; see
     * {@code openspec/changes/lobby-feature-modules/
     * design.md}, decision 13, and the {@code lobby-navigator} spec's "Unbekannte Flag in der
     * Konfiguration" scenario.
     *
     * @param featureFlags the source of truth for which feature names exist
     * @throws NavigatorConflictException if two entries share a slot; see {@link #validate()}
     * @throws ConfigException            if any entry names a feature {@code featureFlags} does not
     *                                    recognize; the exception's section is the id of the module
     *                                    that contributed the offending entry (so a config-sourced
     *                                    entry, always contributed by the {@code navigator} module
     *                                    itself, produces the same {@code navigator.entries}
     *                                    message
     *                                    an operator would expect), its field is {@code entries},
     *                                    and
     *                                    its reason names both the entry and the unknown flag
     */
    public synchronized void validate(FeatureFlags featureFlags) {
        validate();
        for (Origin origin : this.origins) {
            String feature = origin.entry().feature();
            if (feature != null && !featureFlags.exists(feature)) {
                throw ConfigException.invalid("entries", "entry '" + origin.entry().destination() + "' uses unknown feature flag '" + feature + "'").withSection(origin.moduleId());
            }
        }
    }

    /**
     * Returns {@code moduleId}'s own view of this registry, and queues {@code onDisable} to remove
     * every entry the module adds through it once the module is disabled.
     *
     * @param moduleId  the id of the module the view is for
     * @param onDisable accepts the cleanup action to run when the module is disabled - typically
     *                  {@code ModuleContext::onDisable}
     * @return a view scoped to {@code moduleId}
     */
    public View forModule(String moduleId, Consumer<Runnable> onDisable) {
        Objects.requireNonNull(moduleId, "moduleId must not be null");
        Objects.requireNonNull(onDisable, "onDisable must not be null");
        onDisable.accept(() -> removeAll(moduleId));
        return new View(moduleId);
    }

    /**
     * A single module's own handle to {@link NavigatorEntries}, obtained through
     * {@code ModuleContext#navigator()}. Every entry added through it disappears again once the
     * owning module is disabled - a module never has to remove its own entries by hand.
     */
    public final class View {

        private final String moduleId;

        private View(String moduleId) {
            this.moduleId = moduleId;
        }

        /**
         * Adds {@code entry} to the navigator, attributed to this view's module.
         *
         * @param entry the entry to add
         */
        public void add(NavigatorEntry entry) {
            NavigatorEntries.this.add(this.moduleId, entry);
        }
    }
}
