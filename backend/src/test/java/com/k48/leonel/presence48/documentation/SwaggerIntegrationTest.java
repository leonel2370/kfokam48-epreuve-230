package com.k48.leonel.presence48.documentation;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** #96 : Swagger UI, la doc générée et le contrat de référence sont servis sans session. */
@SpringBootTest
@AutoConfigureMockMvc
class SwaggerIntegrationTest {

  @Autowired
  private MockMvc mvc;

  @Test
  void testSwaggerUiEstPublic() throws Exception {
    mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
  }

  @Test
  void testDocGenereeContientLesOperationsImposees() throws Exception {
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/sessions'].post").exists())
        .andExpect(jsonPath("$.paths['/api/presences'].post").exists())
        .andExpect(jsonPath("$.paths['/api/exercices'].post").exists())
        .andExpect(jsonPath("$.paths['/api/relectures/{id}'].post").exists())
        .andExpect(jsonPath("$.paths['/api/tableau'].get").exists());
  }

  @Test
  void testContratDeReferenceServiTelQuel() throws Exception {
    mvc.perform(get("/contrat.yaml"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("title: Contrat PRESENCE48")));
  }

  @Test
  void testConfigurationSwaggerProposeLeContratEnPremier() throws Exception {
    mvc.perform(get("/v3/api-docs/swagger-config"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.urls[0].url").value("/contrat.yaml"))
        .andExpect(jsonPath("$.urls[1].url").value("/v3/api-docs"));
  }
}
