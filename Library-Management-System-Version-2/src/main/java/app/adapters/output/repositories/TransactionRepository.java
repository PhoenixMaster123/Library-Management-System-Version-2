package app.adapters.output.repositories;

import app.adapters.output.entity.TransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
/** Spring Data access to loans. */
@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {
    List<TransactionEntity> findByBookBookId(UUID bookId);

    /** A book can only be out on one loan at a time, so the open one is unambiguous. */
    Optional<TransactionEntity> findFirstByBookBookIdAndReturnDateIsNull(UUID bookId);
    Page<TransactionEntity> findByCustomerCustomerId(UUID customerId, Pageable pageable);
    long countByCustomerCustomerId(UUID customerId);

    /** A loan still out: no return date has been recorded yet. */
    Page<TransactionEntity> findByReturnDateIsNull(Pageable pageable);

    long countByCustomerCustomerIdAndReturnDateIsNull(UUID customerId);

    /** Loans still out and falling due on a given day - the reminder job's daily sweep. */
    List<TransactionEntity> findByReturnDateIsNullAndDueDate(LocalDate dueDate);
}
