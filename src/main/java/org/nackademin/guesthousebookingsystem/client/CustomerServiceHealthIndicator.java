package org.nackademin.guesthousebookingsystem.client;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CustomerServiceHealthIndicator implements HealthIndicator {

    private final RestClient restClient;
    private final String customerServiceUrl;

    public CustomerServiceHealthIndicator(RestClient restClient,
                                          @Value("${customer.service.url}") String customerServiceUrl) {
        this.restClient = restClient;
        this.customerServiceUrl = customerServiceUrl;
    }

    @Override
    public Health health() {
        long start = System.nanoTime();
        try {
            restClient.get()
                    .uri(customerServiceUrl + "/api/customers")
                    .retrieve()
                    .toBodilessEntity();
            return Health.up()
                    .withDetail("latencyMs", elapsedMillis(start))
                    .build();
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("latencyMs", elapsedMillis(start))
                    .build();
        }
    }

    private static long elapsedMillis(long start) {
        return Duration.ofNanos(System.nanoTime() - start).toMillis();
    }
}
