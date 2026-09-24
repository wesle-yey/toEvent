package com.wesleyey.toEvent.model.users;

import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponseDTO saveUser(UserDTO user) {
        userRepository.save(new User(user.getName(), user.getEmail(), user.getPassword()));
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