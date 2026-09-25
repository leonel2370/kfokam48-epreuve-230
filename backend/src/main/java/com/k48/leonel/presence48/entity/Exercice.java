package com.k48.leonel.presence48.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Exercice déposé par un étudiant pour une session, unique par (session, auteur) (RG13). */
@Entity
@Table(name = "exercice")
public class Exercice {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "session_id", nullable = false)
  private Long sessionId;

  @Column(name = "auteur_id", nullable = false)
  private Long auteurId;

  @Column(nullable = false, length = 2048)
  private String lien;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 25)
  private StatutExercice statut = StatutExercice.DEPOSE;

  @Column(name = "depose_at", nullable = false)
  private Instant deposeAt;

  @Column(name = "modifie_at")
  private Instant modifieAt;

  /** Constructeur requis par JPA. */
  protected Exercice() {
    // instancié par Hibernate
  }

  public Exercice(Long sessionId, Long auteurId, String lien, Instant deposeAt) {
    this.sessionId = sessionId;
    this.auteurId = auteurId;
    this.lien = lien;
    this.deposeAt = deposeAt;
  }

  /** SF-7 : un relecteur vient d'être tiré au sort. */
  public void attendreRelecture() {
    statut = StatutExercice.EN_ATTENTE_RELECTURE;
  }

  public Long getId() {
    return id;
  }

  public Long getSessionId() {
    return sessionId;
  }

  public Long getAuteurId() {
    return auteurId;
  }

  public String getLien() {
    return lien;
  }

  public StatutExercice getStatut() {
    return statut;
  }

  public Instant getDeposeAt() {
    return deposeAt;
  }

  public Instant getModifieAt() {
    return modifieAt;
  }
}
