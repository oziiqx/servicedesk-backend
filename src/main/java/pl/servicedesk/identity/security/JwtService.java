package pl.servicedesk.identity.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public AccessToken issueAccessToken(AuthenticatedUser user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        String value = Jwts.builder()
                .issuer(properties.issuer())
                .subject(String.valueOf(user.id()))
                .claim("email", user.getUsername())
                .claim("roles", user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
        return new AccessToken(value, expiresAt);
    }

    public Optional<AccessTokenClaims> readClaims(String token) {
        try {
            Jws<Claims> parsed = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(properties.issuer())
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token);
            Claims claims = parsed.getPayload();
            List<?> roles = claims.get("roles", List.class);
            List<String> authorities = roles == null ? List.of() : roles.stream().map(String::valueOf).toList();
            return Optional.of(new AccessTokenClaims(
                    Long.valueOf(claims.getSubject()),
                    claims.get("email", String.class),
                    authorities));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public record AccessToken(String value, Instant expiresAt) {
    }

    public record AccessTokenClaims(Long userId, String email, List<String> authorities) {
    }
}
