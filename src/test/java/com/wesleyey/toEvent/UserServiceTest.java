package com.wesleyey.toEvent;

import com.wesleyey.toEvent.DTOs.UserDTO;
import com.wesleyey.toEvent.DTOs.UserResponseDTO;
import com.wesleyey.toEvent.exception.UserAlreadyExistsException;
import com.wesleyey.toEvent.exception.UserNotFoundException;
import com.wesleyey.toEvent.models.User;
import com.wesleyey.toEvent.repositories.UserRepository;
import com.wesleyey.toEvent.repositories.EventRepository;
import com.wesleyey.toEvent.exception.EmailQuotaExceededException;
import com.wesleyey.toEvent.exception.VerificationEmailCooldownException;
import com.wesleyey.toEvent.services.EmailQuotaService;
import com.wesleyey.toEvent.services.EmailVerificationResult;
import com.wesleyey.toEvent.services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository repo;
    @Mock
    EventRepository eventRepository;
    @Mock
    PasswordEncoder encoder;
    @Mock
    com.wesleyey.toEvent.services.EmailService emailService;
    @Mock
    com.wesleyey.toEvent.services.EmailValidationService emailValidationService;
    @Mock
    EmailQuotaService emailQuotaService;
    @InjectMocks
    UserService service;

    @Test
    void saveUser_disposableEmail_throwsDisposableEmailException() {
        UserDTO dto = new UserDTO("Fake", "fake@tempmail.com", "12345678");
        org.mockito.Mockito.doThrow(new com.wesleyey.toEvent.exception.DisposableEmailException("E-mail temporário não permitido"))
                .when(emailValidationService).validateEmail("fake@tempmail.com");

        assertThrows(com.wesleyey.toEvent.exception.DisposableEmailException.class,
                () -> service.saveUser(dto, "browser-code"));
        org.mockito.Mockito.verify(repo, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void saveUser_persistsAndReturnsName() {
        UserDTO dto = new UserDTO("Ana", "ana@example.com", "12345678");
        when(repo.findByEmail(dto.getEmail())).thenReturn(Optional.empty());
        when(encoder.encode(dto.getPassword())).thenReturn("encoded-password");
        when(emailQuotaService.tryReserveSend()).thenReturn(true);

        UserResponseDTO response = service.saveUser(dto, "browser-code");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(repo).save(userCaptor.capture());
        assertEquals("Ana", userCaptor.getValue().getName());
        assertEquals("encoded-password", userCaptor.getValue().getPassword());
        assertEquals("Ana", response.name());
        assertEquals(dto.getEmail(), response.email());
    }

    @Test
    void saveUser_verifiedEmail_throwsException() {
        UserDTO dto = new UserDTO("Ana", "ana@example.com", "12345678");
        User verified = new User("Ana", "ana@example.com", "12345678");
        verified.setEmailVerified(true);
        when(repo.findByEmail(dto.getEmail())).thenReturn(Optional.of(verified));
        assertThrows(UserAlreadyExistsException.class,
                () -> service.saveUser(dto, "browser-code"));
        verify(repo, never()).save(any(User.class));
        verifyNoEmailSent();
    }

    @Test
    void saveUser_normalizesEmailForLookupAndPersistence() {
        UserDTO dto = new UserDTO("Ana", " Ana@Example.com ", "12345678");
        when(repo.findByEmail("ana@example.com")).thenReturn(Optional.empty());
        when(encoder.encode(dto.getPassword())).thenReturn("encoded-password");
        when(emailQuotaService.tryReserveSend()).thenReturn(true);

        UserResponseDTO response = service.saveUser(dto, "browser-code");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(repo).findByEmail("ana@example.com");
        verify(repo).save(userCaptor.capture());
        assertEquals("ana@example.com", userCaptor.getValue().getEmail());
        assertEquals("ana@example.com", response.email());
    }

    @Test
    void saveUser_duplicateKeyRace_throwsUserAlreadyExists() {
        UserDTO dto = new UserDTO("Ana", "ana@example.com", "12345678");
        when(repo.findByEmail(dto.getEmail())).thenReturn(Optional.empty());
        when(encoder.encode(dto.getPassword())).thenReturn("encoded-password");
        when(emailQuotaService.tryReserveSend()).thenReturn(true);
        when(repo.save(any(User.class))).thenThrow(new DuplicateKeyException("duplicate email"));

        assertThrows(UserAlreadyExistsException.class, () -> service.saveUser(dto, "browser-code"));
    }

    @Test
    void getUser_notFound_throwsException() {
        when(repo.findById("123")).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class,
                () -> service.getUser("123"));
    }

    @Test
    void deleteUser_deletesUserEventsBeforeDeletingUser() {
        when(repo.existsById("user-123")).thenReturn(true);

        service.deleteUser("user-123");

        var order = inOrder(eventRepository, repo);
        order.verify(eventRepository).deleteByUserId("user-123");
        order.verify(repo).deleteById("user-123");
    }

    @Test
    void deleteUser_notFound_doesNotDeleteEvents() {
        when(repo.existsById("missing")).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> service.deleteUser("missing"));

        org.mockito.Mockito.verifyNoInteractions(eventRepository);
    }

    // ---- Cadastro sobre conta não confirmada ----

    @Test
    void saveUser_existingUnverified_replacesPasswordAndInvalidatesOldLink() {
        User attackerAccount = unverifiedUser();
        attackerAccount.setPassword("attacker-hash");
        attackerAccount.setVerificationToken("old-token");
        attackerAccount.setLastVerificationEmailAt(Instant.now().minus(Duration.ofMinutes(5)));
        UserDTO dto = new UserDTO("Ana Real", "ana@example.com", "senha-da-ana");
        when(repo.findByEmail("ana@example.com")).thenReturn(Optional.of(attackerAccount));
        when(encoder.encode("senha-da-ana")).thenReturn("ana-hash");
        when(emailQuotaService.tryReserveSend()).thenReturn(true);

        service.saveUser(dto, "ana-browser");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(repo).save(saved.capture());
        assertEquals("ana-hash", saved.getValue().getPassword());
        assertEquals("Ana Real", saved.getValue().getName());
        assertNotEquals("old-token", saved.getValue().getVerificationToken());
        verify(emailService).sendVerificationEmail("ana@example.com", "Ana Real", saved.getValue().getVerificationToken());
    }

    @Test
    void saveUser_newUser_expiresIfNeverConfirmedAndRemembersBrowser() {
        UserDTO dto = new UserDTO("Ana", "ana@example.com", "12345678");
        when(repo.findByEmail("ana@example.com")).thenReturn(Optional.empty());
        when(encoder.encode(dto.getPassword())).thenReturn("encoded-password");
        when(emailQuotaService.tryReserveSend()).thenReturn(true);

        service.saveUser(dto, "browser-code");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(repo).save(saved.capture());
        Instant expiresAt = saved.getValue().getUnverifiedExpiresAt();
        assertNotNull(expiresAt);
        assertTrue(expiresAt.isAfter(Instant.now().plus(Duration.ofDays(6))));
        assertNotNull(saved.getValue().getVerificationBrowserHash());
        assertNotEquals("browser-code", saved.getValue().getVerificationBrowserHash());
        assertEquals(1, saved.getValue().getVerificationEmailsInWindow());
    }

    @Test
    void saveUser_existingUnverifiedWithinCooldown_rejectsWithoutSaving() {
        User pending = unverifiedUser();
        pending.setLastVerificationEmailAt(Instant.now().minusSeconds(10));
        when(repo.findByEmail("ana@example.com")).thenReturn(Optional.of(pending));

        VerificationEmailCooldownException ex = assertThrows(VerificationEmailCooldownException.class,
                () -> service.saveUser(new UserDTO("Ana", "ana@example.com", "12345678"), "browser-code"));

        assertTrue(ex.getRetryAfterSeconds() > 0 && ex.getRetryAfterSeconds() <= 60);
        verify(repo, never()).save(any(User.class));
        verifyNoEmailSent();
    }

    // ---- Limite diário global ----

    @Test
    void saveUser_dailyQuotaExhausted_rejectsWithoutSaving() {
        when(repo.findByEmail("ana@example.com")).thenReturn(Optional.empty());
        when(emailQuotaService.tryReserveSend()).thenReturn(false);

        assertThrows(EmailQuotaExceededException.class,
                () -> service.saveUser(new UserDTO("Ana", "ana@example.com", "12345678"), "browser-code"));

        verify(repo, never()).save(any(User.class));
        verifyNoEmailSent();
    }

    @Test
    void resendVerification_dailyQuotaExhausted_doesNotSend() {
        when(repo.findByEmail("ana@example.com")).thenReturn(Optional.of(unverifiedUser()));
        when(emailQuotaService.tryReserveSend()).thenReturn(false);

        service.resendVerification("ana@example.com", "browser-code");

        verify(repo, never()).save(any(User.class));
        verifyNoEmailSent();
    }

    // ---- Limite por conta no reenvio ----

    @Test
    void resendVerification_allowed_sendsNewLinkAndCountsIt() {
        User pending = unverifiedUser();
        pending.setVerificationToken("old-token");
        pending.setLastVerificationEmailAt(Instant.now().minus(Duration.ofMinutes(2)));
        pending.setVerificationEmailWindowStart(Instant.now().minus(Duration.ofHours(1)));
        pending.setVerificationEmailsInWindow(1);
        when(repo.findByEmail("ana@example.com")).thenReturn(Optional.of(pending));
        when(emailQuotaService.tryReserveSend()).thenReturn(true);

        service.resendVerification(" Ana@Example.com ", "browser-code");

        assertNotEquals("old-token", pending.getVerificationToken());
        assertEquals(2, pending.getVerificationEmailsInWindow());
        verify(repo).save(pending);
        verify(emailService).sendVerificationEmail("ana@example.com", "Ana", pending.getVerificationToken());
    }

    @Test
    void resendVerification_withinCooldown_doesNotSend() {
        User pending = unverifiedUser();
        pending.setLastVerificationEmailAt(Instant.now().minusSeconds(30));
        when(repo.findByEmail("ana@example.com")).thenReturn(Optional.of(pending));

        service.resendVerification("ana@example.com", "browser-code");

        verify(repo, never()).save(any(User.class));
        verifyNoEmailSent();
        org.mockito.Mockito.verifyNoInteractions(emailQuotaService);
    }

    @Test
    void resendVerification_dailyMaxPerAccountReached_doesNotSend() {
        User pending = unverifiedUser();
        pending.setLastVerificationEmailAt(Instant.now().minus(Duration.ofMinutes(10)));
        pending.setVerificationEmailWindowStart(Instant.now().minus(Duration.ofHours(3)));
        pending.setVerificationEmailsInWindow(5);
        when(repo.findByEmail("ana@example.com")).thenReturn(Optional.of(pending));

        service.resendVerification("ana@example.com", "browser-code");

        verifyNoEmailSent();
    }

    @Test
    void resendVerification_windowExpired_resetsCounter() {
        User pending = unverifiedUser();
        pending.setLastVerificationEmailAt(Instant.now().minus(Duration.ofHours(25)));
        pending.setVerificationEmailWindowStart(Instant.now().minus(Duration.ofHours(25)));
        pending.setVerificationEmailsInWindow(5);
        when(repo.findByEmail("ana@example.com")).thenReturn(Optional.of(pending));
        when(emailQuotaService.tryReserveSend()).thenReturn(true);

        service.resendVerification("ana@example.com", "browser-code");

        assertEquals(1, pending.getVerificationEmailsInWindow());
        verify(emailService).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void resendVerification_verifiedAccount_doesNothing() {
        User verified = unverifiedUser();
        verified.setEmailVerified(true);
        when(repo.findByEmail("ana@example.com")).thenReturn(Optional.of(verified));

        service.resendVerification("ana@example.com", "browser-code");

        verifyNoEmailSent();
        org.mockito.Mockito.verifyNoInteractions(emailQuotaService);
    }

    // ---- Login automático só no navegador que pediu o link ----

    @Test
    void verifyEmail_sameBrowser_autoLoginAndClearsPendingData() {
        User pending = pendingWithLinkFromBrowser("ana-browser");
        when(repo.findByVerificationToken("link-token")).thenReturn(Optional.of(pending));
        when(repo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmailVerificationResult result = service.verifyEmail("link-token", "ana-browser");

        assertTrue(result.autoLogin());
        assertTrue(result.user().isEmailVerified());
        assertNull(result.user().getVerificationToken());
        assertNull(result.user().getVerificationBrowserHash());
        assertNull(result.user().getUnverifiedExpiresAt());
    }

    @Test
    void verifyEmail_otherBrowser_confirmsButRequiresPassword() {
        User pending = pendingWithLinkFromBrowser("attacker-browser");
        when(repo.findByVerificationToken("link-token")).thenReturn(Optional.of(pending));
        when(repo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmailVerificationResult result = service.verifyEmail("link-token", "ana-browser");

        assertFalse(result.autoLogin());
        assertTrue(result.user().isEmailVerified());
    }

    @Test
    void verifyEmail_noBrowserCookie_confirmsButRequiresPassword() {
        User pending = pendingWithLinkFromBrowser("ana-browser");
        when(repo.findByVerificationToken("link-token")).thenReturn(Optional.of(pending));
        when(repo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertFalse(service.verifyEmail("link-token", null).autoLogin());
    }

    private User pendingWithLinkFromBrowser(String browserCode) {
        // Gera o hash do navegador pelo próprio fluxo de reenvio, sem depender de detalhes internos
        User pending = unverifiedUser();
        when(repo.findByEmail("ana@example.com")).thenReturn(Optional.of(pending));
        when(emailQuotaService.tryReserveSend()).thenReturn(true);
        service.resendVerification("ana@example.com", browserCode);
        pending.setVerificationToken("link-token");
        return pending;
    }

    private static User unverifiedUser() {
        User user = new User("Ana", "ana@example.com", "hash");
        user.setEmailVerified(false);
        return user;
    }

    private void verifyNoEmailSent() {
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString(), anyString());
    }
}
