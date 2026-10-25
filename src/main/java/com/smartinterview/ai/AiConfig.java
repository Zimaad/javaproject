package com.smartinterview.ai;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * AI provider settings. Read from config.properties in the working directory
 * (git-ignored), with environment variables as a fallback, so a key never
 * has to live in source control.
 *
 * Every provider with a key is used, in the order below; if one fails, the
 * next is tried.
 */
public final class AiConfig {

    /** One usable provider. {@code anthropic} selects the Anthropic API; all others are OpenAI-compatible. */
    public record Endpoint(String name, boolean anthropic, String baseUrl, String apiKey, String model) {
    }

    private static final Properties PROPS = load();

    private AiConfig() {}

    private static Properties load() {
        Properties p = new Properties();
        Path file = Path.of("config.properties");
        if (Files.isRegularFile(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                p.load(in);
            } catch (IOException e) {
                System.err.println("Could not read config.properties: " + e.getMessage());
            }
        }
        return p;
    }

    /** Looks up a setting in config.properties, then the environment (FOO_BAR for foo.bar). */
    public static String get(String key, String fallback) {
        String v = PROPS.getProperty(key);
        if (v == null || v.isBlank()) {
            v = System.getenv(key.toUpperCase().replace('.', '_'));
        }
        return v == null || v.isBlank() ? fallback : v.trim();
    }

    /** Providers that have a key, best first. {@code ai.provider} moves one to the front. */
    public static List<Endpoint> endpoints() {
        List<Endpoint> all = new ArrayList<>();
        add(all, "groq", false, "https://api.groq.com/openai/v1", "openai/gpt-oss-120b");
        add(all, "openrouter", false, "https://openrouter.ai/api/v1", "openai/gpt-oss-120b");
        add(all, "gemini", false, "https://generativelanguage.googleapis.com/v1beta/openai", "gemini-3.8-flash");
        add(all, "anthropic", true, "https://api.anthropic.com/v1", "claude-sonnet-5-5");
        add(all, "openai", false, "https://api.openai.com/v1", "gpt-4o-mini");

        String preferred = get("ai.provider", "").toLowerCase();
        all.sort((a, b) -> Boolean.compare(!a.name().equals(preferred), !b.name().equals(preferred)));
        return all;
    }

    private static void add(List<Endpoint> out, String name, boolean anthropic, String baseUrl, String model) {
        String key = get(name + ".api.key", "");
        if (!key.isEmpty()) {
            out.add(new Endpoint(name, anthropic, get(name + ".base.url", baseUrl), key,
                    get(name + ".model", model)));
        }
    }

    public static boolean isConfigured() {
        return !endpoints().isEmpty();
    }
}
