package my.code.transactionproxy.repository;

import my.code.transactionproxy.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionEntityRepository extends JpaRepository<TransactionEntity, Long> {

    List<TransactionEntity> findAllByReciptEdrpouAndDocDateBetween(String edrpou, LocalDate from, LocalDate to);

    boolean existsById(@NonNull Long id);

    @Modifying
    @Query("DELETE FROM TransactionEntity t WHERE t.docDate < :cutoffDate")
    void deleteByDocDateBefore(LocalDate cutoffDate);
}
