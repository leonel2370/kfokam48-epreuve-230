package com.k48.leonel.presence48.dto.response;

import com.k48.leonel.presence48.entity.StatutExercice;
import java.math.BigDecimal;
import java.util.List;

/** Schéma ExerciceAuteur (contrat 2.1) : note retenue, provisoire, commentaires ; jamais les relecteurs (RG8). */
public record ExerciceAuteurReponse(Long id, Long sessionId, String sessionTitre, String lien, StatutExercice statut,
    BigDecimal noteRetenue, boolean provisoire, List<String> commentaires) {
}
