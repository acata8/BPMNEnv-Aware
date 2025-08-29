package org.unicam.intermediate.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.Execution;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.unicam.intermediate.config.GlobalEnvironment;
import org.unicam.intermediate.models.enums.TaskType;
import org.unicam.intermediate.models.pojo.Place;
import org.unicam.intermediate.models.dto.MovementResponse;
import org.unicam.intermediate.models.dto.Response;
import org.unicam.intermediate.service.environmental.MovementService;
import org.unicam.intermediate.service.participant.ParticipantPositionService;
import org.unicam.intermediate.service.xml.AbstractXmlService;
import org.unicam.intermediate.service.xml.XmlServiceDispatcher;
import org.unicam.intermediate.utils.Constants;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/movement")
@Slf4j
@AllArgsConstructor
public class MovementController {

    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final MovementService movementService;
    private final ParticipantPositionService positionService;
    private final XmlServiceDispatcher xmlServiceDispatcher;

    @PostMapping("/gps")
    public ResponseEntity<Response<MovementResponse>> receiveGpsByUser(
            @RequestParam("userId") String userId,
            @RequestParam("lat") double lat,
            @RequestParam("lon") double lon
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Response.error("Missing or invalid userId"));
        }

        try {
            log.info("[GPS] RECEIVED | User: {} | Coordinates: ({}, {})", userId, lat, lon);

            var definitions = repositoryService
                    .createProcessDefinitionQuery()
                    .active()
                    .list();

            for (var def : definitions) {
                // Get all tasks with movement type dynamically
                List<String> movementTaskIds = movementService.getTasksOfType(def, "movement");
                if (movementTaskIds.isEmpty()) {
                    continue;
                }

                List<Execution> executions = movementService
                        .findActiveExecutionsForActivities(def.getId(), movementTaskIds, userId);

                for (Execution exe : executions) {
                    List<String> activeIds = runtimeService
                            .getActiveActivityIds(exe.getId());
                    if (activeIds.isEmpty()) {
                        continue;
                    }
                    String taskId = activeIds.get(0);

                    // Use dynamic XML service dispatch
                    AbstractXmlService xmlSvc = xmlServiceDispatcher
                            .get(Constants.SPACE_NS.getNamespaceUri(), TaskType.MOVEMENT);

                    String varKey = taskId + "." + (xmlSvc != null ? xmlSvc.getLocalName() : "destination");
                    String destinationKey = (String) runtimeService.getVariable(exe.getId(), varKey);

                    if (destinationKey == null) {
                        log.warn("[GPS] No destination variable {} on execution {}", varKey, exe.getId());
                        continue;
                    }

                    Optional<Place> placeOpt = GlobalEnvironment.getInstance()
                            .getData().getPlaces().stream()
                            .filter(p -> p.getId().equals(destinationKey))
                            .findFirst();

                    if (placeOpt.isEmpty()) {
                        continue;
                    }
                    Place place = placeOpt.get();

                    if (place.getLocationArea().contains(lat, lon)) {
                        positionService.updatePosition(userId, lat, lon, destinationKey);

                        // Direct GPS logging
                        log.info("[GPS] ENTERED AREA | User: {} | Coordinates: ({}, {}) | Place: {}",
                                userId, lat, lon, destinationKey);

                        MovementResponse resp = new MovementResponse(
                                true,
                                "Device is inside the area",
                                destinationKey,
                                userId,
                                exe.getProcessInstanceId()
                        );

                        log.info("[GPS] User {} entered area '{}' - preparing response", userId, destinationKey);

                        CompletableFuture.runAsync(() -> {
                            try {
                                Thread.sleep(50);
                                runtimeService.signal(exe.getId());
                                log.debug("[GPS] Execution {} signaled successfully", exe.getId());
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                log.error("[GPS] Signaling interrupted for execution {}: {}", exe.getId(), e.getMessage());
                            } catch (Exception e) {
                                log.error("[GPS] Error signaling execution {}: {}", exe.getId(), e.getMessage(), e);
                            }
                        });
                        log.info("[GPS] Movement response returning immediately for user {}", userId);
                        return ResponseEntity.ok(Response.ok(resp));
                    }
                }
            }
            log.info("[GPS] NOT IN AREA | User: {} | Coordinates: ({}, {}) | Place: unknown", userId, lat, lon);
            MovementResponse resp = new MovementResponse(
                    false,
                    "Device is NOT inside the area",
                    null,
                    userId,
                    null
            );
            return ResponseEntity.ok(Response.ok(resp));

        } catch (Exception ex) {
            log.error("[GPS ERROR] Exception while processing GPS data for user: {}", userId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Response.error("Internal server error: " + ex.getMessage()));
        }
    }
}