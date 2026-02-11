package pl.koszela.nowoczesnebud.Auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.koszela.nowoczesnebud.Model.AppUser;
import pl.koszela.nowoczesnebud.Model.PasswordResetCode;
import pl.koszela.nowoczesnebud.Repository.AppUserRepository;
import pl.koszela.nowoczesnebud.Repository.PasswordResetCodeRepository;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int OTP_LENGTH = 6;

    private final AppUserRepository appUserRepository;
    private final PasswordResetCodeRepository passwordResetCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.auth.password-reset.code-ttl-minutes:10}")
    private int codeTtlMinutes;

    @Value("${app.auth.password-reset.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.auth.password-reset.resend-cooldown-seconds:60}")
    private int resendCooldownSeconds;

    @Value("${app.auth.password-reset.daily-send-limit:10}")
    private int dailySendLimit;

    @Value("${app.auth.password-reset.log-plain-code:false}")
    private boolean logPlainCode;

    @Value("${app.mail.from:}")
    private String mailFrom;

    public PasswordResetService(AppUserRepository appUserRepository,
                                PasswordResetCodeRepository passwordResetCodeRepository,
                                PasswordEncoder passwordEncoder,
                                JavaMailSender mailSender) {
        this.appUserRepository = appUserRepository;
        this.passwordResetCodeRepository = passwordResetCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    @Transactional
    public void sendResetCode(String email) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(normalizeEmail(email)).orElse(null);
        if (user == null) {
            // Celowo nie ujawniamy, czy konto istnieje.
            logger.info("Żądanie resetu dla nieistniejącego emaila: {}", maskEmail(email));
            return;
        }
        issueAndSendCode(user, true);
    }

    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("Niepoprawne dane resetowania hasła."));

        PasswordResetCode activeCode = passwordResetCodeRepository.findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Brak aktywnego kodu resetującego."));

        LocalDateTime now = LocalDateTime.now();
        if (activeCode.getExpiresAt().isBefore(now)) {
            activeCode.setUsed(true);
            passwordResetCodeRepository.save(activeCode);
            throw new IllegalArgumentException("Kod resetujący wygasł. Wyślij nowy.");
        }

        int attemptsAfterIncrement = activeCode.getAttempts() + 1;
        activeCode.setAttempts(attemptsAfterIncrement);

        if (!passwordEncoder.matches(code, activeCode.getCodeHash())) {
            if (attemptsAfterIncrement >= maxAttempts) {
                activeCode.setUsed(true);
                passwordResetCodeRepository.save(activeCode);
                throw new IllegalArgumentException("Przekroczono liczbę prób. Wyślij nowy kod resetujący.");
            }
            passwordResetCodeRepository.save(activeCode);
            throw new IllegalArgumentException("Niepoprawny kod resetujący.");
        }

        activeCode.setUsed(true);
        passwordResetCodeRepository.save(activeCode);

        user.setPassword(passwordEncoder.encode(newPassword));
        appUserRepository.save(user);
    }

    private void issueAndSendCode(AppUser user, boolean enforceCooldown) {
        LocalDateTime now = LocalDateTime.now();
        PasswordResetCode previousCode = passwordResetCodeRepository.findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId())
                .orElse(null);

        if (enforceCooldown && previousCode != null && previousCode.getResendAvailableAt().isAfter(now)) {
            throw new IllegalArgumentException("Nowy kod resetujący możesz wysłać za chwilę.");
        }

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long sentToday = passwordResetCodeRepository.countByUserIdAndCreatedAtAfter(user.getId(), todayStart);
        if (sentToday >= dailySendLimit) {
            throw new IllegalArgumentException("Przekroczono dzienny limit wysyłki kodów resetujących.");
        }

        passwordResetCodeRepository.invalidateAllActiveForUser(user.getId());

        String plainCode = generateOtpCode();
        if (logPlainCode) {
            logger.warn("DEV reset code for {}: {}", maskEmail(user.getEmail()), plainCode);
        }

        PasswordResetCode code = new PasswordResetCode();
        code.setUser(user);
        code.setCodeHash(passwordEncoder.encode(plainCode));
        code.setExpiresAt(now.plusMinutes(codeTtlMinutes));
        code.setUsed(false);
        code.setAttempts(0);
        code.setResendAvailableAt(now.plusSeconds(resendCooldownSeconds));
        passwordResetCodeRepository.save(code);

        sendEmail(user.getEmail(), plainCode, codeTtlMinutes);
    }

    private void sendEmail(String toEmail, String code, int ttlMinutes) {
        if (mailFrom == null || mailFrom.isBlank()) {
            throw new IllegalArgumentException("Brak konfiguracji app.mail.from dla wysyłki email.");
        }

        try {
            logger.info("Próba wysyłki kodu resetującego: from={}, to={}", mailFrom, maskEmail(toEmail));
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(toEmail);
            message.setSubject("Reset hasła - kod weryfikacyjny");
            message.setText(
                    "Kod do resetu hasła: " + code + System.lineSeparator()
                            + "Kod wygasa za " + ttlMinutes + " minut." + System.lineSeparator()
                            + "Jeśli to nie Ty, zignoruj tę wiadomość."
            );
            mailSender.send(message);
            logger.info("Wysyłka kodu resetującego zakończona sukcesem dla {}", maskEmail(toEmail));
        } catch (Exception ex) {
            logger.error("Błąd wysyłki kodu resetującego dla {}: {}", maskEmail(toEmail), ex.getMessage(), ex);
            throw new IllegalArgumentException("Nie udało się wysłać kodu resetującego. Sprawdź konfigurację SMTP.");
        }
    }

    private String generateOtpCode() {
        int maxNumber = (int) Math.pow(10, OTP_LENGTH);
        int value = secureRandom.nextInt(maxNumber);
        return String.format(Locale.ROOT, "%0" + OTP_LENGTH + "d", value);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String maskEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        int atIndex = normalizedEmail.indexOf('@');
        if (atIndex <= 1) {
            return "***";
        }
        return normalizedEmail.substring(0, 2) + "***" + normalizedEmail.substring(atIndex);
    }
}
