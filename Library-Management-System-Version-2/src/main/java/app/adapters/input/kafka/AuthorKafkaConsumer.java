package app.adapters.input.kafka;

import app.domain.models.Author;
import app.domain.port.input.AuthorUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AuthorKafkaConsumer {

    private final AuthorUseCase authorUseCase;

    @Autowired
    public AuthorKafkaConsumer(AuthorUseCase authorUseCase) {
        this.authorUseCase = authorUseCase;
    }

    //@KafkaListener(topics = "authors")
    public void consume(String message) throws JsonProcessingException {
        // Assuming the message is a JSON String
        // Use GSON or Jackson to parse the message

        ObjectMapper mapper = new ObjectMapper();
        Author author = mapper.readValue(message, Author.class);
    }
}
