package pl.koszela.nowoczesnebud.DTO;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Data
public class CalendarReminderRequest {

    @NotBlank(message = "Typ przypomnienia jest wymagany")
    private String method;

    @Min(value = 0, message = "Minuty przypomnienia nie mogą być ujemne")
    @Max(value = 40320, message = "Minuty przypomnienia nie mogą przekroczyć 4 tygodni")
    private Integer minutes;
}

