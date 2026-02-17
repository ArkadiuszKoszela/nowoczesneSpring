package pl.koszela.nowoczesnebud.DTO;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class BusinessStatusRequest {

    @NotBlank(message = "Nazwa statusu jest wymagana")
    @Size(max = 120, message = "Nazwa statusu może mieć maksymalnie 120 znaków")
    private String name;

    @Size(max = 20, message = "Kolor może mieć maksymalnie 20 znaków")
    private String color;

    private Boolean active = true;
    private Boolean requiresNextTask = false;
    private Boolean requiresLossReason = false;
    private Boolean terminal = false;
}

