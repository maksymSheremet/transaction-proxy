package my.code.transactionproxy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import my.code.transactionproxy.dto.TransactionDTO;
import my.code.transactionproxy.dto.TransactionQueryParams;
import my.code.transactionproxy.dto.TransactionResponseDTO;
import my.code.transactionproxy.entity.TransactionEntity;
import my.code.transactionproxy.mapper.TransactionMapper;
import my.code.transactionproxy.repository.TransactionEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import static org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
import static org.springframework.web.reactive.function.client.WebClient.ResponseSpec;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RequestHeadersSpec requestHeadersSpec;

    @Mock
    private ResponseSpec responseSpec;

    @Mock
    private TransactionEntityRepository repository;

    @Mock
    private TransactionCacheService cacheService;

    @Mock
    private TransactionMapper mapper;

    private ObjectMapper objectMapper;
    private TransactionServiceImpl transactionService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        transactionService = new TransactionServiceImpl(
                webClient, objectMapper, repository, cacheService, mapper);
    }

    @Test
    void shouldValidateParameters() {
        // Given
        TransactionQueryParams invalidParams = TransactionQueryParams.builder()
                .edrpous(Collections.emptyList())
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();

        // When & Then
        StepVerifier.create(transactionService.fetchTransactionsPaged(invalidParams))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void shouldValidateDateRange() {
        // Given
        TransactionQueryParams invalidParams = TransactionQueryParams.builder()
                .edrpous(List.of("00013480"))
                .startDate(LocalDate.of(2024, 12, 31))
                .endDate(LocalDate.of(2024, 1, 1))
                .build();

        // When & Then
        StepVerifier.create(transactionService.fetchTransactionsPaged(invalidParams))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void shouldProcessCachedTransactions() throws Exception {
        // Given
        TransactionQueryParams params = TransactionQueryParams.builder()
                .edrpous(Arrays.asList("00013480"))
                .startDate(LocalDate.of(2024, 10, 1))
                .endDate(LocalDate.of(2024, 10, 31))
                .page(0)
                .size(10)
                .build();

        TransactionDTO dto = createTestTransactionDTO();
        TransactionEntity entity = TransactionEntity.builder()
                .id(1L)
                .rawJson(objectMapper.writeValueAsString(dto))
                .hash("test-hash")
                .reciptEdrpou("00013480")
                .docDate(LocalDate.of(2024, 10, 15))
                .build();

        TransactionResponseDTO responseDTO = new TransactionResponseDTO(dto, "test-hash");

        // When
        when(mapper.toResponseDTO(any(TransactionDTO.class), anyString()))
                .thenReturn(responseDTO);
        when(repository.findAllByReciptEdrpouAndDocDateBetween(anyString(), any(), any()))
                .thenReturn(Collections.singletonList(entity));
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class)))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(TransactionDTO.class)).thenReturn(Flux.empty());

        // Then
        StepVerifier.create(transactionService.fetchTransactionsPaged(params))
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getTransactions()).hasSize(1);
                    assertThat(response.getPage().getCurrentPage()).isEqualTo(0);
                    assertThat(response.getPage().getPageSize()).isEqualTo(10);
                })
                .verifyComplete();
    }

    private TransactionDTO createTestTransactionDTO() {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(1L);
        dto.setDoc_vob("TEST_DOC");
        dto.setDoc_number("TEST123");
        dto.setDoc_date(LocalDate.of(2024, 10, 15));
        dto.setAmount(1000.0);
        dto.setCurrency("UAH");
        dto.setPayer_edrpou("12345678");
        dto.setPayer_name("Test Payer");
        dto.setRecipt_edrpou("00013480");
        dto.setRecipt_name("Test Recipient");
        dto.setPayment_details("Test payment");
        return dto;
    }

}