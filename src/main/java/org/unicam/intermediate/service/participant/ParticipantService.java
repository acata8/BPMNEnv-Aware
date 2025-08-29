package org.unicam.intermediate.service.participant;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Collaboration;
import org.camunda.bpm.model.bpmn.instance.Participant;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ParticipantService {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    // Cache collaboration data per business key to avoid repeated BPMN parsing
    private final Map<String, CollaborationData> collaborationCache = new ConcurrentHashMap<>();

    /**
     * Resolves the current participant from the execution context
     */
    public org.unicam.intermediate.models.Participant resolveCurrentParticipant(DelegateExecution execution) {
        CollaborationData collabData = getCollaborationData(execution);
        String currentProcessKey = getCurrentProcessKey(execution);

        return collabData.getParticipantByProcessKey(currentProcessKey);
    }

    /**
     * Resolves the target participant from the binding specification
     */
    public org.unicam.intermediate.models.Participant resolveTargetParticipant(DelegateExecution execution, String bindingSpec) {
        CollaborationData collabData = getCollaborationData(execution);

        // If bindingSpec is a direct participant ID from BPMN, use it
        org.unicam.intermediate.models.Participant directMatch = collabData.getParticipantById(bindingSpec);
        if (directMatch != null) {
            return directMatch;
        }

        // If it's a process key, resolve by process
        org.unicam.intermediate.models.Participant processMatch = collabData.getParticipantByProcessKey(bindingSpec);
        if (processMatch != null) {
            return processMatch;
        }

        // Fallback: assume it's the "other" participant in a 2-participant collaboration
        String currentProcessKey = getCurrentProcessKey(execution);
        return collabData.getOtherParticipant(currentProcessKey);
    }

    /**
     * Gets all participants in the current collaboration
     */
    public List<org.unicam.intermediate.models.Participant> getAllParticipants(DelegateExecution execution) {
        CollaborationData collabData = getCollaborationData(execution);
        return new ArrayList<>(collabData.getAllParticipants());
    }

    private CollaborationData getCollaborationData(DelegateExecution execution) {
        String businessKey = execution.getBusinessKey();
        String cacheKey = businessKey != null ? businessKey : execution.getProcessInstanceId();

        return collaborationCache.computeIfAbsent(cacheKey, k -> extractCollaborationData(execution));
    }

    private CollaborationData extractCollaborationData(DelegateExecution execution) {
        try {
            // Get the BPMN model from any process in this collaboration
            String processDefinitionId = execution.getProcessDefinitionId();
            BpmnModelInstance model = repositoryService.getBpmnModelInstance(processDefinitionId);

            // Find the collaboration
            Collection<Collaboration> collaborations = model.getModelElementsByType(Collaboration.class);
            if (collaborations.isEmpty()) {
                log.warn("[ParticipantService] No collaboration found in BPMN model, creating single participant");
                return createSingleParticipantCollaboration(execution);
            }

            Collaboration collaboration = collaborations.iterator().next();
            Collection<Participant> bpmnParticipants = collaboration.getParticipants();

            CollaborationData collabData = new CollaborationData();

            for (Participant bpmnParticipant : bpmnParticipants) {
                Process process = bpmnParticipant.getProcess();
                if (process != null) {
                    String participantId = bpmnParticipant.getId();
                    String participantName = bpmnParticipant.getName();
                    String processKey = process.getId();

                    org.unicam.intermediate.models.Participant participant =
                            new org.unicam.intermediate.models.Participant(
                                    participantId,
                                    extractRole(participantName),
                                    participantName != null ? participantName : participantId,
                                    processKey
                            );

                    collabData.addParticipant(participant);

                    log.debug("[ParticipantService] Found participant: {} -> {} ({})",
                            participantId, participantName, processKey);
                }
            }

            log.info("[ParticipantService] Extracted collaboration with {} participants",
                    collabData.getParticipantCount());

            return collabData;

        } catch (Exception e) {
            log.error("[ParticipantService] Failed to extract collaboration data: {}", e.getMessage(), e);
            return createSingleParticipantCollaboration(execution);
        }
    }

    private CollaborationData createSingleParticipantCollaboration(DelegateExecution execution) {
        CollaborationData collabData = new CollaborationData();
        String processKey = getCurrentProcessKey(execution);

        org.unicam.intermediate.models.Participant singleParticipant =
                new org.unicam.intermediate.models.Participant(
                        "Participant_" + processKey,
                        processKey.toLowerCase(),
                        processKey,
                        processKey
                );

        collabData.addParticipant(singleParticipant);
        return collabData;
    }

    private String getCurrentProcessKey(DelegateExecution execution) {
        try {
            return repositoryService.getProcessDefinition(execution.getProcessDefinitionId()).getKey();
        } catch (Exception e) {
            log.debug("[ParticipantService] Could not get process key: {}", e.getMessage());
            return "UnknownProcess";
        }
    }

    private String extractRole(String participantName) {
        if (participantName == null || participantName.trim().isEmpty()) {
            return "participant";
        }
        return participantName.toLowerCase().replaceAll("[^a-z0-9]", "_");
    }

    /**
     * Clear cache when processes end or new deployments occur
     */
    public void clearCache() {
        collaborationCache.clear();
        log.info("[ParticipantService] Collaboration cache cleared");
    }

    /**
     * Internal class to hold collaboration data
     */
    private static class CollaborationData {
        private final Map<String, org.unicam.intermediate.models.Participant> participantsById = new HashMap<>();
        private final Map<String, org.unicam.intermediate.models.Participant> participantsByProcessKey = new HashMap<>();
        private final List<org.unicam.intermediate.models.Participant> allParticipants = new ArrayList<>();

        void addParticipant(org.unicam.intermediate.models.Participant participant) {
            participantsById.put(participant.getId(), participant);
            participantsByProcessKey.put(participant.getProcessDefinitionKey(), participant);
            allParticipants.add(participant);
        }

        org.unicam.intermediate.models.Participant getParticipantById(String participantId) {
            return participantsById.get(participantId);
        }

        org.unicam.intermediate.models.Participant getParticipantByProcessKey(String processKey) {
            return participantsByProcessKey.get(processKey);
        }

        org.unicam.intermediate.models.Participant getOtherParticipant(String currentProcessKey) {
            return allParticipants.stream()
                    .filter(p -> !p.getProcessDefinitionKey().equals(currentProcessKey))
                    .findFirst()
                    .orElse(null);
        }

        List<org.unicam.intermediate.models.Participant> getAllParticipants() {
            return Collections.unmodifiableList(allParticipants);
        }

        int getParticipantCount() {
            return allParticipants.size();
        }
    }
}