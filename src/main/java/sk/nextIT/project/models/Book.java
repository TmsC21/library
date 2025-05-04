package sk.nextIT.project.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class Book {
    private Long id;

    @NotBlank(message = "Názov knihy je povinný")
    @Size(max = 15, message = "Názov knihy môže mať maximálne 15 znakov")
    @JsonProperty("Name")
    private String name;

    @NotBlank(message = "Autor je povinný")
    @JsonProperty("Author")
    private String author;

    @JsonProperty("Borrowed")
    @Valid
    private Borrowed borrowed;
}
