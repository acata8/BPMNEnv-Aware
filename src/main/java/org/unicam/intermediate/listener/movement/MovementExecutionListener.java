package org.unicam.intermediate.listener.movement;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;
import org.unicam.intermediate.service.xml.AbstractXmlService;
import org.unicam.intermediate.service.xml.XmlServiceDispatcher;

import static org.unicam.intermediate.utils.Constants.SPACE_NS;

@Slf4j
@Component("movementExecutionListener")
public class MovementExecutionListener implements ExecutionListener {

    private final XmlServiceDispatcher dispatcher;

    public MovementExecutionListener(XmlServiceDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }
    @Override
    public void notify(DelegateExecution execution) {
        AbstractXmlService svc = dispatcher.get(SPACE_NS.getNamespaceUri(), "movement");
        String raw = svc.extractRaw(execution);
        String value = raw != null && raw.startsWith("${") && raw.endsWith("}")
                ? String.valueOf(execution.getVariable(raw.substring(2, raw.length()-1).trim()))
                : raw;

        if (EVENTNAME_START.equals(execution.getEventName())) {
            svc.patchInstanceValue(execution, value);

            var activityId = execution.getCurrentActivityId();
            String varKey = activityId + "." + svc.getLocalName();
            execution.setVariable(varKey, value);
        }
        else if (EVENTNAME_END.equals(execution.getEventName())) {
            svc.restoreInstanceValue(execution, "${destination}");
        }
    }
}
