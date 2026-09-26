package com.k48.leonel.presence48.documentation;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

/**
 * Documentation générale de l'API générée par springdoc (#105).
 *
 * Connexion depuis Swagger UI : appeler d'abord POST /api/auth/login (comptes de démonstration dans le
 * README), puis exécuter les autres opérations — le cookie JSESSIONID est envoyé automatiquement et le
 * jeton CSRF est recopié du cookie XSRF-TOKEN (IntercepteurCsrfSwagger). Les 5 opérations
 * imposées restent publiques (RG22) ; toutes les autres exigent la session (RG25).
 */
@OpenAPIDefinition(info = @Info(title = "API PRESENCE48 — implémentation",
    version = "2.2",
    description = """
        Documentation générée depuis le code (springdoc). Le contrat qui fait foi reste \
        `api/contrat.yaml` — le menu en haut à droite de Swagger UI propose les deux définitions.

        **Comment se connecter** : appeler `POST /api/auth/login` avec un compte de démonstration
        (voir le README), puis exécuter les autres opérations dans la même fenêtre : le cookie
        `JSESSIONID` est envoyé automatiquement et Swagger UI recopie le jeton `XSRF-TOKEN` dans
        l'en-tête `X-XSRF-TOKEN` (intercepteur injecté dans swagger-initializer.js).

        **Accès** : les 5 opérations imposées par le sujet et la connexion restent publiques (RG22) ;
        toutes les autres exigent la session (401 NON_AUTHENTIFIE) et le bon rôle (403 ACCES_REFUSE).

        **Erreurs** : toute erreur renvoie `{ "code": "…", "message": "…" }` — jamais de stack trace
        (contrat, ENF3). Les codes métier réels de chaque opération sont documentés sur ses réponses.
        """),
    servers = @Server(url = "/", description = "même origine"))
@SecurityScheme(name = "cookieAuth", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.COOKIE,
    paramName = "JSESSIONID", description = "Session serveur obtenue par POST /api/auth/login (EF15).")
public final class DocApi {
  private DocApi() {
  }
}
