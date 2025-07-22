package org.unicam.intermediate.controller;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.unicam.intermediate.models.dto.ProcessStartResponse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/process")
@Slf4j
public class ProcessController {

    private final RuntimeService runtimeService;

    private final TaskService taskService;

    public ProcessController(RuntimeService runtimeService,  TaskService taskService) {
        this.runtimeService = runtimeService;
        this.taskService =  taskService;
    }

    @PostMapping("/start")
    public ResponseEntity<ProcessStartResponse> startProcess(
            @RequestParam("processId") String processId,
            @RequestParam("userId") String userId
    ) {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(processId, Map.of("userId", userId));
        log.info("[Process Started] userId={} pid={}", userId, instance.getProcessInstanceId());

        return ResponseEntity.ok(
                new ProcessStartResponse(instance.getProcessInstanceId(), instance.getProcessDefinitionKey())
        );
    }

    @GetMapping("/by-user")
    public ResponseEntity<List<String>> getProcessesByUser(@RequestParam("userId") String userId) {
        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
                .variableValueEquals("userId", userId)
                .active()
                .list();

        List<String> pids = instances.stream()
                .map(ProcessInstance::getProcessInstanceId)
                .collect(Collectors.toList());

        return ResponseEntity.ok(pids);
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<Map<String, String>>> getTasksByUser(@RequestParam("userId") String userId) {
        List<Task> tasks = taskService.createTaskQuery()
                .processVariableValueEquals("userId", userId)
                .active()
                .list();

        List<Map<String, String>> taskSummaries = tasks.stream()
                .map(task -> Map.of(
                        "taskId", task.getId(),
                        "name", task.getName(),
                        "activityId", task.getTaskDefinitionKey(),
                        "pid", task.getProcessInstanceId()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(taskSummaries);
    }




}
