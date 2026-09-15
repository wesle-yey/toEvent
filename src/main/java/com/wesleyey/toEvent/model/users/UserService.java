package com.wesleyey.toEvent.model.users;

import org.springframework.stereotype.Service;

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

    public UserResponseDTO getUser(String id) throws Exception {
        if (userRepository.existsById(id)) {
            User user =  userRepository.findById(id).get();
            return new UserResponseDTO(user.getName(), user.getEmail());
        } else {
            throw new Exception("User not found");
        }
    }

    public void deleteUser(String id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
        }
    }
}
