package org.unicam.intermediate.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.unicam.intermediate.models.dto.websocket.GpsMessage;
import org.unicam.intermediate.models.dto.websocket.GpsResponse;
import org.unicam.intermediate.models.record.MovementResponse;
import org.unicam.intermediate.service.environmental.movement.GpsProcessingService;
import org.unicam.intermediate.service.websocket.WebSocketSessionManager;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class GpsWebSocketHandler extends TextWebSocketHandler {

    private final GpsProcessingService gpsProcessingService;
    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;
    private final RuntimeService runtimeService;

    private final ScheduledExecutorService heartbeatExecutor = Executors.newScheduledThreadPool(1);
    private final ConcurrentHashMap<String, Long> lastActivity = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            String userId = getUserId(session);
            if (userId == null) {
                log.error("[GPS WS] No userId in session attributes");
                session.close(CloseStatus.BAD_DATA.withReason("Missing userId"));
                return;
            }

            sessionManager.addSession(userId, session);
            lastActivity.put(session.getId(), System.currentTimeMillis());

            // Send welcome message
            GpsResponse welcome = GpsResponse.success("CONNECTION",
                    "Connected to GPS tracking service",
                    Map.of("sessionId", session.getId(), "userId", userId));
            sendMessage(session, welcome);

            log.info("[GPS WS] Connection established - userId: {}, sessionId: {}",
                    userId, session.getId());

            // Start heartbeat monitoring
            scheduleHeartbeat(session);

        } catch (Exception e) {
            log.error("[GPS WS] Error in afterConnectionEstablished", e);
            session.close(CloseStatus.SERVER_ERROR.withReason("Server error: " + e.getMessage()));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String userId = getUserId(session);
        lastActivity.put(session.getId(), System.currentTimeMillis());

        try {
            String payload = message.getPayload();
            log.debug("[GPS WS] Received from {}: {}", userId, payload);

            GpsMessage gpsMessage = objectMapper.readValue(payload, GpsMessage.class);

            if (gpsMessage instanceof GpsMessage.LocationUpdate location) {
                handleLocationUpdate(session, userId, location);
            } else if (gpsMessage instanceof GpsMessage.Heartbeat) {
                handleHeartbeat(session, userId);
            } else if (gpsMessage instanceof GpsMessage.StartTracking start) {
                handleStartTracking(session, userId, start);
            } else if (gpsMessage instanceof GpsMessage.StopTracking stop) {
                handleStopTracking(session, userId, stop);
            } else {
                log.warn("[GPS WS] Unknown message type from {}: {}", userId, gpsMessage.getType());
                sendError(session, "UNKNOWN_TYPE", "Unknown message type: " + gpsMessage.getType());
            }


        } catch (Exception e) {
            log.error("[GPS WS] Error processing message from {}: {}", userId, e.getMessage(), e);
            sendError(session, "PROCESSING_ERROR", "Failed to process message: " + e.getMessage());
        }
    }

    private void handleLocationUpdate(WebSocketSession session, String userId, GpsMessage.LocationUpdate location)
            throws IOException {

        if (location.getLat() == null || location.getLon() == null) {
            sendError(session, "INVALID_LOCATION", "Missing latitude or longitude");
            return;
        }

        // Validate coordinates
        if (Math.abs(location.getLat()) > 90 || Math.abs(location.getLon()) > 180) {
            sendError(session, "INVALID_COORDINATES", "Invalid GPS coordinates");
            return;
        }

        log.info("[GPS WS] Location update from {}: ({}, {}) , processInstanceId: {}, businessKey: {}",
                userId, location.getLat(), location.getLon(),
                location.getProcessInstanceId(), location.getBusinessKey());

        try {
            MovementResponse response;

            // Three modes of operation:
            // 1. Process instance ID provided - update specific process instance
            // 2. Business key provided - find and update related process instances
            // 3. Neither provided - update all active movement tasks for user

            if (location.getProcessInstanceId() != null && !location.getProcessInstanceId().isBlank()) {
                // Update specific process instance
                response = gpsProcessingService.processLocationForProcess(
                        userId,
                        location.getLat(),
                        location.getLon(),
                        location.getProcessInstanceId());

            } else if (location.getBusinessKey() != null && !location.getBusinessKey().isBlank()) {
                // Find process instances by business key and update them
                response = processLocationByBusinessKey(
                        userId,
                        location.getLat(),
                        location.getLon(),
                        location.getBusinessKey());

            } else {
                // Update all active movement tasks for this user
                response = gpsProcessingService.processUserLocation(
                        userId,
                        location.getLat(),
                        location.getLon());
            }

            // Send response
            GpsResponse wsResponse = GpsResponse.success("LOCATION_PROCESSED",
                    response.message(),
                    Map.of(
                            "destination", response.destination() != null ? response.destination() : "",
                            "processInstanceId", response.processId() != null ? response.processId() : "",
                            "businessKey", location.getBusinessKey() != null ? location.getBusinessKey() : "",
                            "taskCompleted", response.success()
                    ));

            sendMessage(session, wsResponse);

            // Notify other sessions if task completed
            if (response.success()) {
                sessionManager.broadcastToUser(userId, wsResponse, session.getId());
            }

        } catch (Exception e) {
            log.error("[GPS WS] Failed to process location for {}: {}", userId, e.getMessage(), e);
            sendError(session, "PROCESSING_FAILED", "Failed to process location update");
        }
    }

    // Add this helper method to handle business key correlation
    private MovementResponse processLocationByBusinessKey(String userId, double lat, double lon, String businessKey) {
        // Find process instances with this business key
        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .active()
                .list();

        if (instances.isEmpty()) {
            log.warn("[GPS WS] No active process instances found for businessKey: {}", businessKey);
            return MovementResponse.noActiveTasks(userId);
        }

        // Process location for each matching instance
        for (ProcessInstance instance : instances) {
            MovementResponse response = gpsProcessingService.processLocationForProcess(
                    userId, lat, lon, instance.getProcessInstanceId());

            if (response.success()) {
                // Return first successful match
                return response;
            }
        }

        return MovementResponse.notInTargetArea(userId);
    }

    private void handleHeartbeat(WebSocketSession session, String userId) throws IOException {
        log.trace("[GPS WS] Heartbeat from userId: {}", userId);
        sendMessage(session, GpsResponse.ack("HEARTBEAT"));
    }

    private void handleStartTracking(WebSocketSession session, String userId, GpsMessage.StartTracking start)
            throws IOException {
        log.info("[GPS WS] Start tracking for userId: {}, processInstanceId: {}, businessKey: {}",
                userId, start.getProcessInstanceId(), start.getBusinessKey());

        // Store tracking preferences - use processInstanceId or businessKey
        String trackingKey = start.getProcessInstanceId() != null
                ? start.getProcessInstanceId()
                : start.getBusinessKey();

        if (trackingKey != null) {
            sessionManager.setTrackingProcess(userId, trackingKey);
        }

        GpsResponse response = GpsResponse.success("TRACKING_STARTED",
                "Started tracking for process: " + (start.getProcessInstanceId() != null
                        ? start.getProcessInstanceId()
                        : start.getBusinessKey()),
                Map.of("updateInterval", start.getUpdateInterval() != null ? start.getUpdateInterval() : 5));

        sendMessage(session, response);
    }

    private void handleStopTracking(WebSocketSession session, String userId, GpsMessage.StopTracking stop)
            throws IOException {
        log.info("[GPS WS] Stop tracking for userId: {}, processInstanceId: {}, businessKey: {}",
                userId, stop.getProcessInstanceId(), stop.getBusinessKey());

        sessionManager.clearTrackingProcess(userId);

        String stoppedProcess = stop.getProcessInstanceId() != null
                ? stop.getProcessInstanceId()
                : stop.getBusinessKey();

        sendMessage(session, GpsResponse.success("TRACKING_STOPPED",
                "Stopped tracking" + (stoppedProcess != null ? " for: " + stoppedProcess : ""),
                null));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String userId = getUserId(session);
        log.error("[GPS WS] Transport error for userId: {}", userId, exception);
        sessionManager.removeSession(userId, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = getUserId(session);
        sessionManager.removeSession(userId, session.getId());
        lastActivity.remove(session.getId());

        log.info("[GPS WS] Connection closed - userId: {}, status: {}", userId, status);
    }

    private void scheduleHeartbeat(WebSocketSession session) {
        heartbeatExecutor.scheduleWithFixedDelay(() -> {
            try {
                if (!session.isOpen()) {
                    return;
                }

                Long lastActivityTime = lastActivity.get(session.getId());
                if (lastActivityTime != null) {
                    long inactiveTime = System.currentTimeMillis() - lastActivityTime;

                    // Close if inactive for more than 60 seconds
                    if (inactiveTime > 60000) {
                        log.warn("[GPS WS] Closing inactive session: {}", session.getId());
                        session.close(CloseStatus.GOING_AWAY.withReason("Inactive"));
                    } else if (inactiveTime > 30000) {
                        // Send ping if inactive for more than 30 seconds
                        session.sendMessage(new PingMessage());
                    }
                }
            } catch (Exception e) {
                log.error("[GPS WS] Heartbeat error for session: {}", session.getId(), e);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    private String getUserId(WebSocketSession session) {
        return (String) session.getAttributes().get("userId");
    }

    private void sendMessage(WebSocketSession session, GpsResponse response) throws IOException {
        if (session.isOpen()) {
            String json = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(json));
        }
    }

    private void sendError(WebSocketSession session, String errorType, String message) throws IOException {
        sendMessage(session, GpsResponse.error(errorType, message));
    }
}