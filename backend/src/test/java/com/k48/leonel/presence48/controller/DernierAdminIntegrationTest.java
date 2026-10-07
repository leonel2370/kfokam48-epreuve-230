package com.k48.leonel.presence48.controller;

import static com.k48.leonel.presence48.support.Connexion.connecterAdmin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
 * #130 / RG28 : il reste toujours un administrateur actif. La protection existait sur la
 * désactivation (DELETE) mais pas sur la modification d'un compte (PUT), qui change le rôle et l'état.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DernierAdminIntegrationTest {

  private static final String SUPPRESSION_IMPOSSIBLE = "SUPPRESSION_IMPOSSIBLE";

  @Autowired
  private MockMvc mvc;

  @Autowired
  private JdbcTemplate jdbc;

  private long idCompte(String login) {
    return jdbc.queryForObject("SELECT id FROM utilisateur WHERE login = ?", Long.class, login);
  }

  private int adminsActifs() {
    return jdbc.queryForObject("SELECT COUNT(*) FROM utilisateur WHERE role = 'ADMIN' AND actif", Integer.class);
  }

  private ResultActions modifier(MockHttpSession session, long id, String role, boolean actif) throws Exception {
    return mvc.perform(put("/api/utilisateurs/" + id).session(session).with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"nomAffiche\":\"Compte modifie\",\"role\":\"" + role + "\",\"actif\":" + actif + "}"));
  }

  private long creerSecondAdmin(MockHttpSession session) throws Exception {
    mvc.perform(post("/api/utilisateurs").session(session).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"login\":\"admin2\",\"nomAffiche\":\"Second admin\",\"role\":\"ADMIN\","
                + "\"motDePasseInitial\":\"Provisoire48\"}"))
        .andExpect(status().isCreated());
    return idCompte("admin2");
  }

  @Test
  void testLeDernierAdminActifNePeutPasChangerDeRoleParModification() throws Exception {
    var admin = connecterAdmin(mvc);

    modifier(admin, idCompte("admin"), "FORMATEUR", true)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(SUPPRESSION_IMPOSSIBLE));

    assertThat(adminsActifs()).as("il reste un administrateur actif").isEqualTo(1);
  }

  @Test
  void testLeDernierAdminActifNePeutPasEtreDesactiveParModification() throws Exception {
    var admin = connecterAdmin(mvc);

    modifier(admin, idCompte("admin"), "ADMIN", false)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(SUPPRESSION_IMPOSSIBLE));

    assertThat(adminsActifs()).as("il reste un administrateur actif").isEqualTo(1);
  }

  @Test
  void testUnAdminChangeDeRoleQuandIlEnResteUnAutre() throws Exception {
    var admin = connecterAdmin(mvc);
    long second = creerSecondAdmin(admin);

    modifier(admin, second, "FORMATEUR", true)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("FORMATEUR"));

    assertThat(adminsActifs()).as("l'administrateur d'origine reste seul actif").isEqualTo(1);
  }

  @Test
  void testQuitterLeRoleFormateurRetireSesRattachements() throws Exception {
    var admin = connecterAdmin(mvc);
    long formateur = idCompte("formateur");

    modifier(admin, formateur, "ADMIN", true).andExpect(status().isOk());

    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM formateur_promotion WHERE utilisateur_id = ?",
        Integer.class, formateur))
        .as("un compte qui n'est plus FORMATEUR n'est plus rattaché à une promotion (RG26)").isZero();
  }
}
