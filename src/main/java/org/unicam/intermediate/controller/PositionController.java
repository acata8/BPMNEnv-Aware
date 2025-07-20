package org.unicam.intermediate.controller;

import lombok.extern.slf4j.Slf4j;
import org.unicam.intermediate.service.ParticipantPositionService;
import org.unicam.intermediate.models.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/position")
@Slf4j
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
            log.warn("No coordinate found for participant id: {}", participantId);
            return ResponseEntity.notFound().build();
        }
        log.info("Found coordinate for participant id: {}", participantId);
        return ResponseEntity.ok(coord);
    }
}
