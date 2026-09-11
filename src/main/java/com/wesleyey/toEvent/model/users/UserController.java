package com.wesleyey.toEvent.model.users;
import org.springframework.data.annotation.Id;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller("/api/users")
public class UserController {
    /*
    TODO: ROTAS GET POST PUT DELETE
     */
    UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("{id}")
    public HttpEntity<UserResponseDTO> GetUser(@PathVariable Id id) {
        try {
            UserResponseDTO user= userService.getUserById(id);

            return new HttpEntity<>(new UserResponseDTO(user));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }
    }
