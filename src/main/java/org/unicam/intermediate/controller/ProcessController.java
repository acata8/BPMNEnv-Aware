package org.unicam.intermediate.controller;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.unicam.intermediate.models.dto.ProcessStartResponse;
import org.unicam.intermediate.models.dto.Response;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/process")
@Slf4j
public class ProcessController {

    private final RuntimeService runtimeService;
    private final TaskService taskService;

    public ProcessController(RuntimeService runtimeService, TaskService taskService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    @PostMapping("/start")
    public ResponseEntity<Response<ProcessStartResponse>> startProcess(
            @RequestParam("processId") String processId,
            @RequestParam("userId") String userId
    ) {
        try {
            if (processId == null || processId.isBlank() || userId == null || userId.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Response.error("processId and userId are required"));
            }

            ProcessInstance instance = runtimeService.startProcessInstanceByKey(processId, Map.of("userId", userId));
            log.info("[Process Started] userId={} pid={}", userId, instance.getProcessInstanceId());

            ProcessStartResponse response = new ProcessStartResponse(
                    instance.getProcessInstanceId(),
                    instance.getProcessDefinitionKey()
            );

            return ResponseEntity.ok(Response.ok(response));

        } catch (Exception e) {
            log.error("[Start Process] Failed to start process. processId={}, userId={}", processId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Response.error("Failed to start process: " + e.getMessage()));
        }
    }

    @GetMapping("/by-user")
    public ResponseEntity<Response<List<String>>> getProcessesByUser(@RequestParam("userId") String userId) {
        try {
            if (userId == null || userId.isBlank()) {
                return ResponseEntity.badRequest().body(Response.error("userId is required"));
            }

            List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
                    .variableValueEquals("userId", userId)
                    .active()
                    .list();

            List<String> pids = instances.stream()
                    .map(ProcessInstance::getProcessInstanceId)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Response.ok(pids));

        } catch (Exception e) {
            log.error("[Get Processes] Error retrieving processes for userId={}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Response.error("Failed to retrieve processes: " + e.getMessage()));
        }
    }

    @GetMapping("/tasks")
    public ResponseEntity<Response<List<Map<String, String>>>> getTasksByUser(@RequestParam("userId") String userId) {
        try {
            if (userId == null || userId.isBlank()) {
                return ResponseEntity.badRequest().body(Response.error("userId is required"));
            }

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

            return ResponseEntity.ok(Response.ok(taskSummaries));

        } catch (Exception e) {
            log.error("[Get Tasks] Error retrieving tasks for userId={}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Response.error("Failed to retrieve tasks: " + e.getMessage()));
        }
    }
}
