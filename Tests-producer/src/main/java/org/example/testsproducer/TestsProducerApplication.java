package org.example.testsproducer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.system.ApplicationHome;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@SpringBootApplication
public class TestsProducerApplication {

    public static void main(String[] args) {
        configureExternalApplicationYaml(args);
        SpringApplication.run(TestsProducerApplication.class, args);
    }

    static void configureExternalApplicationYaml(String[] args) {
        if (hasExplicitConfigurationLocation(args)) {
            return;
        }

        Path applicationDirectory = applicationDirectory();
        Path configuration = applicationDirectory.resolve("application.yml");
        System.setProperty(
                "spring.config.location",
                configuration.toUri().toString()
        );
    }

    private static boolean hasExplicitConfigurationLocation(String[] args) {
        return System.getProperty("spring.config.location") != null
                || System.getenv("SPRING_CONFIG_LOCATION") != null
                || Arrays.stream(args).anyMatch(argument ->
                        argument.startsWith("--spring.config.location=")
                );
    }

    private static Path applicationDirectory() {
        ApplicationHome applicationHome = new ApplicationHome(
                TestsProducerApplication.class
        );
        if (Files.isDirectory(applicationHome.getSource().toPath())) {
            return Path.of("").toAbsolutePath();
        }
        return applicationHome.getDir().toPath().toAbsolutePath();
    }
}
