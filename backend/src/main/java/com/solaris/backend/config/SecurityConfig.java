package com.solaris.backend.config;

import com.solaris.backend.security.JwtAuthenticationFilter;
import com.solaris.backend.security.JwtService;
import com.solaris.backend.repository.UserRepository;
import com.solaris.backend.security.SecurityErrorWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final SecurityErrorWriter securityErrorWriter;

    public SecurityConfig(JwtService jwtService, UserRepository userRepository,
                          SecurityErrorWriter securityErrorWriter) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.securityErrorWriter = securityErrorWriter;
    }

    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {

        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(
                jwtService,
                userRepository,
                securityErrorWriter
        );

        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                securityErrorWriter.writeUnauthorized(
                                        request,
                                        response,
                                        "Authentication is required"
                                ))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                securityErrorWriter.writeForbidden(
                                        request,
                                        response,
                                        "You do not have permission to access this resource"
                                )))

                .authorizeHttpRequests(auth -> auth
                        // Registration and token lifecycle endpoints must be reachable before authentication.
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/actuator/health",
                                "/public/**"
                        ).permitAll()

                        // URL-level role checks provide the first boundary; services still enforce ownership rules.
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/api/homeowner/**")
                        .hasRole("HOMEOWNER")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }


}
