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
package net.onelitefeather.titan.feature.radio;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PageLayoutTest {

    @Test
    @DisplayName("zero items fill one page")
    void zeroItemsFillOnePage() {
        Assertions.assertEquals(1, PageLayout.pageCount(0), "an empty category still shows one page");
    }

    @Test
    @DisplayName("one item fills one page")
    void oneItemFillsOnePage() {
        Assertions.assertEquals(1, PageLayout.pageCount(1), "a single title needs one page");
    }

    @Test
    @DisplayName("exactly 45 items fill one page")
    void fortyFiveItemsFillOnePage() {
        Assertions.assertEquals(1, PageLayout.pageCount(45), "45 titles fit the 45 content slots");
    }

    @Test
    @DisplayName("46 items need a second page")
    void fortySixItemsNeedTwoPages() {
        Assertions.assertEquals(2, PageLayout.pageCount(46), "the 46th title overflows onto page 2");
    }

    @Test
    @DisplayName("200 items need five pages")
    void twoHundredItemsNeedFivePages() {
        Assertions.assertEquals(5, PageLayout.pageCount(200), "200 titles at 45 per page make 5 pages");
    }

    @Test
    @DisplayName("index 0 sits on page 1 at slot 0")
    void indexZeroIsPageOneSlotZero() {
        Assertions.assertEquals(1, PageLayout.pageOf(0), "the first title is on page 1");
        Assertions.assertEquals(0, PageLayout.slotOf(0), "the first title is in slot 0");
    }

    @Test
    @DisplayName("index 44 sits on page 1 at slot 44")
    void indexFortyFourIsPageOneSlotFortyFour() {
        Assertions.assertEquals(1, PageLayout.pageOf(44), "title 44 is still on page 1");
        Assertions.assertEquals(44, PageLayout.slotOf(44), "title 44 is in the last content slot");
    }

    @Test
    @DisplayName("index 45 sits on page 2 at slot 0")
    void indexFortyFiveIsPageTwoSlotZero() {
        Assertions.assertEquals(2, PageLayout.pageOf(45), "title 45 starts page 2");
        Assertions.assertEquals(0, PageLayout.slotOf(45), "title 45 is in slot 0 of page 2");
    }

    @Test
    @DisplayName("a page above the last one is clamped to the last page")
    void pageAboveLastIsClampedToLast() {
        Assertions.assertEquals(5, PageLayout.clampPage(9, 200), "page 9 of 5 becomes page 5");
    }

    @Test
    @DisplayName("a page below one is clamped to page one")
    void pageBelowOneIsClampedToOne() {
        Assertions.assertEquals(1, PageLayout.clampPage(0, 200), "page 0 becomes page 1");
        Assertions.assertEquals(1, PageLayout.clampPage(-3, 200), "a negative page becomes page 1");
    }

    @Test
    @DisplayName("the control row occupies slots 45 to 53 of the 54-slot window")
    void controlRowIsSlotsFortyFiveToFiftyThree() {
        Assertions.assertEquals(45, PageLayout.CONTROL_ROW_FIRST, "the control row starts at slot 45");
        Assertions.assertEquals(54, PageLayout.SLOT_COUNT, "the window has six rows of nine slots");
        Assertions.assertEquals(9, PageLayout.SLOT_COUNT - PageLayout.CONTROL_ROW_FIRST, "the control row has nine slots, 45 to 53");
        Assertions.assertEquals(PageLayout.PAGE_SIZE, PageLayout.CONTROL_ROW_FIRST, "content ends where the control row starts");
    }

    @Test
    @DisplayName("turning a page keeps the page count and the control row")
    void turningPageKeepsPageCountAndControlRow() {
        int items = 200;
        int countBefore = PageLayout.pageCount(items);
        int controlRowBefore = PageLayout.CONTROL_ROW_FIRST;

        int lastPage = PageLayout.pageCount(items);
        int clampedNext = PageLayout.clampPage(lastPage + 1, items);
        int clampedPrevious = PageLayout.clampPage(0, items);

        Assertions.assertEquals(countBefore, PageLayout.pageCount(items), "moving to a clamped page must not change the page count");
        Assertions.assertEquals(lastPage, clampedNext, "turning past the last page stays on the last page");
        Assertions.assertEquals(1, clampedPrevious, "turning before the first page stays on page 1");
        Assertions.assertEquals(controlRowBefore, PageLayout.CONTROL_ROW_FIRST, "turning a page must not move the control row");
    }
}
