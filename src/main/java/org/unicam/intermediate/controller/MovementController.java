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
import org.unicam.intermediate.models.pojo.Place;
import org.unicam.intermediate.models.dto.MovementResponse;
import org.unicam.intermediate.models.dto.Response;
import org.unicam.intermediate.service.MovementService;
import org.unicam.intermediate.service.ParticipantPositionService;
import org.unicam.intermediate.service.xml.AbstractXmlService;
import org.unicam.intermediate.service.xml.XmlServiceDispatcher;

import java.util.List;
import java.util.Optional;

import static org.unicam.intermediate.utils.Constants.SPACE_NS;

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
            // 1) tutte le process definition attive
            var definitions = repositoryService
                    .createProcessDefinitionQuery()
                    .active()
                    .list();

            for (var def : definitions) {
                // 2) prendo tutti i task movement per questa definition
                List<String> movementTaskIds = movementService.getMovementTasks(def);
                if (movementTaskIds.isEmpty()) {
                    continue;
                }

                // 3) ottengo le Execution attive su quei task *autorizzate per userId*
                List<Execution> executions = movementService
                        .findActiveExecutionsForActivities(def.getId(), movementTaskIds, userId);

                // 4) per ogni execution, leggo la variabile dinamica taskId.localName
                for (Execution exe : executions) {
                    // (a) recupero l'activityId vero dalla execution
                    List<String> activeIds = runtimeService
                            .getActiveActivityIds(exe.getId());
                    if (activeIds.isEmpty()) {
                        continue;
                    }
                    String taskId = activeIds.get(0);

                    // (b) prendo il servizio basato su space:type ("movement")
                    AbstractXmlService xmlSvc = xmlServiceDispatcher
                            .get(SPACE_NS.getNamespaceUri(),
                                    xmlServiceDispatcher
                                            .get(SPACE_NS.getNamespaceUri(), "movement")
                                            .getTypeKey());

                    // (c) costruisco la chiave dinamica: "Activity_0c0m8b5.destination"
                    String varKey = taskId + "." + xmlSvc.getLocalName();
                    String destinationKey = (String)
                            runtimeService.getVariable(exe.getId(), varKey);

                    if (destinationKey == null) {
                        log.warn("[GPS] no variable {} on execution {}", varKey, exe.getId());
                        continue;
                    }

                    // (d) verifico se il place esiste e l'utente è entrato nell'area
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
                        log.info("[GPS] User {} entered area '{}'", userId, destinationKey);
                        runtimeService.signal(exe.getId());

                        MovementResponse resp = new MovementResponse(
                                true,
                                "Device is inside the area",
                                destinationKey,
                                userId,
                                null
                        );
                        return ResponseEntity.ok(Response.ok(resp));
                    }
                }
            }

            log.info("[GPS] Device NOT in area for user {}", userId);
            MovementResponse resp = new MovementResponse(
                    false,
                    "Device is NOT inside the area",
                    null,
                    userId,
                    null
            );
            return ResponseEntity.ok(Response.ok(resp));

        } catch (Exception ex) {
            log.error("[GPS ERROR] Exception while processing GPS data", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Response.error("Internal server error: " + ex.getMessage()));
        }
    }
}
