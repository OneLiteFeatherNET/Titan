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

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ObjectComponent;
import net.kyori.adventure.text.object.SpriteObjectContents;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class ScoreLabelTest {

    private static boolean hasSlimeSprite(Component component) {
        if (component instanceof ObjectComponent object && object.contents() instanceof SpriteObjectContents sprite && sprite.sprite().equals(Key.key("block/slime_block"))) {
            return true;
        }
        return component.children().stream().anyMatch(ScoreLabelTest::hasSlimeSprite);
    }

    @Test
    void theLabelNamesTheGameAndTheScore() {
        String text = PlainTextComponentSerializer.plainText().serialize(ScoreLabel.render(7));

        assertTrue(text.contains("Jump & Run"), "the title, was: " + text);
        assertTrue(text.endsWith("· 7"), "the score, was: " + text);
    }

    @Test
    void theLabelCarriesTheSlimeBlockSprite() {
        assertTrue(hasSlimeSprite(ScoreLabel.render(7)), "the item icon is part of the text");
    }
}
