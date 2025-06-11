package my.code.transactionproxy.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Глобальний обробник винятків для REST-контролерів, який централізовано обробляє помилки та формує відповіді.
 */
@RestControllerAdvice // Позначає клас як глобальний обробник винятків для REST-контролерів, дозволяючи обробляти помилки в усьому додатку.
@Slf4j // Створює логер (SLF4J) з назвою змінної 'log' для цього класу (Lombok).
public class GlobalExceptionHandler {

    /**
     * Обробляє винятки IllegalArgumentException, які виникають через некоректні параметри запиту.
     * @param ex Виняток IllegalArgumentException.
     * @return ResponseEntity з HTTP-статусом 400 (Bad Request) та інформацією про помилку.
     */
    @ExceptionHandler(IllegalArgumentException.class) // Вказує, що метод обробляє винятки типу IllegalArgumentException.
    public ResponseEntity<Map<String, Object>> handleValidationException(IllegalArgumentException ex) {
        log.warn("Validation error: {}", ex.getMessage()); // Логує попередження з повідомленням про помилку валідації.
        return ResponseEntity.badRequest().body(createErrorResponse( // Повертає ResponseEntity з HTTP-статусом 400.
                HttpStatus.BAD_REQUEST.value(), // Код статусу HTTP (400).
                "Validation Error", // Назва помилки.
                ex.getMessage() // Детальне повідомлення про помилку.
        ));
    }

    /**
     * Обробляє винятки TransactionFetchException, які виникають при невдалому отриманні даних із зовнішнього API.
     * @param ex Виняток TransactionFetchException.
     * @return ResponseEntity з HTTP-статусом 502 (Bad Gateway) та інформацією про помилку.
     */
    @ExceptionHandler(TransactionFetchException.class) // Вказує, що метод обробляє винятки типу TransactionFetchException.
    public ResponseEntity<Map<String, Object>> handleTransactionFetchException(TransactionFetchException ex) {
        log.error("Failed to fetch transactions: {}", ex.getMessage()); // Логує помилку з повідомленням про невдале отримання транзакцій.
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(createErrorResponse( // Повертає ResponseEntity з HTTP-статусом 502.
                HttpStatus.BAD_GATEWAY.value(), // Код статусу HTTP (502).
                "External API Error", // Назва помилки.
                "Failed to fetch data from external service" // Детальне повідомлення про помилку.
        ));
    }

    /**
     * Обробляє винятки TransactionSerializationException, які виникають при проблемах із серіалізацією даних.
     * @param ex Виняток TransactionSerializationException.
     * @return ResponseEntity з HTTP-статусом 500 (Internal Server Error) та інформацією про помилку.
     */
    @ExceptionHandler(TransactionSerializationException.class) // Вказує, що метод обробляє винятки типу TransactionSerializationException.
    public ResponseEntity<Map<String, Object>> handleSerializationException(TransactionSerializationException ex) {
        log.error("Serialization error: {}", ex.getMessage()); // Логує помилку з повідомленням про проблему серіалізації.
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse( // Повертає ResponseEntity з HTTP-статусом 500.
                HttpStatus.INTERNAL_SERVER_ERROR.value(), // Код статусу HTTP (500).
                "Processing Error", // Назва помилки.
                "Failed to process transaction data" // Детальне повідомлення про помилку.
        ));
    }

    /**
     * Обробляє винятки TransactionDeserializationException, які виникають при проблемах із десеріалізацією даних.
     * @param ex Виняток TransactionDeserializationException.
     * @return ResponseEntity з HTTP-статусом 500 (Internal Server Error) та інформацією про помилку.
     */
    @ExceptionHandler(TransactionDeserializationException.class) // Вказує, що метод обробляє винятки типу TransactionDeserializationException.
    public ResponseEntity<Map<String, Object>> handleDeserializationException(TransactionDeserializationException ex) {
        log.error("Deserialization error: {}", ex.getMessage()); // Логує помилку з повідомленням про проблему десеріалізації.
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse( // Повертає ResponseEntity з HTTP-статусом 500.
                HttpStatus.INTERNAL_SERVER_ERROR.value(), // Код статусу HTTP (500).
                "Cache Error", // Назва помилки.
                "Failed to process cached data" // Детальне повідомлення про помилку.
        ));
    }

    /**
     * Обробляє винятки WebClientException, які виникають при проблемах із WebClient (зовнішніми HTTP-запитами).
     * @param ex Виняток WebClientException.
     * @return ResponseEntity з HTTP-статусом 503 (Service Unavailable) та інформацією про помилку.
     */
    @ExceptionHandler(WebClientException.class) // Вказує, що метод обробляє винятки типу WebClientException.
    public ResponseEntity<Map<String, Object>> handleWebClientException(WebClientException ex) {
        log.error("WebClient error: {}", ex.getMessage()); // Логує помилку з повідомленням про проблему WebClient.
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(createErrorResponse( // Повертає ResponseEntity з HTTP-статусом 503.
                HttpStatus.SERVICE_UNAVAILABLE.value(), // Код статусу HTTP (503).
                "Service Unavailable", // Назва помилки.
                "External service is temporarily unavailable" // Детальне повідомлення про помилку.
        ));
    }

    /**
     * Обробляє всі інші непередбачені винятки (загальний обробник).
     * @param ex Виняток типу Exception.
     * @return ResponseEntity з HTTP-статусом 500 (Internal Server Error) та інформацією про помилку.
     */
    @ExceptionHandler(Exception.class) // Вказує, що метод обробляє винятки типу Exception (загальний обробник).
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error: ", ex); // Логує помилку з повним стек-трейсом непередбаченої помилки.
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse( // Повертає ResponseEntity з HTTP-статусом 500.
                HttpStatus.INTERNAL_SERVER_ERROR.value(), // Код статусу HTTP (500).
                "Internal Server Error", // Назва помилки.
                "An unexpected error occurred" // Детальне повідомлення про помилку.
        ));
    }

    /**
     * Створює стандартизовану мапу для відповіді про помилку.
     * @param status Код HTTP-статусу.
     * @param error Назва помилки.
     * @param message Детальне повідомлення про помилку.
     * @return Мапа з інформацією про помилку (час, статус, назва, повідомлення).
     */
    private Map<String, Object> createErrorResponse(int status, String error, String message) {
        Map<String, Object> errorResponse = new LinkedHashMap<>(); // Створює мапу для структурованої відповіді про помилку.
        errorResponse.put("timestamp", LocalDateTime.now()); // Додає поточний час і дату виникнення помилки.
        errorResponse.put("status", status); // Додає HTTP-код статусу.
        errorResponse.put("error", error); // Додає назву помилки.
        errorResponse.put("message", message); // Додає детальне повідомлення про помилку.
        return errorResponse; // Повертає сформовану мапу відповіді.
    }
}
