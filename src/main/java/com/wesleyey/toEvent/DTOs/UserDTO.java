package com.wesleyey.toEvent.DTOs;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserDTO {
    @NotBlank(message = "O nome é obrigatório.")
    @Size(min = 3, message = "O nome deve conter no mínimo 3 letras.")
    String name;

    @NotBlank(message = "O email é obrigatório.")
    @Email(message = "Insira um email válido.")
    String email;

    @NotBlank(message = "A senha é obrigatória.")
    @Size(min = 8, message = "A senha deve conter no mínimo 8 dígitos.")
    String password;

public UserDTO(@NotBlank String name,
               @NotBlank @Email String email,
               @NotBlank @Size(min = 8) String password) {
    this.name = name;
    this.email = email;
    this.password = password;

}
}
