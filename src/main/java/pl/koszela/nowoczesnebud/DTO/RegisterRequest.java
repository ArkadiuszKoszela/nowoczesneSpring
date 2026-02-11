package pl.koszela.nowoczesnebud.DTO;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class RegisterRequest {

    @NotBlank(message = "Login jest wymagany")
    @Size(min = 3, max = 60, message = "Login musi mieć 3-60 znaków")
    private String username;

    @NotBlank(message = "Email jest wymagany")
    @Email(message = "Niepoprawny email")
    @Size(max = 120, message = "Email może mieć maksymalnie 120 znaków")
    private String email;

    @NotBlank(message = "Hasło jest wymagane")
    @Size(min = 8, max = 120, message = "Hasło musi mieć 8-120 znaków")
    private String password;
}
