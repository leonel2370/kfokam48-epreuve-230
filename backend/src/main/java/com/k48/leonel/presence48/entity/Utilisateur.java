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

/** Compte de connexion (migration V3, diagramme D2). Jamais exposé en JSON : passer par ProfilReponse. */
@Entity
@Table(name = "utilisateur")
public class Utilisateur {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 50)
  private String login;

  @Column(name = "mot_de_passe_hash", nullable = false, length = 100)
  private String motDePasseHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private Role role;

  @Column(name = "nom_affiche", nullable = false, length = 150)
  private String nomAffiche;

  @Column(name = "etudiant_id")
  private Long etudiantId;

  @Column(nullable = false)
  private boolean actif = true;

  @Column(name = "doit_changer_mot_de_passe", nullable = false)
  private boolean doitChangerMotDePasse;

  @Column(name = "echecs_connexion", nullable = false)
  private int echecsConnexion;

  @Column(name = "bloque_jusqu_a")
  private Instant bloqueJusquA;

  @Column(name = "cree_at", nullable = false)
  private Instant creeAt;

  public Long getId() {
    return id;
  }

  public String getLogin() {
    return login;
  }

  public void setLogin(String login) {
    this.login = login;
  }

  public String getMotDePasseHash() {
    return motDePasseHash;
  }

  public void setMotDePasseHash(String motDePasseHash) {
    this.motDePasseHash = motDePasseHash;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public String getNomAffiche() {
    return nomAffiche;
  }

  public void setNomAffiche(String nomAffiche) {
    this.nomAffiche = nomAffiche;
  }

  public Long getEtudiantId() {
    return etudiantId;
  }

  public void setEtudiantId(Long etudiantId) {
    this.etudiantId = etudiantId;
  }

  public boolean isActif() {
    return actif;
  }

  public void setActif(boolean actif) {
    this.actif = actif;
  }

  public boolean isDoitChangerMotDePasse() {
    return doitChangerMotDePasse;
  }

  public void setDoitChangerMotDePasse(boolean doitChangerMotDePasse) {
    this.doitChangerMotDePasse = doitChangerMotDePasse;
  }

  public int getEchecsConnexion() {
    return echecsConnexion;
  }

  public void setEchecsConnexion(int echecsConnexion) {
    this.echecsConnexion = echecsConnexion;
  }

  public Instant getBloqueJusquA() {
    return bloqueJusquA;
  }

  public void setBloqueJusquA(Instant bloqueJusquA) {
    this.bloqueJusquA = bloqueJusquA;
  }

  public Instant getCreeAt() {
    return creeAt;
  }

  public void setCreeAt(Instant creeAt) {
    this.creeAt = creeAt;
  }
}
