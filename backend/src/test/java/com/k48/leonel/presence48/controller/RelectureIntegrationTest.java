package com.k48.leonel.presence48.controller;

import static com.k48.leonel.presence48.support.Connexion.MDP_ETUDIANT;
import static com.k48.leonel.presence48.support.Connexion.connecter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** EF8, EF9 / SF-8, SF-9 : liste du relecteur (protégée) et envoi définitif de la note (imposé). */
@SpringBootTest
@AutoConfigureMockMvc
class RelectureIntegrationTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private JdbcTemplate jdbc;

  private long etudiant(String nom) {
    Long id = jdbc.queryForObject("SELECT id FROM etudiant WHERE nom = ?", Long.class, nom);
    return id == null ? -1 : id;
  }

  /** Session P1 où Paul est seul présent et Hugo dépose : renvoie l'id de la relecture assignée à Paul. */
  private long relectureAssigneeAPaul() throws Exception {
    var promotionId = jdbc.queryForObject("SELECT id FROM promotion WHERE nom = 'P1-2026'", Long.class);
    var session = mvc.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON)
            .content("{\"titre\":\"TP relecture\",\"promotionId\":" + promotionId + "}"))
        .andReturn().getResponse().getContentAsString();
    mvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"" + JsonPath.read(session, "$.code") + "\",\"etudiantId\":" + etudiant("Paul Mbarga")
                + "}"))
        .andExpect(status().isCreated());
    var depot = mvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON)
            .content("{\"sessionId\":" + JsonPath.read(session, "$.id") + ",\"etudiantId\":" + etudiant("Hugo Talla")
                + ",\"lien\":\"https://github.com/hugo/tp\"}"))
        .andExpect(jsonPath("$.statut").value("EN_ATTENTE_RELECTURE"))
        .andReturn().getResponse().getContentAsString();
    Long id = jdbc.queryForObject("SELECT id FROM relecture WHERE exercice_id = ?", Long.class,
        ((Number) JsonPath.read(depot, "$.id")).longValue());
    return id == null ? -1 : id;
  }

  private ResultActions rendre(long relectureId, long appelant, String corps) throws Exception {
    return mvc.perform(post("/api/relectures/" + relectureId).header("X-Etudiant-Id", appelant)
        .contentType(MediaType.APPLICATION_JSON).content(corps));
  }

  private static void erreur(ResultActions r, int statut, String code) throws Exception {
    r.andExpect(status().is(statut)).andExpect(jsonPath("$.code").value(code));
  }

  @Test
  void testRg10Rg31RelectureRendueUneSeuleFoisNoteProvisoireTantQuUnSeulPairARendu() throws Exception {
    var relecture = relectureAssigneeAPaul();
    var paul = etudiant("Paul Mbarga");
    rendre(relecture, paul, "{\"note\":15,\"commentaire\":\"Clair et testé\"}").andExpect(status().isOk());
    assertThat(jdbc.queryForObject("SELECT e.statut FROM exercice e JOIN relecture r ON r.exercice_id = e.id "
        + "WHERE r.id = ?", String.class, relecture)).as("un seul des deux pairs a rendu (RG31)")
        .isEqualTo("EN_ATTENTE_RELECTURE");
    erreur(rendre(relecture, paul, "{\"note\":10,\"commentaire\":\"Autre avis\"}"), 409, "RELECTURE_DEJA_RENDUE");
  }

  @Test
  void testRg9Rg5EtAssignationControlees() throws Exception {
    var relecture = relectureAssigneeAPaul();
    var paul = etudiant("Paul Mbarga");
    erreur(rendre(relecture, paul, "{\"note\":21,\"commentaire\":\"x\"}"), 400, "NOTE_INVALIDE");
    erreur(rendre(relecture, paul, "{\"note\":12.5,\"commentaire\":\"x\"}"), 400, "NOTE_INVALIDE");
    erreur(rendre(relecture, paul, "{\"commentaire\":\"x\"}"), 400, "CHAMP_MANQUANT");
    erreur(rendre(relecture, etudiant("Hugo Talla"), "{\"note\":20,\"commentaire\":\"x\"}"), 403, "AUTO_RELECTURE");
    erreur(rendre(relecture, etudiant("Marc Tchoua"), "{\"note\":8,\"commentaire\":\"x\"}"), 403,
        "RELECTEUR_NON_ASSIGNE");
    erreur(rendre(999_999, paul, "{\"note\":8,\"commentaire\":\"x\"}"), 404, "RELECTURE_INTROUVABLE");
    erreur(mvc.perform(post("/api/relectures/" + relecture).contentType(MediaType.APPLICATION_JSON)
        .content("{\"note\":8,\"commentaire\":\"x\"}")), 400, "CHAMP_MANQUANT");
  }

  @Test
  void testRg18RelectureDUneSessionClotureeRenvoie409() throws Exception {
    var enAttente = jdbc.queryForList("""
        SELECT r.id, r.relecteur_id FROM relecture r JOIN exercice e ON e.id = r.exercice_id
        JOIN session s ON s.id = e.session_id WHERE s.code = 'DEMO01' AND r.rendue_at IS NULL""").getFirst();
    erreur(rendre(((Number) enAttente.get("id")).longValue(), ((Number) enAttente.get("relecteur_id")).longValue(),
        "{\"note\":12,\"commentaire\":\"x\"}"), 409, "SESSION_CLOTUREE");
  }

  @Test
  void testSf8ListeProtegeeSansAuteurEtFiltree() throws Exception {
    var relecture = relectureAssigneeAPaul();
    var paul = etudiant("Paul Mbarga");
    mvc.perform(get("/api/etudiants/" + paul + "/relectures")).andExpect(status().isUnauthorized());
    var session = connecter(mvc, "paul", MDP_ETUDIANT);
    mvc.perform(get("/api/etudiants/" + paul + "/relectures").param("statut", "A_FAIRE").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(relecture))
        .andExpect(jsonPath("$[0].sessionTitre").value("TP relecture"))
        .andExpect(jsonPath("$[0].lien").value("https://github.com/hugo/tp"))
        .andExpect(jsonPath("$[0].rendue").value(false))
        .andExpect(jsonPath("$[0].auteurId").doesNotExist());
    mvc.perform(get("/api/etudiants/" + paul + "/relectures").param("statut", "RENDUE").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].rendue").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(true))));
    erreur(mvc.perform(get("/api/etudiants/" + paul + "/relectures").session(connecter(mvc, "awa", MDP_ETUDIANT))),
        403, "IDENTITE_DIFFERENTE");
    erreur(mvc.perform(get("/api/etudiants/999999/relectures").session(session)), 404, "ETUDIANT_INCONNU");
  }
}
