package my.code.transactionproxy.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.code.transactionproxy.dto.PagedTransactionResponse;
import my.code.transactionproxy.dto.TransactionQueryParams;
import my.code.transactionproxy.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/api")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/transactions")
    public Mono<ResponseEntity<PagedTransactionResponse>> getTransactions(
            @RequestParam MultiValueMap<String, String> params) {

        log.info("Received request with params: {}", params);

        return transactionService.fetchTransactionsPaged(TransactionQueryParams.fromMultiValueMap(params))
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("Returning {} transactions",
                        response.getBody().getTransactions().size()));
    }
}

