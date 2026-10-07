package com.k48.leonel.presence48.controller;

import static com.k48.leonel.presence48.support.Connexion.connecterAdmin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * #149 / décisions du PO du 07/10 (#140) : RG32 (une fiche avec historique ne change pas de promotion),
 * RG33 (une fiche désactivée n'est plus tirée au sort, ses relectures assignées restent rendables),
 * RG34 (désactiver une fiche désactive son compte, pas l'inverse).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReglesFichesIntegrationTest {

  private static final String P1 = "P1-2026";
  private static final String P2 = "P2-2026";
  private static final String AWA = "Awa Ndiaye";
  private static final String PAUL = "Paul Mbarga";
  private static final String LINA = "Lina Kamga";
  private static final String HUGO = "Hugo Talla";
  private static final String NORA = "Nora Bella";

  @Autowired
  private MockMvc mvc;

  @Autowired
  private JdbcTemplate jdbc;

  private long idPromotion(String nom) {
    return jdbc.queryForObject("SELECT id FROM promotion WHERE nom = ?", Long.class, nom);
  }

  private long idEtudiant(String nom) {
    return jdbc.queryForObject("SELECT id FROM etudiant WHERE nom = ?", Long.class, nom);
  }

  private boolean ficheActive(long etudiantId) {
    return jdbc.queryForObject("SELECT actif FROM etudiant WHERE id = ?", Boolean.class, etudiantId);
  }

  private boolean compteActif(String login) {
    return jdbc.queryForObject("SELECT actif FROM utilisateur WHERE login = ?", Boolean.class, login);
  }

  private ResultActions modifier(MockHttpSession admin, long etudiantId, String nom, String promotion)
      throws Exception {
    return mvc.perform(put("/api/etudiants/" + etudiantId).session(admin).with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"nom\":\"" + nom + "\",\"promotionId\":" + idPromotion(promotion) + "}"));
  }

  private void desactiverFiche(MockHttpSession admin, long etudiantId) throws Exception {
    mvc.perform(delete("/api/etudiants/" + etudiantId).session(admin).with(csrf()))
        .andExpect(status().isNoContent());
  }

  /**
   * Ouvre une session P1 sans session HTTP (opération imposée) et renvoie [id, code]. Les appels publics
   * portent csrf() : le post-processeur de test l'exige dès qu'il a servi une fois (JOURNAL, #69).
   */
  private Object[] ouvrirSession() throws Exception {
    var reponse = mvc.perform(post("/api/sessions").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"titre\":\"TP regles\",\"promotionId\":" + idPromotion(P1) + "}"))
        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    return new Object[] {((Number) JsonPath.read(reponse, "$.id")).longValue(), JsonPath.read(reponse, "$.code")};
  }

  private void marquerPresent(Object code, long etudiantId) throws Exception {
    mvc.perform(post("/api/presences").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"" + code + "\",\"etudiantId\":" + etudiantId + "}"))
        .andExpect(status().isCreated());
  }

  private long deposer(Object sessionId, long etudiantId) throws Exception {
    var reponse = mvc.perform(post("/api/exercices").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"sessionId\":" + sessionId + ",\"etudiantId\":" + etudiantId
                + ",\"lien\":\"https://github.com/exemple/tp\"}"))
        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    return ((Number) JsonPath.read(reponse, "$.id")).longValue();
  }

  private List<Long> relecteursDe(long exerciceId) {
    return jdbc.queryForList("SELECT relecteur_id FROM relecture WHERE exercice_id = ? ORDER BY id", Long.class,
        exerciceId);
  }

  // --- RG32 ---

  @Test
  void testRg32UneFicheAvecHistoriqueNeChangePasDePromotion() throws Exception {
    long awa = idEtudiant(AWA);

    modifier(connecterAdmin(mvc), awa, AWA, P2)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("DEPLACEMENT_IMPOSSIBLE"));

    assertThat(jdbc.queryForObject("SELECT promotion_id FROM etudiant WHERE id = ?", Long.class, awa))
        .as("Awa, qui a des présences et des notes, reste en P1").isEqualTo(idPromotion(P1));
  }

  @Test
  void testRg32LeNomDUneFicheAvecHistoriqueResteModifiable() throws Exception {
    modifier(connecterAdmin(mvc), idEtudiant(AWA), "Awa N.", P1)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nom").value("Awa N."));
  }

  @Test
  void testRg32UneFicheSansHistoriqueChangeDePromotion() throws Exception {
    modifier(connecterAdmin(mvc), idEtudiant(NORA), NORA, P1)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.promotionId").value(idPromotion(P1)));
  }

  // --- RG33 ---

  @Test
  void testRg33UneFicheDesactiveeNEstPasTireeAuSort() throws Exception {
    var session = ouvrirSession();
    long paul = idEtudiant(PAUL);
    long lina = idEtudiant(LINA);
    marquerPresent(session[1], paul);
    marquerPresent(session[1], lina);
    desactiverFiche(connecterAdmin(mvc), lina);

    long exercice = deposer(session[0], idEtudiant(HUGO));

    assertThat(relecteursDe(exercice))
        .as("Lina, présente mais désactivée, n'est pas candidate : seul Paul relit").containsExactly(paul);
  }

  @Test
  void testRg33UneRelectureDejaAssigneeResteRendable() throws Exception {
    var session = ouvrirSession();
    long paul = idEtudiant(PAUL);
    marquerPresent(session[1], paul);
    long exercice = deposer(session[0], idEtudiant(HUGO));
    long relecture = jdbc.queryForObject("SELECT id FROM relecture WHERE exercice_id = ?", Long.class, exercice);
    desactiverFiche(connecterAdmin(mvc), paul);

    mvc.perform(post("/api/relectures/" + relecture).with(csrf()).header("X-Etudiant-Id", paul)
            .contentType(MediaType.APPLICATION_JSON).content("{\"note\":13,\"commentaire\":\"Correct\"}"))
        .andExpect(status().isOk());
  }

  // --- RG34 ---

  @Test
  void testRg34DesactiverUneFicheDesactiveSonCompte() throws Exception {
    long awa = idEtudiant(AWA);

    desactiverFiche(connecterAdmin(mvc), awa);

    assertThat(ficheActive(awa)).as("la fiche d'Awa est désactivée").isFalse();
    assertThat(compteActif("awa")).as("le compte lié à la fiche est désactivé avec elle").isFalse();
  }

  @Test
  void testRg34UneFicheLieeAUnCompteSansAutreTraceEstDesactivee() throws Exception {
    var admin = connecterAdmin(mvc);
    long nora = idEtudiant(NORA);
    mvc.perform(post("/api/utilisateurs").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"login\":\"nora\",\"nomAffiche\":\"Nora Bella\",\"role\":\"ETUDIANT\","
                + "\"motDePasseInitial\":\"Provisoire48\",\"etudiantId\":" + nora + "}"))
        .andExpect(status().isCreated());

    desactiverFiche(admin, nora);

    assertThat(ficheActive(nora)).as("un compte lié est une trace : la fiche est désactivée, pas supprimée").isFalse();
    assertThat(compteActif("nora")).as("son compte est désactivé avec elle").isFalse();
  }

  @Test
  void testRg34DesactiverSeulementLeCompteLaisseLaFiche() throws Exception {
    long idCompte = jdbc.queryForObject("SELECT id FROM utilisateur WHERE login = 'paul'", Long.class);

    mvc.perform(delete("/api/utilisateurs/" + idCompte).session(connecterAdmin(mvc)).with(csrf()))
        .andExpect(status().isNoContent());

    assertThat(ficheActive(idEtudiant(PAUL))).as("la fiche et son historique ne sont pas touchés").isTrue();
  }
}
