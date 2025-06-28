package org.unicam.intermediate.controller;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.unicam.intermediate.controller.dto.ProcessStartResponse;
import org.unicam.intermediate.controller.dto.ProcessStatusResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/process")
public class ProcessController {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    private final AtomicReference<String> lastPid = new AtomicReference<>();

    @PostMapping("/start")
    public ResponseEntity<ProcessStartResponse> startProcess(@RequestParam("processId") String processId) {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(processId);
        String pid = instance.getProcessInstanceId();
        lastPid.set(pid);
        return ResponseEntity.ok(new ProcessStartResponse(pid, instance.getProcessDefinitionKey()));
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@RequestParam("pid") String pid) {
        HistoricProcessInstance instance = historyService
                .createHistoricProcessInstanceQuery()
                .processInstanceId(pid)
                .singleResult();

        if (instance == null) {
            return ResponseEntity.status(404).body("Process instance not found");
        }

        String status = (instance.getEndTime() == null) ? "ACTIVE_OR_SUSPENDED" : "ENDED";
        return ResponseEntity.ok(new ProcessStatusResponse(pid, status, instance.getEndTime() != null ? instance.getEndTime().toInstant() : null));
    }

    @GetMapping("/last")
    public ResponseEntity<Map<String, String>> getLastPid() {
        String pid = lastPid.get();
        if (pid == null) {
            return ResponseEntity.status(404).body(Map.of("error", "No process started yet"));
        }
        return ResponseEntity.ok(Map.of("pid", pid));
    }
}
