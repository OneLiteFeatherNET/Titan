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
package net.onelitefeather.titan.setup.portal;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta.BillboardConstraints;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.Instance;
import net.onelitefeather.titan.core.portal.Billboard;
import net.onelitefeather.titan.core.portal.LabelText;
import net.onelitefeather.titan.core.portal.PortalLabel;
import net.onelitefeather.titan.core.portal.PortalValidator;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Text preview of the label in a player's draft: one real text display per player and portal,
 * visible to that player only, rendered by the same {@link LabelText} the lobby uses with sample
 * counts. The display lives until {@link #clear} or until the draft has no label.
 */
public final class LabelPreview {

    static final String SAMPLE_ONLINE = "12";
    static final String SAMPLE_MAX = "50";

    private record Key(UUID player, String id) {
    }

    private static final class State {
        private boolean offline;
        private @Nullable Entity entity;
        private @Nullable Component text;
    }

    private final Map<Key, State> states = new ConcurrentHashMap<>();

    /** Chooses the variant the portal's preview shows; it applies from the next {@link #follow}. */
    public void offline(UUID player, String id, boolean offline) {
        states.computeIfAbsent(new Key(player, id), key -> new State()).offline = offline;
    }

    /**
     * Brings the display in line with the draft. Returns the problem when the shown text is
     * invalid; the display then keeps its last valid text.
     */
    public Optional<String> follow(Player player, PortalDraft draft) {
        LabelDraft label = draft.label();
        if (!label.isSet()) {
            clear(player.getUuid(), draft.id());
            return Optional.empty();
        }
        State state = states.computeIfAbsent(new Key(player.getUuid(), draft.id()), key -> new State());
        Instance instance = player.getInstance();
        if (label.position() == null || label.text() == null || instance == null) {
            despawn(state);
            return Optional.empty();
        }
        boolean offlineText = state.offline && label.offlineText() != null;
        String field = offlineText ? "label.offlineText" : "label.text";
        List<String> problems = PortalValidator.textProblems(field, offlineText ? label.offlineText() : label.text());
        if (!problems.isEmpty()) {
            return Optional.of(String.join("; ", problems));
        }
        PortalLabel portalLabel = label.toLabel();
        String task = draft.task() != null ? draft.task() : draft.id();
        Component text = state.offline ? LabelText.render(portalLabel, task, "0", "0", true) : LabelText.render(portalLabel, task, SAMPLE_ONLINE, SAMPLE_MAX, false);
        show(player, state, instance, label, text);
        return Optional.empty();
    }

    /** Removes the preview of one portal and forgets its variant. */
    public void clear(UUID player, String id) {
        State state = states.remove(new Key(player, id));
        if (state != null) {
            despawn(state);
        }
    }

    /** Removes every preview of the player, for when they leave. */
    public void clear(UUID player) {
        states.keySet().stream().filter(key -> key.player().equals(player)).forEach(key -> clear(key.player(), key.id()));
    }

    /** The display of this player's preview of the portal, if one is shown. */
    public Optional<Entity> entity(UUID player, String id) {
        State state = states.get(new Key(player, id));
        return state == null ? Optional.empty() : Optional.ofNullable(state.entity);
    }

    /** Displays currently shown, for tests that check nothing leaks. */
    public int shown() {
        return (int) states.values().stream().filter(state -> state.entity != null).count();
    }

    private static void show(Player player, State state, Instance instance, LabelDraft label, Component text) {
        boolean fixed = label.billboard() == Billboard.FIXED;
        Vec at = label.position();
        Pos target = new Pos(at.x(), at.y(), at.z(), fixed ? label.yaw() : 0f, 0f);
        Entity entity = state.entity;
        if (entity != null && entity.getInstance() != instance) {
            despawn(state);
            entity = null;
        }
        if (entity == null) {
            entity = new Entity(EntityType.TEXT_DISPLAY);
            // Per-player visibility is Minestom's own: no automatic viewers, only the editor.
            entity.setAutoViewable(false);
            entity.setNoGravity(true);
            entity.setInstance(instance, target).join();
            entity.addViewer(player);
            state.entity = entity;
        } else if (!entity.getPosition().equals(target)) {
            entity.teleport(target).join();
        }
        BillboardConstraints constraints = fixed ? BillboardConstraints.FIXED : BillboardConstraints.CENTER;
        entity.editEntityMeta(TextDisplayMeta.class, meta -> {
            meta.setBillboardRenderConstraints(constraints);
            if (!text.equals(state.text)) {
                meta.setText(text);
            }
        });
        state.text = text;
    }

    private static void despawn(State state) {
        if (state.entity != null) {
            state.entity.remove();
            state.entity = null;
            state.text = null;
        }
    }
}
