package ca.yorku.eecs3311.coursepilot.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Loads non-committed local .env values without overriding real environment variables. */
public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    private static final String PROPERTY_SOURCE = "studyforgeDotEnv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path envFile = Path.of(System.getProperty("user.dir", "."), ".env").toAbsolutePath().normalize();
        if (!Files.isRegularFile(envFile)) return;
        try {
            Map<String, Object> values = parse(Files.readAllLines(envFile));
            if (!values.isEmpty()) environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE, values));
        } catch (IOException | RuntimeException ignored) {
            // A local convenience file must never prevent the application from starting.
        }
    }

    static Map<String, Object> parse(Iterable<String> lines) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            int separator = trimmed.indexOf('=');
            if (separator <= 0) continue;
            String key = trimmed.substring(0, separator).trim();
            String value = trimmed.substring(separator + 1).trim();
            if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                value = value.substring(1, value.length() - 1);
            }
            if (key.matches("[A-Za-z_][A-Za-z0-9_]*")) values.putIfAbsent(key, value);
        }
        return values;
    }

    @Override
    public int getOrder() { return Ordered.LOWEST_PRECEDENCE; }
}
