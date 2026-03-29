package com.proximityservice.loadtest.scenario;

import com.proximityservice.loadtest.metrics.MetricsServer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Random;

public class NearbySearchScenario implements Runnable {

    private static final String SCENARIO = "nearby_search";

    private static final double[][] REGIONS = {
        // NYC: lat range [40.70, 40.80], lng range [-74.02, -73.93]
        {40.70, 40.80, -74.02, -73.93},
        // SF: lat range [37.74, 37.82], lng range [-122.43, -122.38]
        {37.74, 37.82, -122.43, -122.38},
        // London: lat range [51.49, 51.53], lng range [-0.15, -0.08]
        {51.49, 51.53, -0.15, -0.08}
    };

    private static final double[] RADII = {0.5, 1.0, 2.0, 5.0, 20.0};

    private final HttpClient httpClient;
    private final String targetUrl;
    private final Timer timer;
    private final Counter successCounter;
    private final Counter errorCounter;
    private final Random random = new Random();

    public NearbySearchScenario(HttpClient httpClient, String targetUrl, MetricsServer metricsServer) {
        this.httpClient = httpClient;
        this.targetUrl = targetUrl;
        this.timer = metricsServer.getTimer(SCENARIO);
        this.successCounter = metricsServer.getCounter(SCENARIO, "success");
        this.errorCounter = metricsServer.getCounter(SCENARIO, "error");
    }

    @Override
    public void run() {
        double[] region = REGIONS[random.nextInt(REGIONS.length)];
        double lat = region[0] + random.nextDouble() * (region[1] - region[0]);
        double lng = region[2] + random.nextDouble() * (region[3] - region[2]);
        double radius = RADII[random.nextInt(RADII.length)];

        String url = String.format("%s/v1/search/nearby?latitude=%.6f&longitude=%.6f&radius=%.1f",
            targetUrl, lat, lng, radius);

        Timer.Sample sample = Timer.start();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            sample.stop(timer);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                successCounter.increment();
            } else {
                errorCounter.increment();
            }
        } catch (Exception e) {
            sample.stop(timer);
            errorCounter.increment();
        }
    }
}
