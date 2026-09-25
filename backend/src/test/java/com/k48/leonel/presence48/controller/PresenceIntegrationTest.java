package com.k48.leonel.presence48.controller;

import static com.k48.leonel.presence48.support.Connexion.MDP_ETUDIANT;
import static com.k48.leonel.presence48.support.Connexion.connecter;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * EF3 / SF-3 / B6 : POST /api/presences, un étudiant de démonstration distinct par test (base H2 partagée).
 * Contexte recréé à chaque test : le post-processeur csrf() remplace le dépôt CSRF du filtre partagé.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PresenceIntegrationTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private JdbcTemplate jdbc;

  private long etudiant(String nom) {
    Long id = jdbc.queryForObject("SELECT id FROM etudiant WHERE nom = ?", Long.class, nom);
    return id == null ? -1 : id;
  }

  private String ouvrirSession(String promotion) throws Exception {
    var promotionId = jdbc.queryForObject("SELECT id FROM promotion WHERE nom = ?", Long.class, promotion);
    var reponse = mvc.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON)
            .content("{\"titre\":\"TP présence\",\"promotionId\":" + promotionId + "}"))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return JsonPath.read(reponse, "$.code");
  }

  private ResultActions marquer(String code, long etudiantId) throws Exception {
    return mvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON)
        .content("{\"code\":\"" + code + "\",\"etudiantId\":" + etudiantId + "}"));
  }

  private static void erreur(ResultActions r, int statut, String code) throws Exception {
    r.andExpect(status().is(statut)).andExpect(jsonPath("$.code").value(code));
  }

  @Test
  void testRg21CodeValideEnregistrePresenceImmediatementPuisRg3DejaPresent() throws Exception {
    var code = ouvrirSession("P1-2026");
    var hugo = etudiant("Hugo Talla");
    marquer(" " + code.toLowerCase() + " ", hugo)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.sessionId").isNumber())
        .andExpect(jsonPath("$.etudiantId").value(hugo))
        .andExpect(jsonPath("$.source").value("ETUDIANT"))
        .andExpect(jsonPath("$.length()").value(4));
    erreur(marquer(code, hugo), 409, "DEJA_PRESENT");
  }

  @Test
  void testRg1CodeExpireRenvoie410() throws Exception {
    jdbc.update("""
        INSERT INTO session (titre, promotion_id, code, ouverture_at, expiration_at, statut)
        SELECT 'TP expiré', id, 'EXPIR2', TIMESTAMP WITH TIME ZONE '2026-01-01 09:00:00+00',
               TIMESTAMP WITH TIME ZONE '2026-01-01 09:15:00+00', 'OUVERTE' FROM promotion WHERE nom = 'P1-2026'
        """);
    erreur(marquer("EXPIR2", etudiant("Marc Tchoua")), 410, "CODE_EXPIRE");
  }

  @Test
  void testRg18SessionClotureeRenvoie409() throws Exception {
    erreur(marquer("DEMO01", etudiant("Hugo Talla")), 409, "SESSION_CLOTUREE");
  }

  @Test
  void testRg19EtudiantDUneAutrePromotionRenvoie400() throws Exception {
    erreur(marquer(ouvrirSession("P1-2026"), etudiant("Nora Bella")), 400, "ETUDIANT_HORS_PROMOTION");
  }

  @Test
  void testCodeInconnuEtudiantInconnuEtChampManquantRenvoient400() throws Exception {
    erreur(marquer("NEXISTE", etudiant("Lea Owona")), 400, "CODE_INCONNU");
    erreur(marquer("DEMO01", 999_999), 400, "ETUDIANT_INCONNU");
    erreur(mvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"X\"}")),
        400, "CHAMP_MANQUANT");
  }

  @Test
  void testRg4CinqCodesInconnusBloquentMemeUnCodeValide() throws Exception {
    var code = ouvrirSession("P2-2026");
    var theo = etudiant("Theo Abena");
    for (var i = 0; i < 5; i++) {
      erreur(marquer("NEXISTE", theo), 400, "CODE_INCONNU");
    }
    erreur(marquer(code, theo), 429, "TROP_DE_TENTATIVES");
  }

  @Test
  void testConnecteAvecLIdentiteDUnAutreEtudiantRenvoie403() throws Exception {
    var code = ouvrirSession("P1-2026");
    erreur(mvc.perform(post("/api/presences").session(connecter(mvc, "awa", MDP_ETUDIANT)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"" + code + "\",\"etudiantId\":" + etudiant("Ines Ngono") + "}")),
        403, "IDENTITE_DIFFERENTE");
  }
}
