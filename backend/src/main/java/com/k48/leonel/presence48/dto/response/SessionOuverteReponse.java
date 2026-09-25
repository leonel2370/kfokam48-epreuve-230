package com.k48.leonel.presence48.dto.response;

import java.time.Instant;

/** Réponse imposée par le contrat : { id, code, ouvertureAt, expirationAt }. */
public record SessionOuverteReponse(Long id, String code, Instant ouvertureAt, Instant expirationAt) {
}
