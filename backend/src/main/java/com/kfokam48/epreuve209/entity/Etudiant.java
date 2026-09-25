package com.kfokam48.epreuve209.entity;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "etudiant")
public class Etudiant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Column(nullable = false)
    private String nom;

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public Promotion getPromotion() { return promotion; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Etudiant other)) return false;
        return id != null && id.equals(other.id);
    }
    @Override
    public int hashCode() { return Objects.hashCode(id); }
}
