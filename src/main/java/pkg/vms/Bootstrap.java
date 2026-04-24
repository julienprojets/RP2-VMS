package pkg.vms;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * Entry point for the jpackage installer only. Writes a file before JavaFX loads so we can tell
 * whether the launcher actually starts the JVM and calls main().
 */
public final class Bootstrap {

    public static void main(String[] args) throws Exception {
        Path marker = Path.of(System.getProperty("user.home"), "vms-bootstrap-ran.txt");
        Files.writeString(marker,
                "Bootstrap.main ran at " + Instant.now() + System.lineSeparator()
                        + "user.dir=" + System.getProperty("user.dir") + System.lineSeparator(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
        HelloApplication.main(args);
    }
}
