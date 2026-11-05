package com.wesleyey.toEvent.controllers;

import com.wesleyey.toEvent.DTOs.AuthenticationDTO;
import com.wesleyey.toEvent.DTOs.UserDTO;
import com.wesleyey.toEvent.DTOs.UserResponseDTO;
import com.wesleyey.toEvent.models.User;
import com.wesleyey.toEvent.services.AuthCookieService;
import com.wesleyey.toEvent.services.TokenService;
import com.wesleyey.toEvent.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

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
    public ResponseEntity<Void> login(@RequestBody @Valid AuthenticationDTO data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);

        var token = tokenService.generateToken((User) auth.getPrincipal());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieService.create(token).toString())
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieService.clear().toString())
                .build();
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@RequestBody @Valid UserDTO data) {
        UserResponseDTO userResponse = userService.saveUser(data);
        return ResponseEntity.ok(userResponse);
    }
}
