package com.k48.leonel.presence48.exception;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ENF3 / B4 : toute erreur renvoie {code, message}, jamais de stack trace ni de page Spring.
 * Exécuté connecté (v2) : on teste ici le handler d'erreurs, pas le contrôle d'accès (voir AuthIntegrationTest).
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.ControleurDeTest.class)
@Import(GlobalExceptionHandlerTest.ControleurDeTest.class)
@WithMockUser
class GlobalExceptionHandlerTest {

  @Autowired
  private MockMvc mvc;

  @Test
  void testMetierExceptionRenvoieSonStatutEtSonCode() throws Exception {
    erreur(mvc.perform(get("/test/metier")), 410, "CODE_EXPIRE");
  }

  @Test
  void testChampManquantRenvoie400() throws Exception {
    erreur(mvc.perform(post("/test/valide").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")),
        400, "CHAMP_MANQUANT");
  }

  @Test
  void testJsonMalFormeRenvoie400() throws Exception {
    erreur(mvc.perform(post("/test/valide").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{pas du json")),
        400, "CHAMP_MANQUANT");
  }

  @Test
  void testParametreEtEnteteManquantsRenvoient400() throws Exception {
    erreur(mvc.perform(get("/test/parametre")), 400, "CHAMP_MANQUANT");
    erreur(mvc.perform(get("/test/entete")), 400, "CHAMP_MANQUANT");
    erreur(mvc.perform(get("/test/parametre").param("id", "abc")), 400, "CHAMP_MANQUANT");
  }

  @Test
  void testAdresseInconnueRenvoie404() throws Exception {
    erreur(mvc.perform(get("/api/nexiste-pas")), 404, "RESSOURCE_INTROUVABLE");
  }

  @Test
  void testMethodeNonAutoriseeRenvoie405() throws Exception {
    erreur(mvc.perform(delete("/test/metier").with(csrf())), 405, "METHODE_NON_AUTORISEE");
  }

  @Test
  void testCorpsNonJsonRenvoie415() throws Exception {
    erreur(mvc.perform(post("/test/valide").with(csrf()).contentType(MediaType.TEXT_PLAIN).content("x")),
        415, "FORMAT_NON_SUPPORTE");
  }

  @Test
  void testContrainteUniqueRenvoie409() throws Exception {
    erreur(mvc.perform(get("/test/integrite")), 409, "CONFLIT");
  }

  @Test
  void testErreurInattendueRenvoie500SansStackTrace() throws Exception {
    erreur(mvc.perform(get("/test/panne")), 500, "ERREUR_INTERNE")
        .andExpect(jsonPath("$.message").value("Une erreur inattendue est survenue."))
        .andExpect(jsonPath("$.trace").doesNotExist());
  }

  private static ResultActions erreur(ResultActions r, int statut, String code) throws Exception {
    return r.andExpect(status().is(statut))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code").value(code))
        .andExpect(jsonPath("$.message").isNotEmpty())
        .andExpect(jsonPath("$.length()").value(2));
  }

  record Corps(@NotBlank String titre) {
  }

  @RestController
  static class ControleurDeTest {

    @GetMapping("/test/metier")
    String metier() {
      throw new MetierException(HttpStatus.GONE, "CODE_EXPIRE", "Le code de présence a expiré.");
    }

    @PostMapping(value = "/test/valide", consumes = MediaType.APPLICATION_JSON_VALUE)
    String valide(@Valid @RequestBody Corps corps) {
      return corps.titre();
    }

    @GetMapping("/test/parametre")
    long parametre(@RequestParam long id) {
      return id;
    }

    @GetMapping("/test/entete")
    String entete(@RequestHeader("X-Etudiant-Id") String id) {
      return id;
    }

    @GetMapping("/test/integrite")
    String integrite() {
      throw new DataIntegrityViolationException("uk_presence_session_etudiant");
    }

    @GetMapping("/test/panne")
    String panne() {
      throw new IllegalStateException("détail interne qui ne doit pas fuiter");
    }
  }
}
