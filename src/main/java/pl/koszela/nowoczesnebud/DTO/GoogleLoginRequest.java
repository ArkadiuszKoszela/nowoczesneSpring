package pl.koszela.nowoczesnebud.DTO;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class GoogleLoginRequest {

    @NotBlank(message = "Google idToken jest wymagany")
    private String idToken;

    private Boolean rememberMe = false;
}
