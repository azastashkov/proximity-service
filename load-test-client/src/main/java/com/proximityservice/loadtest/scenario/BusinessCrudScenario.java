package com.proximityservice.loadtest.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.proximityservice.loadtest.metrics.MetricsServer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Random;

public class BusinessCrudScenario implements Runnable {

    private static final String SCENARIO = "business_crud";

    // NYC lat range [40.70, 40.80], lng range [-74.02, -73.93]
    private static final double NYC_LAT_MIN = 40.70;
    private static final double NYC_LAT_MAX = 40.80;
    private static final double NYC_LNG_MIN = -74.02;
    private static final double NYC_LNG_MAX = -73.93;

    private final HttpClient httpClient;
    private final String targetUrl;
    private final ObjectMapper objectMapper;
    private final Timer timer;
    private final Counter successCounter;
    private final Counter errorCounter;
    private final Random random = new Random();

    public BusinessCrudScenario(HttpClient httpClient, String targetUrl, MetricsServer metricsServer) {
        this.httpClient = httpClient;
        this.targetUrl = targetUrl;
        this.objectMapper = new ObjectMapper();
        this.timer = metricsServer.getTimer(SCENARIO);
        this.successCounter = metricsServer.getCounter(SCENARIO, "success");
        this.errorCounter = metricsServer.getCounter(SCENARIO, "error");
    }

    @Override
    public void run() {
        Timer.Sample sample = Timer.start();
        try {
            long businessId = createBusiness();
            if (businessId < 0) {
                sample.stop(timer);
                errorCounter.increment();
                return;
            }

            boolean readOk = readBusiness(businessId);
            if (!readOk) {
                sample.stop(timer);
                errorCounter.increment();
                deleteBusiness(businessId);
                return;
            }

            boolean deleteOk = deleteBusiness(businessId);
            sample.stop(timer);

            if (deleteOk) {
                successCounter.increment();
            } else {
                errorCounter.increment();
            }
        } catch (Exception e) {
            sample.stop(timer);
            errorCounter.increment();
        }
    }

    private long createBusiness() throws Exception {
        double lat = NYC_LAT_MIN + random.nextDouble() * (NYC_LAT_MAX - NYC_LAT_MIN);
        double lng = NYC_LNG_MIN + random.nextDouble() * (NYC_LNG_MAX - NYC_LNG_MIN);
        int suffix = random.nextInt(100000);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("name", "LoadTest Business " + suffix);
        body.put("description", "Auto-generated load test business");
        body.put("address", suffix + " Test Street");
        body.put("city", "New York");
        body.put("state", "NY");
        body.put("country", "US");
        body.put("zipCode", "10001");
        body.put("latitude", lat);
        body.put("longitude", lng);
        body.put("category", "TEST");
        body.put("phone", "555-0100");
        body.put("website", "https://loadtest.example.com");

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(targetUrl + "/v1/businesses"))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 201) {
            JsonNode json = objectMapper.readTree(response.body());
            return json.path("id").asLong(-1);
        }
        return -1;
    }

    private boolean readBusiness(long id) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(targetUrl + "/v1/businesses/" + id))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() == 200;
    }

    private boolean deleteBusiness(long id) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(targetUrl + "/v1/businesses/" + id))
            .timeout(Duration.ofSeconds(10))
            .DELETE()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() == 204;
    }
}
