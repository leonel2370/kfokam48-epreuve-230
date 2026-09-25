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

/** Présence d'un étudiant à une session, unique par (session, étudiant) (RG3). */
@Entity
@Table(name = "presence")
public class Presence {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "session_id", nullable = false)
  private Long sessionId;

  @Column(name = "etudiant_id", nullable = false)
  private Long etudiantId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private SourcePresence source;

  @Column(name = "marquee_at", nullable = false)
  private Instant marqueeAt;

  /** Constructeur requis par JPA. */
  protected Presence() {
    // instancié par Hibernate
  }

  public Presence(Long sessionId, Long etudiantId, SourcePresence source, Instant marqueeAt) {
    this.sessionId = sessionId;
    this.etudiantId = etudiantId;
    this.source = source;
    this.marqueeAt = marqueeAt;
  }

  public Long getId() {
    return id;
  }

  public Long getSessionId() {
    return sessionId;
  }

  public Long getEtudiantId() {
    return etudiantId;
  }

  public SourcePresence getSource() {
    return source;
  }

  public Instant getMarqueeAt() {
    return marqueeAt;
  }
}
