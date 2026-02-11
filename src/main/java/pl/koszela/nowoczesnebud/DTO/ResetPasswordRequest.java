package pl.koszela.nowoczesnebud.DTO;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "Email jest wymagany")
    @Size(max = 120, message = "Email może mieć maksymalnie 120 znaków")
    private String email;

    @NotBlank(message = "Kod resetujący jest wymagany")
    @Pattern(regexp = "^\\d{6}$", message = "Kod resetujący musi mieć 6 cyfr")
    private String code;

    @NotBlank(message = "Nowe hasło jest wymagane")
    @Size(min = 8, max = 120, message = "Hasło musi mieć 8-120 znaków")
    private String newPassword;
}
