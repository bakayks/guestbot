package com.guestbot.service.auth;

import com.guestbot.core.entity.Owner;
import com.guestbot.core.entity.RefreshToken;
import com.guestbot.core.exception.GuestBotException;
import com.guestbot.repository.OwnerRepository;
import com.guestbot.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final OwnerRepository ownerRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TokenPair register(String email, String password, String name, String phone) {
        if (ownerRepository.existsByEmail(email)) {
            throw new GuestBotException("Email already registered: " + email);
        }

        Owner owner = new Owner();
        owner.setEmail(email);
        owner.setPasswordHash(passwordEncoder.encode(password));
        owner.setName(name);
        owner.setPhone(phone);
        owner = ownerRepository.save(owner);

        return generateTokenPair(owner);
    }

    @Transactional
    public TokenPair login(String email, String password) {
        Owner owner = ownerRepository.findByEmail(email)
            .orElseThrow(() -> new GuestBotException("Invalid credentials"));

        if (!passwordEncoder.matches(password, owner.getPasswordHash())) {
            throw new GuestBotException("Invalid credentials");
        }

        if (!owner.isActive()) {
            throw new GuestBotException("Account is disabled");
        }

        return generateTokenPair(owner);
    }

    @Transactional
    public TokenPair refresh(String refreshToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
            .orElseThrow(() -> new GuestBotException("Invalid refresh token"));

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(stored);
            throw new GuestBotException("Refresh token expired");
        }

        // Rotation: удаляем старый, создаем новый
        refreshTokenRepository.delete(stored);

        Owner owner = stored.getOwner();
        return generateTokenPair(owner);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
            .ifPresent(refreshTokenRepository::delete);
    }

    private TokenPair generateTokenPair(Owner owner) {
        String accessToken = jwtService.generateAccessToken(owner.getId(), owner.getEmail());
        String refreshTokenValue = jwtService.generateRefreshToken(owner.getId());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setOwner(owner);
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(30));
        refreshTokenRepository.save(refreshToken);

        return new TokenPair(accessToken, refreshTokenValue);
    }

    public record TokenPair(String accessToken, String refreshToken) {}
}
