package com.cyclinglab.platform.importer;

import java.io.IOException;
import java.nio.file.Path;
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
            Path.of(profilePath),
            Path.of(libraryPath)
        );
    }
}
