package pl.koszela.nowoczesnebud.DTO;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class ResendVerificationCodeRequest {

    @NotBlank(message = "Email jest wymagany")
    @Size(max = 120, message = "Email może mieć maksymalnie 120 znaków")
    private String email;
}
