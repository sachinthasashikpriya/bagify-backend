package com.mycompany.app.user.util;

import com.mycompany.app.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    // Secure random ≥256-bit key generated once per app start (dev-friendly)
    private static final SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // ─────────────────────────────────────────
    // TOKEN GENERATION
    // ─────────────────────────────────────────

    public String generateToken(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole() != null ? user.getRole().name() : null)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + 86_400_000L))                   // 24 hours
                .signWith(key)
                .compact();
    }

    // ─────────────────────────────────────────
    // TOKEN VALIDATION
    // ─────────────────────────────────────────

    public boolean isTokenValid(String token, String email) {
        final String extractedEmail = extractEmail(token);
        return extractedEmail.equals(email) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ─────────────────────────────────────────
    // CLAIM EXTRACTORS
    // ─────────────────────────────────────────

    // ✅ Extract email from subject
    public static String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // ✅ Extract userId from claims
    public static Integer extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Integer.class));
    }

    // ✅ Extract role from claims
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    // ✅ Extract expiration
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // ✅ Generic claim extractor — all methods above use this
    public static <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // ─────────────────────────────────────────
    // INTERNAL HELPERS
    // ─────────────────────────────────────────

    private static Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}