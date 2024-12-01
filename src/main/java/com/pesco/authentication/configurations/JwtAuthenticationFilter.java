package com.pesco.authentication.configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pesco.authentication.serviceImplementations.JwtServiceImplementations;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtServiceImplementations jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }
            jwt = authHeader.substring(7);
            userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    Claims claims = jwtService.extractAllClaims(jwt);
                    @SuppressWarnings("unchecked")
                    List<String> roles = claims.get("roles", List.class);

                    if (roles != null) {
                        List<SimpleGrantedAuthority> authorities = roles.stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList());

                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                authorities);
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            setErrorResponse(HttpServletResponse.SC_UNAUTHORIZED, response, "Token has expired", "expired_token");
        } catch (JwtException | IllegalArgumentException e) {
            setErrorResponse(HttpServletResponse.SC_UNAUTHORIZED, response, "Invalid token", "invalid_token");
        }
    }

    private void setErrorResponse(int status, HttpServletResponse response, String detail, String code)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("status", status);
        errorDetails.put("title", "Authentication Error");
        errorDetails.put("detail", detail);
        errorDetails.put("code", code);
        clearCookies(response);
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", errorDetails);
        response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
    }

    private void clearCookies(HttpServletResponse response) {
        // Clear the authentication-related cookies
        Cookie authTokenCookie = new Cookie("jwt", null);
        authTokenCookie.setMaxAge(0);
        authTokenCookie.setPath("/");
        response.addCookie(authTokenCookie);
    }
}
