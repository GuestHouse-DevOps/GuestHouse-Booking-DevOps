package org.nackademin.guesthousebookingsystem.health;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component("customerService")
public class CustomerServiceHealthIndicator
        implements HealthIndicator {

    private static final Logger log = LoggerFactory
            .getLogger(CustomerServiceHealthIndicator.class);

    private final RestClient restClient;
    private final String customerServiceUrl;

    public CustomerServiceHealthIndicator(
            RestClient restClient,
            @Value("${customer.service.url}")
            String customerServiceUrl) {
        this.restClient = restClient;
        this.customerServiceUrl = customerServiceUrl;
    }

    @Override
    public Health health() {
        long start = System.nanoTime();

        try {
            log.info("Checking customer service health "
                    + "at {}", customerServiceUrl);

            restClient.get()
                    .uri(customerServiceUrl + "/api/customers")
                    .retrieve()
                    .toBodilessEntity();

            log.info("Customer service health check passed");

            return Health.up()
                    .withDetail("customerService", "UP")
                    .withDetail("responseTimeMs", elapsedMillis(start))
                    .build();

        } catch (Exception e) {
            log.warn("Customer service health check failed: {}",
                    e.getMessage());

            return Health.down()
                    .withDetail("customerService", "DOWN")
                    .withDetail("responseTimeMs", elapsedMillis(start))
                    .build();
        }
    }

    private static long elapsedMillis(long start) {
        return Duration.ofNanos(System.nanoTime() - start).toMillis();
    }
}
