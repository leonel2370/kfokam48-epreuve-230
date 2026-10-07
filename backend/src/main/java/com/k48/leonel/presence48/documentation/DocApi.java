package com.k48.leonel.presence48.documentation;

import com.k48.leonel.presence48.securite.MessagesSecurite;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Documentation générale de l'API générée par springdoc (#105, #139). Tout ce qui est commun à plusieurs
 * opérations est écrit ici une seule fois, pour ne pas diverger du code : la version (lue dans le contrat),
 * les tags, et les réponses 401 et 403 de toute opération protégée.
 */
@Configuration
@SecurityScheme(name = "cookieAuth", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.COOKIE,
    paramName = "JSESSIONID", description = "Session serveur obtenue par POST /api/auth/login (EF15).")
public class DocApi {

  static final String JSON = "application/json";
  private static final String SCHEMA_ERREUR = "#/components/schemas/ErreurReponse";
  private static final Pattern VERSION_DU_CONTRAT = Pattern.compile("(?m)^  version: \"([0-9.]+)\"");

  private static final String DESCRIPTION = """
      Documentation générée depuis le code (springdoc). Le contrat qui fait foi reste \
      `api/contrat.yaml` — le menu en haut à droite de Swagger UI propose les deux définitions.

      **Comment se connecter** : appeler `POST /api/auth/login` avec un compte de démonstration
      (voir le README), puis exécuter les autres opérations dans la même fenêtre : le cookie
      `JSESSIONID` est envoyé automatiquement et Swagger UI recopie le jeton `XSRF-TOKEN` dans
      l'en-tête `X-XSRF-TOKEN`.

      **Accès** : les 5 opérations imposées par le sujet et la connexion restent publiques (RG22) ;
      toutes les autres exigent la session (401 NON_AUTHENTIFIE) et le bon rôle (403 ACCES_REFUSE).

      **Erreurs** : toute erreur renvoie `{ "code": "…", "message": "…" }` — jamais de stack trace
      (contrat, ENF3). Les codes métier réels de chaque opération sont documentés sur ses réponses.
      """;

  /** Tags, dans l'ordre d'affichage : chacun est décrit ici et nulle part ailleurs. */
  private static final Map<String, String> TAGS = tags();

  private static Map<String, String> tags() {
    var tags = new LinkedHashMap<String, String>();
    tags.put("sécurité", "Connexion, profil, mot de passe, récapitulatif (EF15 à EF18, SF-15 à SF-18)");
    tags.put("référentiel", "Listes publiques de sélection : promotions et étudiants actifs (EF1, RG22)");
    tags.put("session", "Ouvrir et lister les sessions (EF2, SF-2)");
    tags.put("présence", "Marquer sa présence avec le code (EF3, SF-3)");
    tags.put("exercice", "Déposer un exercice et consulter ses notes (EF6, EF14)");
    tags.put("relecture", "Relectures assignées et rendu d'une note (EF8, EF9)");
    tags.put("tableau", "Tableau d'une promotion, calculé par le serveur (EF10)");
    tags.put("administration", "Comptes, promotions, formateurs et fiches étudiants (EF21 à EF23, SF-20 à SF-22)");
    return tags;
  }

  /** Version lue dans le contrat servi par l'application : la doc générée ne peut pas en annoncer une autre. */
  @Bean
  OpenApiCustomizer informationsGenerales(@Value("classpath:/static/contrat.yaml") Resource contrat) {
    var version = versionDe(contrat);
    return openApi -> {
      openApi.setInfo(new Info().title("API PRESENCE48 — implémentation").version(version)
          .description(DESCRIPTION));
      openApi.setServers(List.of(new Server().url("/").description("même origine")));
      openApi.setTags(TAGS.entrySet().stream()
          .map(tag -> new Tag().name(tag.getKey()).description(tag.getValue())).toList());
    };
  }

  /**
   * Toute opération protégée répond 401 sans session et peut répondre 403 (rôle, jeton CSRF, mot de passe
   * à changer) : ces deux réponses sont ajoutées ici, avec les messages réellement renvoyés.
   */
  @Bean
  OperationCustomizer reponsesDesOperationsProtegees() {
    return (operation, methode) -> {
      if (operation.getSecurity() == null || operation.getSecurity().isEmpty()) {
        return operation;
      }
      ajouterExemple(operation.getResponses().computeIfAbsent("401",
          statut -> new ApiResponse().description("Non connecté")),
          "non-connecte", "NON_AUTHENTIFIE", MessagesSecurite.NON_AUTHENTIFIE);
      ajouterExemple(operation.getResponses().computeIfAbsent("403",
          statut -> new ApiResponse().description("Rôle insuffisant, jeton CSRF absent ou mot de passe à changer")),
          "acces-refuse", "ACCES_REFUSE", MessagesSecurite.ACCES_REFUSE);
      return operation;
    };
  }

  private static void ajouterExemple(ApiResponse reponse, String nom, String code, String message) {
    if (reponse.getContent() == null) {
      reponse.setContent(new Content());
    }
    var corps = reponse.getContent().computeIfAbsent(JSON,
        type -> new MediaType().schema(new Schema<>().$ref(SCHEMA_ERREUR)));
    if (corps.getExample() != null) {
      // Un exemple unique venu d'une annotation rejoint la liste nommée : les deux formes s'excluent.
      corps.addExamples("metier", new Example().value(corps.getExample()));
      corps.setExample(null);
    }
    corps.addExamples(nom, new Example().value(Map.of("code", code, "message", message)));
  }

  private static String versionDe(Resource contrat) {
    try {
      var texte = contrat.getContentAsString(StandardCharsets.UTF_8);
      var version = VERSION_DU_CONTRAT.matcher(texte);
      if (!version.find()) {
        throw new IllegalStateException("Le contrat ne déclare pas de version.");
      }
      return version.group(1);
    } catch (IOException e) {
      throw new UncheckedIOException("Contrat introuvable dans l'application.", e);
    }
  }
}
