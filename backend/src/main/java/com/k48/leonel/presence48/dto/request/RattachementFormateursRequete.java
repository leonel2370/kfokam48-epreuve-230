package com.k48.leonel.presence48.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * #61 / RG26 : corps de PUT /api/promotions/{promotionId}/formateurs. La liste remplace les rattachements ;
 * vide, elle les retire tous (contrat 2.8, #134). Un identifiant nul est refusé.
 */
public record RattachementFormateursRequete(@NotNull List<@NotNull Long> utilisateurIds) {
}
