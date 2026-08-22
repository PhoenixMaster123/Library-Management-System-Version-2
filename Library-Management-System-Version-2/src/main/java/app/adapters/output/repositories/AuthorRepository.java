package app.adapters.output.repositories;

import app.adapters.output.entity.AuthorEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data access to authors. */
@Repository
public interface AuthorRepository extends JpaRepository<AuthorEntity, UUID> {
    /** The author with exactly this name, or empty. */
    Optional<AuthorEntity> findByName(String name);

    @Query(
            value = "SELECT a FROM AuthorEntity a LEFT JOIN FETCH a.books",
            countQuery = "SELECT COUNT(a) FROM AuthorEntity a"
    )

    /** One page of authors with their books fetched, to avoid a query per row. */
    Page<AuthorEntity> findAllAuthorsWithBooks(Pageable pageable);

    @Query("SELECT a FROM AuthorEntity a LEFT JOIN a.books b " +
            "WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR CAST(a.authorId AS string) LIKE CONCAT('%', :query, '%') " +
            "OR LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(b.isbn) LIKE LOWER(CONCAT('%', :query, '%'))")

    /** One page of authors matching on name, id, or a book's title or ISBN. */
    Page<AuthorEntity> searchAuthorsByQuery(@Param("query") String query, Pageable pageable);

}
