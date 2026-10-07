package com.k48.leonel.presence48.controller;

import static com.k48.leonel.presence48.support.Connexion.MDP_FORMATEUR;
import static com.k48.leonel.presence48.support.Connexion.connecterAvecCsrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * #61 / SF-21, SF-22 / EF22, EF23 : gestion du référentiel depuis l'application.
 * Promotions : ADMIN (RG25), nom unique (409 CONFLIT), suppression refusée si étudiants ou sessions
 * (RG28, 409 SUPPRESSION_IMPOSSIBLE). Rattachement des formateurs (RG26) : seuls des comptes
 * FORMATEUR sont admis. Fiches étudiants : ADMIN et FORMATEUR de la promotion (RG26) ;
 * suppression d'une fiche sans historique, désactivation sinon (RG28) — l'historique reste au tableau.
 * L'unicité des noms est portée par la contrainte V1 UNIQUE(nom) → 409 CONFLIT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReferentielAdminIntegrationTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private JdbcTemplate jdbc;

  private MockHttpSession sessionAdmin;

  /** admin/admin doit changer son mot de passe (RG23) avant d'agir — même logique que #60. */
  private MockHttpSession admin() throws Exception {
    if (sessionAdmin != null) {
      return sessionAdmin;
    }
    var session = connecterAvecCsrf(mvc, "admin", "admin");
    mvc.perform(put("/api/moi/mot-de-passe").session(session).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"ancien\":\"admin\",\"nouveau\":\"Admin48!x\"}"))
        .andExpect(status().isNoContent());
    sessionAdmin = session;
    return session;
  }

  private MockHttpSession formateur() throws Exception {
    return connecterAvecCsrf(mvc, "formateur", MDP_FORMATEUR);
  }

  private long idUtilisateur(String login) {
    return jdbc.queryForObject("SELECT id FROM utilisateur WHERE login = ?", Long.class, login);
  }

  private long idPromotion(String nom) {
    return jdbc.queryForObject("SELECT id FROM promotion WHERE nom = ?", Long.class, nom);
  }

  private ResultActions creerPromotion(MockHttpSession session, String nom) throws Exception {
    return mvc.perform(post("/api/promotions").session(session).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content("{\"nom\":\"" + nom + "\"}"));
  }

  private static void erreur(ResultActions r, int statut, String code) throws Exception {
    r.andExpect(status().is(statut)).andExpect(jsonPath("$.code").value(code));
  }

  @Test
  void testAdminCreeRenommeEtSupprimeUnePromotionVide() throws Exception {
    var s = admin();
    var cree = creerPromotion(s, "P3-2026")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.nom").value("P3-2026"))
        .andReturn().getResponse().getContentAsString();
    long id = ((Number) com.jayway.jsonpath.JsonPath.read(cree, "$.id")).longValue();
    // la liste publique (EF1) la montre
    mvc.perform(get("/api/promotions")).andExpect(jsonPath("$[?(@.nom=='P3-2026')]").exists());
    mvc.perform(put("/api/promotions/" + id).session(s).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{\"nom\":\"P3-2027\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nom").value("P3-2027"));
    mvc.perform(delete("/api/promotions/" + id).session(s).with(csrf()))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/promotions")).andExpect(jsonPath("$[?(@.nom=='P3-2027')]").doesNotExist());
  }

  @Test
  void testNomDejaPrisRenvoie409() throws Exception {
    var s = admin();
    erreur(creerPromotion(s, "P1-2026"), 409, "CONFLIT");
    long p2 = idPromotion("P2-2026");
    erreur(mvc.perform(put("/api/promotions/" + p2).session(s).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{\"nom\":\"P1-2026\"}")), 409, "CONFLIT");
  }

  @Test
  void testSuppressionRefuseeSiEtudiantsOuSessions() throws Exception {
    var s = admin();
    long p1 = idPromotion("P1-2026"); // a des étudiants et une session (V2)
    erreur(mvc.perform(delete("/api/promotions/" + p1).session(s).with(csrf())), 409,
        "SUPPRESSION_IMPOSSIBLE");
  }

  @Test
  void testRattacherUnFormateurLuiOuvreLaPromotion() throws Exception {
    var s = admin();
    long formateur = idUtilisateur("formateur");
    long p2 = idPromotion("P2-2026");
    // avant rattachement : le formateur est hors promotion (RG26) sur les sessions de P2
    erreur(mvc.perform(get("/api/sessions?promotionId=" + p2).session(formateur())), 403, "ACCES_REFUSE");
    mvc.perform(put("/api/promotions/" + p2 + "/formateurs").session(s).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"utilisateurIds\":[" + formateur + "]}"))
        .andExpect(status().isNoContent());
    // après : il voit les sessions de P2
    mvc.perform(get("/api/sessions?promotionId=" + p2).session(formateur()))
        .andExpect(status().isOk());
    // seuls des comptes FORMATEUR sont admis
    erreur(mvc.perform(put("/api/promotions/" + p2 + "/formateurs").session(s).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"utilisateurIds\":[" + idUtilisateur("awa") + "]}")), 400, "ROLE_INCOMPATIBLE");
  }

  @Test
  void testAdminEtFormateurDeLaPromotionCreeUneFicheEtudiant() throws Exception {
    var s = admin();
    mvc.perform(post("/api/etudiants").session(s).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nom\":\"Clara Ndongo\",\"promotionId\":" + idPromotion("P2-2026") + "}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.nom").value("Clara Ndongo"));
    // le formateur de P1 crée dans SA promotion
    mvc.perform(post("/api/etudiants").session(formateur()).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nom\":\"Boris Nkoulou\",\"promotionId\":" + idPromotion("P1-2026") + "}"))
        .andExpect(status().isCreated());
    // mais pas dans celle d'un autre (RG26)
    erreur(mvc.perform(post("/api/etudiants").session(formateur()).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nom\":\"Hors Promo\",\"promotionId\":" + idPromotion("P2-2026") + "}")),
        403, "ACCES_REFUSE");
  }

  @Test
  void testModifierUneFicheEtudiant() throws Exception {
    var s = admin();
    long sara = jdbc.queryForObject("SELECT id FROM etudiant WHERE nom = 'Sara Ebode'", Long.class);
    mvc.perform(put("/api/etudiants/" + sara).session(s).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            // Sara a un historique : elle garde sa promotion (RG32, #149) ; seul son nom change.
            .content("{\"nom\":\"Sara Ebode-Meli\",\"promotionId\":" + idPromotion("P1-2026") + "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nom").value("Sara Ebode-Meli"));
    erreur(mvc.perform(put("/api/etudiants/999999").session(s).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nom\":\"X\",\"promotionId\":" + idPromotion("P1-2026") + "}")), 404,
        "ETUDIANT_INCONNU");
  }

  @Test
  void testRg28SupprimerUneFicheSansHistoriqueDesactiverSinon() throws Exception {
    var s = admin();
    long p1 = idPromotion("P1-2026");
    // fiche neuve sans historique : suppression physique
    var boris = com.jayway.jsonpath.JsonPath.read(
        mvc.perform(post("/api/etudiants").session(s).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Boris Nkoulou\",\"promotionId\":" + p1 + "}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
    mvc.perform(delete("/api/etudiants/" + boris).session(s).with(csrf()))
        .andExpect(status().isNoContent());
    org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
        "SELECT COUNT(*) FROM etudiant WHERE id = ?", Integer.class, boris)).isZero();
    // fiche avec historique (Awa : présence, exercice, note) : désactivation RG28, historique intact
    long awa = jdbc.queryForObject("SELECT id FROM etudiant WHERE nom = 'Awa Ndiaye'", Long.class);
    mvc.perform(delete("/api/etudiants/" + awa).session(s).with(csrf()))
        .andExpect(status().isNoContent());
    org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
        "SELECT actif FROM etudiant WHERE id = ?", Boolean.class, awa)).isFalse();
    // sa ligne reste au tableau (presences > 0)
    mvc.perform(get("/api/tableau?promotionId=" + p1))
        .andExpect(jsonPath("$[?(@.nom=='Awa Ndiaye')].presences").value(1));
  }

  @Test
  void testAccesRefuseAuxNonAdminsSurLesPromotions() throws Exception {
    erreur(creerPromotion(formateur(), "P9-2026"), 403, "ACCES_REFUSE");
    erreur(creerPromotion(connecterAvecCsrf(mvc, "awa", "Etudiant48"), "P9-2026"), 403, "ACCES_REFUSE");
    erreur(mvc.perform(delete("/api/promotions/" + idPromotion("P2-2026")).session(formateur()).with(csrf())),
        403, "ACCES_REFUSE");
  }

  @Test
  void testSansSessionRenvoie401SurLesEcritures() throws Exception {
    erreur(mvc.perform(post("/api/promotions").with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content("{\"nom\":\"P9-2026\"}")), 401, "NON_AUTHENTIFIE");
    erreur(mvc.perform(post("/api/etudiants").with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content("{\"nom\":\"X\",\"promotionId\":1}")),
        401, "NON_AUTHENTIFIE");
  }
}
