package dregex;

import org.junit.jupiter.api.Test;

/**
 * Not really a test, but a way of confirming that tests are running with the intended JVM version.
 */
public class VersionLoggerTest {

    @Test
    public void versionLogger() {
        System.out.printf("Running in JVM version %s%n", Runtime.version());
    }
}
