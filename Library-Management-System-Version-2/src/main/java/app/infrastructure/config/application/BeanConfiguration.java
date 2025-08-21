package app.infrastructure.config.application;

import app.adapters.output.entity.AuthorEntity;
import app.adapters.output.entity.BookEntity;
import app.adapters.output.entity.CustomerEntity;
import app.adapters.output.entity.TransactionEntity;
import app.domain.model.Author;
import app.domain.model.Book;
import app.domain.model.Customer;
import app.domain.model.Transaction;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


@Configuration
public class BeanConfiguration {


    @Bean
    public Gson gson() {
        return new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .setPrettyPrinting()
                .create();
    }

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = getMapper();

        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setSkipNullEnabled(true)
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE);

        // AuthorEntity -> Author
        modelMapper.typeMap(AuthorEntity.class, Author.class).addMappings(m -> {
            m.map(AuthorEntity::getAuthorId, Author::setAuthorId);
            m.map(AuthorEntity::getName, Author::setName);
            m.map(AuthorEntity::getBio, Author::setBio);
            // ModelMapper will also map books automatically if BookEntity <-> Book is registered
        });

        // BookEntity -> Book
        modelMapper.typeMap(BookEntity.class, Book.class).addMappings(m -> {
            m.map(BookEntity::getBookId, Book::setBookId);
            m.map(BookEntity::getTitle, Book::setTitle);
            m.map(BookEntity::getIsbn, Book::setIsbn);
            m.map(BookEntity::getPublicationYear, Book::setPublicationYear);
            m.map(BookEntity::isAvailability, Book::setAvailable);
            m.map(BookEntity::getCreated_at, Book::setCreatedAt);
        });

        // Book -> BookEntity (for saving)
        modelMapper.typeMap(Book.class, BookEntity.class).addMappings(m -> {
            m.map(Book::getBookId, BookEntity::setBookId);
            m.map(Book::getTitle, BookEntity::setTitle);
            m.map(Book::getIsbn, BookEntity::setIsbn);
            m.map(Book::getPublicationYear, BookEntity::setPublicationYear);
            m.map(Book::isAvailable, BookEntity::setAvailability);
            m.map(Book::getCreatedAt, BookEntity::setCreated_at);
        });
        // Register mapping for CustomerEntity -> Customer
        modelMapper.typeMap(CustomerEntity.class, Customer.class);

        // Register mapping for TransactionEntity -> Transaction (nested Customer and Book will use their own mappings)
        modelMapper.typeMap(TransactionEntity.class, Transaction.class);

        return modelMapper;
    }

    private static ModelMapper getMapper() {
        ModelMapper modelMapper = new ModelMapper();

        // String -> LocalDate converter with yyyy-MM-dd format
        modelMapper.addConverter((Converter<String, LocalDate>) mappingContext -> {
            if (mappingContext.getSource() != null) {
                return LocalDate
                        .parse(mappingContext.getSource(),
                                DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
            return null;
        });
        return modelMapper;
    }

}