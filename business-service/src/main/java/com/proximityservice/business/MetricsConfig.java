package com.proximityservice.business;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class MetricsConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> httpHistogramCustomizer() {
        return registry -> registry.config().meterFilter(
                new MeterFilter() {
                    @Override
                    public DistributionStatisticConfig configure(
                            io.micrometer.core.instrument.Meter.Id id,
                            DistributionStatisticConfig config) {
                        if (id.getName().equals("http.server.requests")) {
                            return DistributionStatisticConfig.builder()
                                    .percentilesHistogram(true)
                                    .minimumExpectedValue(Duration.ofMillis(1).toNanos() * 1.0)
                                    .maximumExpectedValue(Duration.ofSeconds(30).toNanos() * 1.0)
                                    .build()
                                    .merge(config);
                        }
                        return config;
                    }
                }
        );
    }
}
