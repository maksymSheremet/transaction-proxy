package my.code.transactionproxy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * Сервіс для моніторингу використання пам’яті в додатку та управління кешем при високому навантаженні.
 */
@Service // Позначає клас як сервіс Spring, який є компонентом бізнес-логіки.
@Slf4j // Створює логер (SLF4J) з назвою змінної 'log' для цього класу (Lombok).
@RequiredArgsConstructor // Автоматично генерує конструктор для всіх final-полів (Lombok).
public class MemoryMonitoringService {

    // Сервіс для управління кешем транзакцій.
    private final TransactionCacheService cacheService; // Інжектований сервіс для очищення кешу, ініціалізується через конструктор.

    // Поріг використання пам’яті, при перевищенні якого запускається очищення кешу.
    private static final double MEMORY_THRESHOLD = 0.8; // Встановлює поріг використання пам’яті (80%) для тригера очищення.

    /**
     * Періодично перевіряє використання пам’яті та очищає кеш при перевищенні порогу.
     * Виконується кожні 60 секунд.
     */
    @Scheduled(fixedRate = 60000) // Позначає метод як заплановане завдання, яке виконується кожні 60 секунд.
    public void monitorMemoryUsage() {
        // Отримує доступ до об’єкта для моніторингу пам’яті JVM.
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean(); // Отримує MemoryMXBean для доступу до метрик пам’яті.

        // Отримує дані про використання пам’яті в купі (heap).
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage(); // Отримує статистику використання пам’яті в купі.

        // Отримує кількість використаної пам’яті в байтах.
        long used = heapUsage.getUsed(); // Зберігає обсяг використаної пам’яті в купі.

        // Отримує максимальний доступний обсяг пам’яті в купі.
        long max = heapUsage.getMax(); // Зберігає максимальний обсяг пам’яті, доступний для купи.

        // Обчислює відсоток використаної пам’яті.
        double usagePercentage = (double) used / max; // Розраховує відсоток використаної пам’яті (used / max).

        // Логує деталі використання пам’яті у мегабайтах і відсотках.
        log.debug("Memory usage: {}MB / {}MB ({}%)",
                used / 1024 / 1024, // Конвертує використану пам’ять у мегабайти.
                max / 1024 / 1024, // Конвертує максимальну пам’ять у мегабайти.
                String.format("%.1f", usagePercentage * 100)); // Форматує відсоток використання з одним знаком після коми.

        // Перевіряє, чи перевищено поріг використання пам’яті.
        if (usagePercentage > MEMORY_THRESHOLD) { // Якщо використання пам’яті перевищує 80%.
            // Логує попередження про високе використання пам’яті та запуск очищення кешу.
            log.warn("Memory usage is high ({}%), triggering cache cleanup",
                    String.format("%.1f", usagePercentage * 100)); // Логує відсоток використання.
            cacheService.cleanupOldEntries(); // Викликає метод очищення старих записів у кеші.

            // Запускає збір сміття для звільнення пам’яті.
            System.gc(); // Викликає Garbage Collector для спроби звільнення пам’яті.
        }
    }

    /**
     * Періодично логує детальну статистику використання пам’яті (купа та неквапна пам’ять).
     * Виконується кожні 300 секунд (5 хвилин).
     */
    @Scheduled(fixedRate = 300000) // Позначає метод як заплановане завдання, яке виконується кожні 300 секунд.
    public void logDetailedMemoryStats() {
        // Отримує доступ до об’єкта для моніторингу пам’яті JVM.
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean(); // Отримує MemoryMXBean для доступу до метрик пам’яті.

        // Отримує дані про використання пам’яті в купі (heap).
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage(); // Отримує статистику використання пам’яті в купі.

        // Отримує дані про використання неквапної пам’яті (non-heap).
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage(); // Отримує статистику використання неквапної пам’яті.

        // Логує статистику пам’яті в купі (використана, виділена, максимальна).
        log.info("Heap Memory: Used={}MB, Committed={}MB, Max={}MB",
                heapUsage.getUsed() / 1024 / 1024, // Конвертує використану пам’ять у мегабайти.
                heapUsage.getCommitted() / 1024 / 1024, // Конвертує виділену пам’ять у мегабайти.
                heapUsage.getMax() / 1024 / 1024); // Конвертує максимальну пам’ять у мегабайти.

        // Логує статистику неквапної пам’яті (використана, виділена).
        log.info("Non-Heap Memory: Used={}MB, Committed={}MB",
                nonHeapUsage.getUsed() / 1024 / 1024, // Конвертує використану пам’ять у мегабайти.
                nonHeapUsage.getCommitted() / 1024 / 1024); // Конвертує виділену пам’ять у мегабайти.
    }
}
