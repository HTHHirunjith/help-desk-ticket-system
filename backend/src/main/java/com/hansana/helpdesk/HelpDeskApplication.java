package com.hansana.helpdesk;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class HelpDeskApplication {

    public static void main(String[] args) {
        loadLocalDotenv();
        SpringApplication.run(HelpDeskApplication.class, args);
    }

    private static void loadLocalDotenv() {
        Path envDirectory = resolveEnvDirectory();
        Dotenv dotenv = Dotenv.configure()
                .directory(envDirectory.toString())
                .filename(".env")
                .ignoreIfMissing()
                .load();

        for (DotenvEntry entry : dotenv.entries()) {
            String key = entry.getKey();
            if (System.getenv(key) == null && System.getProperty(key) == null) {
                System.setProperty(key, entry.getValue());
            }
        }
    }

    private static Path resolveEnvDirectory() {
        Path cwd = Path.of(System.getProperty("user.dir"));
        Path envInCwd = cwd.resolve(".env");
        if (Files.isRegularFile(envInCwd)) {
            return cwd;
        }
        Path backendDir = cwd.resolve("backend");
        if (Files.isRegularFile(backendDir.resolve(".env"))) {
            return backendDir;
        }
        return cwd;
    }
}
