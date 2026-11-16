package com.wesleyey.toEvent.services;

import com.wesleyey.toEvent.exception.DisposableEmailException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import static org.junit.jupiter.api.Assertions.*;

class EmailValidationServiceTest {

    private EmailValidationService emailValidationService;

    @BeforeEach
    void setUp() {
        emailValidationService = new EmailValidationService(new DefaultResourceLoader(), false);
        emailValidationService.loadDisposableDomains();
    }

    @Test
    void shouldLoadDisposableDomains() {
        assertTrue(emailValidationService.getDisposableDomains().size() > 50);
        assertTrue(emailValidationService.isDisposable("10minutemail.com"));
        assertTrue(emailValidationService.isDisposable("tempmail.com"));
        assertTrue(emailValidationService.isDisposable("guerrillamail.com"));
        assertTrue(emailValidationService.isDisposable("mailinator.com"));
    }

    @Test
    void shouldRejectDisposableEmail() {
        assertThrows(DisposableEmailException.class, ()
                -> emailValidationService.validateEmail("usuario@10minutemail.com"));
        assertThrows(DisposableEmailException.class, ()
                -> emailValidationService.validateEmail("teste@temp-mail.org"));
        assertThrows(DisposableEmailException.class, ()
                -> emailValidationService.validateEmail("alvo@sharklasers.com"));
    }

    @Test
    void shouldAcceptLegitimateEmail() {
        assertDoesNotThrow(() -> emailValidationService.validateEmail("wesley@gmail.com"));
        assertDoesNotThrow(() -> emailValidationService.validateEmail("contato@outlook.com"));
        assertDoesNotThrow(() -> emailValidationService.validateEmail("usuario@yahoo.com.br"));
        assertDoesNotThrow(() -> emailValidationService.validateEmail("admin@empresa.com"));
    }

    @Test
    void shouldBeCaseInsensitive() {
        assertThrows(DisposableEmailException.class, ()
                -> emailValidationService.validateEmail("User@10MinuteMail.COM"));
    }

    @Test
    void shouldRejectInvalidFormat() {
        assertThrows(DisposableEmailException.class, () -> emailValidationService.validateEmail("invalido"));
        assertThrows(DisposableEmailException.class, () -> emailValidationService.validateEmail(""));
        assertThrows(DisposableEmailException.class, () -> emailValidationService.validateEmail(null));
    }
}
