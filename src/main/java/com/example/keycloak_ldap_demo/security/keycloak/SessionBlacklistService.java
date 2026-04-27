package com.example.keycloak_ldap_demo.security.keycloak;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionBlacklistService {

    // sessionId → time it was revoked
    private final Map<String, Instant> revokedSessions = new ConcurrentHashMap<>();

    public void revokeSession(String sessionId) {
        revokedSessions.put(sessionId, Instant.now());
    }

    public boolean isRevoked(String sessionId) {
        return revokedSessions.containsKey(sessionId);
    }

    // Clean up entries older than 24 hours — they'd be expired anyway
    @Scheduled(fixedRate = 3_600_000)
    public void purgeOldEntries() {
        Instant cutoff = Instant.now().minusSeconds(86400);
        revokedSessions.entrySet()
                .removeIf(e -> e.getValue().isBefore(cutoff));
    }
}