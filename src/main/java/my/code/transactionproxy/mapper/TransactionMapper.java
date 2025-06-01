package my.code.transactionproxy.mapper;

import my.code.transactionproxy.dto.TransactionDTO;
import my.code.transactionproxy.dto.TransactionResponseDTO;
import my.code.transactionproxy.entity.TransactionEntity;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionEntity toEntity(TransactionDTO dto, String json, String hash) {
        return new TransactionEntity(
                dto.getId(),
                json,
                hash,
                dto.getRecipt_edrpou(),
                dto.getDoc_date()
        );
    }

    public TransactionResponseDTO toResponseDTO(TransactionDTO dto, String hash) {
        return new TransactionResponseDTO(dto, hash);
    }
}
