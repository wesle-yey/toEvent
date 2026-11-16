package com.wesleyey.toEvent.DTOs;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailDTO(
        @NotBlank(message = "Token de verificação é obrigatório")
        String token
) {}
