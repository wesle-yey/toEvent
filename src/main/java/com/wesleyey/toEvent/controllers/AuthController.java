package com.wesleyey.toEvent.controllers;

import com.wesleyey.toEvent.DTOs.AuthenticationDTO;
import com.wesleyey.toEvent.DTOs.ResendVerificationDTO;
import com.wesleyey.toEvent.DTOs.UserDTO;
import com.wesleyey.toEvent.DTOs.UserResponseDTO;
import com.wesleyey.toEvent.DTOs.VerifyEmailDTO;
import com.wesleyey.toEvent.models.User;
import com.wesleyey.toEvent.services.AuthCookieService;
import com.wesleyey.toEvent.services.EmailVerificationResult;
import com.wesleyey.toEvent.services.TokenService;
import com.wesleyey.toEvent.services.UserService;
import jakarta.validation.Valid;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final TokenService tokenService;
    private final AuthCookieService authCookieService;

    public AuthController(AuthenticationManager authenticationManager, UserService userService,
            TokenService tokenService, AuthCookieService authCookieService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.tokenService = tokenService;
        this.authCookieService = authCookieService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody @Valid AuthenticationDTO data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);

        var token = tokenService.generateToken((User) auth.getPrincipal());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieService.create(token).toString())
                .body(Map.of("token", token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieService.clear().toString())
                .build();
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@RequestBody @Valid UserDTO data) {
        String browserCode = newBrowserCode();
        UserResponseDTO userResponse = userService.saveUser(data, browserCode);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieService.createVerificationBrowser(browserCode).toString())
                .body(userResponse);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestBody @Valid VerifyEmailDTO data,
            @CookieValue(name = AuthCookieService.VERIFICATION_BROWSER_COOKIE, required = false) String browserCode) {
        EmailVerificationResult result = userService.verifyEmail(data.token(), browserCode);
        String clearBrowserCookie = authCookieService.clearVerificationBrowser().toString();

        if (!result.autoLogin()) {
            // Link aberto em outro navegador: o e-mail está confirmado, mas a sessão exige a senha
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, clearBrowserCookie)
                    .body(Map.of(
                            "autoLogin", false,
                            "message", "E-mail confirmado! Entre com seu e-mail e senha para continuar."
                    ));
        }

        String jwtToken = tokenService.generateToken(result.user());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieService.create(jwtToken).toString())
                .header(HttpHeaders.SET_COOKIE, clearBrowserCookie)
                .body(Map.of(
                        "autoLogin", true,
                        "token", jwtToken,
                        "message", "E-mail verificado com sucesso! Redirecionando para o seu dashboard..."
                ));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(@RequestBody @Valid ResendVerificationDTO data) {
        // O cookie é enviado sempre, exista ou não a conta, para a resposta não revelar quais e-mails estão cadastrados
        String browserCode = newBrowserCode();
        userService.resendVerification(data.email(), browserCode);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieService.createVerificationBrowser(browserCode).toString())
                .body(Map.of("message", "Se o e-mail estiver cadastrado e pendente de verificação, um novo link foi enviado. "
                        + "Por segurança, só é possível pedir um novo link a cada minuto."));
    }

    private static String newBrowserCode() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
