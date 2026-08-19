package app.infrastructure.config.application;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration.AccessLevel;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Beans that have no more natural home. */
@Configuration
public class BeanConfiguration {

    @Bean
    public Gson gson() {
        return new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .setPrettyPrinting()
                .create();
    }

    /**
     * Only the seeder maps with this, turning the JSON import DTOs into create DTOs; entities are
     * mapped by {@link app.adapters.output.mapper.EntityMapper}. STRICT refuses to guess.
     */
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setSkipNullEnabled(true)
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(AccessLevel.PRIVATE);

        modelMapper.addConverter((Converter<String, LocalDate>) context ->
                context.getSource() == null
                        ? null
                        : LocalDate.parse(context.getSource(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        return modelMapper;
    }
}
