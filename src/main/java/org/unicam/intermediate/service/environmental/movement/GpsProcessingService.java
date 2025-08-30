package org.unicam.intermediate.service.environmental.movement;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.Execution;
import org.springframework.stereotype.Service;
import org.unicam.intermediate.models.record.MovementResponse;
import org.unicam.intermediate.models.enums.TaskType;
import org.unicam.intermediate.service.environmental.EnvironmentDataService;
import org.unicam.intermediate.service.participant.ParticipantPositionService;
import org.unicam.intermediate.service.xml.AbstractXmlService;
import org.unicam.intermediate.service.xml.XmlServiceDispatcher;
import org.unicam.intermediate.utils.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
@AllArgsConstructor
public class GpsProcessingService {

    private final RuntimeService runtimeService;
    private final MovementService movementService;
    private final EnvironmentDataService environmentDataService;
    private final ParticipantPositionService positionService;
    private final XmlServiceDispatcher xmlServiceDispatcher;

    // Thread pool for async execution signaling
    private static final ExecutorService executorService = Executors.newFixedThreadPool(5);

    /**
     * Main entry point for processing GPS coordinates from a user
     */
    public MovementResponse processUserLocation(String userId, double lat, double lon) {
        log.debug("[GPS Service] Processing location for user: {} at ({}, {})", userId, lat, lon);

        // Always update position first (for tracking/monitoring)
        updateUserPosition(userId, lat, lon);

        // Find and check active movement tasks
        List<MovementTask> activeTasks = findActiveMovementTasksForUser(userId);

        if (activeTasks.isEmpty()) {
            log.debug("[GPS Service] No active movement tasks for user: {}", userId);
            return MovementResponse.noActiveTasks(userId);
        }

        // Process each task - return first match
        for (MovementTask task : activeTasks) {
            if (isLocationMatchingDestination(lat, lon, task.getDestinationId())) {
                return handleLocationMatch(task, userId, lat, lon);
            }
        }

        // No matching destinations
        log.info("[GPS Service] User {} not in any target area. Active destinations: {}",
                userId, activeTasks.stream().map(MovementTask::getDestinationId).toList());

        return MovementResponse.notInTargetArea(userId);
    }

    /**
     * Process location for a specific process instance
     */
    public MovementResponse processLocationForProcess(String userId, double lat, double lon, String processInstanceId) {
        log.debug("[GPS Service] Processing location for user: {} in process: {}", userId, processInstanceId);

        updateUserPosition(userId, lat, lon);

        List<MovementTask> tasks = findMovementTasksForProcess(processInstanceId, userId);

        for (MovementTask task : tasks) {
            if (isLocationMatchingDestination(lat, lon, task.getDestinationId())) {
                return handleLocationMatch(task, userId, lat, lon);
            }
        }

        return MovementResponse.notInTargetArea(userId);
    }

    /**
     * Check if user has any active movement tasks
     */
    public boolean hasActiveMovementTasks(String userId) {
        return !findActiveMovementTasksForUser(userId).isEmpty();
    }

    /**
     * Get all destinations user needs to reach
     */
    public List<String> getActiveDestinations(String userId) {
        return findActiveMovementTasksForUser(userId).stream()
                .map(MovementTask::getDestinationId)
                .distinct()
                .toList();
    }

    // ========== Private Helper Methods ==========

    private void updateUserPosition(String userId, double lat, double lon) {
        // Find which place (if any) contains this location
        String currentPlace = environmentDataService.findPlaceContainingLocation(lat, lon)
                .map(place -> place.getId())
                .orElse(null);

        positionService.updatePosition(userId, lat, lon, currentPlace);
        log.trace("[GPS Service] Updated position for user {} to ({}, {}) in place: {}",
                userId, lat, lon, currentPlace);
    }

    private List<MovementTask> findActiveMovementTasksForUser(String userId) {
        List<MovementTask> tasks = new ArrayList<>();

        // Get all active process definitions
        var definitions = movementService.getActiveProcessDefinitionsWithMovementTasks();

        for (var def : definitions) {
            List<String> movementTaskIds = movementService.getTasksOfType(def, TaskType.MOVEMENT);
            if (movementTaskIds.isEmpty()) continue;

            // Find executions for these tasks that this user can access
            List<Execution> executions = movementService
                    .findActiveExecutionsForActivities(def.getId(), movementTaskIds, userId);

            for (Execution exe : executions) {
                MovementTask task = extractMovementTask(exe);
                if (task != null) {
                    tasks.add(task);
                }
            }
        }

        log.debug("[GPS Service] Found {} active movement tasks for user {}", tasks.size(), userId);
        return tasks;
    }

    private List<MovementTask> findMovementTasksForProcess(String processInstanceId, String userId) {
        List<MovementTask> tasks = new ArrayList<>();

        List<Execution> executions = runtimeService.createExecutionQuery()
                .processInstanceId(processInstanceId)
                .list();

        for (Execution exe : executions) {
            MovementTask task = extractMovementTask(exe);
            if (task != null && task.getDestinationId() != null) {
                tasks.add(task);
            }
        }

        return tasks;
    }

    private MovementTask extractMovementTask(Execution execution) {
        List<String> activeIds = runtimeService.getActiveActivityIds(execution.getId());
        if (activeIds.isEmpty()) {
            return null;
        }

        String taskId = activeIds.get(0);

        // Get destination from process variables
        AbstractXmlService xmlSvc = xmlServiceDispatcher
                .get(Constants.SPACE_NS.getNamespaceUri(), TaskType.MOVEMENT);

        if (xmlSvc == null) {
            log.warn("[GPS Service] No XML service found for movement tasks");
            return null;
        }

        String varKey = taskId + "." + xmlSvc.getLocalName();
        String destinationId = (String) runtimeService.getVariable(execution.getId(), varKey);

        if (destinationId == null) {
            log.trace("[GPS Service] No destination found for task {} in execution {}",
                    taskId, execution.getId());
            return null;
        }

        return new MovementTask(
                execution.getId(),
                execution.getProcessInstanceId(),
                taskId,
                destinationId,
                execution
        );
    }

    private boolean isLocationMatchingDestination(double lat, double lon, String destinationId) {
        boolean matches = environmentDataService.isLocationInPlace(lat, lon, destinationId);

        if (matches) {
            log.debug("[GPS Service] Location ({}, {}) matches destination: {}",
                    lat, lon, destinationId);
        }

        return matches;
    }

    private MovementResponse handleLocationMatch(MovementTask task, String userId, double lat, double lon) {
        log.info("[GPS Service] MATCH! User: {} entered area: {} | Task: {} | Process: {}",
                userId, task.getDestinationId(), task.getTaskId(), task.getProcessInstanceId());

        // Update position with the confirmed destination
        positionService.updatePosition(userId, lat, lon, task.getDestinationId());

        // Signal the execution to continue
        signalTaskCompletion(task);

        return new MovementResponse(
                true,
                "Device entered target area: " + task.getDestinationId(),
                task.getDestinationId(),
                userId,
                task.getProcessInstanceId()
        );
    }

    private void signalTaskCompletion(MovementTask task) {
        CompletableFuture.runAsync(() -> {
            try {
                // Small delay to ensure response is sent first
                Thread.sleep(50);

                log.debug("[GPS Service] Signaling execution {} for task {}",
                        task.getExecutionId(), task.getTaskId());

                runtimeService.signal(task.getExecutionId());

                log.info("[GPS Service] Successfully signaled task {} completion", task.getTaskId());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("[GPS Service] Interrupted while signaling execution {}", task.getExecutionId());
            } catch (Exception e) {
                log.error("[GPS Service] Failed to signal execution {} for task {}: {}",
                        task.getExecutionId(), task.getTaskId(), e.getMessage(), e);
            }
        }, executorService);
    }

    /**
     * Inner class to hold movement task data
     */
    @AllArgsConstructor
    @Getter
    private static class MovementTask {
        private final String executionId;
        private final String processInstanceId;
        private final String taskId;
        private final String destinationId;
        private final Execution execution;
    }
}