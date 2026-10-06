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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ObjectComponent;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.onelitefeather.titan.feature.jumprun.course.Mode;
import net.onelitefeather.titan.feature.jumprun.persistence.TopEntry;
import net.onelitefeather.titan.feature.jumprun.persistence.TopThree;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RunSidebarContentTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID ALEX = new UUID(0, 1);
    private static final UUID STEVE = new UUID(0, 2);
    private static final UUID NOTCH = new UUID(0, 3);
    private static final UUID JEB = new UUID(0, 4);

    private RunMessages messages;
    private RunSidebarContent content;

    @BeforeEach
    void register() {
        messages = new RunMessages();
        messages.register();
        content = new RunSidebarContent(messages);
    }

    @AfterEach
    void unregister() {
        messages.close();
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static String value(SidebarLine line) {
        return plain(line.value().orElseThrow(() -> new AssertionError("no value on " + plain(line.text()))));
    }

    private static List<String> rows(List<SidebarLine> lines) {
        return lines.stream().map(line -> plain(line.text()) + line.value().map(value -> "|" + plain(value)).orElse("")).toList();
    }

    private static boolean hasHeadOf(Component component, UUID player) {
        if (component instanceof ObjectComponent object && object.contents() instanceof PlayerHeadObjectContents head && player.equals(head.id())) {
            return true;
        }
        return component.children().stream().anyMatch(child -> hasHeadOf(child, player)) || (component instanceof TranslatableComponent translatable && translatable.arguments().stream().anyMatch(argument -> hasHeadOf(argument.asComponent(), player)));
    }

    private static Optional<TopThree> board() {
        return Optional.of(TopThree.EMPTY.with(new TopEntry(ALEX, "Alex", 88, T0)).with(new TopEntry(STEVE, "Steve", 61, T0)).with(new TopEntry(NOTCH, "Notch", 42, T0)));
    }

    /** A run of text with the colour and boldness it ends up with after style inheritance. */
    private record Span(String text, TextColor colour, boolean bold) {
    }

    private static List<Span> spans(Component component) {
        List<Span> out = new ArrayList<>();
        collect(component, Style.empty(), out);
        return out;
    }

    private static void collect(Component component, Style inherited, List<Span> out) {
        Style style = inherited.merge(component.style());
        if (component instanceof TextComponent text && !text.content().isEmpty()) {
            out.add(new Span(text.content(), style.color(), style.decoration(TextDecoration.BOLD) == TextDecoration.State.TRUE));
        }
        component.children().forEach(child -> collect(child, style, out));
    }

    private static Span span(Component line, String text) {
        return spans(line).stream().filter(s -> s.text().contains(text)).findFirst().orElseThrow(() -> new AssertionError("no span with " + text + " in " + plain(line)));
    }

    private static final TextColor LIGHT = TextColor.fromHexString("#E0E0E0");
    private static final TextColor GOLD = TextColor.fromHexString("#FFD700");
    private static final TextColor BRAND_GREEN = TextColor.fromHexString("#7CFC00");

    @Test
    void scoreAndRecordSplitIntoLabelAndValueColumn() {
        List<SidebarLine> lines = content.lines(7, OptionalInt.of(42), Optional.empty(), JEB, Locale.US);
        assertEquals(List.of("Score|7", "Record|42"), rows(lines), "label left, value right");
    }

    @Test
    void labelsAreLightAndNotGray() {
        List<SidebarLine> lines = content.lines(0, OptionalInt.of(30), Optional.empty(), JEB, Locale.US);
        assertEquals(LIGHT, span(lines.get(0).text(), "Score").colour(), "score label light");
        assertEquals(LIGHT, span(lines.get(1).text(), "Record").colour(), "record label light");
    }

    @Test
    void scoreValueIsBrandGreen() {
        SidebarLine line = content.lines(29, OptionalInt.of(30), Optional.empty(), JEB, Locale.US).get(0);
        assertEquals(BRAND_GREEN, span(line.value().orElseThrow(), "29").colour(), "score value green");
    }

    @Test
    void recordValueIsGold() {
        SidebarLine line = content.lines(0, OptionalInt.of(30), Optional.empty(), JEB, Locale.US).get(1);
        assertEquals(GOLD, span(line.value().orElseThrow(), "30").colour(), "record value gold");
    }

    @Test
    void missingRecordDashIsDarkGrayInTheValueColumn() {
        SidebarLine line = content.lines(0, OptionalInt.empty(), Optional.empty(), JEB, Locale.US).get(1);
        assertEquals("–", value(line), "dash instead of a number");
        assertEquals(NamedTextColor.DARK_GRAY, span(line.value().orElseThrow(), "–").colour(), "dash dark gray");
    }

    @Test
    void recordFollowsTheScoreWhenItExceedsTheBest() {
        assertEquals("50", value(content.lines(50, OptionalInt.of(42), Optional.empty(), JEB, Locale.US).get(1)), "new record shows at once");
    }

    @Test
    void recordFollowsTheScoreWithoutAStoredRecord() {
        assertEquals("3", value(content.lines(3, OptionalInt.empty(), Optional.empty(), JEB, Locale.US).get(1)), "first score is the record");
    }

    @Test
    void withoutLeaderboardOnlyScoreAndRecordShow() {
        assertEquals(List.of("Score|7", "Record|42"), rows(content.lines(7, OptionalInt.of(42), Optional.empty(), JEB, Locale.US)), "only score and record");
    }

    @Test
    void anEmptyLeaderboardShowsNothingBelowTheRecord() {
        assertEquals(2, content.lines(0, OptionalInt.empty(), Optional.of(TopThree.EMPTY), JEB, Locale.US).size(), "no spacer, no header");
    }

    @Test
    void aSpacerAndTheHeaderFollowTheRecordWhenThereAreEntries() {
        List<SidebarLine> lines = content.lines(0, OptionalInt.of(30), board(), JEB, Locale.US);
        assertEquals(List.of("", "Top 3"), rows(lines.subList(2, 4)), "spacer then header, both without a value");
    }

    @Test
    void headerStartsTheBrandGradient() {
        SidebarLine header = content.lines(0, OptionalInt.of(30), board(), JEB, Locale.US).get(3);
        assertEquals(BRAND_GREEN, spans(header.text()).getFirst().colour(), "gradient starts in brand green");
    }

    @Test
    void topLinesShowNameLeftAndScoreRight() {
        List<String> rows = rows(content.lines(0, OptionalInt.of(30), board(), JEB, Locale.US)).subList(4, 7);
        assertEquals(List.of(" " + SidebarPackets.HEAD + " Alex|88", " " + SidebarPackets.HEAD + " Steve|61", " " + SidebarPackets.HEAD + " Notch|42"), rows, "names and scores of the top three");
    }

    @Test
    void everyTopLineCarriesTheHeadOfItsPlayer() {
        List<SidebarLine> lines = content.lines(0, OptionalInt.of(30), board(), JEB, Locale.US);
        assertTrue(hasHeadOf(lines.get(4).text(), ALEX), "head of Alex");
        assertTrue(hasHeadOf(lines.get(5).text(), STEVE), "head of Steve");
        assertTrue(hasHeadOf(lines.get(6).text(), NOTCH), "head of Notch");
    }

    @Test
    void scoresAreColouredByMedalInTheValueColumn() {
        List<SidebarLine> lines = content.lines(0, OptionalInt.of(30), board(), JEB, Locale.US);
        assertEquals(GOLD, span(lines.get(4).value().orElseThrow(), "88").colour(), "gold");
        assertEquals(TextColor.fromHexString("#C0C0C0"), span(lines.get(5).value().orElseThrow(), "61").colour(), "silver");
        assertEquals(TextColor.fromHexString("#CD7F32"), span(lines.get(6).value().orElseThrow(), "42").colour(), "bronze");
    }

    @Test
    void namesAreWhite() {
        SidebarLine line = content.lines(0, OptionalInt.of(30), board(), JEB, Locale.US).get(4);
        assertEquals(NamedTextColor.WHITE, span(line.text(), "Alex").colour(), "name white");
    }

    @Test
    void ownLineHasTheMarkerABoldYellowNameAndABoldMedalScore() {
        SidebarLine line = content.lines(0, OptionalInt.of(61), board(), STEVE, Locale.US).get(5);
        assertTrue(plain(line.text()).startsWith("»"), "marker in front");
        assertEquals(NamedTextColor.YELLOW, span(line.text(), "»").colour(), "marker yellow");
        Span name = span(line.text(), "Steve");
        assertEquals(NamedTextColor.YELLOW, name.colour(), "own name yellow");
        assertTrue(name.bold(), "own name bold");
        Span score = span(line.value().orElseThrow(), "61");
        assertEquals(TextColor.fromHexString("#C0C0C0"), score.colour(), "score keeps the medal colour");
        assertTrue(score.bold(), "own score bold");
    }

    @Test
    void otherLinesHaveNoMarkerAndNothingBold() {
        SidebarLine line = content.lines(0, OptionalInt.of(61), board(), STEVE, Locale.US).get(4);
        assertFalse(plain(line.text()).contains("»"), "no marker");
        assertFalse(spans(line.text()).stream().anyMatch(Span::bold), "name not bold");
        assertFalse(spans(line.value().orElseThrow()).stream().anyMatch(Span::bold), "score not bold");
    }

    @Test
    void namesAreInsertedAsPlainTextNotMarkup() {
        Optional<TopThree> evil = Optional.of(TopThree.EMPTY.with(new TopEntry(ALEX, "<red>Al", 1, T0)));
        SidebarLine line = content.lines(0, OptionalInt.empty(), evil, JEB, Locale.US).get(4);
        assertTrue(plain(line.text()).contains("<red>Al"), "name kept literally");
        assertEquals(NamedTextColor.WHITE, span(line.text(), "<red>Al").colour(), "no markup parsed");
    }

    @Test
    void germanLocaleRendersGermanLabels() {
        assertEquals(List.of("Score|2", "Rekord|5"), rows(content.lines(2, OptionalInt.of(5), Optional.empty(), JEB, Locale.GERMANY)), "German labels");
    }

    @Test
    void unknownLocaleFallsBackToEnglish() {
        assertEquals(List.of("Score|2", "Record|5"), rows(content.lines(2, OptionalInt.of(5), Optional.empty(), JEB, Locale.JAPAN)), "English fallback");
    }

    @Test
    void titleStartsWithTheSlimeSpriteAndNamesTheGameAndTheMode() {
        Component title = RunSidebarContent.title(Mode.HARD);
        assertTrue(plain(title).endsWith("Jump & Run · Hard"), "game and mode");
        assertTrue(title.children().stream().anyMatch(child -> child instanceof ObjectComponent), "sprite in front");
    }
}
