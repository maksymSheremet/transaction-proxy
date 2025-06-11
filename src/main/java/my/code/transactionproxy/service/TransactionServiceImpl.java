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

/**
 * Реалізація сервісу для обробки транзакцій, включаючи отримання даних із зовнішнього API, кешування та пагінацію.
 */
@Service // Позначає клас як сервіс Spring, який є компонентом бізнес-логіки.
@RequiredArgsConstructor // Автоматично генерує конструктор для всіх final-полів (Lombok).
@Slf4j // Створює логер (SLF4J) з назвою змінної 'log' для цього класу (Lombok).
public class TransactionServiceImpl implements TransactionService {

    // Клієнт для виконання HTTP-запитів до зовнішнього API.
    private final WebClient webClient; // Інжектований WebClient для взаємодії з зовнішнім API.

    // Об’єкт для серіалізації/десеріалізації даних у формат JSON.
    private final ObjectMapper objectMapper; // Інжектований ObjectMapper для роботи з JSON.

    // Репозиторій для роботи з сутностями транзакцій у базі даних.
    private final TransactionEntityRepository transactionEntityRepository; // Інжектований репозиторій для доступу до кешованих транзакцій.

    // Сервіс для кешування транзакцій.
    private final TransactionCacheService cacheService; // Інжектований сервіс для управління кешем транзакцій.

    // Мапер для конвертації між DTO та сутностями.
    private final TransactionMapper transactionMapper; // Інжектований мапер для трансформації об’єктів.

    // Максимальна кількість транзакцій за один запит.
    private static final int MAX_TRANSACTIONS_PER_REQUEST = 5000; // Обмеження на кількість транзакцій у відповіді (5000).

    // Рівень паралелізму для обробки транзакцій.
    private static final int PROCESSING_PARALLELISM = 8; // Кількість паралельних потоків для обробки транзакцій.

    /**
     * Отримує сторінку транзакцій на основі параметрів запиту.
     * @param queryParams Параметри запиту для фільтрації та пагінації.
     * @return Mono, що містить PagedTransactionResponse із транзакціями та метаданими сторінки.
     */
    @Override
    public Mono<PagedTransactionResponse> fetchTransactionsPaged(TransactionQueryParams queryParams) {
        // Валідує параметри запиту перед обробкою.
        return validateParams(queryParams) // Викликає метод валідації параметрів.
                .flatMap(this::processTransactionsWithPagination); // Обробляє транзакції з пагінацією після валідації.
    }

    /**
     * Валідує параметри запиту, перевіряючи їх коректність.
     * @param params Параметри запиту.
     * @return Mono, що містить валідовані параметри або виняток у разі помилки.
     */
    private Mono<TransactionQueryParams> validateParams(TransactionQueryParams params) {
        return Mono.fromCallable(() -> {
            // Перевіряє, чи список ЄДРПОУ не є порожнім.
            if (params.getEdrpous() == null || params.getEdrpous().isEmpty()) {
                throw new IllegalArgumentException("EDRPOU list cannot be empty"); // Викидає виняток, якщо список ЄДРПОУ порожній.
            }
            // Перевіряє, чи початкова дата не пізніша за кінцеву.
            if (params.getStartDate().isAfter(params.getEndDate())) {
                throw new IllegalArgumentException("Start date cannot be after end date"); // Викидає виняток, якщо початкова дата пізніша за кінцеву.
            }
            // Обчислює кількість днів між датами.
            long daysBetween = ChronoUnit.DAYS.between(params.getStartDate(), params.getEndDate());
            // Перевіряє, чи діапазон дат не перевищує 92 дні.
            if (daysBetween > 92) {
                throw new IllegalArgumentException(
                        String.format("Date range cannot exceed 92 days. Current range: %d days", daysBetween)); // Викидає виняток, якщо діапазон перевищує 92 дні.
            }
            return params; // Повертає валідовані параметри.
        });
    }

    /**
     * Обробляє транзакції з урахуванням пагінації, комбінуючи кешовані та свіжі дані.
     * @param params Параметри запиту.
     * @return Mono, що містить PagedTransactionResponse із транзакціями.
     */
    private Mono<PagedTransactionResponse> processTransactionsWithPagination(TransactionQueryParams params) {
        // Отримує кешовані транзакції.
        return getCachedTransactions(params) // Викликає метод для отримання кешованих транзакцій.
                .flatMap(cached -> {
                    // Створює набір ID кешованих транзакцій для виключення дублікатів.
                    Set<Long> cachedIds = cached.stream()
                            .map(TransactionEntity::getId) // Отримує ID кожної кешованої транзакції.
                            .collect(Collectors.toSet()); // Збирає ID у Set.

                    // Перетворює кешовані транзакції у Flux<TransactionResponseDTO>.
                    Flux<TransactionResponseDTO> cachedTransactions = Flux.fromIterable(cached)
                            .flatMap(this::convertCachedTransactionSafely);

                    // Отримує свіжі транзакції з зовнішнього API, виключаючи кешовані ID.
                    Flux<TransactionResponseDTO> freshTransactions = fetchFreshTransactionsStream(params, cachedIds); // Отримує свіжі транзакції.

                    // Об’єднує кешовані та свіжі транзакції, застосовує пагінацію.
                    return Flux.concat(cachedTransactions, freshTransactions) // Комбінує потоки кешованих і свіжих транзакцій.
                            .take(MAX_TRANSACTIONS_PER_REQUEST) // Обмежує загальну кількість транзакцій до 5000.
                            .skip((long) params.getPage() * params.getSize()) // Пропускає транзакції для пагінації (зміщення).
                            .take(params.getSize()) // Бере потрібну кількість транзакцій для сторінки.
                            .collectList() // Збирає транзакції у список.
                            .flatMap(transactions -> buildPagedResponse(transactions, params)); // Формує відповідь із метаданими сторінки.
                });
    }

    /**
     * Безпечно конвертує кешовану транзакцію у TransactionResponseDTO.
     * @param entity Сутність транзакції з кешу.
     * @return Mono, що містить TransactionResponseDTO або порожній Mono у разі помилки.
     */
    private Mono<TransactionResponseDTO> convertCachedTransactionSafely(TransactionEntity entity) {
        return Mono.fromCallable(() -> convertCachedTransaction(entity)) // Конвертує сутність у TransactionResponseDTO.
                .onErrorResume(e -> {
                    // Логує помилку десеріалізації кешованої транзакції.
                    log.error("Failed to convert cached transaction {}: {}", entity.getId(), e.getMessage()); // Записує помилку з ID транзакції.
                    return Mono.empty(); // Повертає порожній Mono у разі помилки.
                });
    }

    /**
     * Отримує кешовані транзакції з бази даних на основі параметрів запиту.
     * @param params Параметри запиту.
     * @return Mono, що містить список кешованих транзакцій.
     */
    private Mono<List<TransactionEntity>> getCachedTransactions(TransactionQueryParams params) {
        return Mono.fromCallable(() ->
                params.getEdrpous().stream() // Перебирає список ЄДРПОУ.
                        .flatMap(edrpou -> transactionEntityRepository
                                .findAllByReciptEdrpouAndDocDateBetween(
                                        edrpou, // Шукає транзакції за кодом ЄДРПОУ.
                                        params.getStartDate(), // Фільтрує за початковою датою.
                                        params.getEndDate() // Фільтрує за кінцевою датою.
                                ).stream())
                        .collect(Collectors.toList()) // Збирає результати у список.
        ).subscribeOn(Schedulers.boundedElastic()); // Виконує операцію в пулі потоків boundedElastic.
    }

    /**
     * Отримує потік свіжих транзакцій із зовнішнього API, виключаючи кешовані ID.
     * @param params Параметри запиту.
     * @param excludeIds Набір ID транзакцій для виключення.
     * @return Flux із TransactionResponseDTO свіжих транзакцій.
     */
    private Flux<TransactionResponseDTO> fetchFreshTransactionsStream(TransactionQueryParams params,
                                                                      Set<Long> excludeIds) {
        return webClient.get() // Ініціює GET-запит через WebClient.
                .uri(uriBuilder -> buildUri(uriBuilder, params)) // Формує URI для запиту.
                .retrieve() // Отримує відповідь від API.
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class) // Отримує тіло помилки як рядок.
                                .map(errorBody -> new TransactionFetchException(
                                        "Failed to fetch transactions: " + response.statusCode(), errorBody))) // Викидає виняток у разі помилки.
                .bodyToFlux(TransactionDTO.class) // Перетворює тіло відповіді у потік TransactionDTO.
                .filter(tx -> !excludeIds.contains(tx.getId())) // Фільтрує транзакції, які вже є в кеші.
                .take(MAX_TRANSACTIONS_PER_REQUEST) // Обмежує кількість транзакцій до 5000.
                .flatMap(this::processAndCacheTransaction, PROCESSING_PARALLELISM) // Обробляє та кешує транзакції паралельно (8 потоків).
                .onErrorContinue((throwable, obj) -> {
                    // Логує помилку обробки транзакції, але продовжує обробку інших.
                    log.error("Error processing transaction {}: {}", obj, throwable.getMessage()); // Записує помилку з деталями.
                });
    }

    /**
     * Формує URI для запиту до зовнішнього API на основі параметрів.
     * @param uriBuilder Будівельник URI.
     * @param params Параметри запиту.
     * @return Сформований URI.
     */
    private URI buildUri(UriBuilder uriBuilder, TransactionQueryParams params) {
        var builder = uriBuilder.path("/transactions/") // Встановлює шлях до ендпоінту API.
                .queryParam("startdate", params.getStartDate().toString()) // Додає параметр початкової дати.
                .queryParam("enddate", params.getEndDate().toString()); // Додає параметр кінцевої дати.

        // Додає кожен код ЄДРПОУ як окремий параметр запиту.
        params.getEdrpous().forEach(edrpou ->
                builder.queryParam("recipt_edrpous", edrpou)); // Додає ЄДРПОУ до параметрів запиту.

        return builder.build(); // Повертає сформований URI.
    }

    /**
     * Обробляє та кешує транзакцію.
     * @param dto Об’єкт TransactionDTO.
     * @return Mono, що містить TransactionResponseDTO.
     */
    private Mono<TransactionResponseDTO> processAndCacheTransaction(TransactionDTO dto) {
        return Mono.fromCallable(() -> {
            // Обчислює SHA-256 хеш для транзакції.
            String hash = HashUtil.sha256(dto); // Генерує унікальний хеш транзакції.

            // Кешує транзакцію асинхронно.
            Mono.fromCallable(() -> cacheService.cacheTransaction(dto)) // Викликає метод кешування транзакції.
                    .subscribeOn(Schedulers.boundedElastic()) // Виконує в пулі boundedElastic.
                    .subscribe(
                            result -> log.debug("Transaction {} cached successfully", dto.getId()), // Логує успішне кешування.
                            error -> log.warn("Failed to cache transaction {}: {}", dto.getId(), error.getMessage()) // Логує помилку кешування.
                    );

            // Повертає DTO відповіді з хешем.
            return transactionMapper.toResponseDTO(dto, hash); // Конвертує TransactionDTO у TransactionResponseDTO.
        }).subscribeOn(Schedulers.boundedElastic()); // Виконує операцію в пулі boundedElastic.
    }

    /**
     * Конвертує кешовану транзакцію у TransactionResponseDTO.
     * @param entity Сутність транзакції з кешу.
     * @return TransactionResponseDTO із даними транзакції та хешем.
     * @throws TransactionDeserializationException у разі помилки десеріалізації.
     */
    private TransactionResponseDTO convertCachedTransaction(TransactionEntity entity) {
        try {
            // Десеріалізує JSON із сутності у TransactionDTO.
            TransactionDTO dto = objectMapper.readValue(entity.getRawJson(), TransactionDTO.class); // Перетворює JSON у TransactionDTO.
            return transactionMapper.toResponseDTO(dto, entity.getHash()); // Конвертує DTO у TransactionResponseDTO з хешем.
        } catch (JsonProcessingException e) {
            // Логує помилку десеріалізації кешованої транзакції.
            log.error("Failed to deserialize cached transaction {}: {}", entity.getId(), e.getMessage()); // Записує помилку з ID транзакції.
            throw new TransactionDeserializationException("Failed to process cached transaction", e); // Викидає виняток із деталями.
        }
    }

    /**
     * Формує відповідь із транзакціями та метаданими сторінки.
     * @param transactions Список транзакцій.
     * @param params Параметри запиту.
     * @return Mono, що містить PagedTransactionResponse.
     */
    private Mono<PagedTransactionResponse> buildPagedResponse(List<TransactionResponseDTO> transactions,
                                                              TransactionQueryParams params) {
        return Mono.fromCallable(() -> {
            // Обчислює загальну кількість елементів із урахуванням обмеження.
            long totalElements = Math.min(MAX_TRANSACTIONS_PER_REQUEST, transactions.size() + (long) params.getPage() * params.getSize()); // Визначає загальну кількість елементів.
            // Обчислює загальну кількість сторінок.
            int totalPages = (int) Math.ceil((double) totalElements / params.getSize()); // Розраховує кількість сторінок.

            // Створює метадані сторінки.
            PagedTransactionResponse.PageMetadata metadata = PagedTransactionResponse.PageMetadata.builder()
                    .currentPage(params.getPage()) // Встановлює поточну сторінку.
                    .pageSize(params.getSize()) // Встановлює розмір сторінки.
                    .totalElements(totalElements) // Встановлює загальну кількість елементів.
                    .totalPages(totalPages) // Встановлює загальну кількість сторінок.
                    .hasNext(params.getPage() < totalPages - 1) // Визначає, чи є наступна сторінка.
                    .hasPrevious(params.getPage() > 0) // Визначає, чи є попередня сторінка.
                    .build(); // Створює об’єкт метаданих.

            // Формує фінальну відповідь із транзакціями та метаданими.
            return PagedTransactionResponse.builder()
                    .transactions(transactions) // Встановлює список транзакцій.
                    .page(metadata) // Встановлює метадані сторінки.
                    .build(); // Повертає готовий PagedTransactionResponse.
        });
    }
}
