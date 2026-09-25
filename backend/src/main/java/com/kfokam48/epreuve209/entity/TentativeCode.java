package com.kfokam48.epreuve209.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tentative_code")
public class TentativeCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** NULL = compteur global de l'étudiant (échecs de code inconnu — RG3, CDC v1.3). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private SessionCours session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    @Column(name = "echecs_consecutifs", nullable = false)
    private int echecsConsecutifs;

    @Column(name = "bloque_jusqua")
    private LocalDateTime bloqueJusqua;

    public Long getId() { return id; }
    public SessionCours getSession() { return session; }
    public Etudiant getEtudiant() { return etudiant; }
    public int getEchecsConsecutifs() { return echecsConsecutifs; }
    public LocalDateTime getBloqueJusqua() { return bloqueJusqua; }

    public void setSession(SessionCours session) { this.session = session; }
    public void setEtudiant(Etudiant etudiant) { this.etudiant = etudiant; }
    public void setEchecsConsecutifs(int echecsConsecutifs) { this.echecsConsecutifs = echecsConsecutifs; }
    public void setBloqueJusqua(LocalDateTime bloqueJusqua) { this.bloqueJusqua = bloqueJusqua; }
}
