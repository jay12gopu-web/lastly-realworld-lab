package com.lastly.lab.service;

import com.lastly.lab.domain.UserAccount;
import com.lastly.lab.dto.*;
import com.lastly.lab.repository.UserAccountRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UserAccountRepository users;
    private final PasswordEncoder passwords;
    private final JwtEncoder jwtEncoder;
    private final boolean authEnabled;
    public AuthService(UserAccountRepository users, PasswordEncoder passwords, JwtEncoder jwtEncoder,
                       @Value("${app.auth.enabled:false}") boolean authEnabled) {
        this.users=users; this.passwords=passwords; this.jwtEncoder=jwtEncoder; this.authEnabled=authEnabled;
    }
    private String normalizedEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    @Transactional
    public RegisteredUserResponse register(RegisterRequest request) {
        if (!authEnabled) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authentication disabled");
        String email=normalizedEmail(request.email());
        if (users.existsByEmail(email)) throw new DomainException("Email already registered");
        UserAccount user=users.saveAndFlush(new UserAccount(email,passwords.encode(request.password())));
        return new RegisteredUserResponse(user.getId(), user.getEmail(), user.getRole());
    }
    @Transactional(readOnly=true)
    public AuthResponse login(LoginRequest request) {
        if (!authEnabled) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authentication disabled");
        UserAccount user=users.findByEmail(normalizedEmail(request.email()))
            .orElseThrow(this::badCredentials);
        if (!passwords.matches(request.password(),user.getPasswordHash())) throw badCredentials();
        Instant now=Instant.now();
        long ttlSeconds=900; // 15-minute access token; refresh tokens are later-phase work
        JwtClaimsSet claims=JwtClaimsSet.builder()
            .issuer("lastly-lab")
            .subject(String.valueOf(user.getId()))
            .issuedAt(now)
            .expiresAt(now.plus(ttlSeconds, ChronoUnit.SECONDS))
            .claim("roles",List.of(user.getRole()))
            .build();
        String token=jwtEncoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AuthResponse(token,"Bearer",ttlSeconds);
    }
    private ResponseStatusException badCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid email or password");
    }
}
