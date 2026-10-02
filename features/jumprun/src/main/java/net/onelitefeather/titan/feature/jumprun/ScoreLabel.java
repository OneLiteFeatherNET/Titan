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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta.BillboardConstraints;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import org.jetbrains.annotations.Nullable;

/**
 * The score over the head of the runner, for the other players: a text display riding on the
 * runner, so it follows without a tick task. The text is the same in every language, because a
 * display renders one text for all viewers.
 *
 * <p>Not thread-safe by itself: the caller holds the lock of the run.
 */
final class ScoreLabel {

    private static final String TEMPLATE = "<sprite:blocks:block/slime_block> " + RunTitle.MARKUP + " <gray>·</gray> <white><score></white>";

    /** A passenger sits at the top of the head; this lifts the text over the name tag. */
    private static final Vec ABOVE_NAME_TAG = new Vec(0.0, 0.5, 0.0);

    /** Scores are never negative, so this never equals one. */
    private static final int NOTHING_SHOWN = -1;

    private final Player runner;
    @Nullable
    private HiddenDisplay label;
    private int shownScore = NOTHING_SHOWN;

    ScoreLabel(Player runner) {
        this.runner = runner;
    }

    /** Shows the score, creating the label on first use; an unchanged score sends nothing. */
    void show(int score) {
        if (shownScore == score) {
            return;
        }
        shownScore = score;
        Component text = render(score);
        if (label == null) {
            label = spawn(text);
        } else {
            label.entity().editEntityMeta(TextDisplayMeta.class, meta -> meta.setText(text));
        }
    }

    static Component render(int score) {
        return MiniMessage.miniMessage().deserialize(TEMPLATE, Placeholder.unparsed("score", Integer.toString(score)));
    }

    void remove() {
        if (label != null) {
            label.remove();
            label = null;
            shownScore = NOTHING_SHOWN;
        }
    }

    private HiddenDisplay spawn(Component text) {
        HiddenDisplay display = HiddenDisplay.spawn(runner, EntityType.TEXT_DISPLAY, TextDisplayMeta.class, meta -> {
            meta.setText(text);
            meta.setBillboardRenderConstraints(BillboardConstraints.CENTER);
            meta.setTranslation(ABOVE_NAME_TAG);
        }, runner.getInstance(), runner.getPosition());
        display.whenPlaced(() -> runner.addPassenger(display.entity()));
        return display;
    }
}
