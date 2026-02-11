package pl.koszela.nowoczesnebud.Auth;

import pl.koszela.nowoczesnebud.DTO.AuthResponse;
import pl.koszela.nowoczesnebud.DTO.ForgotPasswordRequest;
import pl.koszela.nowoczesnebud.DTO.GoogleLoginRequest;
import pl.koszela.nowoczesnebud.DTO.LoginRequest;
import pl.koszela.nowoczesnebud.DTO.RegisterRequest;
import pl.koszela.nowoczesnebud.DTO.ResetPasswordRequest;
import pl.koszela.nowoczesnebud.DTO.ResendVerificationCodeRequest;
import pl.koszela.nowoczesnebud.DTO.UserProfileResponse;
import pl.koszela.nowoczesnebud.DTO.VerifyEmailRequest;
import pl.koszela.nowoczesnebud.Model.AppUser;

public interface AuthService {
    AppUser register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse googleLogin(GoogleLoginRequest request);
    void verifyEmail(VerifyEmailRequest request);
    void resendVerificationCode(ResendVerificationCodeRequest request);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
    AuthResponse refreshToken(String refreshToken);
    void logout(String refreshToken);
    UserProfileResponse getCurrentUserProfile(String username);
    long getRefreshTokenExpirationMs();
}
