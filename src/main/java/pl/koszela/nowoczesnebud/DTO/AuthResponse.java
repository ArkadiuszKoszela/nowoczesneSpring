package pl.koszela.nowoczesnebud.DTO;

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
    private String refreshToken;
    private long refreshExpiresInMs;
}
