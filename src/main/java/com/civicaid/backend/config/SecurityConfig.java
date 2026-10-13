
package com.civicaid.backend.config;

import com.civicaid.backend.security.JwtAuthenticationFilter;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(provider);
    }

    @org.springframework.beans.factory.annotation.Value("${cors.allowed-origins:http://localhost:5173,http://localhost:5174,http://localhost:5175,http://127.0.0.1:5173,http://127.0.0.1:5174,http://127.0.0.1:5175}")
    private List<String> allowedOrigins;

    @org.springframework.beans.factory.annotation.Value("${cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*,http://localhost:[*],http://127.0.0.1:[*]}")
    private List<String> allowedOriginPatterns;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            configuration.setAllowedOrigins(allowedOrigins);
        }

        if (allowedOriginPatterns != null && !allowedOriginPatterns.isEmpty()) {
            configuration.setAllowedOriginPatterns(allowedOriginPatterns);
        }

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Accept"
                )
        );

        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {

        http
                .cors(cors -> cors.configurationSource(
                        corsConfigurationSource()
                ))

                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/users/me"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/users/me"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/users/me"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/users/me/functional-needs"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/users/me/functional-needs"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/users/me/lifecycle-records",
                                "/api/users/me/lifecycle-records/**"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/users/me/lifecycle-records"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/users/me/lifecycle-records/**"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/users/me/maintenance-records",
                                "/api/users/me/maintenance-records/**"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/users/me/maintenance-records"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/users/me/maintenance-records/**"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/users/me/maintenance-records/**"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/users/me/repair-records",
                                "/api/users/me/repair-records/**"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/users/me/repair-records"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/users/me/repair-records/**"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/users/me/repair-records/**"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/users/me/saved-resources"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/users/me/saved-resources"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/users/me/saved-resources/**"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/needs"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/at-categories",
                                "/api/at-categories/**"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/products",
                                "/api/products/**"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/schemes",
                                "/api/schemes/**"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/need-category-mappings",
                                "/api/need-category-mappings/**"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/providers",
                                "/api/providers/**"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/provider-service-categories",
                                "/api/provider-service-categories/**"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/provider-service-products",
                                "/api/provider-service-products/**"
                        ).permitAll()

                        .requestMatchers(
                                "/api/admin/**"
                        ).hasRole("ADMIN")

                        .anyRequest().denyAll()
                )

                .exceptionHandling(ex -> ex

                        .authenticationEntryPoint(
                                (request, response, authException) -> {
                                    response.setStatus(401);
                                    response.setContentType(
                                            "application/json"
                                    );
                                    response.setCharacterEncoding("UTF-8");

                                    response.getWriter().write(
                                            "{\"status\":401,"
                                                    + "\"code\":\"AUTHENTICATION_ERROR\","
                                                    + "\"message\":\"Authentication failed\"}"
                                    );
                                }
                        )

                        .accessDeniedHandler(
                                (request, response, accessDeniedException) -> {
                                    response.setStatus(403);
                                    response.setContentType(
                                            "application/json"
                                    );
                                    response.setCharacterEncoding("UTF-8");

                                    response.getWriter().write(
                                            "{\"status\":403,"
                                                    + "\"code\":\"FORBIDDEN\","
                                                    + "\"message\":\"Access denied\"}"
                                    );
                                }
                        )
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
