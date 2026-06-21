package com.mykare.appointment_service.Service.Interface;

import java.util.Date;

public interface TokenBlacklistService {

    void blacklistToken(String tokenId, Date expiration);

    boolean isBlacklisted(String tokenId);

    void removeExpiredTokens();
}