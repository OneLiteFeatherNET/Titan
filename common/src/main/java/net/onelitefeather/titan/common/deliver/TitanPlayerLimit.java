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
package net.onelitefeather.titan.common.deliver;

import java.util.function.IntConsumer;

/**
 * Cross-classloader holder for the lobby's player limit. Like {@link TitanLobbyIdentity} it lives
 * on the shared application classloader and exchanges only JDK types. CloudNet's bridge reads the
 * limit once, while the extensions load and before any Titan bean exists, so the bridge extension
 * registers a listener here and the application announces its configured limit when it is ready.
 */
public final class TitanPlayerLimit {

    private static volatile Integer limit;
    private static volatile IntConsumer listener;

    private TitanPlayerLimit() {
    }

    /** Announces the limit to the registered listener, if any. */
    public static void announce(int maxPlayers) {
        limit = maxPlayers;
        IntConsumer current = listener;
        if (current != null) {
            current.accept(maxPlayers);
        }
    }

    /** Registers the single listener; an already announced limit is delivered right away. */
    public static void onAnnounce(IntConsumer newListener) {
        listener = newListener;
        Integer announced = limit;
        if (announced != null) {
            newListener.accept(announced);
        }
    }

    public static void clear() {
        limit = null;
        listener = null;
    }
}
