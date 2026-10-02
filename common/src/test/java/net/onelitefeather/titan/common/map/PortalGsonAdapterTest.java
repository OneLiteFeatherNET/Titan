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
import net.onelitefeather.titan.core.portal.Billboard;
import net.onelitefeather.titan.core.portal.Disc;
import net.onelitefeather.titan.core.portal.LabelSource;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.PortalLabel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

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

    private static final String BOX_SHAPE = "\"shape\":{\"type\":\"box\",\"min\":{\"x\":0,\"y\":0,\"z\":0},\"max\":{\"x\":1,\"y\":1,\"z\":1}}";

    private static String portalWithLabel(String labelJson) {
        return "{\"id\":\"a\",\"task\":\"T\"," + BOX_SHAPE + ",\"label\":" + labelJson + "}";
    }

    private PortalLabel readLabel(String labelJson) {
        return read(map(portalWithLabel(labelJson))).portals().get(0).label();
    }

    private PortalLabel roundTrip(PortalLabel label) {
        Portal portal = new Portal("a", new Box(new Vec(0, 0, 0), new Vec(1, 1, 1)), "T", null, label);
        LobbyMap written = LobbyMap.lobbyMapBuilder().portals(List.of(portal)).build();
        return read(this.gson.toJson(written)).portals().get(0).label();
    }

    private static final Vec ANCHOR = new Vec(12.5, 66, -3.5);

    @DisplayName("A label is read with position, texts, source, billboard and yaw")
    @Test
    void readsFullLabel() {
        PortalLabel label = readLabel("""
                {"position":{"x":12.5,"y":66,"z":-3.5},"text":"<gold>Survival","offlineText":"<red>soon",
                 "source":{"type":"group","name":"Games"},"billboard":"fixed","yaw":90}""");

        assertEquals(new PortalLabel(ANCHOR, "<gold>Survival", "<red>soon", new LabelSource.Group("Games"), Billboard.FIXED, 90f), label);
    }

    @DisplayName("A label without billboard, source and offlineText gets the defaults")
    @Test
    void readsLabelDefaults() {
        PortalLabel label = readLabel("{\"position\":{\"x\":12.5,\"y\":66,\"z\":-3.5},\"text\":\"t\"}");

        assertEquals(new PortalLabel(ANCHOR, "t", null, null, Billboard.CENTER, 0f), label);
    }

    @DisplayName("Every source type survives a write and a read")
    @ParameterizedTest(name = "{0}")
    @MethodSource("sources")
    void sourceRoundTrips(LabelSource source) {
        PortalLabel label = new PortalLabel(ANCHOR, "t", "o", source, Billboard.CENTER, 0f);

        assertEquals(label, roundTrip(label));
    }

    static Stream<LabelSource> sources() {
        return Stream.of(new LabelSource.Task("Survival"), new LabelSource.Group("Games"), new LabelSource.Service("Survival-1"), new LabelSource.Local(), null);
    }

    @DisplayName("A fixed billboard keeps its yaw through a write and a read")
    @Test
    void fixedBillboardRoundTrips() {
        PortalLabel label = new PortalLabel(ANCHOR, "t", null, null, Billboard.FIXED, 90f);

        assertEquals(label, roundTrip(label));
    }

    @DisplayName("A missing source stays missing instead of becoming the portal task")
    @Test
    void missingSourceIsNotWritten() {
        Portal portal = new Portal("a", new Box(new Vec(0, 0, 0), new Vec(1, 1, 1)), "T", null, new PortalLabel(ANCHOR, "t", null, null, Billboard.CENTER, 0f));

        JsonObject label = this.gson.toJsonTree(LobbyMap.lobbyMapBuilder().portals(List.of(portal)).build()).getAsJsonObject().getAsJsonArray("portals").get(0).getAsJsonObject().getAsJsonObject("label");

        assertTrue(!label.has("source"), "a null source must not be written, was: " + label);
    }

    @DisplayName("A portal without a label is written without the label key")
    @Test
    void portalWithoutLabelHasNoKey() {
        LobbyMap lobbyMap = LobbyMap.lobbyMapBuilder().portals(List.of(new Portal("a", new Box(new Vec(0, 0, 0), new Vec(1, 1, 1)), "T", null))).build();

        JsonObject portal = this.gson.toJsonTree(lobbyMap).getAsJsonObject().getAsJsonArray("portals").get(0).getAsJsonObject();

        assertTrue(!portal.has("label"), "no label must mean no key, was: " + portal);
        assertNull(read(this.gson.toJson(lobbyMap)).portals().get(0).label());
    }

    @DisplayName("An unknown source type reads as a marker instead of failing")
    @Test
    void unknownSourceTypeReadsAsMarker() {
        PortalLabel label = readLabel("{\"position\":{\"x\":0,\"y\":0,\"z\":0},\"text\":\"t\",\"source\":{\"type\":\"proxy\",\"name\":\"x\"}}");

        assertEquals(new LabelSource.Unknown("proxy"), label.source());
    }

    @DisplayName("An unknown billboard reads as a marker instead of failing")
    @Test
    void unknownBillboardReadsAsMarker() {
        PortalLabel label = readLabel("{\"position\":{\"x\":0,\"y\":0,\"z\":0},\"text\":\"t\",\"billboard\":\"spin\"}");

        assertEquals(Billboard.UNKNOWN, label.billboard());
    }

    @DisplayName("A label without position reads with a null position")
    @Test
    void missingPositionReadsAsNull() {
        PortalLabel label = readLabel("{\"text\":\"t\"}");

        assertNull(label.position(), "the validator, not the reader, reports a missing position");
    }

    @DisplayName("An explicit null label reads as no label")
    @Test
    void nullLabelReadsAsNoLabel() {
        assertNull(readLabel("null"), "label: null must mean no label");
    }

    @DisplayName("An explicit null billboard reads as center")
    @Test
    void nullBillboardReadsAsCenter() {
        PortalLabel label = readLabel("{\"position\":{\"x\":0,\"y\":0,\"z\":0},\"text\":\"t\",\"billboard\":null}");

        assertEquals(Billboard.CENTER, label.billboard(), "null billboard must fall back to the default");
    }

    @DisplayName("A yaw that is not a number is a read error")
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"\"abc\"", "{}", "[1]"})
    void nonNumericYawIsAnError(String yaw) {
        String label = "{\"position\":{\"x\":0,\"y\":0,\"z\":0},\"text\":\"t\",\"yaw\":" + yaw + "}";

        assertThrows(JsonParseException.class, () -> readLabel(label), "yaw " + yaw + " must not load");
    }
}
