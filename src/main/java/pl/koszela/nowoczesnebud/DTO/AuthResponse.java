package pl.koszela.nowoczesnebud.DTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private long expiresInMs;
    private String username;
    private String email;
    @JsonIgnore
    private String refreshToken;
    @JsonIgnore
    private long refreshExpiresInMs;
}
