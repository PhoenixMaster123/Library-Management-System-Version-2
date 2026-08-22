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

/** Spring Data access to members. */
@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID> {
    /** The member with exactly this name, or empty. */
    Optional<CustomerEntity> findByName(String name);
    @Query("SELECT c FROM CustomerEntity c LEFT JOIN c.transactions t " +
            "WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR CAST(c.customerId AS string) LIKE CONCAT('%', :query, '%') " +
            "OR CAST(t.transactionId AS string) LIKE CONCAT('%', :query, '%')")

    /** One page of members matching on name, email, id or a loan id. */
    Page<CustomerEntity> searchByQuery(@Param("query") String query, Pageable pageable);
}
