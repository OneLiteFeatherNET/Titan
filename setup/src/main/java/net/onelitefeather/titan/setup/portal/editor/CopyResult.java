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
package net.onelitefeather.titan.setup.portal.editor;

import net.onelitefeather.titan.core.portal.Portal;

import java.util.List;

/** What {@code portal copy <world>} did; the command turns it into chat text. */
public sealed interface CopyResult {

    /**
     * Drafts were opened for {@code portals}. {@code added} and {@code replacing} name the adopted
     * ids by whether the target world already has a portal with that id.
     */
    record Copied(String source, List<Portal> portals, List<String> added, List<String> replacing,
                  List<SkippedPortal> skipped) implements CopyResult {
        public Copied {
            portals = List.copyOf(portals);
            added = List.copyOf(added);
            replacing = List.copyOf(replacing);
            skipped = List.copyOf(skipped);
        }
    }

    /** No world of that name with a map file; {@code available} are the names to choose from. */
    record UnknownWorld(String name, List<String> available) implements CopyResult {
        public UnknownWorld {
            available = List.copyOf(available);
        }
    }

    /** The source is the loaded world itself. */
    record SameWorld(String name) implements CopyResult {
    }

    /** The source world has no portals. */
    record NothingToCopy(String source) implements CopyResult {
    }

    /** The source's map file exists but cannot be read; {@code reason} says why. */
    record Unreadable(String source, String reason) implements CopyResult {
    }
}
