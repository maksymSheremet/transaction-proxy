package my.code.transactionproxy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.code.transactionproxy.dto.TransactionDTO;
import my.code.transactionproxy.dto.TransactionResponseDTO;
import my.code.transactionproxy.entity.TransactionEntity;
import my.code.transactionproxy.exception.TransactionSerializationException;
import my.code.transactionproxy.mapper.TransactionMapper;
import my.code.transactionproxy.repository.TransactionEntityRepository;
import my.code.transactionproxy.util.HashUtil;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервіс для кешування транзакцій у базі даних та очищення старих записів.
 */
@Slf4j // Створює логер (SLF4J) з назвою змінної 'log' для цього класу (Lombok).
@Component // Позначає клас як компонент Spring, який буде автоматично виявлений та зареєстрований.
@RequiredArgsConstructor // Автоматично генерує конструктор для всіх final-полів (Lombok).
public class TransactionCacheService {

    // Репозиторій для взаємодії з базою даних для зберігання транзакцій.
    private final TransactionEntityRepository repository; // Інжектований репозиторій для роботи з сутностями транзакцій.

    // Об’єкт для серіалізації/десеріалізації даних у формат JSON.
    private final ObjectMapper objectMapper; // Інжектований ObjectMapper для роботи з JSON.

    // Мапер для конвертації між DTO та сутностями.
    private final TransactionMapper mapper; // Інжектований мапер для трансформації об’єктів TransactionDTO та TransactionEntity.

    /**
     * Кешує транзакцію в базі даних, якщо вона ще не існує.
     * @param dto Об’єкт TransactionDTO, що містить дані транзакції.
     * @return TransactionResponseDTO з даними транзакції та її хешем.
     */
    @Transactional // Позначає метод як транзакційний, забезпечуючи атомарність операцій із базою даних.
    public TransactionResponseDTO cacheTransaction(TransactionDTO dto) {
        try {
            // Серіалізує об’єкт TransactionDTO у JSON-рядок.
            String json = objectMapper.writeValueAsString(dto); // Перетворює DTO у JSON-рядок за допомогою ObjectMapper.

            // Обчислює SHA-256 хеш для об’єкта транзакції.
            String hash = HashUtil.sha256(dto); // Генерує унікальний хеш транзакції для ідентифікації.

            // Перевіряє, чи транзакція з таким ID уже існує в базі.
            if (!repository.existsById(dto.getId())) { // Якщо транзакція з ID не знайдена в базі.
                // Конвертує DTO у сутність для збереження в базі.
                TransactionEntity entity = mapper.toEntity(dto, json, hash); // Створює сутність TransactionEntity із DTO, JSON і хешу.

                // Зберігає сутність у базі даних.
                repository.save(entity); // Зберігає транзакцію в базі через репозиторій.

                // Логує успішне кешування транзакції.
                log.debug("Transaction {} cached successfully", dto.getId()); // Записує інформаційне повідомлення про успішне кешування.
            } else {
                // Логує, що транзакція вже існує в кеші.
                log.debug("Transaction {} already exists in cache", dto.getId()); // Записує інформаційне повідомлення, якщо транзакція вже є в базі.
            }

            // Повертає DTO відповіді з даними транзакції та її хешем.
            return mapper.toResponseDTO(dto, hash); // Конвертує DTO у TransactionResponseDTO, додаючи хеш.
        } catch (JsonProcessingException e) {
            // Логує помилку серіалізації транзакції.
            log.error("Failed to serialize transaction {}: {}", dto.getId(), e.getMessage()); // Записує помилку серіалізації з ID транзакції.

            // Викидає спеціалізований виняток для помилок серіалізації.
            throw new TransactionSerializationException("Failed to cache transaction", e); // Кидає виняток із деталями помилки.
        } catch (Exception e) {
            // Логує непередбачену помилку під час кешування.
            log.error("Unexpected error caching transaction {}: {}", dto.getId(), e.getMessage()); // Записує несподівану помилку з ID транзакції.

            // Повертає DTO відповіді, навіть якщо сталася помилка, для забезпечення стабільності.
            return mapper.toResponseDTO(dto, HashUtil.sha256(dto)); // Повертає TransactionResponseDTO з обчисленим хешем.
        }
    }

    /**
     * Очищає старі записи транзакцій із бази даних (старші за 30 днів).
     */
    @Transactional // Позначає метод як транзакційний, забезпечуючи атомарність операцій із базою даних.
    public void cleanupOldEntries() {
        try {
            // Видаляє транзакції, датовані раніше, ніж 30 днів тому.
            repository.deleteByDocDateBefore(java.time.LocalDate.now().minusDays(30)); // Видаляє записи з датою, старшою за 30 днів.

            // Логує успішне очищення старих записів.
            log.info("Old cache entries cleaned up successfully"); // Записує інформаційне повідомлення про успішне очищення.
        } catch (Exception e) {
            // Логує помилку під час очищення старих записів.
            log.warn("Failed to cleanup old cache entries: {}", e.getMessage()); // Записує попередження з деталями помилки.
        }
    }
}