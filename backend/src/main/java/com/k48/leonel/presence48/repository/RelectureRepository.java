package com.k48.leonel.presence48.repository;

import com.k48.leonel.presence48.entity.Relecture;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RelectureRepository extends JpaRepository<Relecture, Long> {

  Optional<Relecture> findByExerciceId(Long exerciceId);
}
