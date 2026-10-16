package com.pacgram.common.security;

import com.pacgram.auth.repository.UserAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.JwtException;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtService jwtService;
    private final UserAccountRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserAccountRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            try {
                AccountPrincipal principal = jwtService.parse(authorization.substring(BEARER_PREFIX.length()));
                String expectedRole = principal.administrator() ? "ADMIN" : "USER";
                if (userRepository.existsByIdAndRoleAndAccountStatus(
                        principal.userId(), expectedRole, "ACTIVE")) {
                    var authorities = List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                            principal.administrator() ? "ROLE_ADMIN" : "ROLE_USER"));
                    var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    SecurityContextHolder.clearContext();
                }
            } catch (JwtException | IllegalArgumentException exception) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
