package org.unicam.intermediate.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.unicam.intermediate.models.WaitingBinding;
import org.unicam.intermediate.models.dto.websocket.GpsMessage;
import org.unicam.intermediate.models.dto.websocket.GpsResponse;
import org.unicam.intermediate.models.environmental.Coordinate;
import org.unicam.intermediate.models.pojo.Place;
import org.unicam.intermediate.service.environmental.BindingService;
import org.unicam.intermediate.service.environmental.EnvironmentDataService;
import org.unicam.intermediate.service.environmental.LocationEventService;
import org.unicam.intermediate.service.environmental.ProximityService;
import org.unicam.intermediate.service.participant.ParticipantPositionService;
import org.unicam.intermediate.service.participant.UserParticipantMappingService;
import org.unicam.intermediate.service.websocket.WebSocketSessionManager;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class GpsWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;
    private final ParticipantPositionService positionService;
    private final UserParticipantMappingService userParticipantMapping;
    private final LocationEventService locationEventService;
    private final RuntimeService runtimeService;
    private final EnvironmentDataService environmentDataService;
    private final BindingService bindingService;
    private final ProximityService proximityService;

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

            // Send simple welcome
            Map<String, Object> statusData = Map.of(
                    "sessionId", session.getId(),
                    "userId", userId
            );

            GpsResponse welcome = GpsResponse.success("CONNECTION",
                    "Connected to GPS tracking service", statusData);
            sendMessage(session, welcome);

            log.info("[GPS WS] Connection established - userId: {}, sessionId: {}",
                    userId, session.getId());

            scheduleHeartbeat(session);

        } catch (Exception e) {
            log.error("[GPS WS] Error in afterConnectionEstablished", e);
            session.close(CloseStatus.SERVER_ERROR.withReason("Server error: " + e.getMessage()));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String userId = getUserId(session);
        String businessKey = getBusinessKey(session); // Prendi dalla sessione
        lastActivity.put(session.getId(), System.currentTimeMillis());

        try {
            String payload = message.getPayload();
            log.debug("[GPS WS] Received from {} (BK: {}): {}", userId, businessKey, payload);

            GpsMessage gpsMessage = objectMapper.readValue(payload, GpsMessage.class);

            if (gpsMessage instanceof GpsMessage.LocationUpdate location) {
                handleLocationUpdate(session, userId, businessKey, location);
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

    private void handleLocationUpdate(WebSocketSession session, String userId, String businessKey,
                                      GpsMessage.LocationUpdate location) throws IOException {

        if (location.getLat() == null || location.getLon() == null) {
            sendError(session, "INVALID_LOCATION", "Missing latitude or longitude");
            return;
        }

        if (Math.abs(location.getLat()) > 90 || Math.abs(location.getLon()) > 180) {
            sendError(session, "INVALID_COORDINATES", "Invalid GPS coordinates");
            return;
        }

        log.info("[GPS WS] Location update - User: {}, BK: {}, Location: ({}, {})",
                userId, businessKey, location.getLat(), location.getLon());

        try {
            // Use businessKey from location if provided, otherwise from session
            if (location.getBusinessKey() != null && !location.getBusinessKey().isBlank()) {
                businessKey = location.getBusinessKey();
            }

            // Process location for ALL active tasks with this businessKey
            Map<String, Object> result = processLocationForBusinessKey(
                    userId,
                    businessKey,
                    location.getLat(),
                    location.getLon()
            );

            // Send response
            GpsResponse wsResponse = GpsResponse.success("LOCATION_PROCESSED",
                    "Location update processed", result);

            sendMessage(session, wsResponse);

        } catch (Exception e) {
            log.error("[GPS WS] Failed to process location: {}", e.getMessage(), e);
            sendError(session, "PROCESSING_FAILED", "Failed to process location update");
        }
    }

    private Map<String, Object> processLocationForBusinessKey(String userId, String businessKey,
                                                              double lat, double lon) {
        Map<String, Object> result = new HashMap<>();

        if (businessKey == null || businessKey.isBlank()) {
            log.warn("[GPS WS] No businessKey provided");
            result.put("error", "No businessKey provided");
            return result;
        }

        String participantId = userParticipantMapping.getParticipantIdForUser(businessKey, userId);

        if (participantId == null) {
            // Se non c'è ancora mapping, usa userId come fallback
            log.debug("[GPS WS] No participant mapping yet for user {} in BK {}, using userId",
                    userId, businessKey);
            participantId = userId;
        } else {
            log.debug("[GPS WS] User {} is participant {} for BK {}",
                    userId, participantId, businessKey);
        }

        // Aggiorna posizione per il participant
        String currentPlace = updatePosition(participantId, lat, lon);
        result.put("currentPlace", currentPlace);
        result.put("participantId", participantId);

        // Process ALL types of tasks for this businessKey
        List<String> triggeredEvents = new ArrayList<>();

        // 1. Check movement tasks
        boolean movementCompleted = checkAndSignalMovementTasks(businessKey, userId, lat, lon);
        if (movementCompleted) {
            triggeredEvents.add("MOVEMENT_COMPLETED");
        }

        // 2. Check binding readiness
        boolean bindingReady = checkAndSignalBindings(businessKey, userId);
        if (bindingReady) {
            triggeredEvents.add("BINDING_READY");
        }

        // 3. Check unbinding readiness
        boolean unbindingReady = checkAndSignalUnbindings(businessKey, userId);
        if (unbindingReady) {
            triggeredEvents.add("UNBINDING_READY");
        }

        result.put("triggeredEvents", triggeredEvents);
        result.put("userId", userId);
        result.put("businessKey", businessKey);
        result.put("location", Map.of("lat", lat, "lon", lon));

        log.info("[GPS WS] Processed location for BK {}: triggered {}", businessKey, triggeredEvents);

        return result;
    }

    private String updatePosition(String participantId, double lat, double lon) {
        Optional<Place> place = environmentDataService.findPlaceContainingLocation(lat, lon);
        String placeId = place.map(Place::getId).orElse(null);

        // Aggiorna posizione per il participant
        positionService.updatePosition(participantId, lat, lon, placeId);

        log.trace("[GPS WS] Updated position for participant {} in place: {}",
                participantId, placeId);

        return placeId;
    }


    private boolean checkAndSignalMovementTasks(String businessKey, String userId, double lat, double lon) {
        // Find all active movement tasks for this businessKey
        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .active()
                .list();

        for (ProcessInstance pi : instances) {
            List<Execution> executions = runtimeService.createExecutionQuery()
                    .processInstanceId(pi.getProcessInstanceId())
                    .active()
                    .list();

            for (Execution exe : executions) {
                List<String> activityIds = runtimeService.getActiveActivityIds(exe.getId());

                for (String activityId : activityIds) {
                    // Check if this is a movement task with a destination
                    String destinationKey = activityId + ".destination";
                    Object destination = runtimeService.getVariable(exe.getId(), destinationKey);

                    if (destination != null) {
                        String destId = destination.toString();

                        // Check if we're in the destination
                        if (environmentDataService.isLocationInPlace(lat, lon, destId)) {
                            log.info("[GPS WS] MOVEMENT COMPLETED - User {} reached {} for task {}",
                                    userId, destId, activityId);

                            // Signal the execution
                            runtimeService.signal(exe.getId());
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private boolean checkAndSignalBindings(String businessKey, String userId) {
        List<WaitingBinding> waitingBindings = bindingService.getAllWaitingBindings();

        for (WaitingBinding wb : waitingBindings) {
            if (!wb.getBusinessKey().equals(businessKey)) continue;

            // Check if participants are in same place
            Place bindingPlace = proximityService.getBindingPlace(
                    wb.getCurrentParticipantId(),
                    wb.getTargetParticipantId());

            if (bindingPlace != null) {
                // Find matching waiting binding
                Optional<WaitingBinding> match = bindingService.findWaitingBinding(
                        businessKey, wb.getTargetParticipantId());

                if (match.isPresent()) {
                    log.info("[GPS WS] BINDING READY - Participants in same place: {}",
                            bindingPlace.getName());

                    // Signal both
                    bindingService.removeWaitingBinding(businessKey, wb.getCurrentParticipantId());
                    bindingService.removeWaitingBinding(businessKey, wb.getTargetParticipantId());

                    runtimeService.signal(wb.getExecutionId());
                    runtimeService.signal(match.get().getExecutionId());

                    return true;
                }
            }
        }

        return false;
    }

    private boolean checkAndSignalUnbindings(String businessKey, String userId) {
        // Similar to bindings but for unbinding
        List<WaitingBinding> waitingUnbindings = bindingService.getAllWaitingUnbindings();

        for (WaitingBinding wu : waitingUnbindings) {
            if (!wu.getBusinessKey().equals(businessKey)) continue;

            Place unbindingPlace = proximityService.getBindingPlace(
                    wu.getCurrentParticipantId(),
                    wu.getTargetParticipantId());

            if (unbindingPlace != null) {
                Optional<WaitingBinding> match = bindingService.findWaitingUnbinding(
                        businessKey, wu.getTargetParticipantId());

                if (match.isPresent()) {
                    log.info("[GPS WS] UNBINDING READY - Participants in same place: {}",
                            unbindingPlace.getName());

                    bindingService.removeWaitingUnbinding(businessKey, wu.getCurrentParticipantId());
                    bindingService.removeWaitingUnbinding(businessKey, wu.getTargetParticipantId());

                    runtimeService.signal(wu.getExecutionId());
                    runtimeService.signal(match.get().getExecutionId());

                    return true;
                }
            }
        }

        return false;
    }

    private String getBusinessKey(WebSocketSession session) {
        return (String) session.getAttributes().get("businessKey");
    }

    private void handleHeartbeat(WebSocketSession session, String userId) throws IOException {
        log.trace("[GPS WS] Heartbeat from userId: {}", userId);
        sendMessage(session, GpsResponse.ack("HEARTBEAT"));
    }

    private void handleStartTracking(WebSocketSession session, String userId, GpsMessage.StartTracking start)
            throws IOException {
        log.info("[GPS WS] Start tracking for userId: {}, businessKey: {}",
                userId, start.getBusinessKey());

        // Store tracking preferences
        if (start.getBusinessKey() != null) {
            sessionManager.setTrackingProcess(userId, start.getBusinessKey());
        }

        GpsResponse response = GpsResponse.success("TRACKING_STARTED",
                "Started tracking for business key: " + start.getBusinessKey(),
                Map.of("updateInterval", start.getUpdateInterval() != null ? start.getUpdateInterval() : 5));

        sendMessage(session, response);
    }

    private void handleStopTracking(WebSocketSession session, String userId, GpsMessage.StopTracking stop)
            throws IOException {
        log.info("[GPS WS] Stop tracking for userId: {}, businessKey: {}",
                userId, stop.getBusinessKey());

        sessionManager.clearTrackingProcess(userId);

        sendMessage(session, GpsResponse.success("TRACKING_STOPPED",
                "Stopped tracking for business key: " + stop.getBusinessKey(),
                null));
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

                    if (inactiveTime > 60000) {
                        log.warn("[GPS WS] Closing inactive session: {}", session.getId());
                        session.close(CloseStatus.GOING_AWAY.withReason("Inactive"));
                    } else if (inactiveTime > 30000) {
                        session.sendMessage(new PingMessage());
                    }
                }
            } catch (Exception e) {
                log.error("[GPS WS] Heartbeat error for session: {}", session.getId(), e);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
}