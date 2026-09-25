package com.k48.leonel.presence48.dto.response;

import com.k48.leonel.presence48.entity.StatutSession;
import java.time.Instant;

/** Contrat : components/schemas/Session. */
public record SessionReponse(Long id, String titre, Long promotionId, String code, Instant ouvertureAt,
    Instant expirationAt, StatutSession statut, Instant clotureAt) {
}
