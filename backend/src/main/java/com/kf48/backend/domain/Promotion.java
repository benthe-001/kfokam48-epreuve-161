package com.kf48.relectures.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "promotion")
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String nom;

    protected Promotion() {}
    public Promotion(String nom) { this.nom = nom; }

    public Long getId() { return id; }
    public String getNom() { return nom; }
}