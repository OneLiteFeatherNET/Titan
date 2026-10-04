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
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.spi.ILoggingEvent;
import io.avaje.config.Configuration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The team heads a run reads: a live UUID list whose skins are looked up once, away from the
 * reader.
 */
class HeadProfilesSettingsTest {

    private static final String KEY = "jumprun.heads.profiles";
    private static final UUID ALEX = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
    private static final UUID BOB = UUID.fromString("61699b2e-d327-4a01-9f1e-0ea8c3f06bc6");
    private static final UUID NOBODY = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static Map<UUID, Object> skins() {
        return Map.of(ALEX, FakeHeadSkins.skin("alex"), BOB, FakeHeadSkins.skin("bob"));
    }

    private static JumprunConfig config(Configuration source, FakeHeadSkins skins) {
        return new JumprunConfig(source, new TeamHeads(skins, Runnable::run, new MutableClock()));
    }

    private static Configuration with(String profiles) {
        Configuration source = TestBlocks.shippedConfiguration();
        source.setProperty(KEY, profiles);
        return source;
    }

    private static List<UUID> ids(JumprunConfig config) {
        return config.palettes().heads().stream().map(HeadSkin::id).toList();
    }

    @Test
    void theShippedDefaultIsNoTeamHead() {
        JumprunConfig config = config(TestBlocks.shippedConfiguration(), new FakeHeadSkins(skins()));
        config.readAtStartup();

        assertEquals(List.of(), ids(config), "an empty list shows the plain heads");
    }

    @Test
    void aListOfUuidsIsReadWithItsSkins() {
        JumprunConfig config = config(with(ALEX + ", " + BOB), new FakeHeadSkins(skins()));
        config.readAtStartup();

        assertEquals(List.of(ALEX, BOB), ids(config), "both profiles in the order of the list");
        assertEquals(new HeadSkin(ALEX, "textures-alex", "signature-alex"), config.palettes().heads().getFirst(), "texture and signature come along");
    }

    @Test
    void aYamlListOfUuidsIsRead(@TempDir Path directory) throws IOException {
        Path file = directory.resolve("jumprun.yaml");
        Files.writeString(file, "jumprun:\n  heads:\n    profiles:\n      - " + ALEX + "\n      - " + BOB + "\n");
        Configuration source = Configuration.builder().load(file.toFile()).build();

        assertEquals(List.of(ALEX, BOB), JumprunSettings.headProfiles(source), "a block list as an operator writes it");
    }

    @Test
    void duplicateEntriesCountOnceAndKeepTheOrderOfTheFirst() {
        List<UUID> read = JumprunSettings.headProfiles(with(BOB + "," + ALEX + "," + BOB));

        assertEquals(List.of(BOB, ALEX), read, "each profile once, first occurrence order");
    }

    @Test
    void theSameUuidInAnotherLetterCaseIsADuplicate() {
        List<UUID> read = JumprunSettings.headProfiles(with(ALEX + "," + ALEX.toString().toUpperCase()));

        assertEquals(List.of(ALEX), read, "case does not make another profile");
    }

    @Test
    void anEntryThatIsNoUuidLeavesTheChangeInEffectAndNamesTheKey() {
        Configuration source = with(ALEX.toString());
        JumprunConfig config = config(source, new FakeHeadSkins(skins()));
        config.readAtStartup();

        try (CapturedLog log = new CapturedLog(LiveSetting.class)) {
            source.setProperty(KEY, BOB + ",not-a-uuid");

            assertEquals(List.of(ALEX), ids(config), "the last valid list stays");
            List<ILoggingEvent> warnings = log.warnings();
            assertEquals(1, warnings.size(), "one warning");
            assertTrue(warnings.getFirst().getFormattedMessage().contains(KEY + ": 'not-a-uuid' is not a UUID"), "key and reason: " + warnings.getFirst().getFormattedMessage());
        }
    }

    @Test
    void aWrongEntryAtStartDoesNotAbortTheStartAndShowsPlainHeads() {
        JumprunConfig config = config(with("nonsense"), new FakeHeadSkins(skins()));

        config.readAtStartup();

        assertEquals(List.of(), ids(config), "no team head");
    }

    @Test
    void aUuidWithoutASkinIsSkippedAndNamedInAWarning() {
        JumprunConfig config = config(with(ALEX + "," + NOBODY), new FakeHeadSkins(skins()));

        try (CapturedLog log = new CapturedLog(TeamHeads.class)) {
            config.readAtStartup();

            assertEquals(List.of(ALEX), ids(config), "only the resolvable one");
            assertTrue(log.warnings().stream().anyMatch(line -> line.getFormattedMessage().contains(NOBODY.toString())), "the warning names the uuid");
        }
    }

    @Test
    void aLookupThatFailsIsSkippedLikeAMissingSkin() {
        FakeHeadSkins failing = new FakeHeadSkins(Map.of(ALEX, new IllegalStateException("mojang is down"), BOB, FakeHeadSkins.skin("bob")));
        JumprunConfig config = config(with(ALEX + "," + BOB), failing);

        try (CapturedLog log = new CapturedLog(TeamHeads.class)) {
            config.readAtStartup();

            assertEquals(List.of(BOB), ids(config), "the other one still shows");
            assertTrue(log.warnings().stream().anyMatch(line -> line.getFormattedMessage().contains("mojang is down")), "the reason is logged");
        }
    }

    @Test
    void onlyUnresolvableEntriesGiveAnEmptyList() {
        JumprunConfig config = config(with(NOBODY.toString()), new FakeHeadSkins(skins()));

        config.readAtStartup();

        assertEquals(List.of(), ids(config), "the plain heads show");
    }

    @Test
    void aSkinIsLookedUpOnlyOnceHoweverOftenTheListIsRead() {
        FakeHeadSkins skins = new FakeHeadSkins(skins());
        JumprunConfig config = config(with(ALEX.toString()), skins);
        config.readAtStartup();

        config.palettes();
        config.palettes();

        assertEquals(List.of(ALEX), skins.asked(), "one lookup");
    }

    @Test
    void anUnresolvedSkinIsNotAskedAgainBeforeTheRetryDelay() {
        FakeHeadSkins skins = new FakeHeadSkins(skins());
        MutableClock clock = new MutableClock();
        JumprunConfig config = new JumprunConfig(with(NOBODY.toString()), new TeamHeads(skins, Runnable::run, clock));
        config.readAtStartup();

        clock.advance(TeamHeads.RETRY_AFTER.minusSeconds(1));
        config.palettes();
        config.palettes();

        assertEquals(List.of(NOBODY), skins.asked(), "one lookup, however many runs start meanwhile");
    }

    @Test
    void anUnresolvedSkinIsAskedAgainOnceTheRetryDelayHasPassed() {
        FakeHeadSkins skins = new FakeHeadSkins(skins());
        MutableClock clock = new MutableClock();
        JumprunConfig config = new JumprunConfig(with(NOBODY.toString()), new TeamHeads(skins, Runnable::run, clock));
        config.readAtStartup();

        clock.advance(TeamHeads.RETRY_AFTER);
        config.palettes();

        assertEquals(List.of(NOBODY, NOBODY), skins.asked(), "tried again after the delay");
    }

    @Test
    void aLookupThatRunsOutOfTimeReleasesItsUuidAndIsRetriedAfterTheDelay() {
        FakeHeadSkins skins = new FakeHeadSkins(skins());
        MutableClock clock = new MutableClock();
        List<Runnable> neverRun = new ArrayList<>();
        TeamHeads heads = new TeamHeads(skins, neverRun::add, clock, lookup -> {
            lookup.completeExceptionally(new TimeoutException("hung"));
            return lookup;
        });

        heads.of(List.of(ALEX));
        heads.of(List.of(ALEX));
        clock.advance(TeamHeads.RETRY_AFTER);
        heads.of(List.of(ALEX));

        assertEquals(2, neverRun.size(), "the hung lookup no longer blocks the uuid: one try now, one after the delay, none in between");
    }

    @Test
    void theLookupRunsOnTheExecutorAndNeverInTheReader() {
        FakeHeadSkins skins = new FakeHeadSkins(skins());
        List<Runnable> queued = new ArrayList<>();
        Executor offThread = queued::add;
        JumprunConfig config = new JumprunConfig(with(ALEX.toString()), new TeamHeads(skins, offThread, new MutableClock()));

        config.readAtStartup();

        assertEquals(List.of(), skins.asked(), "nothing looked up while reading");
        assertEquals(List.of(), ids(config), "no skin yet, the plain heads show meanwhile");
        queued.forEach(Runnable::run);
        assertEquals(List.of(ALEX), ids(config), "the skin is there once the lookup has run");
        assertEquals(1, queued.size(), "a second read while the lookup is pending queues nothing");
    }
}
