package org.unicam.intermediate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.unicam.intermediate.service.LogService;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    @Autowired
    private LogService logService;

    @GetMapping
    public ResponseEntity<List<String>> getLogs() {
        return ResponseEntity.ok(logService.getLogs());
    }

    @PostMapping("/clear")
    public ResponseEntity<Void> clearLogs() {
        logService.clear();
        return ResponseEntity.ok().build();
    }
}
