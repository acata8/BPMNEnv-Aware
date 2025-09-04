package org.unicam.intermediate.listener.execution;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;
import org.unicam.intermediate.models.Participant;
import org.unicam.intermediate.models.WaitingBinding;
import org.unicam.intermediate.models.enums.TaskType;
import org.unicam.intermediate.service.MessageFlowRegistry;
import org.unicam.intermediate.service.MessageFlowRegistry.MessageFlowBinding;
import org.unicam.intermediate.service.environmental.BindingService;
import org.unicam.intermediate.service.participant.ParticipantService;
import org.unicam.intermediate.service.xml.XmlServiceDispatcher;

import java.time.Instant;
import java.util.Optional;

import static org.unicam.intermediate.utils.Constants.bindingExecutionListenerBeanName;

@Slf4j
@Component(bindingExecutionListenerBeanName)
@AllArgsConstructor
public class BindingExecutionListener implements ExecutionListener {

    private final BindingService bindingService;
    private final RuntimeService runtimeService;
    private final MessageFlowRegistry messageFlowRegistry;
    private final ParticipantService participantService;
    private final XmlServiceDispatcher dispatcher;

    @Override
    public void notify(DelegateExecution execution) {
        if (EVENTNAME_START.equals(execution.getEventName())) {
            handleBindingStart(execution);
        } else if (EVENTNAME_END.equals(execution.getEventName())) {
            handleBindingEnd(execution);
        }
    }

    private void handleBindingStart(DelegateExecution execution) {
        String processDefinitionId = execution.getProcessDefinitionId();
        String activityId = execution.getCurrentActivityId();
        String businessKey = execution.getBusinessKey();

        // Get message flow binding info
        MessageFlowBinding flowBinding = messageFlowRegistry.getFlowBinding(
                processDefinitionId, activityId);

        if (flowBinding == null) {
            log.error("[BINDING] No message flow found for task: {} in process: {}",
                    activityId, processDefinitionId);
            return;
        }

        // Determine current and target participants based on which task we're in
        String currentParticipantRef;
        String targetParticipantRef;

        if (activityId.equals(flowBinding.getSourceTaskRef())) {
            currentParticipantRef = flowBinding.getSourceParticipantRef();
            targetParticipantRef = flowBinding.getTargetParticipantRef();
        } else {
            currentParticipantRef = flowBinding.getTargetParticipantRef();
            targetParticipantRef = flowBinding.getSourceParticipantRef();
        }

        Participant currentParticipant = participantService.getParticipantById(execution, currentParticipantRef);
        Participant targetParticipant = participantService.getParticipantById(execution, targetParticipantRef);

        log.info("[BINDING] Task {} started - Current: {} waiting for Target: {}",
                activityId,
                currentParticipant != null ? currentParticipant.getDisplayName() : currentParticipantRef,
                targetParticipant != null ? targetParticipant.getDisplayName() : targetParticipantRef);

        // Check if the other participant is already waiting
        Optional<WaitingBinding> waiting = bindingService.findWaitingBinding(
                businessKey, currentParticipantRef);

        if (waiting.isPresent()) {
            WaitingBinding match = waiting.get();
            log.info("[BINDING] MATCH FOUND - Both participants ready, signaling...");

            bindingService.removeWaitingBinding(businessKey, currentParticipantRef);
            runtimeService.signal(match.getExecutionId());
            execution.setVariable("bindingCompleted_" + activityId, true);

        } else {
            // Add to waiting list
            WaitingBinding newWaiting = new WaitingBinding(
                    processDefinitionId,
                    targetParticipantRef,
                    currentParticipantRef,
                    businessKey,
                    execution.getId(),
                    TaskType.BINDING,
                    Instant.now()
            );

            bindingService.addWaitingBinding(newWaiting);
            log.info("[BINDING] Added to waiting list for participant synchronization");
        }
    }

    private void handleBindingEnd(DelegateExecution execution) {
        String activityId = execution.getCurrentActivityId();
        String businessKey = execution.getBusinessKey();
        String processDefinitionId = execution.getProcessDefinitionId();

        MessageFlowRegistry.MessageFlowBinding flowBinding = messageFlowRegistry.getFlowBinding(
                processDefinitionId, activityId);

        if (flowBinding != null) {
            String participantRef = activityId.equals(flowBinding.getSourceTaskRef())
                    ? flowBinding.getSourceParticipantRef()
                    : flowBinding.getTargetParticipantRef();

            bindingService.removeWaitingBinding(businessKey, participantRef);
        }

        execution.removeVariable("bindingCompleted_" + activityId);
        log.info("[BINDING] Task {} ended", activityId);
    }
}