package org.unicam.intermediate.controller;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.unicam.intermediate.models.LocationArea;
import org.unicam.intermediate.models.dto.MovementResponse;
import org.unicam.intermediate.service.ParticipantPositionService;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/movement")
@Slf4j
public class MovementController {

    private final Map<String, LocationArea> destinations = Map.of(
            "CS@Unicam", new LocationArea(
                    13.068471,
                    13.069082,
                    43.139401,
                    43.139775
            )
    );

    private final RuntimeService runtimeService;

    private final RepositoryService repositoryService;

    private final ParticipantPositionService positionService;

    private final IdentityService identityService;

    public MovementController(RuntimeService runtimeService, RepositoryService repositoryService, ParticipantPositionService positionService, IdentityService identityService) {
        this.runtimeService = runtimeService;
        this.repositoryService = repositoryService;
        this.positionService = positionService;
        this.identityService = identityService;
    }

    @PostMapping("/gps")
    public ResponseEntity<MovementResponse> receiveGpsByUser(
            @RequestParam("userId") String userId,
            @RequestParam("lat") double lat,
            @RequestParam("lon") double lon
    ) {
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
                    log.info("[GPS Received] Participant not authorized. participantID: {}", userId);
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
                    LocationArea box = destinations.get(destinationKey);
                    if (box == null) continue;

                    if (box.contains(lat, lon)) {
                        positionService.updatePosition(userId, lat, lon, destinationKey);
                        runtimeService.signal(execution.getId());

                        log.info("[GPS Received] Valid coordinate. participantID: {}", userId);
                        return ResponseEntity.ok(
                                new MovementResponse(true, "Device is inside the area", destinationKey, userId, null)
                        );
                    }
                }
            }
        }

        log.info("[GPS Received] Device is NOT inside the area. participantID: {}", userId);
        return ResponseEntity.ok(
                new MovementResponse(false, "Device is NOT inside the area", null, userId, null)
        );
    }

}
