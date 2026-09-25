package com.k48.leonel.presence48.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ConnexionRequete(@NotBlank String login, @NotBlank String motDePasse) {
}
