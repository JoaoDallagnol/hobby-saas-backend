package io.github.joaodallagnol.backend.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CwebpImageProcessor {

    private static final long PROCESS_TIMEOUT_SECONDS = 60;

    private final String cwebpBinary;
    private final String imageMagickBinary;

    public CwebpImageProcessor(
            @Value("${app.photo-processing.cwebp-binary:/usr/bin/cwebp}") String cwebpBinary,
            @Value("${app.photo-processing.imagemagick-binary:/usr/bin/convert}") String imageMagickBinary
    ) {
        this.cwebpBinary = cwebpBinary;
        this.imageMagickBinary = imageMagickBinary;
    }

    public void normalizeOrientation(Path input, Path output) {
        run(
                orientationCommand(input, output),
                "Image orientation could not be normalized.",
                "ImageMagick is unavailable."
        );
    }

    public void createWebp(Path input, Path output, int width, int quality) {
        List<String> command = List.of(
                cwebpBinary,
                "-quiet",
                "-mt",
                "-q", Integer.toString(quality),
                "-resize", Integer.toString(width), "0",
                input.toString(),
                "-o", output.toString()
        );
        run(command, "Image could not be decoded or converted.", "cwebp is unavailable.");
    }

    List<String> orientationCommand(Path input, Path output) {
        return List.of(
                imageMagickBinary,
                "-limit", "memory", "256MiB",
                "-limit", "map", "512MiB",
                "-limit", "disk", "1GiB",
                input.toString(),
                "-auto-orient",
                "-strip",
                "PNG:" + output
        );
    }

    private void run(List<String> command, String processingError, String unavailableError) {
        try {
            Process process = new ProcessBuilder(command)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("Image processing timed out.");
            }
            if (process.exitValue() != 0) {
                throw new IllegalArgumentException(processingError);
            }
        } catch (IOException ex) {
            throw new IllegalStateException(unavailableError, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Image processing was interrupted.", ex);
        }
    }
}
