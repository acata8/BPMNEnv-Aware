package org.unicam.intermediate.listener.execution;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;
import org.unicam.intermediate.models.Participant;
import org.unicam.intermediate.models.enums.TaskType;
import org.unicam.intermediate.service.participant.ParticipantService;
import org.unicam.intermediate.service.xml.AbstractXmlService;
import org.unicam.intermediate.service.xml.XmlServiceDispatcher;

import static org.unicam.intermediate.utils.Constants.*;

@Slf4j
@Component(movementExecutionListenerBeanName)
public class MovementExecutionListener implements ExecutionListener {

    private final XmlServiceDispatcher dispatcher;
    private final ParticipantService participantService;

    public MovementExecutionListener(XmlServiceDispatcher dispatcher, ParticipantService participantService) {
        this.dispatcher = dispatcher;
        this.participantService = participantService;
    }

    @Override
    public void notify(DelegateExecution execution) {
        if (EVENTNAME_START.equals(execution.getEventName())) {
            handleMovementStart(execution);
        } else if (EVENTNAME_END.equals(execution.getEventName())) {
            handleMovementEnd(execution);
        }
    }

    private void handleMovementStart(DelegateExecution execution) {
        AbstractXmlService svc = dispatcher.get(SPACE_NS.getNamespaceUri(), TaskType.MOVEMENT);
        String raw = svc.extractRaw(execution);
        String value = raw != null && raw.startsWith("${") && raw.endsWith("}")
                ? String.valueOf(execution.getVariable(raw.substring(2, raw.length()-1).trim()))
                : raw;

        svc.patchInstanceValue(execution, value);
        var activityId = execution.getCurrentActivityId();
        String varKey = activityId + "." + svc.getLocalName();
        execution.setVariable(varKey, value);

        Participant participant = participantService.resolveCurrentParticipant(execution);
        String activityName = execution.getCurrentActivityName();
        
        log.info("[MOVEMENT] WAITING | Activity: {} - {} | Participant: {} | Reason: Waiting for GPS coordinates to reach: {}", 
                execution.getCurrentActivityId(),
                activityName != null ? activityName : "(unnamed)",
                participant.toString(),
                value != null ? value : "unknown location");
    }

    private void handleMovementEnd(DelegateExecution execution) {
        AbstractXmlService svc = dispatcher.get(SPACE_NS.getNamespaceUri(), TaskType.MOVEMENT);
        String raw = svc.extractRaw(execution);
        svc.restoreInstanceValue(execution, raw);
    }
}