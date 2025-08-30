package org.unicam.intermediate.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.unicam.intermediate.models.dto.GpsRequest;
import org.unicam.intermediate.models.record.MovementResponse;
import org.unicam.intermediate.models.dto.Response;
import org.unicam.intermediate.service.environmental.movement.GpsProcessingService;

import java.util.List;

@RestController
@RequestMapping("/api/movement")
@Slf4j
@AllArgsConstructor
public class MovementController {

    private final GpsProcessingService gpsProcessingService;

    @PostMapping("/gps")
    public ResponseEntity<Response<MovementResponse>> receiveGps(
            @Valid @RequestBody GpsRequest request) {

        log.info("[GPS API] Received GPS data for user: {}", request.getUserId());

        MovementResponse response = gpsProcessingService.processUserLocation(
                request.getUserId(),
                request.getLat(),
                request.getLon()
        );

        return ResponseEntity.ok(Response.ok(response));
    }

    @PostMapping("/gps/process/{processId}")
    public ResponseEntity<Response<MovementResponse>> receiveGpsForProcess(
            @PathVariable String processId,
            @Valid @RequestBody GpsRequest request) {

        log.info("[GPS API] Received GPS data for user: {} in process: {}",
                request.getUserId(), processId);

        MovementResponse response = gpsProcessingService.processLocationForProcess(
                request.getUserId(),
                request.getLat(),
                request.getLon(),
                processId
        );

        return ResponseEntity.ok(Response.ok(response));
    }

    @GetMapping("/destinations/{userId}")
    public ResponseEntity<Response<List<String>>> getActiveDestinations(
            @PathVariable String userId) {

        List<String> destinations = gpsProcessingService.getActiveDestinations(userId);
        return ResponseEntity.ok(Response.ok(destinations));
    }

    @GetMapping("/status/{userId}")
    public ResponseEntity<Response<Boolean>> hasActiveMovementTasks(
            @PathVariable String userId) {

        boolean hasActive = gpsProcessingService.hasActiveMovementTasks(userId);
        return ResponseEntity.ok(Response.ok(hasActive));
    }
}