package com.k48.leonel.presence48.dto.response;

import com.k48.leonel.presence48.entity.StatutExercice;

/** Réponse imposée par le contrat : { id, statut }. */
public record ExerciceDeposeReponse(Long id, StatutExercice statut) {
}
