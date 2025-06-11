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

/**
 * REST-контролер для обробки HTTP-запитів, пов'язаних із транзакціями.
 * Обробляє запити до ендпоінту /api/v2/api.
 */
@RestController // Позначає клас як REST-контролер, який обробляє HTTP-запити та повертає відповіді у форматі JSON.
@RequestMapping("/api/v2/api") // Вказує базовий шлях для всіх ендпоінтів цього контролера.
@RequiredArgsConstructor // Автоматично генерує конструктор для всіх final-полів (Lombok).
@Slf4j // Створює логер (SLF4J) з назвою змінної 'log' для цього класу (Lombok).
public class TransactionController {

    // Сервіс для обробки бізнес-логіки, пов'язаної з транзакціями.
    private final TransactionService transactionService; // Інжектований сервіс для роботи з транзакціями, ініціалізується через конструктор.

    /**
     * Обробляє GET-запит для отримання сторінки транзакцій із параметрами запиту.
     * @param params Параметри запиту у форматі MultiValueMap.
     * @return Mono, що містить ResponseEntity із відповіддю, яка включає сторінку транзакцій.
     */
    @GetMapping("/transactions") // Позначає метод як обробник GET-запитів до ендпоінту /api/v2/api/transactions.
    public Mono<ResponseEntity<PagedTransactionResponse>> getTransactions(
            @RequestParam MultiValueMap<String, String> params) { // Приймає параметри запиту у форматі MultiValueMap.

        // Логує отримані параметри запиту для дебагінгу.
        log.info("Received request with params: {}", params); // Записує інформаційне повідомлення з параметрами запиту.

        // Викликає сервіс для отримання сторінки транзакцій, конвертуючи параметри у TransactionQueryParams.
        return transactionService.fetchTransactionsPaged(TransactionQueryParams.fromMultiValueMap(params)) // Передає параметри в сервіс, який повертає Mono<PagedTransactionResponse>.
                .map(ResponseEntity::ok) // Перетворює результат у ResponseEntity з HTTP-статусом 200 (OK).
                .doOnSuccess(response -> log.info("Returning {} transactions",
                        response.getBody().getTransactions().size())) // Логує кількість повернутих транзакцій після успішного виконання.
                .onErrorReturn(ResponseEntity.internalServerError().build()); // У разі помилки повертає ResponseEntity з HTTP-статусом 500 (Internal Server Error).
    }
}

