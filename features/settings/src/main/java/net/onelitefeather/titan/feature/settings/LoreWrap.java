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
package net.onelitefeather.titan.feature.settings;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a plain text into lore lines. The client draws lore past the screen edge without
 * wrapping, so the menu wraps the texts itself.
 */
final class LoreWrap {

    private LoreWrap() {
    }

    /**
     * Greedily packs the words of {@code text} into lines of at most {@code width} characters.
     * Whitespace is collapsed; a word longer than {@code width} keeps its own line.
     *
     * @return the lines in order, empty when the text is blank
     */
    static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.strip().split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            if (line.isEmpty()) {
                line.append(word);
            } else if (line.length() + 1 + word.length() <= width) {
                line.append(' ').append(word);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }
}
