package com.mycompany.app.user.config;

import com.mycompany.app.user.util.JwtUtil;
import com.mycompany.app.user.repository.UserRepository;
import com.mycompany.app.user.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // No token → pass through (public endpoints permitted, secured ones will be rejected by Spring Security)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);

        try {
            String  email  = jwtUtil.extractEmail(jwt);
            Integer userId = jwtUtil.extractUserId(jwt);
            String  role   = jwtUtil.extractRole(jwt);

            User user = userRepository.findById(userId).orElse(null);
            if (user == null || !user.isEnabled()) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Account is disabled\"}");
                return;
            }

            String roleName = role != null ? "ROLE_" + role : "ROLE_USER";

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            List.of(new SimpleGrantedAuthority(roleName))
                    );

            authToken.setDetails(userId); // ✅ userId accessible in controller

            SecurityContextHolder.getContext().setAuthentication(authToken);

        } catch (Exception e) {
            // ✅ FIX: Token is present but invalid/expired → respond with 401 immediately.
            // Previously the filter silently cleared the context and fell through, causing
            // Spring Security to return 403 instead of 401. The frontend refresh logic only
            // triggers on 401, so the token was never refreshed and the user was logged out.
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Token expired or invalid\"}");
            return; // ✅ Stop the filter chain — don't continue to the controller
        }

        filterChain.doFilter(request, response);
    }
}