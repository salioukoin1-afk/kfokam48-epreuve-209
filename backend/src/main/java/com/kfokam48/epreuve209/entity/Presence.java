package com.kfokam48.epreuve209.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "presence")
public class Presence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private SessionCours session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    /** RG13 : ETUDIANT (par code) ou FORMATEUR (ajout manuel, US-03). */
    @Column(nullable = false)
    private String source;

    @Column(name = "cree_at", nullable = false)
    private LocalDateTime creeAt;

    public Long getId() { return id; }
    public SessionCours getSession() { return session; }
    public Etudiant getEtudiant() { return etudiant; }
    public String getSource() { return source; }
    public LocalDateTime getCreeAt() { return creeAt; }

    public void setSession(SessionCours session) { this.session = session; }
    public void setEtudiant(Etudiant etudiant) { this.etudiant = etudiant; }
    public void setSource(String source) { this.source = source; }
    public void setCreeAt(LocalDateTime creeAt) { this.creeAt = creeAt; }
}
