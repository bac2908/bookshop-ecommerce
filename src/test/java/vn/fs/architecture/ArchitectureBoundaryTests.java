package vn.fs.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class ArchitectureBoundaryTests {

    private static final Path BACKEND = Path.of("src/main/java/vn/fs/backend");

    @Test
    void innerLayersDoNotDependOnPresentation() throws IOException {
        assertNoImport(BACKEND.resolve("application"),
                "import vn.fs.backend.presentation.");
        assertNoImport(BACKEND.resolve("domain"),
                "import vn.fs.backend.presentation.");
        assertNoImport(BACKEND.resolve("infrastructure"),
                "import vn.fs.backend.presentation.");
    }

    @Test
    void domainDoesNotDependOnOuterLayers() throws IOException {
        assertNoImport(BACKEND.resolve("domain"),
                "import vn.fs.backend.application.",
                "import vn.fs.backend.infrastructure.");
    }

    private void assertNoImport(Path root, String... forbiddenImports) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            List<String> violations = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> forbiddenLines(path, forbiddenImports))
                    .toList();
            assertTrue(violations.isEmpty(),
                    () -> "Architecture dependency violations:\n" + String.join("\n", violations));
        }
    }

    private Stream<String> forbiddenLines(Path file, String[] forbiddenImports) {
        try {
            return Files.readAllLines(file).stream()
                    .filter(line -> Stream.of(forbiddenImports).anyMatch(line::startsWith))
                    .map(line -> file + " -> " + line.trim());
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot inspect " + file, exception);
        }
    }
}
