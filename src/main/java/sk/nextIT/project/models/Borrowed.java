package sk.nextIT.project.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDate;

@Data
public class Borrowed {

    @JsonProperty("FirstName")
    private String firstName = "";
    @JsonProperty("LastName")
    private String lastName = "";
    @PastOrPresent(message = "Dátum výpožičky nemôže byť v budúcnosti")
    @NotNull(message = "Dátum výpožičky je povinný")
    @JsonFormat(pattern = "d.M.yyyy")
    @JsonProperty("From")
    private LocalDate from;
}
