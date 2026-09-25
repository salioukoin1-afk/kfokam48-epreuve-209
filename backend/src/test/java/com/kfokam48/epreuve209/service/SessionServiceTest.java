package com.kfokam48.epreuve209.service;

import com.kfokam48.epreuve209.dto.OuvrirSessionRequest;
import com.kfokam48.epreuve209.dto.SessionResponse;
import com.kfokam48.epreuve209.entity.Promotion;
import com.kfokam48.epreuve209.exception.PromotionInconnueException;
import com.kfokam48.epreuve209.repository.PromotionRepository;
import com.kfokam48.epreuve209.repository.SessionCoursRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * B6 — test unitaire sur une règle métier réelle : RG1 (expiration du code).
 * La durée attendue est celle de l'environnement de test (5 min, application-test.yml) :
 * si quelqu'un code la durée en dur à 15, ce test échoue — c'est le but (US-09).
 */
@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 9, 25, 10, 0);

    @Mock SessionCoursRepository sessions;
    @Mock PromotionRepository promotions;

    private SessionService service;

    @BeforeEach
    void setUp() {
        Clock horlogeFixe = Clock.fixed(MAINTENANT.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new SessionService(sessions, promotions, horlogeFixe);
        ReflectionTestUtils.setField(service, "expirationMinutes", 5);
    }

    @Test
    void expirationAt_egal_ouvertureAt_plus_duree_configuree_RG1() {
        when(promotions.findById(1L)).thenReturn(Optional.of(new Promotion()));
        when(sessions.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        SessionResponse s = service.ouvrirSession(new OuvrirSessionRequest("Algèbre", 1L));

        assertThat(Duration.between(s.ouvertureAt(), s.expirationAt()))
                .as("RG1 : la durée vient de la configuration (5 min en test), pas d'une constante")
                .isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void promotion_inconnue_est_rejetee_400_PROMOTION_INCONNUE() {
        when(promotions.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ouvrirSession(new OuvrirSessionRequest("Test", 99L)))
                .isInstanceOf(PromotionInconnueException.class)
                .hasMessageContaining("99");
    }

    @Test
    void le_code_genere_est_sans_ambiguite() {
        when(promotions.findById(1L)).thenReturn(Optional.of(new Promotion()));
        when(sessions.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        for (int i = 0; i < 50; i++) {
            SessionResponse s = service.ouvrirSession(new OuvrirSessionRequest("Session " + i, 1L));
            assertThat(s.code()).hasSize(6).doesNotContain("0", "O", "1", "I");
        }
    }
}
