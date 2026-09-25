package com.kfokam48.epreuve209.service;

import com.kfokam48.epreuve209.dto.MarquerPresenceRequest;
import com.kfokam48.epreuve209.dto.PresenceResponse;
import com.kfokam48.epreuve209.entity.Etudiant;
import com.kfokam48.epreuve209.entity.Presence;
import com.kfokam48.epreuve209.entity.SessionCours;
import com.kfokam48.epreuve209.entity.TentativeCode;
import com.kfokam48.epreuve209.exception.*;
import com.kfokam48.epreuve209.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * B6 — tests unitaires des règles métier de la présence (D3) :
 * ordre des vérifications, compteur RG3 global, réinitialisation au succès.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PresenceServiceTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 9, 25, 10, 0);

    @Mock SessionCoursRepository sessions;
    @Mock EtudiantRepository etudiants;
    @Mock PresenceRepository presences;
    @Mock TentativeCodeRepository tentatives;

    private PresenceService service;
    private SessionCours sessionActive;

    @BeforeEach
    void setUp() {
        Clock horlogeFixe = Clock.fixed(MAINTENANT.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new PresenceService(sessions, etudiants, presences, tentatives, horlogeFixe);

        sessionActive = new SessionCours();
        org.springframework.test.util.ReflectionTestUtils.setField(sessionActive, "id", 1L);
        sessionActive.setCode("AB12CD");
        sessionActive.setOuvertureAt(MAINTENANT.minusMinutes(1));
        sessionActive.setExpirationAt(MAINTENANT.plusMinutes(14));

        when(sessions.findByCodeIgnoreCase("AB12CD")).thenReturn(Optional.of(sessionActive));
        when(etudiants.findById(101L)).thenReturn(Optional.of(new Etudiant()));
        when(etudiants.getReferenceById(101L)).thenReturn(new Etudiant());
        when(presences.existsBySessionIdAndEtudiantId(1L, 101L)).thenReturn(false);
        when(tentatives.findByEtudiant_IdAndSessionIsNull(101L)).thenReturn(Optional.empty());
        when(presences.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private MarquerPresenceRequest requete() {
        return new MarquerPresenceRequest("ab12cd", 101L);   // casse volontairement différente
    }

    @Test
    void nominal_source_ETUDIANT_et_casse_normalisee() {
        PresenceResponse r = service.marquerPresent(requete());

        assertThat(r.source()).isEqualTo("ETUDIANT");
        assertThat(r.sessionId()).isEqualTo(1L);
        verify(presences).save(any(Presence.class));
    }

    @Test
    void blocage_RG3_en_cours_429_sans_meme_chercher_le_code() {
        TentativeCode compteur = new TentativeCode();
        compteur.setBloqueJusqua(MAINTENANT.plusSeconds(90));
        when(tentatives.findByEtudiant_IdAndSessionIsNull(101L)).thenReturn(Optional.of(compteur));

        // Le code est pourtant VALIDE : c'est le blocage qui doit parler avant tout (D3).
        assertThatThrownBy(() -> service.marquerPresent(requete()))
                .isInstanceOf(BlocageTentativesException.class)
                .hasMessageContaining("90");
        verify(sessions, never()).findByCodeIgnoreCase(any());
    }

    @Test
    void code_inconnu_400_et_alimente_le_compteur_RG3() {
        when(sessions.findByCodeIgnoreCase("ZZZZZZ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marquerPresent(new MarquerPresenceRequest("zzzzzz", 101L)))
                .isInstanceOf(CodeInconnuException.class);
        verify(tentatives).save(any(TentativeCode.class));
    }

    @Test
    void code_inconnu_au_5e_echec_declenche_le_blocage_de_2_minutes() {
        when(sessions.findByCodeIgnoreCase(any())).thenReturn(Optional.empty());
        TentativeCode compteur = new TentativeCode();
        compteur.setEchecsConsecutifs(4);
        when(tentatives.findByEtudiant_IdAndSessionIsNull(101L)).thenReturn(Optional.of(compteur));

        assertThatThrownBy(() -> service.marquerPresent(new MarquerPresenceRequest("xxxxxx", 101L)))
                .isInstanceOf(CodeInconnuException.class);

        verify(tentatives).save(argThat(t -> t.getBloqueJusqua() != null
                && t.getBloqueJusqua().equals(MAINTENANT.plusMinutes(2))));
    }

    @Test
    void code_expire_410_RG1_avant_le_controle_de_cloture() {
        sessionActive.setExpirationAt(MAINTENANT.minusSeconds(1));
        sessionActive.setClotureeAt(MAINTENANT.minusSeconds(2));   // les deux : 410 doit gagner (D3)

        assertThatThrownBy(() -> service.marquerPresent(requete()))
                .isInstanceOf(CodeExpireException.class);
    }

    @Test
    void session_cloturee_409_RG2() {
        sessionActive.setClotureeAt(MAINTENANT.minusSeconds(1));

        assertThatThrownBy(() -> service.marquerPresent(requete()))
                .isInstanceOf(SessionClotureeException.class);
    }

    @Test
    void deja_present_409_RG15() {
        when(presences.existsBySessionIdAndEtudiantId(1L, 101L)).thenReturn(true);

        assertThatThrownBy(() -> service.marquerPresent(requete()))
                .isInstanceOf(DejaPresentException.class);
        verify(presences, never()).save(any());
    }

    @Test
    void le_succes_reinitialise_le_compteur_d_echecs() {
        TentativeCode compteur = new TentativeCode();
        compteur.setEchecsConsecutifs(3);
        when(tentatives.findByEtudiant_IdAndSessionIsNull(101L)).thenReturn(Optional.of(compteur));

        service.marquerPresent(requete());

        assertThat(compteur.getEchecsConsecutifs()).isZero();
        assertThat(compteur.getBloqueJusqua()).isNull();
    }

    @Test
    void les_autres_erreurs_que_le_code_inconnu_ne_touchent_pas_au_compteur() {
        sessionActive.setExpirationAt(MAINTENANT.minusSeconds(1));

        assertThatThrownBy(() -> service.marquerPresent(requete()))
                .isInstanceOf(CodeExpireException.class);
        verify(tentatives, never()).save(any());
    }
}
