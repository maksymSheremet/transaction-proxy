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

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionCacheService {

    private final TransactionEntityRepository repository;
    private final ObjectMapper objectMapper;
    private final TransactionMapper mapper;

    @Transactional
    public TransactionResponseDTO cacheTransaction(TransactionDTO dto) {
        try {
            String json = objectMapper.writeValueAsString(dto);
            String hash = HashUtil.sha256(dto);

            if (!repository.existsById(dto.getId())) {
                TransactionEntity entity = mapper.toEntity(dto, json, hash);
                repository.save(entity);
                log.debug("Transaction {} cached successfully", dto.getId());
            } else {
                log.debug("Transaction {} already exists in cache", dto.getId());
            }

            return mapper.toResponseDTO(dto, hash);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize transaction {}: {}", dto.getId(), e.getMessage());
            throw new TransactionSerializationException("Failed to cache transaction", e);
        } catch (Exception e) {
            log.error("Unexpected error caching transaction {}: {}", dto.getId(), e.getMessage());
            return mapper.toResponseDTO(dto, HashUtil.sha256(dto));
        }
    }

    @Transactional
    public void cleanupOldEntries() {
        try {
            repository.deleteByDocDateBefore(java.time.LocalDate.now().minusDays(30));
            log.info("Old cache entries cleaned up successfully");
        } catch (Exception e) {
            log.warn("Failed to cleanup old cache entries: {}", e.getMessage());
        }
    }
}
