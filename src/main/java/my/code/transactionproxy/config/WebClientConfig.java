package my.code.transactionproxy.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

/**
 * Конфігураційний клас Spring для налаштування WebClient, який використовується для виконання HTTP-запитів до зовнішніх API.
 */
@Configuration // Позначає клас як джерело конфігурації Spring, дозволяючи визначати біни.
public class WebClientConfig {

    /**
     * Створює та налаштовує бін WebClient для використання в додатку.
     * @return Налаштований екземпляр WebClient.
     */
    @Bean // Позначає метод як такий, що створює бін, який буде керованим Spring.
    public WebClient webClient() {
        // Налаштовує пул з'єднань для HTTP-клієнта з використанням ConnectionProvider.
        ConnectionProvider connectionProvider = ConnectionProvider.builder("custom") // Створює будівельник ConnectionProvider з назвою "custom".
                .maxConnections(50) // Встановлює максимальну кількість одночасних з'єднань у пулі (до 50).
                .maxIdleTime(Duration.ofSeconds(20)) // Вказує максимальний час простою з'єднання в пулі (20 секунд).
                .maxLifeTime(Duration.ofSeconds(60)) // Вказує максимальний час життя з'єднання (60 секунд).
                .pendingAcquireTimeout(Duration.ofSeconds(60)) // Встановлює тайм-аут для очікування доступного з'єднання (60 секунд).
                .evictInBackground(Duration.ofSeconds(120)) // Налаштовує фоновий процес видалення застарілих з'єднань кожні 120 секунд.
                .build(); // Створює налаштований ConnectionProvider.

        // Налаштовує HTTP-клієнт на основі Reactor Netty з використанням ConnectionProvider.
        HttpClient httpClient = HttpClient.create(connectionProvider) // Створює HttpClient із заданим ConnectionProvider.
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000) // Встановлює тайм-аут підключення до сервера (30 секунд).
                .responseTimeout(Duration.ofMinutes(10)) // Встановлює тайм-аут для отримання відповіді від сервера (10 хвилин).
                .compress(true); // Вмикає стиснення даних (наприклад, gzip) для зменшення обсягу передачі.

        // Налаштовує стратегії обміну даними для WebClient, зокрема кодеки для серіалізації/десеріалізації.
        ExchangeStrategies strategies = ExchangeStrategies.builder() // Створює будівельник для ExchangeStrategies.
                .codecs(configurer -> { // Налаштовує кодеки для обробки даних.
                    configurer.defaultCodecs().maxInMemorySize(100 * 1024 * 1024); // Встановлює максимальний розмір даних у пам'яті (100 МБ).
                    configurer.defaultCodecs().enableLoggingRequestDetails(false); // Вимикає логування деталей запитів для безпеки.
                })
                .build(); // Створює налаштовані ExchangeStrategies.

        // Створює та повертає екземпляр WebClient із заданими параметрами.
        return WebClient.builder() // Створює будівельник WebClient.
                .baseUrl("https://api.spending.gov.ua/api/v2/api") // Встановлює базову URL-адресу для всіх запитів WebClient.
                .exchangeStrategies(strategies) // Застосовує налаштовані стратегії обміну даними.
                .clientConnector(new ReactorClientHttpConnector(httpClient)) // Встановлює HttpClient як конектор для виконання запитів.
                .build(); // Створює фінальний екземпляр WebClient.
    }
}