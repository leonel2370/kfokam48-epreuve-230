package com.k48.leonel.presence48.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/** HYP-6 / ENF4 : 6 caractères parmi 32 symboles sans ambiguïté (pas de O/0/I/1), soit environ 10^9 codes. */
@Component
public class GenerateurCode {

  static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  static final int LONGUEUR = 6;

  private static final SecureRandom ALEATOIRE = new SecureRandom();

  public String nouveauCode() {
    var code = new StringBuilder(LONGUEUR);
    for (var i = 0; i < LONGUEUR; i++) {
      code.append(ALPHABET.charAt(ALEATOIRE.nextInt(ALPHABET.length())));
    }
    return code.toString();
  }
}
