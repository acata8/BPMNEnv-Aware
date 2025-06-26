package org.unicam.intermediate.Controller;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.Execution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.unicam.intermediate.Utils.CoordinateBox;

import java.util.Map;

@RestController
@RequestMapping("/api/movement")
public class MovementController {

    private final Map<String, CoordinateBox> destinations = Map.of(
        "Position_T1", new CoordinateBox(10.0, 15.0, 20.0, 25.0)  // xMin, xMax, yMin, yMax
    );

    @Autowired
    private RuntimeService runtimeService;

    @PostMapping("/gps")
    public ResponseEntity<String> receiveGps(
            @RequestParam("pid") String pid,
            @RequestParam("lat") double lat,
            @RequestParam("lon") double lon) {

        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(pid)
                .activityId("Activity_1stc37q") // ID del task 'movement'
                .active()
                .singleResult();

        if (execution == null) {
            return ResponseEntity.status(404).body("Execution not found");
        }

        // Prendo la destinazione che ora è hardcodata
        String destinationKey = (String) runtimeService.getVariable(execution.getId(), "destination");
        CoordinateBox box = destinations.get(destinationKey);

        if (box != null && box.contains(lat, lon)) {
            runtimeService.signal(execution.getId());
            return ResponseEntity.ok("Device is inside the area");
        }

        return ResponseEntity.ok("Device is NOT inside the area");
    }

}
