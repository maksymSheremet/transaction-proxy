package my.code.transactionproxy.service;

import my.code.transactionproxy.dto.PagedTransactionResponse;
import my.code.transactionproxy.dto.TransactionQueryParams;
import reactor.core.publisher.Mono;

public interface TransactionService {

    Mono<PagedTransactionResponse> fetchTransactionsPaged(TransactionQueryParams queryParams);
}
