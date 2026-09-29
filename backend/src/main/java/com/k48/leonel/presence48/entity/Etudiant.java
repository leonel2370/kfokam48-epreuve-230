package com.k48.leonel.presence48.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "etudiant")
public class Etudiant {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 150)
  private String nom;

  @Column(name = "promotion_id", nullable = false)
  private Long promotionId;

  @Column(nullable = false)
  private boolean actif = true;

  /** Constructeur requis par JPA. */
  protected Etudiant() {
    // instancié par Hibernate
  }

  /** #61 : création d'une fiche étudiant par l'ADMIN ou le FORMATEUR de la promotion. */
  public Etudiant(String nom, Long promotionId) {
    this.nom = nom;
    this.promotionId = promotionId;
    this.actif = true;
  }

  public Long getId() {
    return id;
  }

  public String getNom() {
    return nom;
  }

  public void setNom(String nom) {
    this.nom = nom;
  }

  public Long getPromotionId() {
    return promotionId;
  }

  public void setPromotionId(Long promotionId) {
    this.promotionId = promotionId;
  }

  public boolean isActif() {
    return actif;
  }

  public void setActif(boolean actif) {
    this.actif = actif;
  }
}
