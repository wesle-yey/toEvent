package com.wesleyey.toEvent.services;

import com.wesleyey.toEvent.exception.EmailQuotaExceededException;
import com.wesleyey.toEvent.exception.InvalidTokenException;
import com.wesleyey.toEvent.exception.VerificationEmailCooldownException;
import com.wesleyey.toEvent.exception.UserNotFoundException;
import com.wesleyey.toEvent.exception.UserAlreadyExistsException;
import com.wesleyey.toEvent.DTOs.UserDTO;
import com.wesleyey.toEvent.DTOs.UserResponseDTO;
import com.wesleyey.toEvent.models.User;
import com.wesleyey.toEvent.repositories.EventRepository;
import com.wesleyey.toEvent.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    static final Duration VERIFICATION_EMAIL_COOLDOWN = Duration.ofSeconds(60);
    static final Duration VERIFICATION_EMAIL_WINDOW = Duration.ofHours(24);
    static final int MAX_VERIFICATION_EMAILS_PER_WINDOW = 5;
    static final Duration UNVERIFIED_ACCOUNT_TTL = Duration.ofDays(7);

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final EmailValidationService emailValidationService;
    private final EmailQuotaService emailQuotaService;

    public UserService(UserRepository userRepository, EventRepository eventRepository,
            PasswordEncoder passwordEncoder, EmailService emailService,
            EmailValidationService emailValidationService, EmailQuotaService emailQuotaService) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.emailValidationService = emailValidationService;
        this.emailQuotaService = emailQuotaService;
    }

    public UserResponseDTO saveUser(UserDTO user, String browserCode) {
        String email = User.normalizeEmail(user.getEmail());
        emailValidationService.validateEmail(email);

        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent() && existing.get().isEmailVerified()) {
            throw new UserAlreadyExistsException("User already exists");
        }

        // Conta ainda não confirmada: o novo cadastro substitui nome e senha. Quem provar ser dono do
        // e-mail (clicando no link mais recente) fica com a senha que valer nesse momento, e um cadastro
        // anterior feito por outra pessoa com o mesmo e-mail perde o efeito.
        User target = existing.orElseGet(() -> new User(user.getName(), email, null));
        Instant now = Instant.now();
        ensureCanSendVerificationEmail(target, now);
        if (!emailQuotaService.tryReserveSend()) {
            throw new EmailQuotaExceededException(
                    "Cadastros temporariamente indisponíveis. Tente novamente mais tarde.");
        }

        target.setName(user.getName());
        target.setPassword(passwordEncoder.encode(user.getPassword()));
        target.setEmailVerified(false);
        target.setUnverifiedExpiresAt(now.plus(UNVERIFIED_ACCOUNT_TTL));
        String token = prepareVerification(target, browserCode, now);

        try {
            userRepository.save(target);
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw new UserAlreadyExistsException("User already exists");
        }

        emailService.sendVerificationEmail(email, user.getName(), token);
        return new UserResponseDTO(user.getName(), email);
    }

    public EmailVerificationResult verifyEmail(String token, String browserCode) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Token de verificação não informado.");
        }

        User user = userRepository.findByVerificationToken(token.trim())
                .orElseThrow(() -> new InvalidTokenException("Token de verificação inválido ou inexistente."));

        if (user.getVerificationTokenExpiresAt() != null && user.getVerificationTokenExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("O link de verificação expirou. Solicite um novo envio.");
        }

        // Login automático só no navegador que pediu este link. Em outro navegador (ou se outra pessoa
        // pediu o link depois), o e-mail é confirmado, mas é preciso entrar com a senha.
        boolean autoLogin = browserCode != null && user.getVerificationBrowserHash() != null
                && MessageDigest.isEqual(
                        hashBrowserCode(browserCode).getBytes(StandardCharsets.UTF_8),
                        user.getVerificationBrowserHash().getBytes(StandardCharsets.UTF_8));

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        user.setVerificationBrowserHash(null);
        user.setUnverifiedExpiresAt(null);
        return new EmailVerificationResult(userRepository.save(user), autoLogin);
    }

    public void resendVerification(String rawEmail, String browserCode) {
        if (rawEmail == null || rawEmail.isBlank()) {
            return;
        }

        String email = User.normalizeEmail(rawEmail);
        Optional<User> found = userRepository.findByEmail(email);
        if (found.isEmpty() || found.get().isEmailVerified()) {
            return;
        }

        User user = found.get();
        Instant now = Instant.now();
        try {
            ensureCanSendVerificationEmail(user, now);
        } catch (VerificationEmailCooldownException ex) {
            // Resposta igual à de sucesso, para não revelar quais e-mails têm conta pendente
            logger.warn("Reenvio de confirmação ignorado: limite por conta atingido");
            return;
        }
        if (!emailQuotaService.tryReserveSend()) {
            return;
        }

        String token = prepareVerification(user, browserCode, now);
        userRepository.save(user);
        emailService.sendVerificationEmail(user.getEmail(), user.getName(), token);
    }

    /** Barra envios seguidos para a mesma conta: 1 a cada 60s e no máximo 5 a cada 24h. */
    private void ensureCanSendVerificationEmail(User user, Instant now) {
        Instant last = user.getLastVerificationEmailAt();
        if (last != null && last.plus(VERIFICATION_EMAIL_COOLDOWN).isAfter(now)) {
            long wait = Math.max(1, Duration.between(now, last.plus(VERIFICATION_EMAIL_COOLDOWN)).toSeconds());
            throw new VerificationEmailCooldownException(
                    "Acabamos de enviar um link para este e-mail. Aguarde " + wait + " segundos para pedir outro.", wait);
        }

        Instant windowStart = user.getVerificationEmailWindowStart();
        boolean windowActive = windowStart != null && windowStart.plus(VERIFICATION_EMAIL_WINDOW).isAfter(now);
        if (windowActive && user.getVerificationEmailsInWindow() >= MAX_VERIFICATION_EMAILS_PER_WINDOW) {
            long wait = Math.max(1, Duration.between(now, windowStart.plus(VERIFICATION_EMAIL_WINDOW)).toSeconds());
            throw new VerificationEmailCooldownException(
                    "Limite de envios para este e-mail atingido. Tente novamente amanhã.", wait);
        }
    }

    /** Gera um novo link (invalidando o anterior), registra o envio e o navegador que o pediu. */
    private String prepareVerification(User user, String browserCode, Instant now) {
        Instant windowStart = user.getVerificationEmailWindowStart();
        if (windowStart == null || !windowStart.plus(VERIFICATION_EMAIL_WINDOW).isAfter(now)) {
            user.setVerificationEmailWindowStart(now);
            user.setVerificationEmailsInWindow(0);
        }
        user.setVerificationEmailsInWindow(user.getVerificationEmailsInWindow() + 1);
        user.setLastVerificationEmailAt(now);

        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setVerificationTokenExpiresAt(now.plus(24, ChronoUnit.HOURS));
        user.setVerificationBrowserHash(browserCode == null ? null : hashBrowserCode(browserCode));
        return token;
    }

    static String hashBrowserCode(String browserCode) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(browserCode.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível", ex);
        }
    }

    public UserResponseDTO getUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User id " + id + " not found"));
        return new UserResponseDTO(user.getName(), user.getEmail());
    }

    public void deleteUser(String id) {
        if (userRepository.existsById(id)) {
            eventRepository.deleteByUserId(id);
            userRepository.deleteById(id);
        } else {
            throw new UserNotFoundException("User id " + id + " not found");
        }
    }
}