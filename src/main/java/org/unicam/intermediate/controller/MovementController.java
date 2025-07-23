package org.unicam.intermediate.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.unicam.intermediate.config.GlobalEnvironment;
import org.unicam.intermediate.models.pojo.Place;
import org.unicam.intermediate.models.dto.MovementResponse;
import org.unicam.intermediate.models.dto.Response;
import org.unicam.intermediate.service.ParticipantPositionService;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/movement")
@Slf4j
@AllArgsConstructor
public class MovementController {

    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final ParticipantPositionService positionService;
    private final IdentityService identityService;

    @PostMapping("/gps")
    public ResponseEntity<Response<MovementResponse>> receiveGpsByUser(
            @RequestParam("userId") String userId,
            @RequestParam("lat") double lat,
            @RequestParam("lon") double lon
    ) {
        try {
            if (userId == null || userId.isBlank()) {
                return ResponseEntity.badRequest().body(
                        Response.error("Missing or invalid userId")
                );
            }

            List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery().active().list();

            for (ProcessDefinition def : definitions) {
                BpmnModelInstance model = repositoryService.getBpmnModelInstance(def.getId());
                if (model == null) continue;

                boolean hasMovementTask = model.getModelElementsByType(Task.class)
                        .stream()
                        .anyMatch(task -> {
                            ExtensionElements ext = task.getExtensionElements();
                            if (ext == null) return false;

                            return ext.getElementsQuery()
                                    .list()
                                    .stream()
                                    .anyMatch(el ->
                                            "type".equals(el.getDomElement().getLocalName()) &&
                                                    "movement".equals(el.getDomElement().getTextContent())
                                    );
                        });

                if (!hasMovementTask) continue;

                // Qui c'è da rivedere sicuramente
                Collection<Process> processes = model.getModelElementsByType(Process.class);
                for (Process process : processes) {
                    boolean authorized = false;

                    String candidateUsers = process.getCamundaCandidateStarterUsers();
                    String candidateGroups = process.getCamundaCandidateStarterGroups();

                    if (candidateUsers != null) {
                        authorized = Arrays.stream(candidateUsers.split(","))
                                .anyMatch(u -> u.trim().equalsIgnoreCase(userId));
                    }

                    if (!authorized && candidateGroups != null) {
                        List<Group> userGroups = identityService.createGroupQuery()
                                .groupMember(userId)
                                .list();

                        Set<String> userGroupIds = userGroups.stream()
                                .map(Group::getId)
                                .collect(Collectors.toSet());

                        authorized = Arrays.stream(candidateGroups.split(","))
                                .map(String::trim)
                                .anyMatch(userGroupIds::contains);
                    }

                    if (!authorized) {
                        continue;
                    }

                    List<Execution> executions = runtimeService.createExecutionQuery()
                            .processDefinitionId(def.getId())
                            .activityId("Move_To_Destination")
                            .active()
                            .list();

                    for (Execution execution : executions) {
                        Object destObj = runtimeService.getVariable(execution.getId(), "destination");
                        if (destObj == null) continue;

                        String destinationKey = destObj.toString();

                        Optional<Place> maybePlace = GlobalEnvironment.getInstance()
                                .getData()
                                .getPlaces()
                                .stream()
                                .filter(p -> p.getId().equals(destinationKey))
                                .findFirst();

                        if (maybePlace.isEmpty()) continue;

                        Place place = maybePlace.get();

                        if (place.getLocationArea().contains(lat, lon)) {
                            positionService.updatePosition(userId, lat, lon, destinationKey);
                            runtimeService.signal(execution.getId());
                            log.info("[GPS] User {} entered area '{}'", userId, destinationKey);
                            MovementResponse response = new MovementResponse(true, "Device is inside the area", destinationKey, userId, null);
                            return ResponseEntity.ok(Response.ok(response));
                        }
                    }
                }
            }

            log.info("[GPS] Device NOT in area for user {}", userId);
            return ResponseEntity.ok(Response.ok(
                    new MovementResponse(false, "Device is NOT inside the area", null, userId, null)
            ));

        } catch (Exception ex) {
            log.error("[GPS ERROR] Exception while processing GPS data", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Response.error("Internal server error: " + ex.getMessage()));
        }
    }
}
