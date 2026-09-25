package com.k48.leonel.presence48.controller;

import com.k48.leonel.presence48.dto.request.OuvertureSessionRequete;
import com.k48.leonel.presence48.dto.response.SessionOuverteReponse;
import com.k48.leonel.presence48.dto.response.SessionReponse;
import com.k48.leonel.presence48.service.SessionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

  private final SessionService sessions;

  public SessionController(SessionService sessions) {
    this.sessions = sessions;
  }

  /** Opération imposée (EF2) : publique sans session, contrôlée si l'appelant est connecté. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SessionOuverteReponse ouvrir(@Valid @RequestBody OuvertureSessionRequete corps) {
    return sessions.ouvrir(corps.titre(), corps.promotionId());
  }

  @GetMapping
  public List<SessionReponse> lister(@RequestParam Long promotionId) {
    return sessions.lister(promotionId);
  }
}
