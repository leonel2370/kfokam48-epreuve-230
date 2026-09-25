package com.k48.leonel.presence48.repository;

import com.k48.leonel.presence48.entity.Promotion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

  List<Promotion> findAllByOrderByNomAsc();
}
