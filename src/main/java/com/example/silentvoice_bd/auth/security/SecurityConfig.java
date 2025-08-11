package com.example.silentvoice_bd.auth.security;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.silentvoice_bd.auth.service.CustomUserDetailsService;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder auth
                = http.getSharedObject(AuthenticationManagerBuilder.class);
        auth.authenticationProvider(daoAuthenticationProvider());
        return auth.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (stateless API)
                .csrf(csrf -> csrf.disable())
                // Enable CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Stateless session - every request needs token
                .sessionManagement(sm
                        -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Authorization rules (order matters)
                .authorizeHttpRequests(auth -> auth
                // Public authentication endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/auth/**").permitAll()
                // Allow preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Allow AI status without auth
                .requestMatchers("/api/ai/status").permitAll()
                // Secure all other AI endpoints
                .requestMatchers("/api/ai/**").authenticated()
                // Video endpoints
                .requestMatchers(HttpMethod.POST, "/api/videos/upload").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/videos").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/videos/*/info").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/videos/*/status").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/videos/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/videos/*/stream").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/videos/*/download").permitAll()
                // Audio endpoints
                .requestMatchers("/api/audio/**").authenticated()
                // Media and WebSocket
                .requestMatchers("/api/media/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                // Swagger / OpenAPI
                .requestMatchers(HttpMethod.GET, "/swagger-ui/**", "/v3/api-docs/**")
                .permitAll()
                // All other requests require authentication
                .anyRequest().authenticated()
                )
                // Add JWT filter
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .authenticationProvider(daoAuthenticationProvider());

        return http.build();
    }

    /**
     * CORS configuration to allow the React frontend (localhost:3000) to call
     * both HTTP REST and SockJS handshake endpoints.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        config.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source
                = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
