package my.code.transactionproxy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PagedTransactionResponse {
    private List<TransactionResponseDTO> transactions;
    private PageMetadata page;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class PageMetadata {
        private int currentPage;
        private int pageSize;
        private long totalElements;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }
}