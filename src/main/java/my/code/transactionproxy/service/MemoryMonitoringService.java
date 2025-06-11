package my.code.transactionproxy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemoryMonitoringService {

    private final TransactionCacheService cacheService;
    private static final double MEMORY_THRESHOLD = 0.8;

    @Scheduled(fixedRate = 60000)
    public void monitorMemoryUsage() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();

        long used = heapUsage.getUsed();
        long max = heapUsage.getMax();
        double usagePercentage = (double) used / max;

        log.debug("Memory usage: {}MB / {}MB ({}%)",
                used / 1024 / 1024,
                max / 1024 / 1024,
                String.format("%.1f", usagePercentage * 100));

        if (usagePercentage > MEMORY_THRESHOLD) {
            log.warn("Memory usage is high ({}%), triggering cache cleanup",
                    String.format("%.1f", usagePercentage * 100));
            cacheService.cleanupOldEntries();

            System.gc();
        }

        if (usagePercentage > MEMORY_THRESHOLD) {
            log.warn("Memory usage is high ({}%), triggering cache cleanup",
                    String.format("%.1f", usagePercentage * 100));
            cacheService.cleanupOldEntries();

            log.info("Cache cleanup completed, waiting for JVM to manage memory");
        }
    }

    @Scheduled(fixedRate = 300000)
    public void logDetailedMemoryStats() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

        log.info("Heap Memory: Used={}MB, Committed={}MB, Max={}MB",
                heapUsage.getUsed() / 1024 / 1024,
                heapUsage.getCommitted() / 1024 / 1024,
                heapUsage.getMax() / 1024 / 1024);

        log.info("Non-Heap Memory: Used={}MB, Committed={}MB",
                nonHeapUsage.getUsed() / 1024 / 1024,
                nonHeapUsage.getCommitted() / 1024 / 1024);
    }
}
