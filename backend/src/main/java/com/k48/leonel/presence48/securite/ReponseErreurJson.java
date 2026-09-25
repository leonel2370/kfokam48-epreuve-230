package com.k48.leonel.presence48.securite;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;

/** Écrit {code, message} depuis les filtres de sécurité, avant que Spring MVC n'intervienne (ENF3). */
final class ReponseErreurJson {

  private ReponseErreurJson() {
  }

  static void ecrire(HttpServletResponse reponse, int statut, String code, String message) throws IOException {
    reponse.setStatus(statut);
    reponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
    reponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
    reponse.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message.replace("\"", "\\\"") + "\"}");
  }
}
