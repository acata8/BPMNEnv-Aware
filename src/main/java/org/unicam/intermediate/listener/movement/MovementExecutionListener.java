package org.unicam.intermediate.listener.movement;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.camunda.bpm.model.xml.instance.DomElement;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.unicam.intermediate.service.xml.XmlServiceDispatcher;

@Slf4j
@Component("movementExecutionListener")
public class MovementExecutionListener implements ExecutionListener {

    private final XmlServiceDispatcher dispatcher;

    public MovementExecutionListener(XmlServiceDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void notify(DelegateExecution execution) {

        ExtensionElements ext = execution.getBpmnModelElementInstance()
                .getExtensionElements();

        if (ext == null) {
            return;
        }

        String spaceType = ext.getDomElement().getChildElements().stream()
                .filter(d -> "type".equals(d.getLocalName()))
                .findFirst()
                .map(DomElement::getTextContent)
                .map(String::trim)
                .orElse(null);
        if (spaceType == null) {
            return;
        }

        String namespace = "http://space";
        var service = dispatcher.get(namespace, spaceType);
        if (service != null) {
            String raw = service.extractRaw(execution);

            if (raw == null) {
                log.warn("[Movement] <space:destination> not found");
                return;
            }

            String varName = raw;
            if (raw.startsWith("${") && raw.endsWith("}")) {
                varName = raw.substring(2, raw.length() - 1).trim();
            }

            Object value = execution.getVariable(varName);
            execution.setVariable("destination", value);
            log.info("[Movement] 'destination' set from <space:destination> with ({} → {})", varName, value);
            service.updateValue(execution, (String) value);
        }
    }
}
