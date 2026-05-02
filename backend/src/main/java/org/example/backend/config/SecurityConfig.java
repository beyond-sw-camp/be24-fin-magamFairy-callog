package org.example.backend.config;

import lombok.RequiredArgsConstructor;
import org.example.backend.config.filter.JwtFilter;
import org.example.backend.config.filter.LoginFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {
    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final LoginFilter loginFilter;
    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain config(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);

        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/login", "/login", "/error").permitAll()
                .requestMatchers("/auth/reissue", "/auth/logout").permitAll()
                .requestMatchers("/auth/signup").permitAll()
                .requestMatchers("/auth/usercreate").hasAnyAuthority("ROLE_ADMIN", "ROLE_GENERAL_MANAGER", "ROLE_MANAGER")
                .requestMatchers("/auth/manage").hasAnyAuthority("ROLE_GENERAL_MANAGER")
                .requestMatchers("/auth/manage/users").hasAnyAuthority("ROLE_ADMIN", "ROLE_GENERAL_MANAGER", "ROLE_MANAGER")
                .requestMatchers(
                        "/auth/userdelete",
                        "/auth/resetpassword",
                        "/administrator/users",
                        "/admin/users"
                ).hasAnyAuthority("ROLE_ADMIN", "ROLE_GENERAL_MANAGER", "ROLE_MANAGER")
                .requestMatchers("/administrator/**", "/admin/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers(
                        "/workspace/**",
                        "/file/**",
                        "/ws-stomp/**",
                        "/notification/subscribe",
                        "/api/sse/**",
                        "/sse/**"
                ).permitAll()
                .requestMatchers(HttpMethod.GET,    "/campaigns/*/members").authenticated()
                .requestMatchers(HttpMethod.GET,    "/campaigns/*/members/candidates/**")
                    .hasAnyAuthority("ROLE_GENERAL_MANAGER", "ROLE_MANAGER")
                .requestMatchers(HttpMethod.POST,   "/campaigns/*/members")
                    .hasAnyAuthority("ROLE_GENERAL_MANAGER", "ROLE_MANAGER")
                .requestMatchers(HttpMethod.POST,   "/campaigns/*/members/invite-partner")
                    .access((supplier, ctx) -> new AuthorizationDecision(
                        supplier.get().getAuthorities().stream().anyMatch(a ->
                            a.getAuthority().equals("ROLE_GENERAL_MANAGER") || a.getAuthority().equals("ROLE_MANAGER"))
                        && supplier.get().getAuthorities().stream().anyMatch(a ->
                            a.getAuthority().equals("ORG_AFFILIATE"))
                    ))
                .requestMatchers(HttpMethod.PATCH,  "/campaigns/*/members/*")
                    .hasAuthority("ROLE_GENERAL_MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/campaigns/*/members/*")
                    .hasAnyAuthority("ROLE_GENERAL_MANAGER", "ROLE_MANAGER")
                .requestMatchers(HttpMethod.POST,   "/campaigns/*/kpis")
                    .hasAnyAuthority("ROLE_GENERAL_MANAGER", "ROLE_MANAGER")
                .requestMatchers(HttpMethod.POST,   "/campaigns/*/kpis/import-framework")
                    .hasAnyAuthority("ROLE_GENERAL_MANAGER", "ROLE_MANAGER")
                .requestMatchers(HttpMethod.PATCH,  "/campaigns/*/kpis/**")
                    .hasAnyAuthority("ROLE_GENERAL_MANAGER", "ROLE_MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/campaigns/*/kpis/*")
                    .hasAnyAuthority("ROLE_GENERAL_MANAGER", "ROLE_MANAGER")
                .requestMatchers("/campaigns/**").authenticated()
                .requestMatchers(
                        "/matching/**"
                        )
                .hasAnyAuthority("ROLE_ADMIN", "ROLE_GENERAL_MANAGER", "ROLE_MANAGER")
                .requestMatchers(
                        "/matching/benefit/**",
                        "/matching/asset/**",
                        "/matching/evaluation/list/**"
                )
                .hasAnyAuthority("ROLE_USER")
                .anyRequest().authenticated()
        );

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(loginFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        configuration.setAllowedOriginPatterns(List.of(
                frontendUrl,
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://192.168.*:*",
                "http://10.*:*",
                "http://172.*:*"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Set-Cookie", "Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
