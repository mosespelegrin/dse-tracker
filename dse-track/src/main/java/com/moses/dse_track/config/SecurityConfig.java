package com.moses.dse_track.config;

import com.moses.dse_track.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Disable CSRF — not needed for REST APIs
                http.csrf(AbstractHttpConfigurer::disable)

                // Without this, Spring Security's own filter chain intercepts and
                // rejects the browser's CORS preflight (OPTIONS) request on any
                // protected endpoint before it ever reaches CorsConfig's mapping —
                // preflight carries no auth token, so it gets blocked outright.
                // This picks up CorsConfig's WebMvcConfigurer mapping automatically
                // (no separate CorsConfigurationSource bean needed).
                .cors(Customizer.withDefaults())

                // Don't use sessions — JWT is stateless
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Define which endpoints need a token
                .authorizeHttpRequests(auth -> auth
                        // Unlike the rest of /auth/**, this one acts on the calling
                        // user (via SecurityContext) and must come first so the
                        // permitAll below doesn't swallow it.
                        .requestMatchers(HttpMethod.PUT, "/auth/change-password").authenticated()

                        // Public  no token needed
                        // These are the exceptions — public
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/stocks").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                        // Spring re-dispatches internally to /error whenever a request fails
                        // inside the DispatcherServlet (e.g. a @Valid validation failure) —
                        // without this, AuthorizationFilter re-evaluates that internal
                        // dispatch as an anonymous request to an unlisted path, denies it,
                        // and silently replaces the intended 400 (with its field-error
                        // messages) with a bare, bodiless 403 on every single one.
                        .requestMatchers("/error").permitAll()

                        .requestMatchers("/", "/index.html", "/dashboard.html",
                                "/change-password.html", "/admin.html",
                                "/**/*.html", "/**/*.css", "/**/*.js").permitAll()
                        // Search engine crawlers hit these with no auth token — must stay public
                        .requestMatchers("/robots.txt", "/sitemap.xml").permitAll()

                        // Shared/global fundamentals data — restricted to admins so any
                        // authenticated user can't overwrite it (see FundamentalsService).
                        .requestMatchers(HttpMethod.PUT, "/stocks/*/fundamentals").hasRole("ADMIN")

                        // Admin dashboard — activity log / analytics.
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Everything else needs a valid JWT token
                        .anyRequest().authenticated()
                )

                // Add JWT filter before Spring's default auth filter
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}