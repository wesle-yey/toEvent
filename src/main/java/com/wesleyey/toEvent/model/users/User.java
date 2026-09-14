package com.wesleyey.toEvent.model.users;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;

@Data
@Document
public class User {
    @Id
    private String id;

    private String name;

    private String email;

    private String password;

    // Construtor de usuário sem ID, pois o ID é gerado automaticamente pelo MongoDB
    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }
}
