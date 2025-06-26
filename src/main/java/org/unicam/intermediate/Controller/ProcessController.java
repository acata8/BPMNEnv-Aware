package org.unicam.intermediate.Controller;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/process")
public class ProcessController {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private org.camunda.bpm.engine.HistoryService historyService;


    @PostMapping("/start")
    public ResponseEntity<String> startProcess() {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("Process_0yudeo4");
        return ResponseEntity.ok(instance.getProcessInstanceId());
    }

    @GetMapping("/status")
    public ResponseEntity<String> getStatus(@RequestParam("pid") String pid) {
        HistoricProcessInstance instance = historyService
                .createHistoricProcessInstanceQuery()
                .processInstanceId(pid)
                .singleResult();

        if (instance == null) return ResponseEntity.ok("Process instance not found");
        if (instance.getEndTime() == null) return ResponseEntity.ok("ACTIVE or SUSPENDED");
        return ResponseEntity.ok("Ended at " + instance.getEndTime());
    }
}
