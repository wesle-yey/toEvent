package com.wesleyey.toEvent.controllers;

import com.wesleyey.toEvent.DTOs.AuthenticationDTO;
import com.wesleyey.toEvent.DTOs.LoginResponseDTO;
import com.wesleyey.toEvent.DTOs.UserDTO;
import com.wesleyey.toEvent.DTOs.UserResponseDTO;
import com.wesleyey.toEvent.models.User;
import com.wesleyey.toEvent.services.TokenService;
import com.wesleyey.toEvent.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final TokenService tokenService;

    public AuthController(AuthenticationManager authenticationManager, UserService userService, TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid AuthenticationDTO data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);

        var token = tokenService.generateToken((User) auth.getPrincipal());
        return ResponseEntity.ok(new LoginResponseDTO(token));
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@RequestBody @Valid UserDTO data) {
        UserResponseDTO userResponse = userService.saveUser(data);
        return ResponseEntity.ok(userResponse);
    }
}
