package org.nackademin.guesthousebookingsystem.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CustomerServiceHealthIndicatorTest {

    private MockRestServiceServer server;
    private CustomerServiceHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        indicator = new CustomerServiceHealthIndicator(
                builder.build(),
                "http://customers"
        );
    }

    @Test
    void upWhenCustomerServiceResponds() {
        server.expect(
                requestTo("http://customers/api/customers")
        ).andRespond(withSuccess());

        assertThat(indicator.health().getStatus())
                .isEqualTo(Status.UP);
    }

    @Test
    void downWhenCustomerServiceFails() {
        server.expect(
                requestTo("http://customers/api/customers")
        ).andRespond(withServerError());

        assertThat(indicator.health().getStatus())
                .isEqualTo(Status.DOWN);
    }
}