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

/** Séance de cours (table session). Nommée SessionCours pour ne pas la confondre avec une session HTTP. */
@Entity
@Table(name = "session")
public class SessionCours {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 200)
  private String titre;

  @Column(name = "promotion_id", nullable = false)
  private Long promotionId;

  @Column(nullable = false, length = 6)
  private String code;

  @Column(name = "ouverture_at", nullable = false)
  private Instant ouvertureAt;

  @Column(name = "expiration_at", nullable = false)
  private Instant expirationAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StatutSession statut = StatutSession.OUVERTE;

  @Column(name = "cloture_at")
  private Instant clotureAt;

  /** Constructeur requis par JPA. */
  protected SessionCours() {
    // instancié par Hibernate
  }

  public SessionCours(String titre, Long promotionId, String code, Instant ouvertureAt, Instant expirationAt) {
    this.titre = titre;
    this.promotionId = promotionId;
    this.code = code;
    this.ouvertureAt = ouvertureAt;
    this.expirationAt = expirationAt;
  }

  public boolean estCloturee() {
    return statut == StatutSession.CLOTUREE;
  }

  public boolean codeExpire(Instant maintenant) {
    return !maintenant.isBefore(expirationAt);
  }

  public Long getId() {
    return id;
  }

  public String getTitre() {
    return titre;
  }

  public Long getPromotionId() {
    return promotionId;
  }

  public String getCode() {
    return code;
  }

  public Instant getOuvertureAt() {
    return ouvertureAt;
  }

  public Instant getExpirationAt() {
    return expirationAt;
  }

  public StatutSession getStatut() {
    return statut;
  }

  public Instant getClotureAt() {
    return clotureAt;
  }
}
