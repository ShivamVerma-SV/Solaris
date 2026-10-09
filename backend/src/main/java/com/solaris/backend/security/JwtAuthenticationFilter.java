package com.solaris.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.dao.DataAccessException;
import com.solaris.backend.entity.User;
import com.solaris.backend.repository.UserRepository;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter  {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final SecurityErrorWriter securityErrorWriter;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository,
                                   SecurityErrorWriter securityErrorWriter) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.securityErrorWriter = securityErrorWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!header.startsWith("Bearer ") || header.length() == 7) {
            securityErrorWriter.writeUnauthorized(request, response, "Malformed Authorization header");
            return;
        }

        String token = header.substring(7).trim();

        try {
            Claims claims = jwtService.extractClaims(token);

            if (!TokenType.ACCESS.name().equals(claims.get("type", String.class))) {
                securityErrorWriter.writeUnauthorized(request, response, "Only access tokens may authenticate API requests");
                return;
            }

            String email = claims.getSubject();
            String role = claims.get("role", String.class);
            Number userIdClaim = claims.get("userId", Number.class);
            if (email == null || role == null || userIdClaim == null) {
                securityErrorWriter.writeUnauthorized(request, response, "Access token is invalid");
                return;
            }

            // Re-read the user instead of trusting claims alone. Disabling an account or changing its
            // role therefore takes effect immediately, even if an older access token has not expired.
            User user = userRepository.findById(userIdClaim.longValue()).orElse(null);
            if (user == null || !user.isEnabled() || !user.getEmail().equalsIgnoreCase(email)
                    || !user.getRole().name().equals(role)) {
                securityErrorWriter.writeUnauthorized(request, response, "Access token is no longer valid");
                return;
            }

            var authority =
                    new SimpleGrantedAuthority("ROLE_" + role);

            var authentication =
                    new UsernamePasswordAuthenticationToken(
                            new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole()),
                            null,
                            List.of(authority)
                    );

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            securityErrorWriter.writeUnauthorized(request, response, "Access token is invalid or expired");
            return;
        } catch (DataAccessException exception) {
            SecurityContextHolder.clearContext();
            securityErrorWriter.writeServiceUnavailable(
                    request, response, "Authentication data is temporarily unavailable");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
