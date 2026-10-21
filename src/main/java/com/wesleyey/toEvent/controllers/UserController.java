package com.wesleyey.toEvent.controllers;

import com.wesleyey.toEvent.DTOs.UserDTO;
import com.wesleyey.toEvent.DTOs.UserResponseDTO;
import com.wesleyey.toEvent.exception.ErrorResponse;
import com.wesleyey.toEvent.exception.UserAlreadyExistsException;
import com.wesleyey.toEvent.exception.UserNotFoundException;
import com.wesleyey.toEvent.models.User;
import com.wesleyey.toEvent.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserResponseDTO getUser(@AuthenticationPrincipal User user) {
        return userService.getUser(user.getId());
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal User user) {
        userService.deleteUser(user.getId());
        return ResponseEntity.noContent().build();
    }

    // ----- EXCEPTION HANDLERS -----
    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleUserNotFound(UserNotFoundException ex, HttpServletRequest request) {
        return new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage())
                .withPath(request.getRequestURI());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleUserAlreadyExists(UserAlreadyExistsException ex,
                                                 HttpServletRequest request) {
        return new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage())
                .withPath(request.getRequestURI());
    }
}
