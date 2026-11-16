package com.wesleyey.toEvent.services;

import com.wesleyey.toEvent.exception.InvalidTokenException;
import com.wesleyey.toEvent.exception.UserNotFoundException;
import com.wesleyey.toEvent.exception.UserAlreadyExistsException;
import com.wesleyey.toEvent.DTOs.UserDTO;
import com.wesleyey.toEvent.DTOs.UserResponseDTO;
import com.wesleyey.toEvent.models.User;
import com.wesleyey.toEvent.repositories.EventRepository;
import com.wesleyey.toEvent.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final EmailValidationService emailValidationService;

    public UserService(UserRepository userRepository, EventRepository eventRepository,
            PasswordEncoder passwordEncoder, EmailService emailService,
            EmailValidationService emailValidationService) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.emailValidationService = emailValidationService;
    }

    public UserResponseDTO saveUser(UserDTO user) {
        String email = User.normalizeEmail(user.getEmail());
        emailValidationService.validateEmail(email);
        if (userRepository.findByEmail(email).isPresent()) {
            throw new UserAlreadyExistsException("User already exists");
        }

        String token = UUID.randomUUID().toString();
        User newUser = new User(
                user.getName(),
                email,
                passwordEncoder.encode(user.getPassword()));
        newUser.setEmailVerified(false);
        newUser.setVerificationToken(token);
        newUser.setVerificationTokenExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));

        try {
            userRepository.save(newUser);
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw new UserAlreadyExistsException("User already exists");
        }

        emailService.sendVerificationEmail(email, user.getName(), token);
        return new UserResponseDTO(user.getName(), email);
    }

    public void verifyEmail(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Token de verificação não informado.");
        }

        User user = userRepository.findByVerificationToken(token.trim())
                .orElseThrow(() -> new InvalidTokenException("Token de verificação inválido ou inexistente."));

        if (user.getVerificationTokenExpiresAt() != null && user.getVerificationTokenExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("O link de verificação expirou. Solicite um novo envio.");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        userRepository.save(user);
    }

    public void resendVerification(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            return;
        }

        String email = User.normalizeEmail(rawEmail);
        userRepository.findByEmail(email).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                String token = UUID.randomUUID().toString();
                user.setVerificationToken(token);
                user.setVerificationTokenExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
                userRepository.save(user);
                emailService.sendVerificationEmail(user.getEmail(), user.getName(), token);
            }
        });
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