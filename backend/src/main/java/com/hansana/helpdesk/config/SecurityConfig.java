package com.hansana.helpdesk.config;

import com.hansana.helpdesk.auth.security.JwtAccessDeniedHandler;
import com.hansana.helpdesk.auth.security.JwtAuthenticationEntryPoint;
import com.hansana.helpdesk.auth.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint jwtAuthEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    public SecurityConfig() {
        this.jwtAuthFilter = null;
        this.jwtAuthEntryPoint = null;
        this.jwtAccessDeniedHandler = null;
    }

    @Autowired
    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthFilter,
            JwtAuthenticationEntryPoint jwtAuthEntryPoint,
            JwtAccessDeniedHandler jwtAccessDeniedHandler
    ) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.jwtAuthEntryPoint = jwtAuthEntryPoint;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (jwtAuthEntryPoint != null || jwtAccessDeniedHandler != null) {
            http.exceptionHandling(ex -> {
                if (jwtAuthEntryPoint != null) {
                    ex.authenticationEntryPoint(jwtAuthEntryPoint);
                }
                if (jwtAccessDeniedHandler != null) {
                    ex.accessDeniedHandler(jwtAccessDeniedHandler);
                }
            });
        }

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/v1/auth/me").authenticated()
                .requestMatchers("/api/v1/test/admin").hasRole("ADMIN")
                .requestMatchers("/api/v1/test/agent").hasAnyRole("SUPPORT_AGENT", "ADMIN")
                .requestMatchers("/api/v1/test/user").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/categories").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/users/**").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/tickets").hasRole("USER")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/v1/tickets/*/assignment").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/v1/tickets/*/assignment").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/tickets/*/start").hasRole("SUPPORT_AGENT")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/tickets/*/resolve").hasRole("SUPPORT_AGENT")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/tickets/*/confirm-resolution").hasRole("USER")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/tickets/*/reject-resolution").hasRole("USER")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/tickets/*/close").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v1/tickets/*/priority").hasAnyRole("SUPPORT_AGENT", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v1/tickets/*").hasRole("USER")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/tickets/*/audit").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/tickets/*/comments").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/tickets/*/comments").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/tickets/**").authenticated()
                .anyRequest().authenticated()
        );

        if (jwtAuthFilter != null) {
            http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
