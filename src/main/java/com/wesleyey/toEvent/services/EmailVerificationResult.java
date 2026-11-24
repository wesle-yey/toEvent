package com.wesleyey.toEvent.services;

import com.wesleyey.toEvent.models.User;

/** Resultado da confirmação de e-mail: o usuário confirmado e se este navegador pode entrar automaticamente. */
public record EmailVerificationResult(User user, boolean autoLogin) {
}
