package pl.koszela.nowoczesnebud.DTO;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class VerifyEmailRequest {

    @NotBlank(message = "Email jest wymagany")
    @Size(max = 120, message = "Email może mieć maksymalnie 120 znaków")
    private String email;

    @NotBlank(message = "Kod weryfikacyjny jest wymagany")
    @Pattern(regexp = "^\\d{6}$", message = "Kod weryfikacyjny musi mieć 6 cyfr")
    private String code;
}
