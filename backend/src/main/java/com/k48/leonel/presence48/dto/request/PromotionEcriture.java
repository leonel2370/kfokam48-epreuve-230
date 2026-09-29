package com.k48.leonel.presence48.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** #61 / SF-21 : corps de POST et PUT /api/promotions (contrat PromotionEcriture). */
public record PromotionEcriture(@NotBlank @Size(max = 100) String nom) {
}
