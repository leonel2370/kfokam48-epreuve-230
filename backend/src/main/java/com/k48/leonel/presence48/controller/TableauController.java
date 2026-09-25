package com.k48.leonel.presence48.controller;

import com.k48.leonel.presence48.dto.response.LigneTableauReponse;
import com.k48.leonel.presence48.service.TableauService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TableauController {

  private final TableauService tableau;

  public TableauController(TableauService tableau) {
    this.tableau = tableau;
  }

  /** Opération imposée (EF10). */
  @GetMapping("/api/tableau")
  public List<LigneTableauReponse> tableau(@RequestParam Long promotionId) {
    return tableau.tableau(promotionId);
  }
}
