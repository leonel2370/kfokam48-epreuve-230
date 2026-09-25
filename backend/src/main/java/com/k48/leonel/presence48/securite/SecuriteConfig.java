package com.k48.leonel.presence48.securite;

import com.k48.leonel.presence48.repository.UtilisateurRepository;
import java.util.Arrays;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Sécurité v2 (#54) : session serveur, BCrypt, CSRF par cookie (SPA), 401/403 au format du contrat.
 * RG22 : les 5 opérations imposées, la connexion et les listes de sélection restent publiques.
 */
@Configuration
@EnableMethodSecurity
public class SecuriteConfig {

  private static final Set<String> METHODES_SURES = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

  /** Routes publiques (RG22). Sans session, les opérations imposées se comportent comme en v1 (B2). */
  static RequestMatcher[] routesPubliques() {
    var m = PathPatternRequestMatcher.withDefaults();
    return new RequestMatcher[] {
        m.matcher(HttpMethod.POST, "/api/auth/login"),
        m.matcher(HttpMethod.POST, "/api/sessions"),
        m.matcher(HttpMethod.POST, "/api/presences"),
        m.matcher(HttpMethod.POST, "/api/exercices"),
        m.matcher(HttpMethod.POST, "/api/relectures/{id}"),
        m.matcher(HttpMethod.GET, "/api/tableau"),
        m.matcher(HttpMethod.GET, "/api/promotions"),
        m.matcher(HttpMethod.GET, "/api/promotions/{promotionId}/etudiants")
    };
  }

  /**
   * CSRF exigé sur toute écriture dès qu'une session existe (y compris sur les routes publiques appelées
   * connecté, sinon un site tiers pourrait agir avec le cookie de la victime). Sans session, les routes
   * publiques n'en demandent pas : c'est ainsi que le contrat imposé reste appelable tel quel (B2).
   */
  static RequestMatcher csrfExige(RequestMatcher[] publiques) {
    return requete -> {
      if (METHODES_SURES.contains(requete.getMethod())) {
        return false;
      }
      var publique = Arrays.stream(publiques).anyMatch(m -> m.matches(requete));
      return !publique || requete.getSession(false) != null;
    };
  }

  @Bean
  SecurityFilterChain chaineDeSecurite(HttpSecurity http, UtilisateurRepository utilisateurs) {
    var publiques = routesPubliques();
    http
        .authorizeHttpRequests(a -> a
            .requestMatchers(publiques).permitAll()
            .anyRequest().authenticated())
        .csrf(c -> c.spa().requireCsrfProtectionMatcher(csrfExige(publiques)))
        .securityContext(s -> s.securityContextRepository(depotDeContexte()))
        .exceptionHandling(e -> e
            .authenticationEntryPoint(GestionnairesErreurSecurite.nonAuthentifie())
            .accessDeniedHandler(GestionnairesErreurSecurite.accesRefuse()))
        .logout(l -> l
            .logoutRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/auth/logout"))
            .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
            .deleteCookies("JSESSIONID"))
        .httpBasic(b -> b.disable())
        .formLogin(f -> f.disable())
        .addFilterAfter(new ChangementMotDePasseFiltre(utilisateurs), AuthorizationFilter.class);
    return http.build();
  }

  @Bean
  SecurityContextRepository depotDeContexte() {
    return new HttpSessionSecurityContextRepository();
  }

  @Bean
  PasswordEncoder encodeurDeMotDePasse() {
    return new BCryptPasswordEncoder();
  }
}
