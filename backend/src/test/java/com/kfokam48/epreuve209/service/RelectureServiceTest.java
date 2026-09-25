package com.kfokam48.epreuve209.service;

import com.kfokam48.epreuve209.dto.NoterRelectureRequest;
import com.kfokam48.epreuve209.dto.RelectureResponse;
import com.kfokam48.epreuve209.entity.Exercice;
import com.kfokam48.epreuve209.entity.Relecture;
import com.kfokam48.epreuve209.entity.SessionCours;
import com.kfokam48.epreuve209.exception.*;
import com.kfokam48.epreuve209.repository.ExerciceRepository;
import com.kfokam48.epreuve209.repository.RelectureRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RelectureServiceTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 9, 25, 10, 0);

    @Mock RelectureRepository relectures;
    @Mock ExerciceRepository exercices;

    private RelectureService service;
    private Relecture relecture;
    private Exercice exercice;
    private SessionCours session;

    @BeforeEach
    void setUp() {
        Clock horlogeFixe = Clock.fixed(MAINTENANT.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new RelectureService(relectures, exercices, horlogeFixe);

        session = new SessionCours();   // non clôturée
        exercice = new Exercice();
        org.springframework.test.util.ReflectionTestUtils.setField(exercice, "id", 42L);
        exercice.setSession(session);
        exercice.setStatut("EN_RELECTURE");

        relecture = new Relecture();
        relecture.setExercice(exercice);
        // Auteur id=101, relecteur id=102 : câblés via les entités ci-dessous.
        com.kfokam48.epreuve209.entity.Etudiant auteur = etudiant(101L);
        com.kfokam48.epreuve209.entity.Etudiant relecteur = etudiant(102L);
        org.springframework.test.util.ReflectionTestUtils.setField(exercice, "auteur", auteur);
        org.springframework.test.util.ReflectionTestUtils.setField(relecture, "relecteur", relecteur);

        // Le service recharge l'exercice comme instance managée avant d'écrire RELU.
        when(exercices.findById(42L)).thenReturn(Optional.of(exercice));
    }

    private static com.kfokam48.epreuve209.entity.Etudiant etudiant(long id) {
        com.kfokam48.epreuve209.entity.Etudiant e = new com.kfokam48.epreuve209.entity.Etudiant();
        org.springframework.test.util.ReflectionTestUtils.setField(e, "id", id);
        return e;
    }

    private NoterRelectureRequest requete(int note) {
        return new NoterRelectureRequest(note, "Bon travail, approfondis la partie 2.");
    }

    @Test
    void rendu_nominal_exercice_passe_a_RELU() {
        when(relectures.findById(7L)).thenReturn(Optional.of(relecture));

        RelectureResponse r = service.rendre(7L, 102L, requete(15));

        assertThat(r.note()).isEqualTo(15);
        assertThat(exercice.getStatut()).isEqualTo("RELU");
        assertThat(relecture.getRendueAt()).isNotNull();
        verify(exercices).save(exercice);
    }

    @Test
    void auto_relecture_403_RG4_meme_si_deja_rendue() {
        relecture.setRendueAt(MAINTENANT.minusMinutes(5));
        when(relectures.findById(7L)).thenReturn(Optional.of(relecture));

        // L'auteur (101) tente de noter : 403 AVANT le 409 (autorisation avant état).
        assertThatThrownBy(() -> service.rendre(7L, 101L, requete(15)))
                .isInstanceOf(AutoRelectureException.class);
    }

    @Test
    void double_POST_409_RELECTURE_DEJA_RENDUE() {
        relecture.setRendueAt(MAINTENANT.minusMinutes(5));
        when(relectures.findById(7L)).thenReturn(Optional.of(relecture));

        assertThatThrownBy(() -> service.rendre(7L, 102L, requete(15)))
                .isInstanceOf(RelectureDejaRendueException.class);
    }

    @Test
    void note_hors_bornes_400_RG8() {
        when(relectures.findById(7L)).thenReturn(Optional.of(relecture));

        assertThatThrownBy(() -> service.rendre(7L, 102L, requete(21)))
                .isInstanceOf(NoteInvalideException.class);
        assertThatThrownBy(() -> service.rendre(7L, 102L, requete(-1)))
                .isInstanceOf(NoteInvalideException.class);
    }

    @Test
    void bornes_0_et_20_incluses_RG8() {
        when(relectures.findById(7L)).thenReturn(Optional.of(relecture));

        assertThat(service.rendre(7L, 102L, requete(0)).note()).isZero();

        // Borne 20 : une deuxième relecture fraîche sur le même exercice
        // (on ignore RG5 ici, seul le passage des bornes est testé).
        Relecture r2 = new Relecture();
        r2.setExercice(exercice);
        org.springframework.test.util.ReflectionTestUtils.setField(r2, "relecteur", etudiant(103L));
        when(relectures.findById(8L)).thenReturn(Optional.of(r2));

        assertThat(service.rendre(8L, 103L, requete(20)).note()).isEqualTo(20);
    }

    @Test
    void correction_PATCH_autorisee_tant_que_session_non_cloturee_RG9() {
        relecture.setRendueAt(MAINTENANT.minusMinutes(5));
        when(relectures.findById(7L)).thenReturn(Optional.of(relecture));

        RelectureResponse r = service.corriger(7L, 102L, requete(18));

        assertThat(r.note()).isEqualTo(18);
        assertThat(relecture.getMajAt()).isNotNull();
    }

    @Test
    void correction_apres_cloture_409_SESSION_CLOTUREE_RG9() {
        relecture.setRendueAt(MAINTENANT.minusMinutes(5));
        session.setClotureeAt(MAINTENANT.minusSeconds(1));
        when(relectures.findById(7L)).thenReturn(Optional.of(relecture));

        assertThatThrownBy(() -> service.corriger(7L, 102L, requete(18)))
                .isInstanceOf(SessionClotureeException.class);
        assertThat(relecture.getNote()).isNull();   // dernière valeur valide conservée
    }

    @Test
    void POST_apres_cloture_409_SESSION_CLOTUREE() {
        when(relectures.findById(7L)).thenReturn(Optional.of(relecture));
        session.setClotureeAt(MAINTENANT.minusSeconds(1));

        assertThatThrownBy(() -> service.rendre(7L, 102L, requete(15)))
                .isInstanceOf(SessionClotureeException.class);
    }
}
