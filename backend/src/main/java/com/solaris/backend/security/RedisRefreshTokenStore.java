package com.solaris.backend.security;

import com.solaris.backend.exception.TokenStoreUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {
    private static final String KEY_PREFIX = "solaris:refresh:";
    // Comparing and deleting in one Redis script prevents two concurrent refresh requests from
    // successfully spending the same token.
    private static final DefaultRedisScript<Long> CONSUME_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    @Override
    public void store(String tokenId, Long userId, String tokenHash, Duration ttl) {
        try {
            // Redis receives only a digest, so the bearer token itself is not recoverable from the store.
            redisTemplate.opsForValue().set(key(tokenId), value(userId, tokenHash), ttl);
        } catch (DataAccessException exception) {
            throw new TokenStoreUnavailableException(exception);
        }
    }

    @Override
    public boolean consume(String tokenId, Long userId, String tokenHash) {
        try {
            Long removed = redisTemplate.execute(
                    CONSUME_SCRIPT,
                    List.of(key(tokenId)),
                    value(userId, tokenHash)
            );
            return removed != null && removed == 1L;
        } catch (DataAccessException exception) {
            throw new TokenStoreUnavailableException(exception);
        }
    }

    private String key(String tokenId) {
        return KEY_PREFIX + tokenId;
    }

    private String value(Long userId, String tokenHash) {
        return userId + ":" + tokenHash;
    }
}
