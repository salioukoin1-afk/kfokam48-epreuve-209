package com.kfokam48.epreuve209.repository;

import com.kfokam48.epreuve209.entity.Etudiant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {

    List<Etudiant> findByPromotionId(Long promotionId);
}
