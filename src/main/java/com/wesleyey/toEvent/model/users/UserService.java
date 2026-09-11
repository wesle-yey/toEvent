package com.wesleyey.toEvent.model.users;

import org.springframework.data.annotation.Id;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponseDTO getUserById(Id id) throws Exception {
        if (id != null) {
            try {
                Optional<User> myUser= userRepository.findById(id);
                if (myUser.isPresent()) {
                    User user = myUser.get();
                    return new UserResponseDTO(user.getName(), user.getEmail());
                }
            }  catch (Exception e) {
                throw new Exception(e.getMessage());
            }
        } else {
            return null;
        }

        return null;
    }
}
