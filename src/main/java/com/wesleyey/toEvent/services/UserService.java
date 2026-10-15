package com.wesleyey.toEvent.services;

import com.wesleyey.toEvent.models.User;
import com.wesleyey.toEvent.DTOs.UserDTO;
import com.wesleyey.toEvent.repositories.UserRepository;
import com.wesleyey.toEvent.DTOs.UserResponseDTO;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService{
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,  PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponseDTO saveUser(UserDTO user) {
        userRepository.save(new User(
                user.getName(),
                user.getEmail(),
                passwordEncoder.encode(user.getPassword())));
        return new UserResponseDTO(user.getName(), user.getEmail());
    }

    public UserResponseDTO getUser(String id) throws ChangeSetPersister.NotFoundException {

        Optional<User> user = Optional.of(
                userRepository.findById(id)
                        .orElseThrow(ChangeSetPersister.NotFoundException::new)
        );

        return new UserResponseDTO(user.get().getName(), user.get().getEmail());
        }

    public void deleteUser(String id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
        }
    }
}