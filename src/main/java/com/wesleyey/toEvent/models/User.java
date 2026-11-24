package com.wesleyey.toEvent.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Data
@Document(collection = "users")
public class User implements UserDetails {
    @Id
    private String id;

    private String name;

    @Indexed(unique = true)
    private String email;

    private String password;

    boolean premium = false;

    private boolean emailVerified = false;

    private String verificationToken;

    private Instant verificationTokenExpiresAt;

    // Hash do código guardado em cookie no navegador que pediu o último envio do link.
    // Só esse navegador entra automaticamente ao confirmar o e-mail.
    private String verificationBrowserHash;

    // Controle de envios do link de confirmação por conta (cooldown + máximo por 24h)
    private Instant lastVerificationEmailAt;
    private Instant verificationEmailWindowStart;
    private int verificationEmailsInWindow;

    // Contas não confirmadas são apagadas pelo MongoDB (índice TTL) quando esta data passa.
    // Fica nulo depois da confirmação, então contas confirmadas nunca expiram.
    @Indexed(expireAfter = "0s")
    private Instant unverifiedExpiresAt;

    public User(String name, String email, String password) {
        this.name = name;
        this.email = normalizeEmail(email);
        this.password = password;
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.emailVerified;
    }

    public boolean isPremium() {
        return premium;
    }
}
