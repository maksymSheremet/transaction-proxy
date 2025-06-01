package my.code.transactionproxy.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Data
@Builder
public class TransactionQueryParams {
    private List<String> edrpous;
    private LocalDate startDate;
    private LocalDate endDate;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 1000;

    private static final int MAX_PAGE_SIZE = 2000;
    private static final int MIN_PAGE_SIZE = 1;

    public static TransactionQueryParams fromMultiValueMap(MultiValueMap<String, String> params) {
        List<String> edrpous = params.get("recipt_edrpous");
        LocalDate startDate = LocalDate.parse(requireNonNull(params.getFirst("startdate")));
        LocalDate endDate = LocalDate.parse(requireNonNull(params.getFirst("enddate")));

        int page = params.getFirst("page") != null ?
                Math.max(0, Integer.parseInt(requireNonNull(params.getFirst("page")))) : 0;

        int size = params.getFirst("size") != null ?
                Integer.parseInt(requireNonNull(params.getFirst("size"))) : 1000;

        size = Math.max(MIN_PAGE_SIZE, Math.min(size, MAX_PAGE_SIZE));

        return TransactionQueryParams.builder()
                .edrpous(edrpous)
                .startDate(startDate)
                .endDate(endDate)
                .page(page)
                .size(size)
                .build();
    }

    public int getSize() {
        return Math.max(MIN_PAGE_SIZE, Math.min(size, MAX_PAGE_SIZE));
    }
}
