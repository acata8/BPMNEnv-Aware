package org.unicam.intermediate.controller;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final IdentityService identityService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getTasksByUser(@RequestParam("userId") String userId) {
        Set<String> seenTaskIds = new HashSet<>();
        List<Map<String, Object>> result = new ArrayList<>();

        // Recupera gruppi a cui l'utente appartiene
        List<Group> userGroups = identityService.createGroupQuery()
                .groupMember(userId)
                .list();

        List<String> groupIds = userGroups.stream().map(Group::getId).collect(Collectors.toList());

        // Task assegnate direttamente
        taskService.createTaskQuery().taskAssignee(userId).active().list()
                .forEach(task -> {
                    if (seenTaskIds.add(task.getId())) {
                        result.add(taskToMap(task));
                    }
                });

        // Task candidate per utente
        taskService.createTaskQuery().taskCandidateUser(userId).active().list()
                .forEach(task -> {
                    if (seenTaskIds.add(task.getId())) {
                        result.add(taskToMap(task));
                    }
                });

        // Task candidate per gruppo
        for (String groupId : groupIds) {
            taskService.createTaskQuery().taskCandidateGroup(groupId).active().list()
                    .forEach(task -> {
                        if (seenTaskIds.add(task.getId())) {
                            result.add(taskToMap(task));
                        }
                    });
        }

        return ResponseEntity.ok(result);
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

    /**
     * POST /api/tasks/{taskId}/claim?userId=demo
     */
    @PostMapping("/{taskId}/claim")
    public ResponseEntity<String> claimTask(@PathVariable String taskId, @RequestParam String userId) {
        taskService.claim(taskId, userId);
        return ResponseEntity.ok("Task " + taskId + " claimed by " + userId);
    }

    /**
     * POST /api/tasks/{taskId}/complete
     * Body: { "variables": { "isCritical": { "value": true, "type": "Boolean" } } }
     */
    @PostMapping("/{taskId}/complete")
    public ResponseEntity<String> completeTask(
            @PathVariable String taskId,
            @RequestBody(required = false) Map<String, Map<String, Object>> variables
    ) {
        Map<String, Object> camundaVars = new HashMap<>();

        if (variables != null && variables.containsKey("variables")) {
            for (Map.Entry<String, Object> entry : variables.get("variables").entrySet()) {
                Map<String, Object> varMap = (Map<String, Object>) entry.getValue();
                camundaVars.put(entry.getKey(), varMap.get("value"));
            }
        }

        taskService.complete(taskId, Variables.fromMap(camundaVars));
        return ResponseEntity.ok("Task " + taskId + " completed");
    }
}
