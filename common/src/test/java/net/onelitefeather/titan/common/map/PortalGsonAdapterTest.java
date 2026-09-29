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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.Disc;
import net.onelitefeather.titan.core.portal.Portal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalGsonAdapterTest {

    /** The contract of design D1, verbatim, wrapped in a map file. */
    private static final String CONTRACT = """
            {"name":"world","portals":[
              {"id": "survival", "task": "Survival", "shape": {"type": "box", "min": {"x": 10, "y": 64, "z": 10}, "max": {"x": 14, "y": 68, "z": 11}}},
              {"id": "elytra-ring", "task": "ElytraRace", "permission": null, "shape": {"type": "disc", "center": {"x": 0.5, "y": 72, "z": 40.5}, "radius": 5.5, "normal": {"x": 0, "y": 0, "z": 1}}}
            ]}""";

    private final Gson gson = MapGson.create();

    private LobbyMap read(String json) {
        return this.gson.fromJson(json, LobbyMap.class);
    }

    private static String map(String portalJson) {
        return "{\"name\":\"world\",\"portals\":[" + portalJson + "]}";
    }

    @DisplayName("The box of the contract is read with its block coordinates")
    @Test
    void readsBox() {
        Portal portal = read(CONTRACT).portals().get(0);

        assertEquals("survival", portal.id());
        assertEquals("Survival", portal.task());
        assertEquals(new Box(new Vec(10, 64, 10), new Vec(14, 68, 11)), portal.shape(), "min and max must be read as Vec");
    }

    @DisplayName("The disc of the contract is read with center, radius and normal")
    @Test
    void readsDisc() {
        Portal portal = read(CONTRACT).portals().get(1);

        assertEquals(new Disc(new Vec(0.5, 72, 40.5), 5.5, new Vec(0, 0, 1)), portal.shape());
    }

    @DisplayName("A missing permission and an explicit null both mean everyone")
    @Test
    void missingAndNullPermissionAreNull() {
        List<Portal> portals = read(CONTRACT).portals();

        assertNull(portals.get(0).permission(), "missing permission");
        assertNull(portals.get(1).permission(), "explicit null permission");
    }

    @DisplayName("A permission is read as given")
    @Test
    void readsPermission() {
        Portal portal = read(map("""
                {"id":"a","task":"T","permission":"titan.portal.a","shape":{"type":"box","min":{"x":0,"y":0,"z":0},"max":{"x":1,"y":1,"z":1}}}""")).portals().get(0);

        assertEquals("titan.portal.a", portal.permission());
    }

    @DisplayName("Unknown fields on the portal and on the shape are ignored")
    @Test
    void ignoresUnknownFields() {
        Portal portal = read(map("""
                {"id":"a","task":"T","color":"red","shape":{"type":"box","note":1,"min":{"x":0,"y":0,"z":0},"max":{"x":1,"y":1,"z":1}}}""")).portals().get(0);

        assertInstanceOf(Box.class, portal.shape(), "a newer file must not break an older lobby");
    }

    @DisplayName("A hand-written normal is normalised when the portal is read")
    @Test
    void normalIsNormalised() {
        Portal portal = read(map("""
                {"id":"a","task":"T","shape":{"type":"disc","center":{"x":0,"y":0,"z":0},"radius":1,"normal":{"x":0,"y":0,"z":5}}}""")).portals().get(0);

        Disc disc = assertInstanceOf(Disc.class, portal.shape());
        assertEquals(1.0, disc.normal().length(), 1e-9, "the lobby normalises the normal on load");
    }

    @DisplayName("A map without portals yields an empty list")
    @Test
    void mapWithoutPortalsIsEmpty() {
        LobbyMap lobbyMap = read("{\"name\":\"world\"}");

        assertTrue(lobbyMap.portals().isEmpty(), "a missing list must read as empty, never null");
    }

    @DisplayName("Reading and writing the contract again yields the same portals")
    @Test
    void roundTripKeepsPortals() {
        LobbyMap original = read(CONTRACT);

        LobbyMap again = read(this.gson.toJson(original));

        assertEquals(original.portals(), again.portals());
    }

    @DisplayName("A written box follows the contract layout")
    @Test
    void writesTheBoxContract() {
        LobbyMap lobbyMap = LobbyMap.lobbyMapBuilder().portals(List.of(new Portal("a", new Box(new Vec(1, 2, 3), new Vec(4, 5, 6)), "T", "perm"))).build();

        JsonObject portal = this.gson.toJsonTree(lobbyMap).getAsJsonObject().getAsJsonArray("portals").get(0).getAsJsonObject();

        assertEquals("a", portal.get("id").getAsString());
        assertEquals("T", portal.get("task").getAsString());
        assertEquals("perm", portal.get("permission").getAsString());
        JsonObject shape = portal.getAsJsonObject("shape");
        assertEquals("box", shape.get("type").getAsString());
        assertEquals(1.0, shape.getAsJsonObject("min").get("x").getAsDouble());
        assertEquals(6.0, shape.getAsJsonObject("max").get("z").getAsDouble());
    }

    @DisplayName("A written disc follows the contract layout")
    @Test
    void writesTheDiscContract() {
        LobbyMap lobbyMap = LobbyMap.lobbyMapBuilder().portals(List.of(new Portal("a", new Disc(new Vec(1, 2, 3), 4.5, new Vec(0, 1, 0)), "T", null))).build();

        JsonObject portal = this.gson.toJsonTree(lobbyMap).getAsJsonObject().getAsJsonArray("portals").get(0).getAsJsonObject();

        JsonElement permission = portal.get("permission");
        assertTrue(permission == null || permission.isJsonNull(), "no permission is written as absent or null");
        JsonObject shape = portal.getAsJsonObject("shape");
        assertEquals("disc", shape.get("type").getAsString());
        assertEquals(4.5, shape.get("radius").getAsDouble());
        assertEquals(1.0, shape.getAsJsonObject("normal").get("y").getAsDouble());
        assertEquals(3.0, shape.getAsJsonObject("center").get("z").getAsDouble());
    }

    @DisplayName("An unknown shape type is a read error that names the type")
    @Test
    void unknownTypeIsAnError() {
        JsonParseException failure = assertThrows(JsonParseException.class, () -> read(map("""
                {"id":"a","task":"T","shape":{"type":"sphere"}}""")));

        assertTrue(failure.getMessage().contains("sphere"), "the message must name the type, was: " + failure.getMessage());
    }

    @DisplayName("A missing required field is a read error")
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"{\"id\":\"a\",\"task\":\"T\"}", "{\"id\":\"a\",\"task\":\"T\",\"shape\":{}}", "{\"id\":\"a\",\"task\":\"T\",\"shape\":{\"type\":\"box\",\"max\":{\"x\":1,\"y\":1,\"z\":1}}}", "{\"id\":\"a\",\"task\":\"T\",\"shape\":{\"type\":\"box\",\"min\":{\"x\":1,\"y\":1}, \"max\":{\"x\":1,\"y\":1,\"z\":1}}}", "{\"id\":\"a\",\"task\":\"T\",\"shape\":{\"type\":\"disc\",\"center\":{\"x\":0,\"y\":0,\"z\":0},\"normal\":{\"x\":0,\"y\":0,\"z\":1}}}", "{\"id\":\"a\",\"task\":\"T\",\"shape\":{\"type\":\"disc\",\"center\":{\"x\":0,\"y\":0,\"z\":0},\"radius\":1}}", "{\"id\":\"a\",\"task\":\"T\",\"shape\":{\"type\":\"disc\",\"radius\":1,\"normal\":{\"x\":0,\"y\":0,\"z\":1}}}"
    })
    void missingRequiredFieldIsAnError(String portalJson) {
        assertThrows(JsonParseException.class, () -> read(map(portalJson)), "a portal without a required field must not load half-built");
    }
}
