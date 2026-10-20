package com.wesleyey.toEvent;

import com.wesleyey.toEvent.DTOs.UserDTO;
import com.wesleyey.toEvent.exception.UserAlreadyExistsException;
import com.wesleyey.toEvent.exception.UserNotFoundException;
import com.wesleyey.toEvent.models.User;
import com.wesleyey.toEvent.repositories.UserRepository;
import com.wesleyey.toEvent.services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository repo;
    @Mock
    PasswordEncoder encoder;
    @InjectMocks
    UserService service;

    @Test
    void saveUser_duplicatedEmail_throwsException() {
        UserDTO dto = new UserDTO("Ana", "ana@example.com", "12345678");
        when(repo.findByEmail(dto.getEmail())).thenReturn(Optional.of(new User("Ana", "ana@example.com", "12345678")));
        assertThrows(UserAlreadyExistsException.class,
                () -> service.saveUser(dto));
    }

    @Test
    void getUser_notFound_throwsException() {
        when(repo.findById("123")).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class,
                () -> service.getUser("123"));
    }
}
