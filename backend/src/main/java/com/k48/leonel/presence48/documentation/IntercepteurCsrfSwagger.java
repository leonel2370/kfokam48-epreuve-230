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
 */
@Component
public class IntercepteurCsrfSwagger implements SwaggerIndexTransformer {

  private static final byte[] INITIALISATION = """
      window.ui = SwaggerUIBundle({
        "configUrl": "/v3/api-docs/swagger-config",
        "dom_id": "#swagger-ui",
        "requestInterceptor": function (requete) {
          function lireCookie(nom) {
            var m = document.cookie.match('(^|;)\\s*' + nom + '\\s*=\\s*([^;]+)');
            return m ? m.pop().trim() : '';
          }
          var jeton = lireCookie('XSRF-TOKEN');
          if (jeton) {
            requete.headers['X-XSRF-TOKEN'] = jeton;
          }
          return Promise.resolve(requete);
        }
      });
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
