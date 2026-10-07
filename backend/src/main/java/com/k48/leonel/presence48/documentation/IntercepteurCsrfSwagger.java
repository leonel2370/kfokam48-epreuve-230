package com.k48.leonel.presence48.documentation;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

/**
 * #105 : Swagger UI doit envoyer le jeton CSRF sur les écritures, comme le frontend Angular (ENF11).
 * springdoc 3.x n'a plus de propriété `springdoc.swagger-ui.csrf.enabled` : on remplace
 * `swagger-initializer.js` par une initialisation qui recopie le cookie `XSRF-TOKEN` dans l'en-tête
 * `X-XSRF-TOKEN` (même cookie et même en-tête que le frontend, app.config.ts).
 * #139 : l'initialisation garde les préréglages et la mise en page de Swagger UI (barre du haut, choix
 * de la définition) et lit le cookie sans expression régulière.
 */
@Component
public class IntercepteurCsrfSwagger implements SwaggerIndexTransformer {

  private static final byte[] INITIALISATION = """
      window.onload = function () {
        window.ui = SwaggerUIBundle({
          configUrl: "/v3/api-docs/swagger-config",
          dom_id: "#swagger-ui",
          deepLinking: true,
          presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
          plugins: [SwaggerUIBundle.plugins.DownloadUrl],
          layout: "StandaloneLayout",
          requestInterceptor: function (requete) {
            var jeton = document.cookie.split("; ")
              .map(function (cookie) { return cookie.split("="); })
              .filter(function (paire) { return paire[0] === "XSRF-TOKEN"; })
              .map(function (paire) { return decodeURIComponent(paire.slice(1).join("=")); })[0];
            if (jeton) {
              requete.headers["X-XSRF-TOKEN"] = jeton;
            }
            return requete;
          }
        });
      };
      """.getBytes(StandardCharsets.UTF_8);

  @Override
  public Resource transform(HttpServletRequest requete, Resource resource, ResourceTransformerChain chaine)
      throws IOException {
    if (!resource.getURL().toString().endsWith("swagger-initializer.js")) {
      return resource;
    }
    return new TransformedResource(resource, INITIALISATION);
  }
}
