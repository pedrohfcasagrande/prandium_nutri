package com.prandiumnutri.security;

import com.prandiumnutri.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final long LAST_SEEN_THROTTLE_MS = 5 * 60 * 1000L; // 5 min

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;
    private final UserRepository userRepository;

    private final ConcurrentHashMap<Long, Long> lastSeenCache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = null;
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            token = auth.substring(7);
        } else {
            // Fase 9G-1: EventSource nao aceita header custom — fallback pra ?access_token=
            // Aplica so ao SSE endpoint pra minimizar exposicao do token em access logs.
            String uri = request.getRequestURI();
            if (uri != null && uri.endsWith("/stream")) {
                token = request.getParameter("access_token");
            }
        }
        if (token != null && !token.isBlank() && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtService.parse(token);
                String email = claims.getSubject();
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                if (userDetails instanceof AppUserDetails app) {
                    touchLastSeen(app.getUser().getId());
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                // token invalido -> segue como anonimo, endpoints protegidos retornam 401
            }
        }
        chain.doFilter(request, response);
    }

    private void touchLastSeen(Long userId) {
        long now = System.currentTimeMillis();
        Long prev = lastSeenCache.get(userId);
        if (prev != null && (now - prev) < LAST_SEEN_THROTTLE_MS) {
            return;
        }
        lastSeenCache.put(userId, now);
        try {
            userRepository.updateLastSeenAt(userId, LocalDateTime.now());
        } catch (Exception e) {
            log.debug("Falha ao atualizar last_seen_at do user {}: {}", userId, e.getMessage());
        }
    }
}
