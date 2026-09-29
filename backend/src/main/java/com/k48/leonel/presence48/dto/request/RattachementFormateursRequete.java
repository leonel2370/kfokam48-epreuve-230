package com.k48.leonel.presence48.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** #61 / RG26 : corps de PUT /api/promotions/{promotionId}/formateurs. */
public record RattachementFormateursRequete(@NotEmpty List<Long> utilisateurIds) {
}
