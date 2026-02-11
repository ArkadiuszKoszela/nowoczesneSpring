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
import pl.koszela.nowoczesnebud.Model.EmailVerificationCode;
import pl.koszela.nowoczesnebud.Repository.AppUserRepository;
import pl.koszela.nowoczesnebud.Repository.EmailVerificationCodeRepository;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class EmailVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final int OTP_LENGTH = 6;

    private final EmailVerificationCodeRepository emailVerificationCodeRepository;
    private final AppUserRepository appUserRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.auth.email-verification.code-ttl-minutes:10}")
    private int codeTtlMinutes;

    @Value("${app.auth.email-verification.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.auth.email-verification.resend-cooldown-seconds:60}")
    private int resendCooldownSeconds;

    @Value("${app.auth.email-verification.daily-send-limit:10}")
    private int dailySendLimit;

    @Value("${app.auth.email-verification.log-plain-code:false}")
    private boolean logPlainCode;

    @Value("${app.mail.from:}")
    private String mailFrom;

    public EmailVerificationService(EmailVerificationCodeRepository emailVerificationCodeRepository,
                                    AppUserRepository appUserRepository,
                                    JavaMailSender mailSender,
                                    PasswordEncoder passwordEncoder) {
        this.emailVerificationCodeRepository = emailVerificationCodeRepository;
        this.appUserRepository = appUserRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void sendRegistrationCode(AppUser user) {
        logger.info("Start wysyłki kodu rejestracyjnego dla userId={}, email={}", user.getId(), maskEmail(user.getEmail()));
        issueAndSendCode(user, false);
    }

    @Transactional
    public void resendCode(String email) {
        AppUser user = findUserByEmail(email);
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalArgumentException("Ten email jest już zweryfikowany");
        }
        logger.info("Start resend kodu weryfikacyjnego dla userId={}, email={}", user.getId(), maskEmail(user.getEmail()));
        issueAndSendCode(user, true);
    }

    @Transactional
    public void verifyCode(String email, String code) {
        AppUser user = findUserByEmail(email);
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return;
        }

        EmailVerificationCode activeCode = emailVerificationCodeRepository
                .findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Brak aktywnego kodu. Wyślij nowy kod."));

        LocalDateTime now = LocalDateTime.now();
        if (activeCode.getExpiresAt().isBefore(now)) {
            activeCode.setUsed(true);
            emailVerificationCodeRepository.save(activeCode);
            throw new IllegalArgumentException("Kod weryfikacyjny wygasł. Wyślij nowy kod.");
        }

        int attemptsAfterIncrement = activeCode.getAttempts() + 1;
        activeCode.setAttempts(attemptsAfterIncrement);

        if (!passwordEncoder.matches(code, activeCode.getCodeHash())) {
            if (attemptsAfterIncrement >= maxAttempts) {
                activeCode.setUsed(true);
                emailVerificationCodeRepository.save(activeCode);
                throw new IllegalArgumentException("Przekroczono liczbę prób. Wyślij nowy kod.");
            }
            emailVerificationCodeRepository.save(activeCode);
            throw new IllegalArgumentException("Niepoprawny kod weryfikacyjny.");
        }

        activeCode.setUsed(true);
        emailVerificationCodeRepository.save(activeCode);
        user.setEmailVerified(true);
        appUserRepository.save(user);
    }

    private AppUser findUserByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        return appUserRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono konta dla podanego emaila."));
    }

    private void issueAndSendCode(AppUser user, boolean enforceCooldown) {
        LocalDateTime now = LocalDateTime.now();
        EmailVerificationCode previousCode = emailVerificationCodeRepository
                .findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId())
                .orElse(null);

        if (enforceCooldown && previousCode != null && previousCode.getResendAvailableAt().isAfter(now)) {
            throw new IllegalArgumentException("Nowy kod możesz wysłać za chwilę.");
        }

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long sentToday = emailVerificationCodeRepository.countByUserIdAndCreatedAtAfter(user.getId(), todayStart);
        if (sentToday >= dailySendLimit) {
            throw new IllegalArgumentException("Przekroczono dzienny limit wysyłki kodów.");
        }

        emailVerificationCodeRepository.invalidateAllActiveForUser(user.getId());

        String plainCode = generateOtpCode();
        if (logPlainCode) {
            logger.warn("DEV OTP code for {}: {}", maskEmail(user.getEmail()), plainCode);
        }
        EmailVerificationCode code = new EmailVerificationCode();
        code.setUser(user);
        code.setCodeHash(passwordEncoder.encode(plainCode));
        code.setExpiresAt(now.plusMinutes(codeTtlMinutes));
        code.setUsed(false);
        code.setAttempts(0);
        code.setResendAvailableAt(now.plusSeconds(resendCooldownSeconds));
        emailVerificationCodeRepository.save(code);

        sendEmail(user.getEmail(), plainCode, codeTtlMinutes);
    }

    private String generateOtpCode() {
        int maxNumber = (int) Math.pow(10, OTP_LENGTH);
        int value = secureRandom.nextInt(maxNumber);
        return String.format(Locale.ROOT, "%0" + OTP_LENGTH + "d", value);
    }

    private void sendEmail(String toEmail, String code, int ttlMinutes) {
        if (mailFrom == null || mailFrom.isBlank()) {
            throw new IllegalArgumentException("Brak konfiguracji app.mail.from dla wysyłki email.");
        }

        try {
            logger.info("Próba wysyłki OTP email: from={}, to={}", mailFrom, maskEmail(toEmail));
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(toEmail);
            message.setSubject("Kod weryfikacyjny - NowoczesneBud");
            message.setText(
                    "Twój kod weryfikacyjny: " + code + System.lineSeparator()
                            + "Kod wygasa za " + ttlMinutes + " minut." + System.lineSeparator()
                            + "Jeśli to nie Ty rejestrowałeś konto, zignoruj tę wiadomość."
            );
            mailSender.send(message);
            logger.info("Wysyłka OTP zakończona sukcesem dla {}", maskEmail(toEmail));
        } catch (Exception ex) {
            logger.error("Błąd wysyłki OTP dla {}: {}", maskEmail(toEmail), ex.getMessage(), ex);
            throw new IllegalArgumentException("Nie udało się wysłać kodu email. Sprawdź konfigurację SMTP.");
        }
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
