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
package net.onelitefeather.titan.feature.portal;

import java.util.Objects;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta.BillboardConstraints;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.Instance;
import net.onelitefeather.titan.core.portal.Billboard;
import net.onelitefeather.titan.core.portal.PortalLabel;
import org.jetbrains.annotations.Nullable;

/** One shared text display in the world; sends its text only when it changed. */
final class LabelDisplay {

    private final Consumer<Component> send;
    private final Runnable despawn;
    private @Nullable Component last;

    LabelDisplay(Consumer<Component> send, Runnable despawn) {
        this.send = Objects.requireNonNull(send, "send");
        this.despawn = Objects.requireNonNull(despawn, "despawn");
    }

    static LabelDisplay spawn(Instance instance, PortalLabel label) {
        Entity entity = new Entity(EntityType.TEXT_DISPLAY);
        boolean fixed = label.billboard() == Billboard.FIXED;
        entity.editEntityMeta(TextDisplayMeta.class, meta -> meta.setBillboardRenderConstraints(fixed ? BillboardConstraints.FIXED : BillboardConstraints.CENTER));
        entity.setNoGravity(true);
        entity.setInstance(instance, new Pos(label.position().x(), label.position().y(), label.position().z(), fixed ? label.yaw() : 0f, 0f)).join();
        return new LabelDisplay(text -> entity.editEntityMeta(TextDisplayMeta.class, meta -> meta.setText(text)), entity::remove);
    }

    /** Returns whether the text differed from the last one sent and was sent. */
    boolean update(Component rendered) {
        if (rendered.equals(this.last)) {
            return false;
        }
        this.last = rendered;
        this.send.accept(rendered);
        return true;
    }

    void remove() {
        this.despawn.run();
    }
}
