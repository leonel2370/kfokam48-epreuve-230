package com.k48.leonel.presence48.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "promotion")
public class Promotion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 100)
  private String nom;

  /** Constructeur requis par JPA. */
  protected Promotion() {
    // instancié par Hibernate
  }

  public Long getId() {
    return id;
  }

  public String getNom() {
    return nom;
  }
}
