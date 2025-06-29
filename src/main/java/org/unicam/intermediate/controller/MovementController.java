package org.unicam.intermediate.controller;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Participant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.unicam.intermediate.controller.dto.MovementResponse;
import org.unicam.intermediate.models.LocationArea;
import org.unicam.intermediate.service.LogService;
import org.unicam.intermediate.service.ParticipantPositionService;

import java.util.Map;

@RestController
@RequestMapping("/api/movement")
public class MovementController {

    private final Map<String, LocationArea> destinations = Map.of(
            "CS@Unicam", new LocationArea(
                    13.068471,
                    13.069082,
                    43.139401,
                    43.139775
            )
    );

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ParticipantPositionService positionService;

    @Autowired
    private LogService logService;

    @PostMapping("/gps")
    public ResponseEntity<MovementResponse> receiveGps(
            @RequestParam("pid") String pid,
            @RequestParam("lat") double lat,
            @RequestParam("lon") double lon,
            @RequestParam("participantId") String participantId,
            @RequestParam("activityId") String activityId) {

        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(pid)
                .activityId(activityId)
                .active()
                .singleResult();

        if (execution == null) {
            return ResponseEntity.status(404).body(
                    new MovementResponse(false, "Execution not found", null, participantId)
            );
        }

        String destinationKey = (String) runtimeService.getVariable(execution.getId(), "destination");
        LocationArea box = destinations.get(destinationKey);

        if (box != null && box.contains(lat, lon)) {

            ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(pid)
                    .singleResult();

            BpmnModelInstance modelInstance = repositoryService.getBpmnModelInstance(instance.getProcessDefinitionId());

            Participant participant = modelInstance
                    .getModelElementsByType(Participant.class)
                    .stream()
                    .filter(p -> p.getId().equals(participantId))
                    .findFirst()
                    .orElse(null);

            if (participant != null) {
                positionService.updatePosition(participantId, lat, lon, destinationKey);
        logService.addLog("[GPS Received] Valid coordinate. participantID: " + participantId);

        runtimeService.signal(execution.getId());

            logService.addLog("[Participant "+participantId+" position] Update " + positionService.getDestination(participantId));

                return ResponseEntity.ok(
                        new MovementResponse(true, "Device is inside the area", destinationKey, participantId)
                );
            }


        logService.addLog("[GPS Received] Correct position, participantID: " + participantId + "  not found");
            return ResponseEntity.status(404).body(
                    new MovementResponse(false, "Correct position, but participant not found", destinationKey, participantId)
            );

        }


        logService.addLog("[GPS Received] Device is NOT inside the area. participantID: " + participantId);
        return ResponseEntity.ok(
                new MovementResponse(false, "Device is NOT inside the area", destinationKey, participantId)
        );
    }


}
