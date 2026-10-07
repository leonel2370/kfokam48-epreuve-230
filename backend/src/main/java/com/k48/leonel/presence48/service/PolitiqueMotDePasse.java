package com.k48.leonel.presence48.service;

import com.k48.leonel.presence48.exception.MetierException;
import org.springframework.http.HttpStatus;

/** RG24 : règle unique de solidité d'un mot de passe, partagée par le profil et la gestion des comptes. */
public final class PolitiqueMotDePasse {

  public static final int LONGUEUR_MIN = 8;

  private PolitiqueMotDePasse() {
  }

  /** Refuse un mot de passe trop court : 400 MOT_DE_PASSE_TROP_FAIBLE. */
  public static void exigerLongueur(String motDePasse) {
    if (motDePasse == null || motDePasse.length() < LONGUEUR_MIN) {
      throw new MetierException(HttpStatus.BAD_REQUEST, "MOT_DE_PASSE_TROP_FAIBLE",
          "Le mot de passe doit contenir au moins " + LONGUEUR_MIN + " caractères.");
    }
  }
}
