package io.github.joaodallagnol.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class CwebpImageProcessorTest {

    @Test
    void shouldApplyExifOrientationBeforeRemovingMetadata() {
        CwebpImageProcessor processor = new CwebpImageProcessor("/usr/bin/cwebp", "/usr/bin/convert");

        List<String> command = processor.orientationCommand(
                Path.of("/tmp/input"),
                Path.of("/tmp/oriented.png")
        );

        assertThat(command).containsSubsequence(
                "/tmp/input",
                "-auto-orient",
                "-strip",
                "PNG:/tmp/oriented.png"
        );
        assertThat(command.indexOf("-auto-orient")).isLessThan(command.indexOf("-strip"));
    }

    @Test
    void shouldLimitImageMagickResourcesForUntrustedUploads() {
        CwebpImageProcessor processor = new CwebpImageProcessor("/usr/bin/cwebp", "/usr/bin/convert");

        assertThat(processor.orientationCommand(Path.of("input"), Path.of("output")))
                .containsSubsequence("-limit", "memory", "256MiB")
                .containsSubsequence("-limit", "map", "512MiB")
                .containsSubsequence("-limit", "disk", "1GiB");
    }
}
