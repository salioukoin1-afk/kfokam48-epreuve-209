package com.kfokam48.epreuve209.service;

import com.kfokam48.epreuve209.dto.DeposerExerciceRequest;
import com.kfokam48.epreuve209.dto.ExerciceResponse;
import com.kfokam48.epreuve209.entity.Exercice;
import com.kfokam48.epreuve209.entity.SessionCours;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExerciceServiceTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 9, 25, 10, 0);

    @Mock SessionCoursRepository sessions;
    @Mock com.kfokam48.epreuve209.repository.EtudiantRepository etudiants;
    @Mock PresenceRepository presences;
    @Mock ExerciceRepository exercices;
    @Mock RelectureRepository relectures;

    private ExerciceService service;
    private SessionCours session;

    @BeforeEach
    void setUp() {
        Clock horlogeFixe = Clock.fixed(MAINTENANT.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new ExerciceService(sessions, etudiants, presences, exercices, relectures, horlogeFixe);
        when(etudiants.getReferenceById(any(Long.class))).thenAnswer(inv -> {
            com.kfokam48.epreuve209.entity.Etudiant e = new com.kfokam48.epreuve209.entity.Etudiant();
            org.springframework.test.util.ReflectionTestUtils.setField(e, "id", (Long) inv.getArgument(0));
            return e;
        });

        session = new SessionCours();
        org.springframework.test.util.ReflectionTestUtils.setField(session, "id", 1L);
        when(sessions.findById(1L)).thenReturn(Optional.of(session));
        when(presences.existsBySessionIdAndEtudiantId(1L, 101L)).thenReturn(true);
        when(presences.findEtudiantIdsBySessionId(1L)).thenReturn(List.of(101L, 102L, 103L));
        when(relectures.findRelecteurIdsBySessionId(1L)).thenReturn(List.of());
        when(exercices.findBySessionIdAndAuteurId(1L, 101L)).thenReturn(Optional.empty());
        when(exercices.save(any())).thenAnswer(inv -> {
            Exercice e = inv.getArgument(0);
            if (e.getId() == null) {
                org.springframework.test.util.ReflectionTestUtils.setField(e, "id", 42L);
            }
            return e;
        });
    }

    private DeposerExerciceRequest requete(String lien) {
        return new DeposerExerciceRequest(1L, 101L, lien);
    }

    @Test
    void premier_depot_201_DEPOSE_puis_EN_RELECTURE_quand_un_candidat_existe() {
        ExerciceResponse r = service.deposer(requete("https://exemples.fr/travail"));

        assertThat(r.statut()).isEqualTo("EN_RELECTURE");
        verify(relectures).save(any());
        verify(exercices).save(any(Exercice.class));
    }

    @Test
    void sans_candidat_eligible_statut_EN_ATTENTE_RELECTEUR_selon_RG14() {
        // Seul l'auteur est présent : aucun candidat (RG6).
        when(presences.findEtudiantIdsBySessionId(1L)).thenReturn(List.of(101L));

        ExerciceResponse r = service.deposer(requete("https://exemples.fr/travail"));

        assertThat(r.statut()).isEqualTo("DEPOSE");
        verify(relectures, never()).save(any());
    }

    @Test
    void etudiant_absent_400_NON_PRESENT_decision_section_7() {
        when(presences.existsBySessionIdAndEtudiantId(1L, 101L)).thenReturn(false);

        assertThatThrownBy(() -> service.deposer(requete("https://exemples.fr/travail")))
                .isInstanceOf(NonPresentException.class);
    }

    @Test
    void lien_invalide_400() {
        assertThatThrownBy(() -> service.deposer(requete("ftp://bizarre")))
                .isInstanceOf(LienInvalideException.class);
        verify(exercices, never()).save(any());
    }

    @Test
    void remplacement_pendant_EN_RELECTURE_autorise_RG12_meme_id_pas_de_reassignation() {
        Exercice existant = new Exercice();
        org.springframework.test.util.ReflectionTestUtils.setField(existant, "id", 42L);
        existant.setStatut("EN_RELECTURE");
        existant.setLien("https://ancien.fr/v1");
        when(exercices.findBySessionIdAndAuteurId(1L, 101L)).thenReturn(Optional.of(existant));

        ExerciceResponse r = service.deposer(requete("https://nouveau.fr/v2"));

        assertThat(r.id()).isEqualTo(42L);
        assertThat(r.statut()).isEqualTo("EN_RELECTURE");
        assertThat(existant.getLien()).isEqualTo("https://nouveau.fr/v2");
        verify(relectures, never()).save(any());   // jamais de réassignation
    }

    @Test
    void remplacement_apres_RELU_409_EXERCICE_DEJA_DEPOSE() {
        Exercice existant = new Exercice();
        existant.setStatut("RELU");
        when(exercices.findBySessionIdAndAuteurId(1L, 101L)).thenReturn(Optional.of(existant));

        assertThatThrownBy(() -> service.deposer(requete("https://nouveau.fr/v2")))
                .isInstanceOf(ExerciceDejaDeposeException.class);
    }

    @Test
    void le_relecteur_assigne_n_est_jamais_l_auteur_RG6() {
        // Auteur présent + deux autres présents : l'assigné doit être 102 ou 103, jamais 101.
        when(relectures.findRelecteurIdsBySessionId(1L)).thenReturn(List.of());
        when(relectures.save(any())).thenAnswer(inv -> inv.getArgument(0));

        for (int i = 0; i < 20; i++) {
            when(exercices.findBySessionIdAndAuteurId(1L, 101L)).thenReturn(Optional.empty());
            service.deposer(requete("https://exemples.fr/essai-" + i));
        }
        verify(relectures, times(20)).save(argThat(r ->
                !r.getRelecteur().getId().equals(101L)));
    }

    @Test
    void session_cloturee_409_RG11() {
        session.setClotureeAt(MAINTENANT.minusSeconds(1));

        assertThatThrownBy(() -> service.deposer(requete("https://exemples.fr/travail")))
                .isInstanceOf(SessionClotureeException.class);
    }
}
