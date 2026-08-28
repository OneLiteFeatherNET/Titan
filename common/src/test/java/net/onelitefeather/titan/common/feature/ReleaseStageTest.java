/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.onelitefeather.titan.common.feature;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseStageTest {

    private static final UUID TEAM = UUID.randomUUID();
    private static final UUID LITE = UUID.randomUUID();
    private static final UUID ANYONE = UUID.randomUUID();

    private final TestFeatureAudience audience = new TestFeatureAudience().grantPermission(TEAM, ReleaseStage.INTERNAL_PERMISSION).joinGroup(LITE, ReleaseStage.LITE_GROUP);

    @Test
    @DisplayName("stage ids are read back from their flag file spelling")
    void fromIdReadsTheFlagFileSpelling() {
        assertEquals(Optional.of(ReleaseStage.INTERNAL), ReleaseStage.fromId("internal"));
        assertEquals(Optional.of(ReleaseStage.LITE), ReleaseStage.fromId(" LITE "));
        assertEquals(Optional.of(ReleaseStage.GA), ReleaseStage.fromId("ga"));
    }

    @Test
    @DisplayName("an absent or unknown stage id resolves to nothing, not to a wider audience")
    void fromIdRejectsUnknownValues() {
        assertTrue(ReleaseStage.fromId(null).isEmpty());
        assertTrue(ReleaseStage.fromId("").isEmpty());
        assertTrue(ReleaseStage.fromId("public").isEmpty());
        assertEquals(ReleaseStage.INTERNAL, ReleaseStage.DEFAULT);
    }

    @Test
    @DisplayName("internal admits only the team")
    void internalAdmitsOnlyTheTeam() {
        assertTrue(ReleaseStage.INTERNAL.admits(TEAM, this.audience));
        assertFalse(ReleaseStage.INTERNAL.admits(LITE, this.audience));
        assertFalse(ReleaseStage.INTERNAL.admits(ANYONE, this.audience));
    }

    @Test
    @DisplayName("lite admits the lite group and keeps the team")
    void liteAdmitsTheGroupAndTheTeam() {
        assertTrue(ReleaseStage.LITE.admits(LITE, this.audience));
        assertTrue(ReleaseStage.LITE.admits(TEAM, this.audience));
        assertFalse(ReleaseStage.LITE.admits(ANYONE, this.audience));
    }

    @Test
    @DisplayName("ga admits everyone without asking the permission backend")
    void gaAdmitsEveryone() {
        assertTrue(ReleaseStage.GA.admits(ANYONE, this.audience));
        assertTrue(ReleaseStage.GA.admits(LITE, this.audience));
        assertTrue(ReleaseStage.GA.admits(TEAM, this.audience));
    }
}
