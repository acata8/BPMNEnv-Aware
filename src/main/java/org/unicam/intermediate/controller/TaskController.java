package org.unicam.intermediate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.unicam.intermediate.models.dto.Response;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    private final TaskService taskService;
    private final IdentityService identityService;

    @GetMapping("/getTasksByUser")
    public ResponseEntity<Response<List<Map<String, Object>>>> getTasksByUser(@RequestParam("userId") String userId) {
        try {
            if (userId == null || userId.isBlank()) {
                return ResponseEntity.badRequest().body(Response.error("userId is required"));
            }

            Set<String> seenTaskIds = new HashSet<>();
            List<Map<String, Object>> result = new ArrayList<>();

            // Gruppi dell'utente
            List<Group> userGroups = identityService.createGroupQuery()
                    .groupMember(userId)
                    .list();
            List<String> groupIds = userGroups.stream().map(Group::getId).toList();

            // Task assegnate
            taskService.createTaskQuery().taskAssignee(userId).active().list()
                    .forEach(task -> {
                        if (seenTaskIds.add(task.getId())) {
                            result.add(taskToMap(task));
                        }
                    });

            // Task candidate utente
            taskService.createTaskQuery().taskCandidateUser(userId).active().list()
                    .forEach(task -> {
                        if (seenTaskIds.add(task.getId())) {
                            result.add(taskToMap(task));
                        }
                    });

            // Task candidate gruppi
            for (String groupId : groupIds) {
                taskService.createTaskQuery().taskCandidateGroup(groupId).active().list()
                        .forEach(task -> {
                            if (seenTaskIds.add(task.getId())) {
                                result.add(taskToMap(task));
                            }
                        });
            }

            return ResponseEntity.ok(Response.ok(result));

        } catch (Exception e) {
            log.error("[Get Tasks] Error retrieving tasks for userId={}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Response.error("Failed to retrieve tasks: " + e.getMessage()));
        }
    }

    @PostMapping("/{taskId}/claim")
    public ResponseEntity<Response<String>> claimTask(
            @PathVariable("taskId") String taskId,
            @RequestParam("userId") String userId) {
        try {
            if (taskId == null || taskId.isBlank() || userId == null || userId.isBlank()) {
                return ResponseEntity.badRequest().body(Response.error("taskId and userId are required"));
            }

            taskService.claim(taskId, userId);
            return ResponseEntity.ok(Response.ok("Task " + taskId + " claimed by " + userId));

        } catch (Exception e) {
            log.error("[Claim Task] Failed to claim taskId={} by userId={}", taskId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Response.error("Failed to claim task: " + e.getMessage()));
        }
    }

    @PostMapping("/{taskId}/complete")
    public ResponseEntity<Response<String>> completeTask(
            @PathVariable("taskId") String taskId,
            @RequestBody(required = false) Map<String, Map<String, Object>> variables) {
        try {
            if (taskId == null || taskId.isBlank()) {
                return ResponseEntity.badRequest().body(Response.error("taskId is required"));
            }

            Map<String, Object> camundaVars = new HashMap<>();

            if (variables != null && variables.containsKey("variables")) {
                for (Map.Entry<String, Object> entry : variables.get("variables").entrySet()) {
                    Map<String, Object> varMap = (Map<String, Object>) entry.getValue();
                    if (varMap != null && varMap.containsKey("value")) {
                        camundaVars.put(entry.getKey(), varMap.get("value"));
                    }
                }
            }

            taskService.complete(taskId, Variables.fromMap(camundaVars));
            return ResponseEntity.ok(Response.ok("Task " + taskId + " completed"));

        } catch (Exception e) {
            log.error("[Complete Task] Failed to complete taskId={}", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Response.error("Failed to complete task: " + e.getMessage()));
        }
    }

    private Map<String, Object> taskToMap(Task task) {
        Map<String, Object> taskMap = new HashMap<>();
        taskMap.put("id", task.getId());
        taskMap.put("name", task.getName());
        taskMap.put("assignee", task.getAssignee());
        taskMap.put("processInstanceId", task.getProcessInstanceId());
        taskMap.put("taskDefinitionKey", task.getTaskDefinitionKey());
        return taskMap;
    }
}
