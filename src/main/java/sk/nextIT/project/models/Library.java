package sk.nextIT.project.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class Library {
    @JsonProperty("Book")
    private List<Book> Book;
}
