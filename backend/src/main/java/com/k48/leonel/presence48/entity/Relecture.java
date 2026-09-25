package com.k48.leonel.presence48.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Relecture d'un exercice par un pair : au plus une par exercice (RG6), définitive une fois rendue (RG10). */
@Entity
@Table(name = "relecture")
public class Relecture {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "exercice_id", nullable = false)
  private Long exerciceId;

  @Column(name = "relecteur_id", nullable = false)
  private Long relecteurId;

  private Integer note;

  private String commentaire;

  @Column(name = "assignee_at", nullable = false)
  private Instant assigneeAt;

  @Column(name = "rendue_at")
  private Instant rendueAt;

  /** Constructeur requis par JPA. */
  protected Relecture() {
    // instancié par Hibernate
  }

  public Relecture(Long exerciceId, Long relecteurId, Instant assigneeAt) {
    this.exerciceId = exerciceId;
    this.relecteurId = relecteurId;
    this.assigneeAt = assigneeAt;
  }

  public boolean estRendue() {
    return rendueAt != null;
  }

  /** RG10 : la note et le commentaire sont figés à l'envoi. */
  public void rendre(int note, String commentaire, Instant maintenant) {
    this.note = note;
    this.commentaire = commentaire;
    this.rendueAt = maintenant;
  }

  public Long getId() {
    return id;
  }

  public Long getExerciceId() {
    return exerciceId;
  }

  public Long getRelecteurId() {
    return relecteurId;
  }

  public Integer getNote() {
    return note;
  }

  public String getCommentaire() {
    return commentaire;
  }

  public Instant getAssigneeAt() {
    return assigneeAt;
  }

  public Instant getRendueAt() {
    return rendueAt;
  }
}
