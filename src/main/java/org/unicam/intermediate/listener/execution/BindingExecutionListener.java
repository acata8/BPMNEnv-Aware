package org.unicam.intermediate.listener.execution;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;
import org.unicam.intermediate.models.Participant;
import org.unicam.intermediate.models.WaitingBinding;
import org.unicam.intermediate.models.enums.TaskType;
import org.unicam.intermediate.service.environmental.BindingService;
import org.unicam.intermediate.service.participant.ParticipantService;
import org.unicam.intermediate.service.xml.AbstractXmlService;
import org.unicam.intermediate.service.xml.XmlServiceDispatcher;

import java.time.Instant;
import java.util.Optional;

import static org.unicam.intermediate.utils.Constants.SPACE_NS;
import static org.unicam.intermediate.utils.Constants.bindingExecutionListenerBeanName;

@Slf4j
@Component(bindingExecutionListenerBeanName)
public class BindingExecutionListener implements ExecutionListener {

    private final XmlServiceDispatcher dispatcher;
    private final BindingService bindingService;
    private final RuntimeService runtimeService;
    private final ParticipantService participantService;

    public BindingExecutionListener(XmlServiceDispatcher dispatcher, 
                                   BindingService bindingService, 
                                   RuntimeService runtimeService,
                                   ParticipantService participantService) {
        this.dispatcher = dispatcher;
        this.bindingService = bindingService;
        this.runtimeService = runtimeService;
        this.participantService = participantService;
    }

    @Override
    public void notify(DelegateExecution execution) {
        if (EVENTNAME_START.equals(execution.getEventName())) {
            handleBindingStart(execution);
        } else if (EVENTNAME_END.equals(execution.getEventName())) {
            handleBindingEnd(execution);
        }
    }

    private void handleBindingStart(DelegateExecution execution) {
        AbstractXmlService svc = dispatcher.get(SPACE_NS.getNamespaceUri(), TaskType.BINDING);
        String bindingSpec = svc != null ? svc.extractRaw(execution) : null;
        
        Participant currentParticipant = participantService.resolveCurrentParticipant(execution);
        Participant targetParticipant = participantService.resolveTargetParticipant(execution, bindingSpec);
        
        if (targetParticipant == null) {
            log.warn("[BINDING] Could not resolve target participant for binding: {}", bindingSpec);
            return;
        }

        String businessKey = execution.getBusinessKey();
        String executionId = execution.getId();
        String activityId = execution.getCurrentActivityId();
        String activityName = execution.getCurrentActivityName();

        log.info("[BINDING] TASK STARTED | Activity: {} - {} | BusinessKey: {} | Participant: {} | Target: {}", 
                activityId, 
                activityName != null ? activityName : "(unnamed)",
                businessKey != null ? businessKey : "(none)",
                currentParticipant.toString(),
                targetParticipant.toString());

        Optional<WaitingBinding> waitingBinding = bindingService.findWaitingBinding(businessKey, currentParticipant.getId());
        
        if (waitingBinding.isPresent()) {
            WaitingBinding waiting = waitingBinding.get();
            
            log.info("[BINDING] MATCH FOUND | Current: {} | Target: {} | BusinessKey: {} | Status: Both participants synchronized!", 
                    currentParticipant.toString(),
                    targetParticipant.toString(),
                    businessKey);
            
            bindingService.removeWaitingBinding(businessKey, currentParticipant.getId());
            runtimeService.signal(waiting.getExecutionId());
            execution.setVariable("bindingCompleted_" + activityId, true);
            
            log.info("[BINDING] COMPLETED | Activity: {} | Participant: {} | Result: Both participants synchronized, continuing immediately", 
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
            
            bindingService.addWaitingBinding(newWaiting);
            
            log.info("[BINDING] WAITING | Activity: {} - {} | Participant: {} | Reason: Added to waiting list for: {}", 
                    activityId, 
                    activityName != null ? activityName : "(unnamed)",
                    currentParticipant.toString(),
                    targetParticipant.toString());
        }
    }

    private void handleBindingEnd(DelegateExecution execution) {
        Participant currentParticipant = participantService.resolveCurrentParticipant(execution);
        String businessKey = execution.getBusinessKey();
        String activityId = execution.getCurrentActivityId();
        String activityName = execution.getCurrentActivityName();
        
        bindingService.removeWaitingBinding(businessKey, currentParticipant.getId());
        execution.removeVariable("bindingCompleted_" + activityId);
        
        log.info("[BINDING] TASK ENDED | Activity: {} - {} | BusinessKey: {} | Participant: {}", 
                activityId, 
                activityName != null ? activityName : "(unnamed)",
                businessKey != null ? businessKey : "(none)",
                currentParticipant.toString());
    }
}