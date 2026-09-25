package com.kfokam48.epreuve209.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "exercice")
public class Exercice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private SessionCours session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant auteur;

    @Column(nullable = false)
    private String lien;

    /** États de D4 : DEPOSE | EN_ATTENTE_RELECTEUR | EN_RELECTURE | RELU. */
    @Column(nullable = false)
    private String statut;

    @Column(name = "depose_at", nullable = false)
    private LocalDateTime deposeAt;

    public Long getId() { return id; }
    public SessionCours getSession() { return session; }
    public Etudiant getAuteur() { return auteur; }
    /** Alias de rôle : le déposant vu comme « auteur » (RG4/RG6). */
    public Etudiant getEtudiant() { return auteur; }
    public String getLien() { return lien; }
    public String getStatut() { return statut; }
    public LocalDateTime getDeposeAt() { return deposeAt; }

    public void setSession(SessionCours session) { this.session = session; }
    public void setAuteur(Etudiant auteur) { this.auteur = auteur; }
    public void setLien(String lien) { this.lien = lien; }
    public void setStatut(String statut) { this.statut = statut; }
    public void setDeposeAt(LocalDateTime deposeAt) { this.deposeAt = deposeAt; }
}
