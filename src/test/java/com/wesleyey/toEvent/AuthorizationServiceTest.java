package com.wesleyey.toEvent;

import com.wesleyey.toEvent.models.User;
import com.wesleyey.toEvent.repositories.UserRepository;
import com.wesleyey.toEvent.services.AuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    AuthorizationService authorizationService;

    @Test
    void loadUserByUsername_normalizesEmailBeforeLookup() {
        User user = new User("Ana", "ana@example.com", "encoded-password");
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(user));

        User loaded = (User) authorizationService.loadUserByUsername(" Ana@Example.com ");

        assertSame(user, loaded);
        verify(userRepository).findByEmail("ana@example.com");
    }
}
