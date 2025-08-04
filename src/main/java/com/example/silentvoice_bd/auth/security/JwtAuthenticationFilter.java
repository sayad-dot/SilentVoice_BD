package com.example.silentvoice_bd.auth.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.silentvoice_bd.auth.service.CustomUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // CRITICAL: Skip JWT validation for authentication endpoints
        if (isAuthenticationEndpoint(requestPath)) {
            logger.debug("Skipping JWT validation for authentication endpoint: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        // CRITICAL: Skip JWT validation for video streaming endpoints
        if (isVideoStreamingEndpoint(requestPath)) {
            // For video streaming, validate token from URL parameter if present
            String tokenParam = request.getParameter("token");
            if (tokenParam != null && !tokenParam.isEmpty()) {
                try {
                    if (jwtTokenProvider.validateToken(tokenParam)) {
                        String username = jwtTokenProvider.getEmailFromToken(tokenParam);
                        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                            UsernamePasswordAuthenticationToken authToken
                                    = new UsernamePasswordAuthenticationToken(
                                            userDetails, null, userDetails.getAuthorities());
                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authToken);
                        }
                    }
                } catch (Exception e) {
                    logger.debug("Token validation failed for video streaming: {}", e.getMessage());
                }
            }
            filterChain.doFilter(request, response);
            return;
        }

        // Regular JWT validation for other endpoints
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        if (jwtTokenProvider.validateToken(jwt)) {
            username = jwtTokenProvider.getEmailFromToken(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Check if the request is for authentication endpoints (login/register)
     */
    private boolean isAuthenticationEndpoint(String requestPath) {
        return requestPath != null && (requestPath.startsWith("/api/auth/")
                || requestPath.startsWith("/auth/")
                || requestPath.equals("/api/auth/login")
                || requestPath.equals("/api/auth/register")
                || requestPath.equals("/auth/login")
                || requestPath.equals("/auth/register"));
    }

    /**
     * Check if the request is for video streaming endpoints
     */
    private boolean isVideoStreamingEndpoint(String requestPath) {
        return requestPath != null && (requestPath.matches("/api/videos/[^/]+/stream")
                || requestPath.matches("/api/videos/[^/]+/download")
                || requestPath.startsWith("/api/media/"));
    }
}
