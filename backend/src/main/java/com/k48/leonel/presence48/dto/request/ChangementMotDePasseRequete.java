package com.k48.leonel.presence48.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChangementMotDePasseRequete(@NotBlank String ancien, @NotBlank String nouveau) {
}
