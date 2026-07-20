package io.github.joaodallagnol.backend.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CwebpImageProcessor {

    private final String binary;

    public CwebpImageProcessor(@Value("${app.photo-processing.cwebp-binary:/usr/bin/cwebp}") String binary) {
        this.binary = binary;
    }

    public void createWebp(Path input, Path output, int width, int quality) {
        List<String> command = List.of(
                binary,
                "-quiet",
                "-mt",
                "-q", Integer.toString(quality),
                "-resize", Integer.toString(width), "0",
                input.toString(),
                "-o", output.toString()
        );
        try {
            Process process = new ProcessBuilder(command)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!process.waitFor(60, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("cwebp timed out.");
            }
            if (process.exitValue() != 0) {
                throw new IllegalArgumentException("Image could not be decoded or converted.");
            }
        } catch (IOException ex) {
            throw new IllegalStateException("cwebp is unavailable.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Image processing was interrupted.", ex);
        }
    }
}
