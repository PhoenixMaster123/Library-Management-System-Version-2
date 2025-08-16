package app.adapters.output.repositories;

import app.adapters.output.entity.CustomerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID> {
    Optional<CustomerEntity> findByName(String name);
    @Query("SELECT c FROM CustomerEntity c LEFT JOIN c.transactions t " +
            "WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR CAST(c.customerId AS string) LIKE CONCAT('%', :query, '%') " +
            "OR CAST(t.transactionId AS string) LIKE CONCAT('%', :query, '%')")
    Page<CustomerEntity> searchByQuery(@Param("query") String query, Pageable pageable);
}
