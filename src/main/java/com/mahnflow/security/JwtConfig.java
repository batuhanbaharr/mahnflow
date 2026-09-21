package com.mahnflow.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

// for the demo i sign tokens with one shared secret (HMAC), so you don't need
// keycloak or auth0 just to try the api. if i did this for real i'd point the
// decoder at an identity provider with issuer-uri instead — the secret approach
// is definitely not production safe. see docs/adr/0004.
@Configuration
public class JwtConfig {

    private final SecretKeySpec key;

    public JwtConfig(@Value("${mahnflow.jwt.secret}") String secret) {
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    // a tiny encoder so the dev token endpoint can mint test tokens.
    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }
}
