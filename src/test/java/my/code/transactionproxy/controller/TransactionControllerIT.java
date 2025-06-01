package my.code.transactionproxy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import my.code.transactionproxy.dto.PagedTransactionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
class TransactionControllerIT {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnTransactionsWithPagination() {
        // Given
        String edrpou = "00013480";
        String startDate = "2024-10-29";
        String endDate = "2024-10-31";

        // When & Then
        webTestClient
                .mutate()
                .responseTimeout(Duration.ofMinutes(2))
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v2/api/transactions")
                        .queryParam("recipt_edrpous", edrpou)
                        .queryParam("startdate", startDate)
                        .queryParam("enddate", endDate)
                        .queryParam("page", 0)
                        .queryParam("size", 10)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(PagedTransactionResponse.class)
                .value(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getTransactions()).isNotNull();
                    assertThat(response.getPage()).isNotNull();
                    assertThat(response.getPage().getCurrentPage()).isEqualTo(0);
                    assertThat(response.getPage().getPageSize()).isEqualTo(10);

                    response.getTransactions().forEach(transaction -> {
                        assertThat(transaction.getHash()).isNotNull();
                        assertThat(transaction.getHash()).isNotEmpty();
                        assertThat(transaction.getTransaction()).isNotNull();
                    });
                });
    }

    @Test
    void shouldReturnTransactionsWithMultipleEdrpous() {
        // Given
        String[] edrpous = {"14360570", "21560766"};
        String startDate = "2024-10-01";
        String endDate = "2024-10-31";

        // When & Then
        webTestClient
                .mutate()
                .responseTimeout(Duration.ofMinutes(2))
                .build()
                .get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/api/v2/api/transactions")
                            .queryParam("startdate", startDate)
                            .queryParam("enddate", endDate)
                            .queryParam("page", 0)
                            .queryParam("size", 100);

                    for (String edrpou : edrpous) {
                        builder = builder.queryParam("recipt_edrpous", edrpou);
                    }

                    return builder.build();
                })
                .exchange()
                .expectStatus().isOk()
                .expectBody(PagedTransactionResponse.class)
                .value(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getTransactions()).isNotNull();
                    assertThat(response.getPage()).isNotNull();

                    if (!response.getTransactions().isEmpty()) {
                        boolean hasMultipleEdrpous = response.getTransactions().stream()
                                                             .map(tx -> tx.getTransaction().getRecipt_edrpou())
                                                             .distinct()
                                                             .count() > 1;

                        assertThat(response.getTransactions().stream()
                                .allMatch(tx -> tx.getTransaction().getRecipt_edrpou() != null))
                                .isTrue();
                    }
                });
    }

    @Test
    void shouldHandleInvalidDateRange() {
        // Given - invalid date range (start after end)
        String edrpou = "00013480";
        String startDate = "2024-12-31";
        String endDate = "2024-10-01";

        // When & Then
        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v2/api/transactions")
                        .queryParam("recipt_edrpous", edrpou)
                        .queryParam("startdate", startDate)
                        .queryParam("enddate", endDate)
                        .build())
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void shouldHandleMissingParameters() {
        // When & Then - missing required parameters
        webTestClient
                .get()
                .uri("/api/v2/api/transactions")
                .exchange()
                .expectStatus().is5xxServerError();
    }
}