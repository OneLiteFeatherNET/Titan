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
package net.onelitefeather.titan.buildsrc.quality;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A ratchet over PMD violations (openspec {@code maintainability-gates}, design D4): the baseline
 * allows a fixed number of violations per file and rule, more is a failure, fewer is a hint that the
 * baseline can shrink. Keys are {@code <root-relative file>#<rule>}.
 */
public final class PmdRatchet {

    private static final String HEADER = """
            # Frozen PMD violations of production code: <root-relative file>#<rule>=<count allowed>.
            # Grows only with a reviewed reason and maintainer approval. Shrink it by fixing violations,
            # then run ./gradlew pmdRatchet -PupdatePmdBaseline and review the diff.
            """;

    private PmdRatchet() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /** The verdict of one comparison: failures break the build, shrinkable entries are reported only. */
    public record Outcome(List<String> failures, List<String> shrinkable) {

        public boolean passed() {
            return failures.isEmpty();
        }
    }

    /** Counts the violations of one PMD XML report per file and rule; the file names are made relative to {@code root}. */
    public static Map<String, Integer> countViolations(Path report, Path root) throws IOException {
        Map<String, Integer> counts = new TreeMap<>();
        violationLines(report, root).forEach((key, lines) -> counts.put(key, lines.size()));
        return counts;
    }

    /** The lines of the violations of one PMD XML report per file and rule, for messages that point at them. */
    public static Map<String, List<Integer>> violationLines(Path report, Path root) throws IOException {
        Map<String, List<Integer>> lines = new TreeMap<>();
        Path base = root.toAbsolutePath().normalize();
        for (Element file : elements(parse(report), "file")) {
            Path relative = base.relativize(Path.of(file.getAttribute("name")).toAbsolutePath().normalize());
            String path = relative.toString().replace('\\', '/');
            NodeList violations = file.getElementsByTagName("violation");
            for (int i = 0; i < violations.getLength(); i++) {
                Element violation = (Element) violations.item(i);
                String key = path + "#" + violation.getAttribute("rule");
                lines.computeIfAbsent(key, unused -> new ArrayList<>()).add(Integer.parseInt(violation.getAttribute("beginline")));
            }
        }
        return lines;
    }

    /** Compares the actual counts with the baseline. A missing baseline entry allows nothing. */
    public static Outcome compare(Map<String, Integer> actual, Map<String, Integer> baseline) {
        List<String> failures = new ArrayList<>();
        List<String> shrinkable = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : new TreeMap<>(actual).entrySet()) {
            int allowed = baseline.getOrDefault(entry.getKey(), 0);
            if (entry.getValue() > allowed) {
                failures.add(entry.getKey() + " has " + entry.getValue() + " violations, the baseline allows " + allowed);
            }
        }
        for (Map.Entry<String, Integer> entry : new TreeMap<>(baseline).entrySet()) {
            int found = actual.getOrDefault(entry.getKey(), 0);
            if (found < entry.getValue()) {
                shrinkable.add(entry.getKey() + " can shrink its baseline from " + entry.getValue() + " to " + found);
            }
        }
        return new Outcome(List.copyOf(failures), List.copyOf(shrinkable));
    }

    /** Reads the baseline; a missing file is an empty baseline. */
    public static Map<String, Integer> readBaseline(Path file) throws IOException {
        Map<String, Integer> baseline = new HashMap<>();
        if (!Files.isRegularFile(file)) {
            return baseline;
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        for (String key : properties.stringPropertyNames()) {
            baseline.put(key, Integer.parseInt(properties.getProperty(key).trim()));
        }
        return baseline;
    }

    /** Writes the baseline sorted by key, so a regenerated file shows only the real changes in review. */
    public static void writeBaseline(Path file, Map<String, Integer> counts) throws IOException {
        StringBuilder text = new StringBuilder(HEADER);
        for (Map.Entry<String, Integer> entry : new TreeMap<>(counts).entrySet()) {
            text.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
        }
        Files.createDirectories(file.toAbsolutePath().getParent());
        Files.writeString(file, text.toString(), StandardCharsets.UTF_8);
    }

    private static Document parse(Path report) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(report.toFile());
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Cannot read the PMD report " + report, e);
        }
    }

    private static List<Element> elements(Document document, String tag) {
        List<Element> elements = new ArrayList<>();
        NodeList nodes = document.getElementsByTagName(tag);
        for (int i = 0; i < nodes.getLength(); i++) {
            elements.add((Element) nodes.item(i));
        }
        return elements;
    }
}
