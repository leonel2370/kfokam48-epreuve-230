package com.k48.leonel.presence48.dto.response;

import com.k48.leonel.presence48.entity.SourcePresence;

/** Réponse imposée par le contrat : { id, sessionId, etudiantId, source }. */
public record PresenceReponse(Long id, Long sessionId, Long etudiantId, SourcePresence source) {
}
