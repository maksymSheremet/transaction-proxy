package my.code.transactionproxy.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфігураційний клас Spring, який налаштовує ObjectMapper для роботи з JSON-серіалізацією та десеріалізацією.
 */
@Configuration // Позначає клас як джерело конфігурації Spring, дозволяючи визначати біни.
public class JacksonConfig {

    /**
     * Створює та налаштовує бін ObjectMapper для використання в додатку.
     * @return Налаштований екземпляр ObjectMapper.
     */
    @Bean // Позначає метод як такий, що створює бін, який буде керованим Spring.
    public ObjectMapper objectMapper() {
        return new ObjectMapper() // Створює новий екземпляр ObjectMapper, основного класу Jackson для роботи з JSON.
                .registerModule(new JavaTimeModule()) // Реєструє модуль JavaTimeModule для підтримки типів дати та часу з Java 8 (наприклад, LocalDate, LocalDateTime).
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Вимикає серіалізацію дат у вигляді числових міток часу, забезпечуючи вивід дат у текстовому форматі (наприклад, "2025-06-10").
    }
}