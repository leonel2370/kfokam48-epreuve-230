package com.k48.leonel.presence48.securite;

import com.k48.leonel.presence48.repository.UtilisateurRepository;
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

  /** Routes publiques (RG22). Sans session, les opérations imposées se comportent comme en v1 (B2). */
  static RequestMatcher[] routesPubliques() {
    PathPatternRequestMatcher.Builder m = PathPatternRequestMatcher.withDefaults();
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

  @Bean
  SecurityFilterChain chaineDeSecurite(HttpSecurity http, UtilisateurRepository utilisateurs) throws Exception {
    RequestMatcher[] publiques = routesPubliques();
    http
        .authorizeHttpRequests(a -> a
            .requestMatchers(publiques).permitAll()
            .anyRequest().authenticated())
        .csrf(c -> c.spa().ignoringRequestMatchers(publiques))
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
