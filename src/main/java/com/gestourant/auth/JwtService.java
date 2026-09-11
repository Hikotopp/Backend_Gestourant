package com.gestourant.auth;

import com.gestourant.user.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final byte[] key; private final long expiration;
    public JwtService(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration}") long expiration) { this.key = secret.getBytes(StandardCharsets.UTF_8); this.expiration = expiration; }
    public String generate(User user) { return Jwts.builder().setSubject(user.getEmail()).claim("username", user.getUsername()).claim("role", user.getRole().name()).setIssuedAt(new Date()).setExpiration(new Date(System.currentTimeMillis() + expiration)).signWith(Keys.hmacShaKeyFor(key)).compact(); }
    public io.jsonwebtoken.Claims parse(String token) { return Jwts.parserBuilder().setSigningKey(Keys.hmacShaKeyFor(key)).build().parseClaimsJws(token).getBody(); }
}
