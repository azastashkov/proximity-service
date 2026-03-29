package com.proximityservice.loadtest.config;

public record LoadTestConfig(String targetUrl, int concurrency, int durationSeconds, double searchRatio) {

    public static LoadTestConfig fromEnv() {
        return new LoadTestConfig(
            env("TARGET_URL", "http://localhost:80"),
            Integer.parseInt(env("CONCURRENCY", "50")),
            Integer.parseInt(env("DURATION_SECONDS", "300")),
            Double.parseDouble(env("SEARCH_RATIO", "0.8"))
        );
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }
}
