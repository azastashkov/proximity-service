package com.proximityservice.loadtest.metrics;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class MetricsServer {

    private final int port;
    private final PrometheusMeterRegistry registry;
    private HttpServer httpServer;

    public MetricsServer(int port) {
        this.port = port;
        this.registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }

    public void start() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/metrics", exchange -> {
            String body = registry.scrape();
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; version=0.0.4; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        httpServer.start();
        System.out.println("Metrics server started on port " + port);
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    public Timer getTimer(String scenario) {
        return Timer.builder("loadtest_request_duration_seconds")
            .description("Load test request duration")
            .tag("scenario", scenario)
            .publishPercentiles(0.5, 0.75, 0.95, 0.99)
            .publishPercentileHistogram()
            .minimumExpectedValue(Duration.ofMillis(1))
            .maximumExpectedValue(Duration.ofSeconds(30))
            .register(registry);
    }

    public Counter getCounter(String scenario, String status) {
        return Counter.builder("loadtest_requests_total")
            .description("Total load test requests")
            .tag("scenario", scenario)
            .tag("status", status)
            .register(registry);
    }
}
