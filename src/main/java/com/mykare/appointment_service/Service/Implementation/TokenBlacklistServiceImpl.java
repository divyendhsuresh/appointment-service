package com.mykare.appointment_service.Service.Implementation;

import com.mykare.appointment_service.Service.Interface.TokenBlacklistService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistServiceImpl
        implements TokenBlacklistService {

    private final Map<String, Date> blacklistedTokens =
            new ConcurrentHashMap<>();

    @Override
    public void blacklistToken(String tokenId, Date expiration) {

        blacklistedTokens.put(tokenId, expiration);
    }

    @Override
    public boolean isBlacklisted(String tokenId) {

        if (tokenId == null) {
            return false;
        }

        Date expiration =
                blacklistedTokens.get(tokenId);

        if (expiration == null) {
            return false;
        }

        if (expiration.before(new Date())) {
            blacklistedTokens.remove(tokenId);
            return false;
        }

        return true;
    }

    @Override
    @Scheduled(fixedRate = 600000)
    public void removeExpiredTokens() {

        Instant now = Instant.now();

        blacklistedTokens.entrySet()
                .removeIf(entry ->
                        entry.getValue()
                                .toInstant()
                                .isBefore(now)
                );
    }
}