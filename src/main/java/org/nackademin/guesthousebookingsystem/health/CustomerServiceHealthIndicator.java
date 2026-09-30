package org.nackademin.guesthousebookingsystem.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CustomerServiceHealthIndicator
        implements HealthIndicator {

    private static final Logger log = LoggerFactory
            .getLogger(CustomerServiceHealthIndicator.class);

    private final RestClient restClient;
    private final String customerServiceUrl;

    public CustomerServiceHealthIndicator(
            RestClient restClient,
            @Value("${customer.service.url:http://localhost:8081}")
            String customerServiceUrl) {
        this.restClient = restClient;
        this.customerServiceUrl = customerServiceUrl;
    }

    @Override
    public Health health() {
        try {
            log.info("Checking health of customer service "
                    + "at {}", customerServiceUrl);

            restClient.get()
                    .uri(customerServiceUrl + "/api/customers")
                    .retrieve()
                    .toBodilessEntity();

            log.info("Customer service is UP");

            return Health.up()
                    .withDetail("customerService", "UP")
                    .withDetail("url", customerServiceUrl)
                    .build();

        } catch (Exception e) {
            log.warn("Customer service is not reachable: {}",
                    e.getMessage());

            return Health.down()
                    .withDetail("customerService", "UNAVAILABLE")
                    .withDetail("url", customerServiceUrl)
                    .withDetail("note",
                            "Customer service is down but "
                                    + "booking service is still operational")
                    .build();
        }
    }
}