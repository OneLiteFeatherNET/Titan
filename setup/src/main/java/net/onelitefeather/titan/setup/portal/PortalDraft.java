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

import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.Disc;
import net.onelitefeather.titan.core.portal.Portal;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * One player's unsaved work on one portal. Readable everywhere in the module (the preview and the
 * guided flow show it); only the {@link PortalEditor} changes it.
 */
public final class PortalDraft {

    /** The shape kind a draft is heading for. */
    public enum Form {
        BOX, RING
    }

    private final String id;
    private @Nullable Form form;
    private @Nullable Vec corner1;
    private @Nullable Vec corner2;
    private @Nullable Vec centre;
    private @Nullable Vec normal;
    private @Nullable Double radius;
    private @Nullable String task;
    private @Nullable String permission;

    PortalDraft(String id) {
        this.id = id;
    }

    /** A draft holding the values of a saved portal, so edits start from what is there. */
    static PortalDraft of(Portal portal) {
        PortalDraft draft = new PortalDraft(portal.id());
        draft.task = portal.task();
        draft.permission = portal.permission();
        switch (portal.shape()) {
            case Box box -> {
                draft.form = Form.BOX;
                draft.corner1 = box.min();
                draft.corner2 = box.max();
            }
            case Disc disc -> {
                draft.form = Form.RING;
                draft.centre = disc.center();
                draft.radius = disc.radius();
                draft.normal = disc.normal();
            }
        }
        return draft;
    }

    public String id() {
        return id;
    }

    public @Nullable Form form() {
        return form;
    }

    public @Nullable Vec corner1() {
        return corner1;
    }

    public @Nullable Vec corner2() {
        return corner2;
    }

    public @Nullable Vec centre() {
        return centre;
    }

    public @Nullable Vec normal() {
        return normal;
    }

    public @Nullable Double radius() {
        return radius;
    }

    public @Nullable String task() {
        return task;
    }

    public @Nullable String permission() {
        return permission;
    }

    /** What is still needed before {@code save} can succeed, in the order a builder works. */
    public List<Missing> missing() {
        List<Missing> missing = new ArrayList<>();
        if (form == null) {
            missing.add(Missing.FORM);
        } else if (form == Form.BOX) {
            if (corner1 == null) {
                missing.add(Missing.CORNER_1);
            }
            if (corner2 == null) {
                missing.add(Missing.CORNER_2);
            }
        } else {
            if (centre == null) {
                missing.add(Missing.CENTRE);
            }
            if (radius == null) {
                missing.add(Missing.RADIUS);
            }
        }
        if (task == null) {
            missing.add(Missing.TASK);
        }
        return missing;
    }

    public boolean complete() {
        return missing().isEmpty();
    }

    /** The portal this draft describes, or empty while parts are missing. */
    Optional<Portal> toPortal() {
        if (!complete()) {
            return Optional.empty();
        }
        if (form == Form.BOX) {
            Vec min = new Vec(Math.min(corner1.x(), corner2.x()), Math.min(corner1.y(), corner2.y()), Math.min(corner1.z(), corner2.z()));
            Vec max = new Vec(Math.max(corner1.x(), corner2.x()), Math.max(corner1.y(), corner2.y()), Math.max(corner1.z(), corner2.z()));
            return Optional.of(new Portal(id, new Box(min, max), task, permission));
        }
        return Optional.of(new Portal(id, new Disc(centre, radius, normal), task, permission));
    }

    /** Switching form drops what belongs to the other one; the same form keeps everything. */
    void form(Form form) {
        if (this.form == form) {
            return;
        }
        this.form = form;
        this.corner1 = null;
        this.corner2 = null;
        this.centre = null;
        this.normal = null;
        this.radius = null;
    }

    void corner1(Vec corner) {
        form(Form.BOX);
        this.corner1 = corner;
    }

    void corner2(Vec corner) {
        form(Form.BOX);
        this.corner2 = corner;
    }

    void centre(Vec centre, Vec normal) {
        form(Form.RING);
        this.centre = centre;
        this.normal = normal;
    }

    void radius(double radius) {
        form(Form.RING);
        this.radius = radius;
    }

    void task(String task) {
        this.task = task;
    }

    void permission(@Nullable String permission) {
        this.permission = permission;
    }
}
