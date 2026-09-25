package com.k48.leonel.presence48.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;

/** RG4 (Q4) : compteur de codes inconnus consécutifs et blocage temporaire, par étudiant. */
@Entity
@Table(name = "tentative_code")
public class TentativeCode {

  /** Nombre de codes inconnus consécutifs qui déclenche le blocage. */
  public static final int ECHECS_MAX = 5;
  /** Durée du blocage. */
  public static final Duration DUREE_BLOCAGE = Duration.ofMinutes(2);

  @Id
  @Column(name = "etudiant_id")
  private Long etudiantId;

  @Column(name = "echecs_consecutifs", nullable = false)
  private int echecsConsecutifs;

  @Column(name = "bloque_jusqu_a")
  private Instant bloqueJusquA;

  /** Constructeur requis par JPA. */
  protected TentativeCode() {
    // instancié par Hibernate
  }

  public TentativeCode(Long etudiantId) {
    this.etudiantId = etudiantId;
  }

  public boolean estBloque(Instant maintenant) {
    return bloqueJusquA != null && maintenant.isBefore(bloqueJusquA);
  }

  /** Un code inconnu de plus ; au 5e, blocage de 2 minutes et compteur remis à zéro. */
  public void echec(Instant maintenant) {
    echecsConsecutifs++;
    if (echecsConsecutifs >= ECHECS_MAX) {
      bloqueJusquA = maintenant.plus(DUREE_BLOCAGE);
      echecsConsecutifs = 0;
    }
  }

  /** Une tentative réussie remet le compteur à zéro (RG4). */
  public void reussite() {
    echecsConsecutifs = 0;
    bloqueJusquA = null;
  }

  public Long getEtudiantId() {
    return etudiantId;
  }

  public int getEchecsConsecutifs() {
    return echecsConsecutifs;
  }

  public Instant getBloqueJusquA() {
    return bloqueJusquA;
  }
}
