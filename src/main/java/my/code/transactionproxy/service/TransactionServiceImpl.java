package my.code.transactionproxy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.code.transactionproxy.dto.PagedTransactionResponse;
import my.code.transactionproxy.dto.TransactionDTO;
import my.code.transactionproxy.dto.TransactionQueryParams;
import my.code.transactionproxy.dto.TransactionResponseDTO;
import my.code.transactionproxy.entity.TransactionEntity;
import my.code.transactionproxy.exception.TransactionDeserializationException;
import my.code.transactionproxy.exception.TransactionFetchException;
import my.code.transactionproxy.mapper.TransactionMapper;
import my.code.transactionproxy.repository.TransactionEntityRepository;
import my.code.transactionproxy.util.HashUtil;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final TransactionEntityRepository transactionEntityRepository;
    private final TransactionCacheService cacheService;
    private final TransactionMapper transactionMapper;

    private static final int MAX_TRANSACTIONS_PER_REQUEST = 5000;
    private static final int PROCESSING_PARALLELISM = 8;

    @Override
    public Mono<PagedTransactionResponse> fetchTransactionsPaged(TransactionQueryParams queryParams) {
        return validateParams(queryParams)
                .flatMap(this::processTransactionsWithPagination);
    }

    private Mono<TransactionQueryParams> validateParams(TransactionQueryParams params) {
        return Mono.fromCallable(() -> {
            if (params.getEdrpous() == null || params.getEdrpous().isEmpty()) {
                throw new IllegalArgumentException("EDRPOU list cannot be empty");
            }
            if (params.getStartDate().isAfter(params.getEndDate())) {
                throw new IllegalArgumentException("Start date cannot be after end date");
            }
            long daysBetween = ChronoUnit.DAYS.between(params.getStartDate(), params.getEndDate());
            if (daysBetween > 92) {
                throw new IllegalArgumentException(
                        String.format("Date range cannot exceed 92 days. Current range: %d days", daysBetween));
            }
            return params;
        });
    }

    private Mono<PagedTransactionResponse> processTransactionsWithPagination(TransactionQueryParams params) {
        return getCachedTransactions(params)
                .flatMap(cached -> {
                    Set<Long> cachedIds = cached.stream()
                            .map(TransactionEntity::getId)
                            .collect(Collectors.toSet());

                    Flux<TransactionResponseDTO> cachedTransactions = Flux.fromIterable(cached)
                            .flatMap(this::convertCachedTransactionSafely);

                    Flux<TransactionResponseDTO> freshTransactions = fetchFreshTransactionsStream(params, cachedIds);

                    return Flux.concat(cachedTransactions, freshTransactions)
                            .take(MAX_TRANSACTIONS_PER_REQUEST)
                            .skip((long) params.getPage() * params.getSize())
                            .take(params.getSize())
                            .collectList()
                            .flatMap(transactions -> buildPagedResponse(transactions, params));
                });
    }

    private Mono<TransactionResponseDTO> convertCachedTransactionSafely(TransactionEntity entity) {
        return Mono.fromCallable(() -> convertCachedTransaction(entity))
                .onErrorResume(e -> {
                    log.error("Failed to convert cached transaction {}: {}", entity.getId(), e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<List<TransactionEntity>> getCachedTransactions(TransactionQueryParams params) {
        return Mono.fromCallable(() ->
                params.getEdrpous().stream()
                        .flatMap(edrpou -> transactionEntityRepository
                                .findAllByReciptEdrpouAndDocDateBetween(
                                        edrpou,
                                        params.getStartDate(),
                                        params.getEndDate()
                                ).stream())
                        .collect(Collectors.toList())
        ).subscribeOn(Schedulers.boundedElastic());
    }

    private Flux<TransactionResponseDTO> fetchFreshTransactionsStream(TransactionQueryParams params,
                                                                      Set<Long> excludeIds) {
        return webClient.get()
                .uri(uriBuilder -> buildUri(uriBuilder, params))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .map(errorBody -> new TransactionFetchException(
                                        "Failed to fetch transactions: " + response.statusCode(), errorBody)))
                .bodyToFlux(TransactionDTO.class)
                .filter(tx -> !excludeIds.contains(tx.getId()))
                .take(MAX_TRANSACTIONS_PER_REQUEST)
                .flatMap(this::processAndCacheTransaction, PROCESSING_PARALLELISM)
                .onErrorContinue((throwable, obj) -> {
                    log.error("Error processing transaction {}: {}", obj, throwable.getMessage());
                });
    }

    private URI buildUri(UriBuilder uriBuilder, TransactionQueryParams params) {
        var builder = uriBuilder.path("/transactions/")
                .queryParam("startdate", params.getStartDate().toString())
                .queryParam("enddate", params.getEndDate().toString());

        params.getEdrpous().forEach(edrpou ->
                builder.queryParam("recipt_edrpous", edrpou));

        return builder.build();
    }

    private Mono<TransactionResponseDTO> processAndCacheTransaction(TransactionDTO dto) {
        return Mono.fromCallable(() -> {
            String hash = HashUtil.sha256(dto);

            Mono.fromCallable(() -> cacheService.cacheTransaction(dto))
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe(
                            result -> log.debug("Transaction {} cached successfully", dto.getId()),
                            error -> log.warn("Failed to cache transaction {}: {}", dto.getId(), error.getMessage())
                    );

            return transactionMapper.toResponseDTO(dto, hash);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private TransactionResponseDTO convertCachedTransaction(TransactionEntity entity) {
        try {
            TransactionDTO dto = objectMapper.readValue(entity.getRawJson(), TransactionDTO.class);
            return transactionMapper.toResponseDTO(dto, entity.getHash());
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize cached transaction {}: {}", entity.getId(), e.getMessage());
            throw new TransactionDeserializationException("Failed to process cached transaction", e);
        }
    }

    private Mono<PagedTransactionResponse> buildPagedResponse(List<TransactionResponseDTO> transactions,
                                                              TransactionQueryParams params) {
        return Mono.fromCallable(() -> {
            long totalElements = Math.min(MAX_TRANSACTIONS_PER_REQUEST, transactions.size() + (long) params.getPage() * params.getSize());
            int totalPages = (int) Math.ceil((double) totalElements / params.getSize());

            PagedTransactionResponse.PageMetadata metadata = PagedTransactionResponse.PageMetadata.builder()
                    .currentPage(params.getPage())
                    .pageSize(params.getSize())
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .hasNext(params.getPage() < totalPages - 1)
                    .hasPrevious(params.getPage() > 0)
                    .build();

            return PagedTransactionResponse.builder()
                    .transactions(transactions)
                    .page(metadata)
                    .build();
        });
    }
}
