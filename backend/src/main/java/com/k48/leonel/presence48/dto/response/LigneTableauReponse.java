package com.k48.leonel.presence48.dto.response;

import java.math.BigDecimal;

/** Ligne imposée par le contrat ; moyenne null si l'étudiant n'a aucune note (RG16). */
public record LigneTableauReponse(Long etudiantId, String nom, int presences, int exercicesDeposes,
    BigDecimal moyenne, int relecturesEnAttente) {
}
