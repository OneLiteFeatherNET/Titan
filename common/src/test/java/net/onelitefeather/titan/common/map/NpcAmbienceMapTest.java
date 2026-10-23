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
package net.onelitefeather.titan.common.map;

import com.google.gson.Gson;
import java.util.List;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.ambience.AmbienceZone;
import net.onelitefeather.titan.core.npc.LobbyNpc;
import net.onelitefeather.titan.core.npc.NpcSkin;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NpcAmbienceMapTest {

    private static final String MAP = """
            {
              "name": "halloween_lobbyleyna",
              "npcs": [
                {
                  "id": "slender",
                  "task": "Slender",
                  "position": { "x": 76.5, "y": 80.0, "z": 157.5, "yaw": -90.0, "pitch": 0.0 },
                  "skin": { "value": "dGV4dHVyZQ==", "signature": "sig" },
                  "lookAtPlayers": true,
                  "permission": "titan.navigator.slender"
                }
              ],
              "ambience": [
                {
                  "id": "slender-tunnel",
                  "area": { "min": { "x": 82, "y": 78, "z": 152 }, "max": { "x": 123, "y": 90, "z": 163 } },
                  "particles": { "type": "white_ash", "count": 3 },
                  "sounds": { "key": "ambient.cave", "minSeconds": 20, "maxSeconds": 60 },
                  "signs": [ { "position": { "x": 98, "y": 82, "z": 152.5 }, "key": "titan.ambience.sign.watching" } ],
                  "pages": [ { "position": { "x": 90.5, "y": 81.2, "z": 151.5 } } ],
                  "apparitions": [ { "position": { "x": 112.5, "y": 80, "z": 157.5 } } ]
                }
              ]
            }
            """;

    private static LobbyMap read(String json) {
        return MapGson.create().fromJson(json, LobbyMap.class);
    }

    @DisplayName("A map file with npcs and ambience reads both lists")
    @Test
    void readsNpcsAndAmbience() {
        LobbyMap map = read(MAP);
        Assertions.assertEquals(1, map.npcs().size(), "one NPC in the file");
        Assertions.assertEquals(1, map.ambience().size(), "one zone in the file");
    }

    @DisplayName("An NPC reads its position with facing, skin, flags and permission")
    @Test
    void readsNpcFields() {
        LobbyNpc npc = read(MAP).npcs().getFirst();
        Assertions.assertEquals("slender", npc.id(), "the id");
        Assertions.assertEquals(new Pos(76.5, 80.0, 157.5, -90f, 0f), npc.position(), "the position with its facing");
        Assertions.assertEquals(new NpcSkin("dGV4dHVyZQ==", "sig"), npc.skin(), "the skin texture and signature");
        Assertions.assertTrue(npc.lookAtPlayers(), "lookAtPlayers is read");
        Assertions.assertEquals("titan.navigator.slender", npc.permission(), "the permission is read");
    }

    @DisplayName("An ambience zone reads its area, particles, sound, sign, page and apparition")
    @Test
    void readsZoneFields() {
        AmbienceZone zone = read(MAP).ambience().getFirst();
        Assertions.assertEquals(new Vec(82, 78, 152), zone.area().min(), "the box starts at its min");
        Assertions.assertEquals(new AmbienceZone.Particles("white_ash", 3), zone.particles(), "the particles");
        Assertions.assertEquals(new AmbienceZone.Sounds("ambient.cave", 20, 60), zone.sounds(), "the sound interval");
        Assertions.assertEquals("titan.ambience.sign.watching", zone.signs().getFirst().key(), "the sign key");
        Assertions.assertEquals(new Vec(90.5, 81.2, 151.5), zone.pages().getFirst(), "the page position");
        Assertions.assertEquals(new Vec(112.5, 80, 157.5), zone.apparitions().getFirst(), "the apparition position");
    }

    @DisplayName("A missing field is read as absent, not as an exception, so the validator can report it")
    @Test
    void missingFieldsAreAbsent() {
        LobbyMap map = read("{\"npcs\":[{\"id\":\"x\"}],\"ambience\":[{\"id\":\"y\",\"area\":{\"min\":{\"x\":0,\"y\":0}}}]}");
        Assertions.assertNull(map.npcs().getFirst().task(), "a missing task stays null");
        Assertions.assertNull(map.npcs().getFirst().position(), "a missing position stays null");
        Assertions.assertNull(map.ambience().getFirst().area(), "an incomplete box stays null");
    }

    @DisplayName("A map without npcs or ambience writes back without those keys")
    @Test
    void emptyPartsAreNotWritten() {
        String json = MapGson.create().toJson(new LobbyMap("world", new Pos(0, 66, 0), List.of()));
        Assertions.assertFalse(json.contains("npcs") || json.contains("ambience"), "no empty npcs or ambience keys: " + json);
    }

    @DisplayName("Writing and reading a map keeps its NPCs and zones")
    @Test
    void roundTripKeepsNpcsAndZones() {
        Gson gson = MapGson.create();
        LobbyMap original = read(MAP);
        LobbyMap again = gson.fromJson(gson.toJson(original), LobbyMap.class);
        Assertions.assertEquals(original.npcs(), again.npcs(), "the NPCs survive a round trip");
        Assertions.assertEquals(original.ambience(), again.ambience(), "the zones survive a round trip");
    }

    @DisplayName("The copy path for setspawn keeps the NPCs and zones of the map")
    @Test
    void copyKeepsNpcsAndZones() {
        LobbyMap original = read(MAP);
        LobbyMap copy = LobbyMap.lobbyMapBuilder(original).spawn(new Pos(1, 66, 1)).build();
        Assertions.assertEquals(original.npcs(), copy.npcs(), "a spawn change must keep the NPCs");
        Assertions.assertEquals(original.ambience(), copy.ambience(), "a spawn change must keep the zones");
    }
}
