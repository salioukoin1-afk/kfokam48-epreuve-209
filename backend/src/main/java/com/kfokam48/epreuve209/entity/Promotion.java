package com.kfokam48.epreuve209.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "promotion")
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @OneToMany(mappedBy = "promotion")
    private List<Etudiant> etudiants = new ArrayList<>();

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public List<Etudiant> getEtudiants() { return etudiants; }
}
