package com.kfokam48.epreuve209.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Ligne créée à l'assignation du relecteur (RG14, D2 v1.2) : rendue_at NULL tant que
 * la note n'est pas rendue — c'est l'état EN_RELECTURE de D4. Le rendu (EF7) remplit
 * note, commentaire et rendue_at ; PATCH (EF8/RG9) les remplace jusqu'à la clôture.
 */
@Entity
@Table(name = "relecture")
public class Relecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercice_id", nullable = false, unique = true)
    private Exercice exercice;

    /** ≠ auteur (RG4) ; jamais exposé à l'auteur (RG7). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "relecteur_id", nullable = false)
    private Etudiant relecteur;

    @Column(nullable = true)
    private Integer note;

    @Column(length = 2000)
    private String commentaire;

    @Column(name = "rendue_at")
    private LocalDateTime rendueAt;

    @Column(name = "maj_at")
    private LocalDateTime majAt;

    public Long getId() { return id; }
    public Exercice getExercice() { return exercice; }
    public Etudiant getRelecteur() { return relecteur; }
    public Integer getNote() { return note; }
    public String getCommentaire() { return commentaire; }
    public LocalDateTime getRendueAt() { return rendueAt; }
    public LocalDateTime getMajAt() { return majAt; }

    public void setExercice(Exercice exercice) { this.exercice = exercice; }
    public void setRelecteur(Etudiant relecteur) { this.relecteur = relecteur; }
    public void setNote(Integer note) { this.note = note; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }
    public void setRendueAt(LocalDateTime rendueAt) { this.rendueAt = rendueAt; }
    public void setMajAt(LocalDateTime majAt) { this.majAt = majAt; }
}
