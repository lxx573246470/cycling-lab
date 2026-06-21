package com.cyclinglab.platform.importer;

import com.cyclinglab.platform.common.AppProperties;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/library")
@RequiredArgsConstructor
public class LibraryImportController {

    private final MarkdownImporter markdownImporter;
    private final AppProperties appProperties;

    /**
     * M1 import tool. Pass the path to {@code profile/rider-profile.md} and
     * the {@code plans/library/} directory. The tool reads the markdown,
     * creates a profile + templates for the current user, and returns a
     * per-resource report. The caller should review the warnings before
     * relying on the data (§15.1.5).
     */
    @PostMapping("/import-from-md")
    public ImportReport importFromMd(
        @RequestParam(defaultValue = "profile/rider-profile.md") String profilePath,
        @RequestParam(defaultValue = "plans/library") String libraryPath
    ) throws IOException {
        return markdownImporter.importFromPaths(
            resolveContentPath(profilePath),
            resolveContentPath(libraryPath)
        );
    }

    private Path resolveContentPath(String rawPath) {
        Path path = Path.of(rawPath);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        String configuredRoot = appProperties.content() == null ? null : appProperties.content().root();
        Path root = (configuredRoot == null || configuredRoot.isBlank())
            ? Paths.get("..")
            : Paths.get(configuredRoot);
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path resolved = normalizedRoot.resolve(path).normalize();
        if (!resolved.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException("path escapes content root: " + rawPath);
        }
        return resolved;
    }
}
