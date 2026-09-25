package com.kfokam48.epreuve209.repository;

import com.kfokam48.epreuve209.entity.SessionCours;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SessionCoursRepository extends JpaRepository<SessionCours, Long> {

    Optional<SessionCours> findByCodeIgnoreCase(String code);
}
