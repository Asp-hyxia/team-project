package architecture;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

/** Checks declared imports across every production source, including future teammates' code. */
class DependencyRuleTest {
    @Test void importsPointInward() throws Exception {
        Map<String, Set<String>> allowed = Map.of(
                "entity", Set.of("java"),
                "use_case", Set.of("java", "entity", "use_case"),
                "interface_adapter", Set.of("java", "use_case", "interface_adapter"),
                "view", Set.of("java", "javax", "interface_adapter"),
                "data_access", Set.of("java", "com", "entity", "use_case", "data_access"),
                "infrastructure", Set.of("java", "com", "entity", "use_case", "infrastructure"));
        Pattern imports = Pattern.compile("(?m)^import (?:static )?([a-zA-Z_][\\w]*)\\.");
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
                String layer = Path.of("src/main/java").relativize(file).getName(0).toString();
                if (layer.equals("app")) { continue; }
                assertTrue(allowed.containsKey(layer), "Unknown layer: " + file);
                Matcher matcher = imports.matcher(Files.readString(file));
                while (matcher.find()) { assertTrue(allowed.get(layer).contains(matcher.group(1)), "Outward dependency: " + file + " -> " + matcher.group()); }
            }
        }
    }
}
