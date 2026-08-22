package app.infrastructure.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

/** Two filter chains: a stateless bearer-token one for the API, a session-backed one for the pages. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    public static final String ADMIN = "ADMIN";

    /** Everything the API serves. Anything not listed here is a web page. */
    private static final String[] API_PATHS = {
            "/api/**", "/admin/**", "/books/**", "/authors/**", "/customers/**", "/transactions/**"
    };

    @Value("${library.cors.allowed-origins:}")
    private String allowedOrigins;

    private final UserDetailsServiceImpl userDetailsService;
    private final AuthenticationFilter authenticationFilter;

    /** Cross-origin rules for the API. Empty by default, which allows nothing. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        if (origins.isEmpty()) {
            return source;
        }

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        // The pagination links are custom headers, and a browser hides those from cross-origin
        // JavaScript unless they are named here.
        config.setExposedHeaders(List.of("Authorization", "self", "next", "prev"));
        // The token travels in a header, not a cookie, so credentials are not needed - and leaving
        // them off is what allows an explicit origin list to stay strict.
        config.setAllowCredentials(false);
        config.setMaxAge(Duration.ofHours(1));

        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /** Response headers that limit what a browser will do with our pages: CSP, HSTS and friends. */
    private static void hardenHeaders(HeadersConfigurer<HttpSecurity> headers) {
        headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(String.join("; ",
                        "default-src 'self'",
                        // The catalogue covers come from Open Library; data: covers inline SVG.
                        "img-src 'self' data: https://covers.openlibrary.org",
                        "script-src 'self'",
                        "style-src 'self' 'unsafe-inline'",
                        "frame-ancestors 'none'",
                        "base-uri 'self'",
                        "form-action 'self'")))
                .referrerPolicy(referrer -> referrer.policy(
                        ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .maxAgeInSeconds(31536000));
    }

    /** The stateless chain for /api and the other JSON endpoints. */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(API_PATHS)
                .cors(Customizer.withDefaults())
                // Safe to disable only because this chain is stateless and authenticates from a
                // header: a browser does not attach an Authorization header to a cross-site form
                // post, which is what CSRF relies on.
                .csrf(AbstractHttpConfigurer::disable)
                .headers(SecurityConfig::hardenHeaders)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/login", "/api/register").permitAll()
                        .requestMatchers("/api/logout").permitAll()
                        // Changing the catalogue and seeing the members are both administrator work.
                        .requestMatchers("/admin/**").hasRole(ADMIN)
                        .requestMatchers("/customers/**").hasRole(ADMIN)
                        // Reading someone else's loans by id is too; members use /transactions/me.
                        .requestMatchers("/transactions/history/**").hasRole(ADMIN)
                        .anyRequest().authenticated()
                )
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .logoutSuccessHandler(jsonLogoutSuccessHandler())
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler(jsonAccessDeniedHandler())
                )
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /** The session-backed form-login chain for the server-rendered pages. */
    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/error", "/favicon.ico").permitAll()
                        .requestMatchers("/index.html", "/css/**", "/js/**", "/images/**").permitAll()
                        // Only where the console exists at all - see application.properties.
                        // Reachable without authentication, so it must never be on in a deployment.
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )
                // Username/password form, rendered by templates/login.html and posted back to /login.
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .headers(headers -> {
                    hardenHeaders(headers);
                    // The H2 console renders in a frame; it only exists under the dev profile.
                    headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable);
                })
                // Form login needs a session to remember who signed in.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .build();
    }

    /** Answers sign-out with JSON instead of a redirect. */
    private LogoutSuccessHandler jsonLogoutSuccessHandler() {
        return (request, response, authentication) -> {
            response.setStatus(HttpStatus.OK.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"Logout successful\"}");
        };
    }

    /** Answers a forbidden request with JSON instead of a redirect. */
    private AccessDeniedHandler jsonAccessDeniedHandler() {
        return (request, response, exception) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"Forbidden: this area is reserved for administrators\"}");
        };
    }

    /** The hash stored passwords are checked against. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Checks a username and password against the stored accounts. */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /** The manager form login and the API both authenticate through. */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
