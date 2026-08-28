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
package net.onelitefeather.titan.common.season;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import net.onelitefeather.titan.common.feature.ReleaseStage;
import net.onelitefeather.titan.common.feature.SeasonWindowActivationStrategy;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Reads season files into {@link SeasonDefinition}s.
 *
 * <p>The loader is strict on purpose. Anything it cannot read is a
 * {@link SeasonConfigurationException} naming the file and the value — never a season that loads
 * with one effect quietly missing. That is US-4.04, and it is worth the strictness: a season is
 * looked at once a year, so a mistake found at startup costs minutes and the same mistake found in
 * production costs the season.
 *
 * <p>An absent {@code seasons} directory is not a mistake. The lobby runs without any season at
 * all (NFR-003), and {@link #loadAll(Path)} returns an empty list for a directory that is not
 * there.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class SeasonLoader {

    /** File extension a season file must have to be picked up. */
    public static final String FILE_EXTENSION = ".json";

    /** Directory seasons are read from, relative to the working directory of the process. */
    public static final String DIRECTORY = "seasons";

    private static final Logger LOGGER = LoggerFactory.getLogger(SeasonLoader.class);

    private final Gson gson;
    private final ZoneId defaultZone;
    private final NamedWindowResolver namedWindows;

    private SeasonLoader(ZoneId defaultZone, NamedWindowResolver namedWindows) {
        this.defaultZone = defaultZone;
        this.namedWindows = namedWindows;
        this.gson = new GsonBuilder().registerTypeAdapter(SeasonEffect.class, new SeasonEffectAdapter()).registerTypeAdapter(Pos.class, new PosAdapter()).registerTypeHierarchyAdapter(Key.class, new KeyAdapter()).create();
    }

    /**
     * Creates a loader that plans seasons in the given zone and cannot resolve named windows yet.
     *
     * @param defaultZone the zone a season file that names none is read in
     * @return a loader for files that spell their windows out
     */
    @Contract(value = "_ -> new", pure = true)
    public static SeasonLoader create(ZoneId defaultZone) {
        return new SeasonLoader(defaultZone, NamedWindowResolver.unavailable());
    }

    /**
     * Creates a loader that can also resolve windows a file names rather than spells out.
     *
     * @param defaultZone  the zone a season file that names none is read in
     * @param namedWindows the resolver for named windows; see {@link NamedWindowResolver} for what
     *                     spec stage 2 plugs in here
     * @return a loader for both kinds of window
     */
    @Contract(value = "_, _ -> new", pure = true)
    public static SeasonLoader create(ZoneId defaultZone, NamedWindowResolver namedWindows) {
        return new SeasonLoader(defaultZone, namedWindows);
    }

    /**
     * Reads every season file in a directory.
     *
     * @param directory the directory to read, usually {@value #DIRECTORY} next to the process
     * @return the seasons, ordered by {@link SeasonDefinition#BY_PRIORITY} so the result never
     *         depends on the order the file system happened to list the files in (US-4.05)
     * @throws SeasonConfigurationException when a file cannot be read, or when two files claim the
     *                                      same id
     */
    public List<SeasonDefinition> loadAll(Path directory) {
        if (!Files.isDirectory(directory)) {
            LOGGER.info("No season directory at {}; running without seasonal content", directory.toAbsolutePath());
            return List.of();
        }
        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.list(directory)) {
            stream.filter(Files::isRegularFile).filter(path -> path.getFileName().toString().endsWith(FILE_EXTENSION)).forEach(files::add);
        } catch (IOException exception) {
            throw new SeasonConfigurationException(directory.toString(), "the season directory could not be listed", exception);
        }
        Map<String, Path> seen = new HashMap<>();
        List<SeasonDefinition> definitions = new ArrayList<>(files.size());
        for (Path file : files) {
            SeasonDefinition definition = load(file);
            Path previous = seen.put(definition.id(), file);
            if (previous != null) {
                throw new SeasonConfigurationException(file.getFileName().toString(), "season id '" + definition.id() + "' is already used by " + previous.getFileName());
            }
            definitions.add(definition);
        }
        definitions.sort(SeasonDefinition.BY_PRIORITY);
        return List.copyOf(definitions);
    }

    /**
     * Reads one season file.
     *
     * @param file the file to read
     * @return the season it describes
     * @throws SeasonConfigurationException when the file cannot be read
     */
    public SeasonDefinition load(Path file) {
        String source = file.getFileName().toString();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return parse(source, this.gson.fromJson(reader, SeasonFile.class));
        } catch (IOException exception) {
            throw new SeasonConfigurationException(source, "the file could not be read", exception);
        } catch (SeasonConfigurationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new SeasonConfigurationException(source, rootMessage(exception), exception);
        }
    }

    /**
     * Reads a season from a string, for tests and for the smoke run of an archived season.
     *
     * @param source a name for the season used in error messages
     * @param json   the season as JSON
     * @return the season it describes
     * @throws SeasonConfigurationException when the JSON cannot be read
     */
    public SeasonDefinition parse(String source, String json) {
        try {
            return parse(source, this.gson.fromJson(json, SeasonFile.class));
        } catch (SeasonConfigurationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new SeasonConfigurationException(source, rootMessage(exception), exception);
        }
    }

    private SeasonDefinition parse(String source, @Nullable SeasonFile file) {
        if (file == null) {
            throw new SeasonConfigurationException(source, "the file is empty");
        }
        if (file.id() == null || file.id().isBlank()) {
            throw new SeasonConfigurationException(source, "no id; a season needs one, and it is also the name the release gate knows it by");
        }
        ReleaseStage stage = file.stage() == null ? ReleaseStage.DEFAULT : ReleaseStage.fromId(file.stage()).orElseThrow(() -> new SeasonConfigurationException(source, "stage='" + file.stage() + "' is not internal, lite or ga"));
        SeasonWindow window = window(source, file.window());
        List<SeasonEffect> effects = file.effects() == null ? List.of() : file.effects();
        // enabled defaults to true: a file that exists is meant to run, and the kill switch is
        // something an operator reaches for deliberately. priority defaults to 0.
        boolean enabled = file.enabled() == null || file.enabled();
        int priority = file.priority() == null ? 0 : file.priority();
        for (SeasonEffect effect : effects) {
            checkRegistries(source, effect);
        }
        try {
            return new SeasonDefinition(file.id(), enabled, priority, stage, blankToNull(file.world()), window, effects);
        } catch (IllegalArgumentException exception) {
            throw new SeasonConfigurationException(source, exception.getMessage(), exception);
        }
    }

    /**
     * Checks that every key an effect names exists in the game's registries.
     *
     * <p>A misspelt block is the mistake a season file is most likely to contain and the one that
     * costs most: unchecked, it turns into either a silent hole in the build or an exception thrown
     * at the moment the season goes live. Resolving the keys here makes it a startup failure with
     * the file name on it instead.
     *
     * <p>The switch is exhaustive over the sealed hierarchy and has no {@code default}, so an
     * effect type that names a new kind of registry key does not compile until this method has been
     * told how to check it.
     */
    private static void checkRegistries(String source, SeasonEffect effect) {
        switch (effect) {
            case SeasonEffect.PlaceDecoration decoration -> {
                if (Block.fromKey(decoration.block()) == null) {
                    throw new SeasonConfigurationException(source, "block='" + decoration.block().asString() + "' is not a known block");
                }
            }
            case SeasonEffect.ReplaceIcon icon -> {
                if (Material.fromKey(icon.material()) == null) {
                    throw new SeasonConfigurationException(source, "material='" + icon.material().asString() + "' is not a known item material");
                }
            }
            // A sound key is not resolved: resource packs may add sounds the server does not know,
            // and refusing an unknown one here would make the server the authority on a client-side
            // registry. MiniMessage texts are parsed where they are used.
            case SeasonEffect.PlaceDisplay ignored -> {
            }
            case SeasonEffect.AmbientSound ignored -> {
            }
            case SeasonEffect.MessagePrefix ignored -> {
            }
        }
    }

    private SeasonWindow window(String source, @Nullable SeasonFile.WindowSpec spec) {
        if (spec == null) {
            return SeasonWindow.always(this.defaultZone);
        }
        ZoneId zone = zone(source, spec.zone());
        if (spec.named() != null && !spec.named().isBlank()) {
            if (spec.from() != null || spec.to() != null) {
                throw new SeasonConfigurationException(source, "the window names '" + spec.named() + "' and also spells out from/to; use one or the other");
            }
            if (spec.year() == null) {
                throw new SeasonConfigurationException(source, "the window names '" + spec.named() + "' but no year to resolve it in");
            }
            return this.namedWindows.resolve(spec.named(), spec.year(), zone).orElseThrow(() -> new SeasonConfigurationException(source, "the window names '" + spec.named() + "', which no installed season strategy knows; spell the window out as from/to, or install a NamedWindowResolver"));
        }
        LocalDateTime from = bound(source, "from", spec.from());
        LocalDateTime to = bound(source, "to", spec.to());
        try {
            return new SeasonWindow(from, to, zone);
        } catch (IllegalArgumentException exception) {
            throw new SeasonConfigurationException(source, exception.getMessage(), exception);
        }
    }

    private ZoneId zone(String source, @Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return this.defaultZone;
        }
        try {
            return ZoneId.of(raw.trim());
        } catch (DateTimeException exception) {
            throw new SeasonConfigurationException(source, "zone='" + raw.trim() + "' is not a known time zone", exception);
        }
    }

    private static @Nullable LocalDateTime bound(String source, String field, @Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        try {
            return value.indexOf('T') < 0 ? LocalDate.parse(value).atStartOfDay() : LocalDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            throw new SeasonConfigurationException(source, field + "='" + value + "' is not a date (2026-12-01) or a date-time (2026-12-01T18:00)", exception);
        }
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Digs out the message that actually says what is wrong. Gson wraps an exception thrown by a
     * record's constructor in a {@link RuntimeException} whose message is the constructor
     * signature and the argument array — accurate, and useless to the person holding the file.
     */
    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        String message = current.getMessage();
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                message = current.getMessage();
            }
        }
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    /**
     * The raw shape of a season file, before anything has been validated. Kept apart from
     * {@link SeasonDefinition} so that a missing or malformed value is reported by this loader,
     * with the file name attached, instead of surfacing as a null somewhere in the lobby.
     */
    private record SeasonFile(@Nullable String id, @Nullable Boolean enabled,
                              @Nullable Integer priority, @Nullable String stage,
                              @Nullable String world, @Nullable WindowSpec window,
                              @Nullable List<SeasonEffect> effects) {

        /** The raw shape of the {@code window} object; see {@link NamedWindowResolver}. */
        private record WindowSpec(@Nullable String from, @Nullable String to, @Nullable String zone,
                                  @Nullable String named, @Nullable Integer year) {
        }
    }

    /**
     * Turns the {@code type} field of an effect into the record it belongs to, and refuses a type
     * nobody implements.
     *
     * <p>This is the load-time half of US-4.04. The compile-time half is the sealed hierarchy
     * itself, and neither half covers the other: this one catches a typo in a file, that one
     * catches a gap in the code.
     */
    private static final class SeasonEffectAdapter implements JsonDeserializer<SeasonEffect>, JsonSerializer<SeasonEffect> {

        @Override
        public SeasonEffect deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            if (!json.isJsonObject()) {
                throw new JsonParseException("an effect must be an object with a type, got " + json);
            }
            JsonObject object = json.getAsJsonObject();
            JsonElement rawType = object.get("type");
            String id = rawType == null || !rawType.isJsonPrimitive() ? null : rawType.getAsString();
            SeasonEffect.Type type = SeasonEffect.Type.fromId(id).orElseThrow(() -> new JsonParseException(id == null ? "an effect has no type; known types are: " + SeasonEffect.Type.knownIds() : "unknown season effect type '" + id + "'; known types are: " + SeasonEffect.Type.knownIds()));
            return context.deserialize(json, type.effectClass());
        }

        @Override
        public JsonElement serialize(SeasonEffect src, Type typeOfSrc, JsonSerializationContext context) {
            // Exhaustive over the sealed hierarchy and without a default on purpose: a new effect
            // record does not compile until it has been considered here as well.
            JsonElement element = switch (src) {
                case SeasonEffect.PlaceDecoration decoration ->
                    context.serialize(decoration, SeasonEffect.PlaceDecoration.class);
                case SeasonEffect.PlaceDisplay display ->
                    context.serialize(display, SeasonEffect.PlaceDisplay.class);
                case SeasonEffect.AmbientSound sound ->
                    context.serialize(sound, SeasonEffect.AmbientSound.class);
                case SeasonEffect.ReplaceIcon icon ->
                    context.serialize(icon, SeasonEffect.ReplaceIcon.class);
                case SeasonEffect.MessagePrefix prefix ->
                    context.serialize(prefix, SeasonEffect.MessagePrefix.class);
            };
            element.getAsJsonObject().add("type", new JsonPrimitive(src.type().id()));
            return element;
        }
    }

    /**
     * Reads a position as {@code {"x": 0.5, "y": 65, "z": 0.5}}, with an optional yaw and pitch.
     *
     * <p>Aves ships an adapter for this, but it returns a {@code Vec} whenever yaw and pitch are
     * absent — which is right for its own callers and wrong here, where the field is declared as a
     * {@code Pos} and would fail with a class cast at load. Decoration is placed far more often
     * than it is aimed, so the yaw and pitch stay optional and default to zero.
     */
    private static final class PosAdapter implements JsonDeserializer<Pos>, JsonSerializer<Pos> {

        @Override
        public Pos deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            if (!json.isJsonObject()) {
                throw new JsonParseException("a position must be an object with x, y and z, got " + json);
            }
            JsonObject object = json.getAsJsonObject();
            return new Pos(coordinate(object, "x"), coordinate(object, "y"), coordinate(object, "z"), object.has("yaw") ? object.get("yaw").getAsFloat() : 0.0f, object.has("pitch") ? object.get("pitch").getAsFloat() : 0.0f);
        }

        @Override
        public JsonElement serialize(Pos src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject object = new JsonObject();
            object.addProperty("x", src.x());
            object.addProperty("y", src.y());
            object.addProperty("z", src.z());
            if (src.yaw() != 0.0f || src.pitch() != 0.0f) {
                object.addProperty("yaw", src.yaw());
                object.addProperty("pitch", src.pitch());
            }
            return object;
        }

        private static double coordinate(JsonObject object, String name) {
            JsonElement element = object.get(name);
            if (element == null || !element.isJsonPrimitive()) {
                throw new JsonParseException("a position needs an " + name + " coordinate");
            }
            return element.getAsDouble();
        }
    }

    /**
     * Reads a key as the plain string an operator would write, {@code minecraft:jack_o_lantern},
     * rather than as the two-field object Aves' adapter expects. A season file is edited by hand;
     * the shorter form is the one that gets typed correctly.
     */
    private static final class KeyAdapter implements JsonDeserializer<Key>, JsonSerializer<Key> {

        @Override
        public Key deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            if (!json.isJsonPrimitive()) {
                throw new JsonParseException("a key must be a string like minecraft:jack_o_lantern, got " + json);
            }
            String raw = json.getAsString().trim();
            try {
                return Key.key(raw);
            } catch (RuntimeException exception) {
                throw new JsonParseException("'" + raw + "' is not a valid key; expected something like minecraft:jack_o_lantern", exception);
            }
        }

        @Override
        public JsonElement serialize(Key src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.asString());
        }
    }

    /**
     * Returns the zone seasons are planned in when nothing says otherwise.
     *
     * @return the editorial time zone, {@code Europe/Berlin}
     */
    @Contract(pure = true)
    public static ZoneId editorialZone() {
        return SeasonWindowActivationStrategy.DEFAULT_ZONE;
    }
}
