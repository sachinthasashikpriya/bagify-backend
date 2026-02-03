
package com.mycompany.app.user.util;

import com.mycompany.app.user.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    // Secure random ≥256-bit key generated once per app start (dev-friendly)
    private final SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    public String generateToken(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("role", user.getRole() != null ? user.getRole().name() : null)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + 86_400_000L)) // 24h
                .signWith(key) // For JJWT 0.11.x, passing the SecretKey is enough
                .compact();
    }
}
