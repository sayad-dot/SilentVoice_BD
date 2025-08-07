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
                // Disable CSRF (we're a stateless REST API)
                .csrf(csrf -> csrf.disable())
                // Enable CORS with our configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Do not create sessions; every request must carry a token
                .sessionManagement(session
                        -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // Authorization rules - ORDER MATTERS!
                .authorizeHttpRequests(auth -> auth
                // CRITICAL: Public authentication endpoints FIRST
                .requestMatchers("/api/auth/**").permitAll() // This includes our new Google endpoints
                .requestMatchers("/auth/**").permitAll()
                // Public endpoints
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Video endpoints - streaming and download allowed, others require auth
                .requestMatchers(HttpMethod.POST, "/api/videos/upload").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/videos").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/videos/*/info").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/videos/*/status").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/videos/**").authenticated()
                // Allow streaming and download (handled by controller token validation)
                .requestMatchers(HttpMethod.GET, "/api/videos/*/stream").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/videos/*/download").permitAll()
                // Audio endpoints require authentication
                .requestMatchers(HttpMethod.GET, "/api/audio/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/audio/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/audio/**").authenticated()
                // AI endpoints require authentication
                .requestMatchers("/api/ai/**").authenticated()
                // Allow media endpoints for BdSLW-60 videos
                .requestMatchers("/api/media/**").permitAll()
                // Learning endpoints require authentication
                .requestMatchers("/api/learning/**").authenticated()
                // Allow all SockJS/WebSocket handshake traffic
                .requestMatchers("/ws/**").permitAll()
                // Swagger / OpenAPI
                .requestMatchers(HttpMethod.GET, "/swagger-ui/**", "/v3/api-docs/**")
                .permitAll()
                // Everything else requires a valid JWT
                .anyRequest().authenticated()
                )
                // Plug in our JWT filter before Spring's username/password filter
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)
                // And use our DAO auth provider
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

        // Use specific origins with credentials (recommended approach)
        config.setAllowedOrigins(Arrays.asList("http://localhost:3000"));

        // Allow all HTTP methods (GET, POST, etc)
        config.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // Allow all headers (Authorization, Content-Type, etc)
        config.setAllowedHeaders(Arrays.asList("*"));

        // Allow cookies / auth headers (needed for JWT tokens)
        config.setAllowCredentials(true);

        // Cache preflight response for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
