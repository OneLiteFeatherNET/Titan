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
package net.onelitefeather.titan.buildsrc.config;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Concatenates every column's own {@code titan/defaults/<column>.yaml} into one classpath
 * {@code application.yaml}, keeping every file's comments verbatim - the files are
 * concatenated, never deep-merged. A pure function over strings/files, so it is testable without a
 * Gradle project.
 *
 * <p>Before concatenating, every file is parsed on its own with SnakeYAML's duplicate-key
 * detection ({@link LoaderOptions#setAllowDuplicateKeys(boolean)}) and flattened to dotted keys, so
 * two files claiming the same top-level section (or the same nested key) fail the build instead of
 * one silently shadowing the other - the files themselves are never deep-merged, so a silent
 * shadow would otherwise go unnoticed until a module read the wrong value at runtime.
 */
public final class DefaultsMerger {

    private DefaultsMerger() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * @param files the default files to concatenate, in the order they should appear in the result
     * @return the files' content, concatenated in order
     * @throws DefaultsConflictException if two files set the same dotted key
     */
    public static String merge(List<File> files) {
        Map<String, File> ownerByKey = new LinkedHashMap<>();
        StringBuilder merged = new StringBuilder();
        for (File file : files) {
            String content = read(file);
            for (String key : flattenedKeysOf(content, file)) {
                File owner = ownerByKey.putIfAbsent(key, file);
                if (owner != null) {
                    throw new DefaultsConflictException("Key '" + key + "' is set by both " + owner.getName() + " and " + file.getName());
                }
            }
            if (!merged.isEmpty()) {
                merged.append('\n');
            }
            merged.append(content);
        }
        return merged.toString();
    }

    /** @return every dotted key {@code yamlContent} sets at a leaf, e.g. {@code "spawn.minHeight"} */
    static Set<String> flattenedKeysOf(String yamlContent, File file) {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(options);
        Object parsed;
        try {
            parsed = yaml.load(yamlContent);
        } catch (RuntimeException exception) {
            // SnakeYAML's own message already names the duplicate key and the position; naming the
            // file here is the only piece it cannot know itself.
            throw new DefaultsConflictException("Invalid YAML in " + file.getName() + ": " + exception.getMessage());
        }
        Set<String> keys = new LinkedHashSet<>();
        flatten(parsed, "", keys);
        return keys;
    }

    // Every level of the path is registered, not just the leaves: a second file touching the same
    // top-level section (e.g. "features") must conflict even if its own nested keys differ.
    private static void flatten(Object node, String prefix, Set<String> keys) {
        if (!(node instanceof Map<?, ?> map)) {
            return;
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? String.valueOf(entry.getKey()) : prefix + "." + entry.getKey();
            keys.add(key);
            flatten(entry.getValue(), key, keys);
        }
    }

    private static String read(File file) {
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read " + file, exception);
        }
    }
}
