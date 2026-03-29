package com.proximityservice.loadtest;

import com.proximityservice.loadtest.config.LoadTestConfig;
import com.proximityservice.loadtest.metrics.MetricsServer;
import com.proximityservice.loadtest.scenario.BusinessCrudScenario;
import com.proximityservice.loadtest.scenario.NearbySearchScenario;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class LoadTestApplication {

    public static void main(String[] args) throws Exception {
        LoadTestConfig config = LoadTestConfig.fromEnv();
        System.out.printf("Starting load test: url=%s, concurrency=%d, duration=%ds, searchRatio=%.2f%n",
            config.targetUrl(), config.concurrency(), config.durationSeconds(), config.searchRatio());

        MetricsServer metricsServer = new MetricsServer(8082);
        metricsServer.start();

        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        NearbySearchScenario searchScenario = new NearbySearchScenario(httpClient, config.targetUrl(), metricsServer);
        BusinessCrudScenario crudScenario = new BusinessCrudScenario(httpClient, config.targetUrl(), metricsServer);

        AtomicLong totalRequests = new AtomicLong();
        AtomicLong lastSnapshot = new AtomicLong();
        Instant startTime = Instant.now();
        Instant endTime = startTime.plusSeconds(config.durationSeconds());

        ScheduledExecutorService reporter = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "status-reporter");
            t.setDaemon(true);
            return t;
        });
        reporter.scheduleAtFixedRate(() -> {
            long total = totalRequests.get();
            long delta = total - lastSnapshot.getAndSet(total);
            long elapsed = Duration.between(startTime, Instant.now()).toSeconds();
            double rps = delta / 5.0;
            System.out.printf("[%3ds] RPS: %.1f  Total: %d%n", elapsed, rps, total);
        }, 5, 5, TimeUnit.SECONDS);

        Random random = new Random();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < config.concurrency(); i++) {
                executor.submit(() -> {
                    while (Instant.now().isBefore(endTime)) {
                        try {
                            if (random.nextDouble() < config.searchRatio()) {
                                searchScenario.run();
                            } else {
                                crudScenario.run();
                            }
                            totalRequests.incrementAndGet();
                        } catch (Exception e) {
                            // swallow unexpected errors to keep the loop running
                        }
                    }
                });
            }
        } // virtual thread executor waits for all tasks to finish

        reporter.shutdown();

        long elapsed = Duration.between(startTime, Instant.now()).toSeconds();
        long total = totalRequests.get();
        System.out.printf("Load test complete. Duration: %ds, Total requests: %d, Avg RPS: %.1f%n",
            elapsed, total, elapsed > 0 ? (double) total / elapsed : 0);

        System.out.println("Keeping metrics server alive for 30s for final Prometheus scrape...");
        Thread.sleep(30_000);
        metricsServer.stop();
    }
}
