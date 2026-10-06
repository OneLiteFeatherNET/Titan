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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.object.ObjectContents;
import net.onelitefeather.titan.feature.jumprun.course.Mode;
import net.onelitefeather.titan.feature.jumprun.persistence.TopEntry;
import net.onelitefeather.titan.feature.jumprun.persistence.TopThree;

/** What the sidebar of a run shows, as lines from top to bottom; a pure function of its inputs. */
final class RunSidebarContent {

    /** The most lines {@link #lines} returns: score, record, a gap, the header and the places. */
    static final int MAX_LINES = 2 + 1 + 1 + TopThree.SIZE;

    private static final String TITLE_TEMPLATE = RunTitle.SPRITE + " " + RunTitle.MARKUP + " <gray>·</gray> <mode>";

    private final RunMessages messages;

    RunSidebarContent(RunMessages messages) {
        this.messages = messages;
    }

    /** The title is the same in every language, like the score label. */
    static Component title(Mode mode) {
        return MiniMessage.miniMessage().deserialize(TITLE_TEMPLATE, Placeholder.component("mode", mode.label()));
    }

    /**
     * @param top the leaderboard of the mode; without entries the lines end after the record
     */
    List<SidebarLine> lines(int score, OptionalInt best, Optional<TopThree> top, UUID self, Locale locale) {
        List<SidebarLine> lines = new ArrayList<>();
        lines.add(SidebarLine.withValue(messages.sidebarScoreLabel(locale), messages.sidebarScoreValue(locale, score)));
        lines.add(SidebarLine.withValue(messages.sidebarRecordLabel(locale), record(score, best, locale)));
        List<TopEntry> entries = top.map(TopThree::entries).orElse(List.of());
        if (!entries.isEmpty()) {
            lines.add(SidebarLine.textOnly(Component.empty()));
            lines.add(SidebarLine.textOnly(messages.sidebarTopHeader(locale)));
            for (int place = 0; place < entries.size(); place++) {
                lines.add(topLine(place + 1, entries.get(place), self, locale));
            }
        }
        return List.copyOf(lines);
    }

    private Component record(int score, OptionalInt best, Locale locale) {
        if (best.isEmpty()) {
            return score > 0 ? messages.sidebarRecordValue(locale, score) : messages.sidebarNoRecord(locale);
        }
        return messages.sidebarRecordValue(locale, Math.max(score, best.getAsInt()));
    }

    /** The own line is marked with a bold yellow name and a bold score in the medal colour. */
    private SidebarLine topLine(int place, TopEntry entry, UUID self, Locale locale) {
        boolean own = entry.player().equals(self);
        Component head = Component.object(ObjectContents.playerHead(entry.player()));
        Component name = own ? Component.text(entry.name(), NamedTextColor.YELLOW, TextDecoration.BOLD) : Component.text(entry.name());
        Component marker = own ? messages.sidebarSelfMarker(locale) : Component.space();
        Component value = messages.sidebarPlaceValue(locale, place, entry.score());
        return SidebarLine.withValue(messages.sidebarTopEntry(locale, marker, head, name), own ? value.decoration(TextDecoration.BOLD, true) : value);
    }
}
