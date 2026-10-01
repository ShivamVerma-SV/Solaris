package com.solaris.backend.security;

import java.time.Duration;

public interface RefreshTokenStore {
    void store(String tokenId, Long userId, String tokenHash, Duration ttl);

    boolean consume(String tokenId, Long userId, String tokenHash);
}
