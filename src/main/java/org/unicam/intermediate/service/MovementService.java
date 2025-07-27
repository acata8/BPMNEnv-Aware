package org.unicam.intermediate.service;

import lombok.AllArgsConstructor;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class MovementService {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final IdentityService identityService;


    public boolean hasMovementTask(ProcessDefinition definition){
        BpmnModelInstance instance = repositoryService.getBpmnModelInstance(definition.getId());

        return instance
                .getModelElementsByType(Task.class)
                .stream()
                .anyMatch(task -> {
                    ExtensionElements ext = task.getExtensionElements();
                    if (ext == null) return false;
                    return ext.getElementsQuery().list().stream()
                            .anyMatch(el ->
                                    "type".equals(el.getDomElement().getLocalName()) &&
                                            "movement".equals(el.getDomElement().getTextContent().trim())
                            );
                });
    }

    public List<String> getMovementTasks(ProcessDefinition definition){

        BpmnModelInstance model = repositoryService.getBpmnModelInstance(definition.getId());

        return model.getModelElementsByType(Task.class).stream()
                .filter(task -> {
                    ExtensionElements ext = task.getExtensionElements();
                    if (ext == null) return false;
                    return ext.getElementsQuery().list().stream()
                            .anyMatch(el ->
                                    "type".equals(el.getDomElement().getLocalName())
                                            && "movement".equals(el.getDomElement().getTextContent().trim())
                            );
                })
                .map(Task::getId)
                .toList();
    }

    /**
     * Ritorna tutte le execution attive per la processDefinitionId
     * fermate su uno qualsiasi degli activityIds passati.
     */
    public List<Execution> findActiveExecutionsForActivities(
            String processDefinitionId,
            List<String> activityIds,
            String userId) {

        List<Execution> executions = new ArrayList<>();
        for (String taskId : activityIds) {
            List<Execution> execs = runtimeService.createExecutionQuery()
                    .processDefinitionId(processDefinitionId)
                    .activityId(taskId)
                    .active()
                    .list();

            for (Execution exe : execs) {
                // controllo l'autorizzazione **sul singolo task**
                if (!isUserAuthorizedOnTask(processDefinitionId, taskId, userId)) {
                    continue;
                }
                executions.add(exe);
            }
        }
        return executions;
    }

    /**
     * Controlla se l’utente può sbloccare il singolo task definito in BPMN:
     * legge camunda:candidateStarterUsers/Groups sull’elemento <task> in XML.
     *
     * @param processDefinitionId la definitionId del processo
     * @param taskId              l'id del task (movementTaskId)
     * @param userId              l'utente da verificare
     * @return true se l’utente è in candidateStarterUsers o in uno dei candidateStarterGroups,
     *         o se nessuno di questi attributi è presente sul task
     */
    private boolean isUserAuthorizedOnTask(String processDefinitionId,
                                           String taskId,
                                           String userId) {

        BpmnModelInstance model = repositoryService.getBpmnModelInstance(processDefinitionId);
        ModelElementInstance elem = model.getModelElementById(taskId);
        if (!(elem instanceof UserTask userTask)) {
            return true;
        }

        // candidateUsers sul UserTask
        String users = userTask.getCamundaCandidateUsers();
        if (users != null && !users.isBlank()) {
            return Arrays.stream(users.split(","))
                    .map(String::trim)
                    .anyMatch(u -> u.equalsIgnoreCase(userId));
        }

        // candidateGroups sul UserTask
        String groups = userTask.getCamundaCandidateGroups();
        if (groups != null && !groups.isBlank()) {
            Set<String> userGroups = identityService.createGroupQuery()
                    .groupMember(userId)
                    .list().stream()
                    .map(Group::getId)
                    .collect(Collectors.toSet());
            return Arrays.stream(groups.split(","))
                    .map(String::trim)
                    .anyMatch(userGroups::contains);
        }

        return true;
    }


}
