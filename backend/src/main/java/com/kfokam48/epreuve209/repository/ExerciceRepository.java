package com.kfokam48.epreuve209.repository;

import com.kfokam48.epreuve209.entity.Exercice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ExerciceRepository extends JpaRepository<Exercice, Long> {

    Optional<Exercice> findBySessionIdAndAuteurId(Long sessionId, Long auteurId);

    long countByAuteurId(Long auteurId);
}
