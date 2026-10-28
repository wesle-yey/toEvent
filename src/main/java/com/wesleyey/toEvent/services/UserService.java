package com.wesleyey.toEvent.services;

import com.wesleyey.toEvent.exception.UserNotFoundException;
import com.wesleyey.toEvent.exception.UserAlreadyExistsException;
import com.wesleyey.toEvent.models.User;
import com.wesleyey.toEvent.DTOs.UserDTO;
import com.wesleyey.toEvent.repositories.UserRepository;
import com.wesleyey.toEvent.DTOs.UserResponseDTO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService{
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,  PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponseDTO saveUser(UserDTO user) {
        String email = User.normalizeEmail(user.getEmail());
        if (userRepository.findByEmail(email).isPresent()) {
            throw new UserAlreadyExistsException("User already exists");
        }

        try {
            userRepository.save(new User(
                    user.getName(),
                    email,
                    passwordEncoder.encode(user.getPassword())));
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw new UserAlreadyExistsException("User already exists");
        }
        return new UserResponseDTO(user.getName(), email);
    }

    public UserResponseDTO getUser(String id) {
    User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException("User id " + id + " not found"));
    return new UserResponseDTO(user.getName(), user.getEmail());
}

    public void deleteUser(String id) {
    if (userRepository.existsById(id)) {
        userRepository.deleteById(id);
    } else {
        throw new UserNotFoundException("User id " + id + " not found");
    }
}
}