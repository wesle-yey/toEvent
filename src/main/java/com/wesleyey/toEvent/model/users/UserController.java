package com.wesleyey.toEvent.model.users;

import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/users")
public class UserController {
    /*
    TODO: ROTAS PUT DELETE
     */

    UserService userService;

    public UserController(
            UserService userService
    ) {
        this.userService = userService;
    }

    @GetMapping
    public UserResponseDTO getUser(@RequestParam String id) throws Exception {
        return userService.getUser(id);
    }

    @PostMapping
    public UserResponseDTO criarUsuario(@RequestBody UserDTO userDTO) {
        return userService.saveUser(userDTO);
    }
}
