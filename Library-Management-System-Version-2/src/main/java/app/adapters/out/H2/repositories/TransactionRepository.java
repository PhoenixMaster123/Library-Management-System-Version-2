package app.adapters.out.H2.repositories;

import app.adapters.out.H2.entity.TransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {
    List<TransactionEntity> findByBookBookId(UUID bookId);
    Page<TransactionEntity> findByCustomerCustomerId(UUID customerId, Pageable pageable);
    long countByCustomer_CustomerId(UUID customerId);
}
