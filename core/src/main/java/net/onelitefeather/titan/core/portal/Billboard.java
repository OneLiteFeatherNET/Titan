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
package net.onelitefeather.titan.core.portal;

/**
 * How a label display is oriented: {@code center} turns to each player, {@code fixed} keeps its
 * yaw.
 */
public enum Billboard {
    CENTER("center"), FIXED("fixed"),
    /** Marks an unrecognised value in the map file so {@link PortalValidator} can report it. */
    UNKNOWN("unknown");

    private final String id;

    Billboard(String id) {
        this.id = id;
    }

    /** The spelling used in the map file. */
    public String id() {
        return this.id;
    }

    /** The billboard for a map file value; {@link #UNKNOWN} for anything unrecognised. */
    public static Billboard byId(String id) {
        for (Billboard billboard : values()) {
            if (billboard != UNKNOWN && billboard.id.equals(id)) {
                return billboard;
            }
        }
        return UNKNOWN;
    }
}
