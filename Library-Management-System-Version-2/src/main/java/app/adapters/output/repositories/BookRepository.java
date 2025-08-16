package app.adapters.output.repositories;

import app.adapters.output.entity.BookEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookRepository extends JpaRepository<BookEntity, UUID> {
    @Query("SELECT b FROM BookEntity b LEFT JOIN FETCH b.authors WHERE b.title = :title")
    Optional<BookEntity> findBookByTitle(@Param("title") String title);

    @Query("SELECT b FROM BookEntity b JOIN b.authors a WHERE a.name = :author AND b.availability = :isAvailable")
    List<BookEntity> findBooksByAuthor(@Param("author") String author, @Param("isAvailable") boolean isAvailable);

    @Query("SELECT b FROM BookEntity b WHERE b.isbn = :isbn")
    Optional<BookEntity> findBooksByIsbn(@Param("isbn") String isbn);
    Optional<BookEntity> findBookByBookId(@Param("id") UUID id);

    @Query("SELECT b FROM BookEntity b LEFT JOIN b.authors a " +
            "WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(b.isbn) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR CAST(b.publicationYear AS string) LIKE CONCAT('%', :query, '%') " +
            "OR LOWER(a.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<BookEntity> findBooksByQuery(@Param("query") String query, Pageable pageable);
}
