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

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.LabelSource;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.PortalProblem;
import net.onelitefeather.titan.core.portal.PortalValidator;
import net.onelitefeather.titan.setup.portal.PortalEditResult.Cancelled;
import net.onelitefeather.titan.setup.portal.PortalEditResult.Complete;
import net.onelitefeather.titan.setup.portal.PortalEditResult.Invalid;
import net.onelitefeather.titan.setup.portal.PortalEditResult.LabelUpdated;
import net.onelitefeather.titan.setup.portal.PortalEditResult.Pending;
import net.onelitefeather.titan.setup.portal.PortalEditResult.Rejected;
import net.onelitefeather.titan.setup.portal.PortalEditResult.Removed;
import net.onelitefeather.titan.setup.portal.PortalEditResult.Saved;
import net.onelitefeather.titan.setup.portal.PortalEditResult.Unknown;
import net.onelitefeather.titan.setup.portal.PortalEditResult.Updated;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * The rules of editing portals, free of Minestom commands and files: players are UUIDs, positions
 * and look directions are values. Every edit only changes the player's draft; {@link #save} is the
 * one way into the {@link PortalStore}.
 */
public final class PortalEditor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortalEditor.class);
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_-]+");
    /** Words the command uses in the id position. */
    private static final Set<String> RESERVED_IDS = Set.of("list", "show", "create");
    private static final String NO_PERMISSION = "none";

    private final PortalStore store;
    private final Map<UUID, Map<String, PortalDraft>> drafts = new HashMap<>();

    public PortalEditor(PortalStore store) {
        this.store = store;
    }

    /** Opens a draft that shows the guided flow's next step after every edit. */
    public PortalEditResult create(UUID player, String id) {
        return edit(player, id, draft -> draft.guided(true));
    }

    public PortalEditResult corner1(UUID player, String id, Point position) {
        return edit(player, id, draft -> draft.corner1(blockOf(position)));
    }

    public PortalEditResult corner2(UUID player, String id, Point position) {
        return edit(player, id, draft -> draft.corner2(blockOf(position)));
    }

    public PortalEditResult shape(UUID player, String id, PortalDraft.Form form) {
        return edit(player, id, draft -> draft.form(form));
    }

    /**
     * Sets centre and normal from where the player stands and looks, without touching the radius.
     */
    public PortalEditResult centre(UUID player, String id, Point eye, Vec look) {
        return edit(player, id, draft -> draft.centre(DiscPlacement.centre(eye), DiscPlacement.normal(look)));
    }

    /** Sets only the radius of a ring draft. */
    public PortalEditResult radius(UUID player, String id, double radius) {
        Optional<Invalid> invalid = invalidRadius(radius);
        if (invalid.isPresent()) {
            return invalid.get();
        }
        return edit(player, id, draft -> draft.radius(radius), draft -> draft.form() == PortalDraft.Form.BOX ? Optional.of(new Invalid("a box has no radius, use 'shape ring' or 'centre' first")) : Optional.empty());
    }

    /** Shorthand for {@link #centre} and {@link #radius} in one step. */
    public PortalEditResult disc(UUID player, String id, Point eye, Vec look, double radius) {
        Optional<Invalid> invalid = invalidRadius(radius);
        if (invalid.isPresent()) {
            return invalid.get();
        }
        return edit(player, id, draft -> {
            draft.centre(DiscPlacement.centre(eye), DiscPlacement.normal(look));
            draft.radius(radius);
        });
    }

    public PortalEditResult task(UUID player, String id, String task) {
        if (task == null || task.isBlank()) {
            return new Invalid("the task must not be empty");
        }
        return edit(player, id, draft -> draft.task(task.trim()));
    }

    /** {@code none} removes the permission. */
    public PortalEditResult permission(UUID player, String id, String permission) {
        if (permission == null || permission.isBlank()) {
            return new Invalid("the permission must not be empty, use 'none' to remove it");
        }
        String value = permission.trim();
        return edit(player, id, draft -> draft.permission(NO_PERMISSION.equals(value) ? null : value));
    }

    /** Anchors the label where the player stands, unrounded. */
    public PortalEditResult labelHere(UUID player, String id, Point position) {
        return editLabel(player, id, draft -> draft.labelPosition(new Vec(position.x(), position.y(), position.z())));
    }

    /** The text is MiniMessage; its rules are the validator's, applied on save. */
    public PortalEditResult labelText(UUID player, String id, String text) {
        return editLabelText(player, id, text, "label text", draft -> draft.labelText(text));
    }

    public PortalEditResult labelOffline(UUID player, String id, String text) {
        return editLabelText(player, id, text, "offline text", draft -> draft.labelOffline(text));
    }

    /**
     * Refuses at once what {@code PortalValidator} would reject on save (unknown type, missing
     * name); {@code local} ignores the name.
     */
    public PortalEditResult labelSource(UUID player, String id, String type, @Nullable String name) {
        LabelSource source = LabelSource.of(type, name == null || name.isBlank() ? null : name.trim());
        List<String> reasons = PortalValidator.sourceProblems(source);
        if (!reasons.isEmpty()) {
            return new Invalid(String.join("; ", reasons));
        }
        return editLabel(player, id, draft -> draft.labelSource(source));
    }

    /** Takes every part of the label from the draft; a saved label goes with the next save. */
    public PortalEditResult labelRemove(UUID player, String id) {
        return editLabel(player, id, PortalDraft::removeLabel);
    }

    /** Writes the complete, valid draft to the store; otherwise reports why and keeps the draft. */
    public PortalEditResult save(UUID player, String id) {
        Optional<Invalid> invalid = invalidId(id);
        if (invalid.isPresent()) {
            return invalid.get();
        }
        PortalDraft draft = draftsOf(player).get(id);
        if (draft == null) {
            return new Unknown(id);
        }
        Optional<Portal> portal = draft.toPortal();
        if (portal.isEmpty()) {
            return new Pending(id, draft.missing());
        }
        List<Portal> saved = store.portals();
        int position = indexOf(saved, id);
        List<Portal> next = new ArrayList<>(saved);
        if (position < 0) {
            position = next.size();
            next.add(portal.get());
        } else {
            next.set(position, portal.get());
        }
        int edited = position;
        // The validator is the lobby's own check; only the edited entry's problems concern this save.
        List<PortalProblem> problems = PortalValidator.problems(next).stream().filter(problem -> problem.index() == edited).toList();
        if (!problems.isEmpty()) {
            return new Rejected(id, problems);
        }
        store.save(next);
        LOGGER.info("Saved portal {} in world {}", id, store.world());
        dropDraft(player, id);
        return edited < saved.size() ? new Updated(portal.get()) : new Saved(portal.get());
    }

    /** Discards the player's draft; the saved portal stays. */
    public PortalEditResult cancel(UUID player, String id) {
        Optional<Invalid> invalid = invalidId(id);
        if (invalid.isPresent()) {
            return invalid.get();
        }
        return dropDraft(player, id) ? new Cancelled(id) : new Unknown(id);
    }

    /** Deletes the saved portal and the player's draft of it. */
    public PortalEditResult remove(UUID player, String id) {
        Optional<Invalid> invalid = invalidId(id);
        if (invalid.isPresent()) {
            return invalid.get();
        }
        List<Portal> saved = store.portals();
        boolean wasSaved = indexOf(saved, id) >= 0;
        if (wasSaved) {
            store.save(saved.stream().filter(portal -> !id.equals(portal.id())).toList());
            LOGGER.info("Removed portal {} from world {}", id, store.world());
        }
        boolean hadDraft = dropDraft(player, id);
        return wasSaved || hadDraft ? new Removed(id) : new Unknown(id);
    }

    /** The player's draft with this id, if there is one. */
    public Optional<PortalDraft> draft(UUID player, String id) {
        Map<String, PortalDraft> own = drafts.get(player);
        return own == null ? Optional.empty() : Optional.ofNullable(own.get(id));
    }

    /** The player's open drafts, oldest first. */
    public List<PortalDraft> drafts(UUID player) {
        Map<String, PortalDraft> own = drafts.get(player);
        return own == null ? List.of() : List.copyOf(own.values());
    }

    /** Forgets every draft of the player, for when they leave. */
    public void discardAll(UUID player) {
        drafts.remove(player);
    }

    private PortalEditResult editLabelText(UUID player, String id, String text, String what, Consumer<PortalDraft> change) {
        return text.isBlank() ? new Invalid("the " + what + " must not be empty") : editLabel(player, id, change);
    }

    /** An edit answered with the label's state instead of the generic progress. */
    private PortalEditResult editLabel(UUID player, String id, Consumer<PortalDraft> change) {
        PortalEditResult result = edit(player, id, change);
        if (result instanceof Invalid) {
            return result;
        }
        PortalDraft draft = draftsOf(player).get(id);
        return new LabelUpdated(id, draft.label(), draft.missing());
    }

    private PortalEditResult edit(UUID player, String id, Consumer<PortalDraft> change) {
        return edit(player, id, change, draft -> Optional.empty());
    }

    /** Applies {@code change} unless {@code guard} objects to the draft's current state. */
    private PortalEditResult edit(UUID player, String id, Consumer<PortalDraft> change, Function<PortalDraft, Optional<Invalid>> guard) {
        Optional<Invalid> invalid = invalidId(id);
        if (invalid.isPresent()) {
            return invalid.get();
        }
        PortalDraft draft = draftsOf(player).computeIfAbsent(id, this::startDraft);
        Optional<Invalid> refused = guard.apply(draft);
        if (refused.isPresent()) {
            return refused.get();
        }
        change.accept(draft);
        List<Missing> missing = draft.missing();
        return missing.isEmpty() ? new Complete(id) : new Pending(id, missing);
    }

    private PortalDraft startDraft(String id) {
        return store.portals().stream().filter(portal -> id.equals(portal.id())).findFirst().map(PortalDraft::of).orElseGet(() -> new PortalDraft(id));
    }

    private Map<String, PortalDraft> draftsOf(UUID player) {
        return drafts.computeIfAbsent(player, ignored -> new LinkedHashMap<>());
    }

    private boolean dropDraft(UUID player, String id) {
        Map<String, PortalDraft> own = drafts.get(player);
        if (own == null) {
            return false;
        }
        boolean removed = own.remove(id) != null;
        if (own.isEmpty()) {
            drafts.remove(player);
        }
        return removed;
    }

    private static int indexOf(List<Portal> portals, String id) {
        for (int index = 0; index < portals.size(); index++) {
            if (id.equals(portals.get(index).id())) {
                return index;
            }
        }
        return -1;
    }

    private static Vec blockOf(Point position) {
        return new Vec(Math.floor(position.x()), Math.floor(position.y()), Math.floor(position.z()));
    }

    private static Optional<Invalid> invalidId(String id) {
        if (id == null || !ID_PATTERN.matcher(id).matches()) {
            return Optional.of(new Invalid("the id may only contain a-z, 0-9, '-' and '_'"));
        }
        if (RESERVED_IDS.contains(id)) {
            return Optional.of(new Invalid("'" + id + "' is reserved and cannot be used as an id"));
        }
        return Optional.empty();
    }

    private static Optional<Invalid> invalidRadius(double radius) {
        if (!(radius > 0) || Double.isInfinite(radius)) {
            return Optional.of(new Invalid("the radius must be a number greater than 0"));
        }
        return Optional.empty();
    }
}
