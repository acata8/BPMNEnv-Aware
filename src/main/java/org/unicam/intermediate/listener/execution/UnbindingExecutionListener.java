package org.unicam.intermediate.listener.execution;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;
import org.unicam.intermediate.models.Participant;
import org.unicam.intermediate.models.WaitingBinding;
import org.unicam.intermediate.service.environmental.BindingService;
import org.unicam.intermediate.service.participant.ParticipantService;

import java.time.Instant;
import java.util.Optional;

import static org.unicam.intermediate.utils.Constants.unbindingExecutionListenerBeanName;

@Slf4j
@Component(unbindingExecutionListenerBeanName)
public class UnbindingExecutionListener implements ExecutionListener {

    private final BindingService bindingService;
    private final RuntimeService runtimeService;
    private final ParticipantService participantService;

    public UnbindingExecutionListener(BindingService bindingService, 
                                     RuntimeService runtimeService,
                                     ParticipantService participantService) {
        this.bindingService = bindingService;
        this.runtimeService = runtimeService;
        this.participantService = participantService;
    }

    @Override
    public void notify(DelegateExecution execution) {
        if (EVENTNAME_START.equals(execution.getEventName())) {
            handleUnbindingStart(execution);
        } else if (EVENTNAME_END.equals(execution.getEventName())) {
            handleUnbindingEnd(execution);
        }
    }

    private void handleUnbindingStart(DelegateExecution execution) {
        Participant currentParticipant = participantService.resolveCurrentParticipant(execution);
        Participant targetParticipant = participantService.resolveTargetParticipant(execution, null);
        
        if (targetParticipant == null) {
            log.warn("[UNBINDING] Could not resolve target participant for unbinding");
            return;
        }

        String businessKey = execution.getBusinessKey();
        String executionId = execution.getId();
        String activityId = execution.getCurrentActivityId();
        String activityName = execution.getCurrentActivityName();

        log.info("[UNBINDING] TASK STARTED | Activity: {} - {} | BusinessKey: {} | Participant: {} | Target: {}", 
                activityId, 
                activityName != null ? activityName : "(unnamed)",
                businessKey != null ? businessKey : "(none)",
                currentParticipant.toString(),
                targetParticipant.toString());

        Optional<WaitingBinding> waitingUnbinding = bindingService.findWaitingUnbinding(businessKey, currentParticipant.getId());
        
        if (waitingUnbinding.isPresent()) {
            WaitingBinding waiting = waitingUnbinding.get();
            
            log.info("[UNBINDING] MATCH FOUND | Current: {} | Target: {} | BusinessKey: {} | Status: Both participants synchronized!", 
                    currentParticipant.toString(),
                    targetParticipant.toString(),
                    businessKey);
            
            bindingService.removeWaitingUnbinding(businessKey, currentParticipant.getId());
            runtimeService.signal(waiting.getExecutionId());
            execution.setVariable("unbindingCompleted_" + activityId, true);
            
            log.info("[UNBINDING] COMPLETED | Activity: {} | Participant: {} | Result: Both participants disconnected, continuing immediately", 
                    activityId,
                    currentParticipant.toString());
            
        } else {
            WaitingBinding newWaiting = new WaitingBinding(
                    execution.getProcessDefinitionId(),
                    targetParticipant.getId(),
                    currentParticipant.getId(),
                    businessKey,
                    executionId,
                    null,
                    Instant.now()
            );
            
            bindingService.addWaitingUnbinding(newWaiting);
            
            log.info("[UNBINDING] WAITING | Activity: {} - {} | Participant: {} | Reason: Added to waiting list for: {} to disconnect", 
                    activityId, 
                    activityName != null ? activityName : "(unnamed)",
                    currentParticipant.toString(),
                    targetParticipant.toString());
        }
    }

    private void handleUnbindingEnd(DelegateExecution execution) {
        Participant currentParticipant = participantService.resolveCurrentParticipant(execution);
        String businessKey = execution.getBusinessKey();
        String activityId = execution.getCurrentActivityId();
        String activityName = execution.getCurrentActivityName();
        
        bindingService.removeWaitingUnbinding(businessKey, currentParticipant.getId());
        execution.removeVariable("unbindingCompleted_" + activityId);
        
        log.info("[UNBINDING] TASK ENDED | Activity: {} - {} | BusinessKey: {} | Participant: {}", 
                activityId, 
                activityName != null ? activityName : "(unnamed)",
                businessKey != null ? businessKey : "(none)",
                currentParticipant.toString());
    }
}