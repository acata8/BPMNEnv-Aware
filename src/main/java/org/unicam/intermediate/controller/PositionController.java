package org.unicam.intermediate.controller;

import org.unicam.intermediate.service.ParticipantPositionService;
import org.unicam.intermediate.models.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/position")
public class PositionController {

    @Autowired
    private ParticipantPositionService positionService;

    @GetMapping("/all")
    public ResponseEntity<Map<String, Coordinate>> getAllPositions() {
        return ResponseEntity.ok(positionService.getAllPositions());
    }

    @GetMapping("/{participantId}")
    public ResponseEntity<Coordinate> getPosition(@PathVariable("participantId") String participantId) {
        Coordinate coord = positionService.getPosition(participantId);
        if (coord == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(coord);
    }
}
