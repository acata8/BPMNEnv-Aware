package org.unicam.intermediate.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.unicam.intermediate.models.Coordinate;
import org.unicam.intermediate.models.dto.Response;
import org.unicam.intermediate.service.ParticipantPositionService;

import java.util.Map;

@RestController
@RequestMapping("/api/position")
@Slf4j
@AllArgsConstructor
public class PositionController {

    private final ParticipantPositionService positionService;

    @GetMapping("/all")
    public ResponseEntity<Response<Map<String, Coordinate>>> getAllPositions() {
        try {
            Map<String, Coordinate> all = positionService.getAllPositions();
            return ResponseEntity.ok(Response.ok(all));
        } catch (Exception e) {
            log.error("[Position API] Error getting all positions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Response.error("Internal server error while retrieving all positions"));
        }
    }

    @GetMapping("/{participantId}")
    public ResponseEntity<Response<Coordinate>> getPosition(@PathVariable("participantId") String participantId) {
        try {
            if (participantId == null || participantId.isBlank()) {
                log.warn("[Position API] Invalid participantId received");
                return ResponseEntity.badRequest()
                        .body(Response.error("Invalid participantId"));
            }

            Coordinate coord = positionService.getPosition(participantId);
            if (coord == null) {
                log.warn("[Position API] No coordinate found for participant id: {}", participantId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Response.error("No coordinate found for participant id: " + participantId));
            }

            log.info("[Position API] Found coordinate for participant id: {}", participantId);
            return ResponseEntity.ok(Response.ok(coord));

        } catch (Exception e) {
            log.error("[Position API] Error retrieving position for participant: {}", participantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Response.error("Internal server error while retrieving position"));
        }
    }
}
