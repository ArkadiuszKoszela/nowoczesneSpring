package pl.koszela.nowoczesnebud.DTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class LoginRequest {

    @NotBlank(message = "Email jest wymagany")
    @Size(max = 120, message = "Email może mieć maksymalnie 120 znaków")
    @JsonAlias({"username", "login"})
    private String email;

    @NotBlank(message = "Hasło jest wymagane")
    private String password;

    private Boolean rememberMe = false;
}
