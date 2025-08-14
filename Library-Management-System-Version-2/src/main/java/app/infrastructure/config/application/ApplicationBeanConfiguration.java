package app.infrastructure.config.application;

import app.adapters.in.dto.CreateNewAuthor;
import app.adapters.in.dto.CreateNewBook;
import app.adapters.in.dto.CreateNewCustomer;
import app.adapters.in.dto.importData.ImportAuthorDto;
import app.adapters.in.dto.importData.ImportBookDto;
import app.adapters.in.dto.importData.ImportCustomerDto;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


@Configuration
public class ApplicationBeanConfiguration {


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
                .setMatchingStrategy(MatchingStrategies.STRICT) // match only exact names
                .setSkipNullEnabled(true) // don’t overwrite existing non-null fields
                .setFieldMatchingEnabled(true) // allow mapping directly to fields without setters
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE);

        // Null-safe converters for primitive targets
        Converter<Integer, Integer> nullSafeInt = ctx -> ctx.getSource() == null ? 0 : ctx.getSource();
        Converter<Boolean, Boolean> nullSafeBoolTrue = ctx -> ctx.getSource() == null ? Boolean.TRUE : ctx.getSource();

        // Element mapping: ImportAuthorDto -> CreateNewAuthor
        modelMapper.createTypeMap(ImportAuthorDto.class, CreateNewAuthor.class)
                .addMappings(m -> {
                    m.map(ImportAuthorDto::getName, CreateNewAuthor::setName);
                    m.map(ImportAuthorDto::getBio, CreateNewAuthor::setBio);
                });

        // List mapping will be inferred if element mapping exists

        // ImportBookDto -> CreateNewBook
        TypeMap<ImportBookDto, CreateNewBook> bookMap = modelMapper.createTypeMap(ImportBookDto.class, CreateNewBook.class);
        bookMap.addMappings(m -> {
            m.using(nullSafeInt).map(ImportBookDto::getPublicationYear, CreateNewBook::setPublicationYear);
            m.map(ImportBookDto::getTitle, CreateNewBook::setTitle);
            m.map(ImportBookDto::getIsbn, CreateNewBook::setIsbn);
            m.map(ImportBookDto::getAuthors, CreateNewBook::setAuthors);
        });

        // ImportCustomerDto -> CreateNewCustomer (isActive -> privileges)
        TypeMap<ImportCustomerDto, CreateNewCustomer> customerMap = modelMapper.createTypeMap(ImportCustomerDto.class, CreateNewCustomer.class);
        customerMap.addMappings(m -> {
            m.map(ImportCustomerDto::getName, CreateNewCustomer::setName);
            m.map(ImportCustomerDto::getEmail, CreateNewCustomer::setEmail);
            m.using(nullSafeBoolTrue).map(ImportCustomerDto::getIsActive, CreateNewCustomer::setPrivileges);
        });

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