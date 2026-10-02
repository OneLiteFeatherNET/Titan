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
package net.onelitefeather.titan.feature.jumprun;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.EntityMeta;
import net.minestom.server.instance.Instance;

/**
 * A display entity that everyone but the runner sees (until told otherwise), without gravity and
 * physics so it stays
 * where it is put.
 *
 * <p>Minestom may still be loading the chunk when {@link #remove()} is called, and an entity
 * removed by then is registered in the instance once the chunk arrives, with nobody left to remove
 * it. So the removal waits for the placement.
 *
 * <p>Entity calls from the thread of the runner are fine for displays in chunks of other tick
 * threads: Minestom's tracker uses concurrent collections, the viewers are guarded by the
 * entity's own mutex and the dispatcher takes removals through a queue.
 */
final class HiddenDisplay {

    private final Entity entity;
    private final CompletableFuture<Void> placed;
    private final AtomicBoolean visibleToRunner;

    private HiddenDisplay(Entity entity, CompletableFuture<Void> placed, AtomicBoolean visibleToRunner) {
        this.entity = entity;
        this.placed = placed;
        this.visibleToRunner = visibleToRunner;
    }

    static <M extends EntityMeta> HiddenDisplay spawn(Player runner, EntityType type, Class<M> metaType, Consumer<M> meta, Instance instance, Pos position) {
        return spawn(runner, type, metaType, meta, instance, position, false);
    }

    static <M extends EntityMeta> HiddenDisplay spawn(Player runner, EntityType type, Class<M> metaType, Consumer<M> meta, Instance instance, Pos position, boolean visibleToRunner) {
        AtomicBoolean runnerSees = new AtomicBoolean(visibleToRunner);
        Entity display = new Entity(type);
        display.editEntityMeta(metaType, meta);
        display.setNoGravity(true);
        display.setHasPhysics(false);
        display.updateViewableRule(viewer -> viewer != runner || runnerSees.get());
        CompletableFuture<Void> placed = display.setInstance(instance, position);
        return new HiddenDisplay(display, placed, runnerSees);
    }

    /** Lets the runner see the display too, or not; takes effect for the viewers right away. */
    void showToRunner(boolean visible) {
        visibleToRunner.set(visible);
        entity.updateViewableRule();
    }

    Entity entity() {
        return entity;
    }

    /** Runs the action once the display is in the instance, unless it was removed by then. */
    void whenPlaced(Runnable action) {
        placed.thenRun(() -> {
            if (!entity.isRemoved()) {
                action.run();
            }
        });
    }

    /** Removes the display once it is placed; completes when it is gone. */
    CompletableFuture<Void> remove() {
        return placed.thenRun(entity::remove);
    }
}
