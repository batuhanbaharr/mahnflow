package com.mahnflow.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

// demo helper. gives you a ready jwt so you can call the api with curl/postman
// without setting up a real login server. turned off in prod (see application.yml).
@RestController
@ConditionalOnProperty(name = "mahnflow.dev-token.enabled", havingValue = "true")
public class DevTokenController {

    private final JwtEncoder encoder;

    public DevTokenController(JwtEncoder encoder) {
        this.encoder = encoder;
    }

    // example: GET /dev/token?role=ADMIN
    @GetMapping("/dev/token")
    public Map<String, String> token(@RequestParam(defaultValue = "ACCOUNTANT") String role) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("mahnflow-dev")
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofHours(8)))
                .subject("demo-user")
                .claim("roles", List.of(role))
                .build();
        // i have to set HS256 myself here. took me ages to figure out why token
        // minting failed: the nimbus encoder defaults to RS256, but my key is a
        // shared secret (HMAC), so i must tell it to use HS256 explicitly.
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return Map.of("token", value, "role", role);
    }
}
