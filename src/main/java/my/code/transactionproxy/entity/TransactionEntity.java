package my.code.transactionproxy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "transactions")
public class TransactionEntity {

    @Id
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String rawJson;

    @Column(nullable = false, length = 128)
    private String hash;

    @Column(nullable = false)
    private String reciptEdrpou;

    private LocalDate docDate;
}
