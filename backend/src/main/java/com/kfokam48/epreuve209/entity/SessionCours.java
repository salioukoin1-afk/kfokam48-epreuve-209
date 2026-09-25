package com.kfokam48.epreuve209.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "session_cours")
public class SessionCours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Column(nullable = false)
    private String titre;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "ouverture_at", nullable = false)
    private LocalDateTime ouvertureAt;

    @Column(name = "expiration_at", nullable = false)
    private LocalDateTime expirationAt;

    @Column(name = "cloturee_at")
    private LocalDateTime clotureeAt;

    public Long getId() { return id; }
    public String getTitre() { return titre; }
    public String getCode() { return code; }
    public LocalDateTime getOuvertureAt() { return ouvertureAt; }
    public LocalDateTime getExpirationAt() { return expirationAt; }
    public LocalDateTime getClotureeAt() { return clotureeAt; }

    public void setPromotion(Promotion promotion) { this.promotion = promotion; }
    public void setTitre(String titre) { this.titre = titre; }
    public void setCode(String code) { this.code = code; }
    public void setOuvertureAt(LocalDateTime ouvertureAt) { this.ouvertureAt = ouvertureAt; }
    public void setExpirationAt(LocalDateTime expirationAt) { this.expirationAt = expirationAt; }
    public void setClotureeAt(LocalDateTime clotureeAt) { this.clotureeAt = clotureeAt; }
}
