// src/main/java/org/unicam/intermediate/service/participant/UserParticipantMappingService.java

package org.unicam.intermediate.service.participant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class UserParticipantMappingService {

    // businessKey -> userId -> participantId
    private final Map<String, Map<String, String>> mappings = new ConcurrentHashMap<>();

    // userId -> active tracking context
    private final Map<String, TrackingContext> activeTracking = new ConcurrentHashMap<>();

    /**
     * Registra quale participant un user sta impersonando per un dato businessKey
     */
    public void registerUserAsParticipant(String businessKey, String userId, String participantId) {
        mappings.computeIfAbsent(businessKey, k -> new ConcurrentHashMap<>())
                .put(userId, participantId);

        log.info("[UserMapping] Registered user {} as participant {} for businessKey {}",
                userId, participantId, businessKey);
    }

    /**
     * Set active tracking for a user (quando seleziona un task)
     */
    public void setActiveTracking(String userId, String taskId, String processInstanceId,
                                  String businessKey, String participantId, String participantName) {
        TrackingContext context = new TrackingContext(
                taskId, processInstanceId, businessKey, participantId, participantName, Instant.now()
        );
        activeTracking.put(userId, context);

        // Also register the mapping
        if (businessKey != null && participantId != null) {
            registerUserAsParticipant(businessKey, userId, participantId);
        }

        log.info("[UserMapping] Set active tracking for user {} -> participant {}", userId, participantId);
    }

    /**
     * Get active tracking context for a user
     */
    public TrackingContext getActiveTracking(String userId) {
        return activeTracking.get(userId);
    }

    /**
     * Clear tracking for a user
     */
    public void clearTracking(String userId) {
        activeTracking.remove(userId);
    }

    /**
     * Ottieni il participantId per un user in un dato businessKey
     */
    public String getParticipantIdForUser(String businessKey, String userId) {
        // Prima controlla il tracking attivo
        TrackingContext tracking = activeTracking.get(userId);
        if (tracking != null && tracking.getBusinessKey().equals(businessKey)) {
            return tracking.getParticipantId();
        }

        // Poi controlla i mapping salvati
        Map<String, String> businessKeyMappings = mappings.get(businessKey);
        if (businessKeyMappings != null) {
            return businessKeyMappings.get(userId);
        }

        return null;
    }

    @Data
    @AllArgsConstructor
    public static class TrackingContext {
        private String taskId;
        private String processInstanceId;
        private String businessKey;
        private String participantId;
        private String participantName;
        private Instant startedAt;
    }
}