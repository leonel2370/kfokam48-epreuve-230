package com.k48.leonel.presence48.dto.response;

import java.util.List;

/** #60 / SF-20 : page de comptes (contrat : contenu, page, taille, total). */
public record PageUtilisateursReponse(List<UtilisateurReponse> contenu, int page, int taille, long total) {
}
