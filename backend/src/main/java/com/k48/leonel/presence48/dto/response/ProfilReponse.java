package com.k48.leonel.presence48.dto.response;

import com.k48.leonel.presence48.entity.Role;
import java.util.List;

/** Contrat : components/schemas/Profil. */
public record ProfilReponse(Long id, String login, String nomAffiche, Role role, Long etudiantId,
    List<Long> promotionIds, boolean doitChangerMotDePasse) {
}
