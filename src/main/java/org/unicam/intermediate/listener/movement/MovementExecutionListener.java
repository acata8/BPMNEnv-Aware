package org.unicam.intermediate.listener.movement;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.xml.instance.DomElement;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MovementExecutionListener implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {
        Object existing = execution.getVariable("destination");
        if (existing != null) {
            log.info("[MovementExecutionListener] destination already set to '{}'", existing);
            return;
        }

        String fromExtension = extractDestinationFromModel(execution);
        if (fromExtension != null) {
            execution.setVariable("destination", fromExtension);
            log.info("[MovementExecutionListener] destination set from <space:destination>: '{}'", fromExtension);
        } else {
            String fallback = execution.getCurrentActivityId();
            execution.setVariable("destination", fallback);
            log.info("[MovementExecutionListener] destination not defined, using fallback '{}'", fallback);
        }
    }

    private String extractDestinationFromModel(DelegateExecution execution) {
        try {
            ModelElementInstance modelElement = execution.getBpmnModelElementInstance();
            if (modelElement == null) return null;

            if (!(modelElement instanceof Task)) return null;

            ExtensionElements extensionElements = ((Task) modelElement).getExtensionElements();
            if (extensionElements == null) return null;

            for (DomElement domElement : extensionElements.getDomElement().getChildElements()) {
                if ("destination".equals(domElement.getLocalName())
                        && "http://space".equals(domElement.getNamespaceURI())) {
                    String value = domElement.getTextContent();
                    return (value != null && !value.isBlank()) ? value.trim() : null;
                }
            }

        } catch (Exception e) {
            log.warn("[MovementExecutionListener] Failed to extract <space:destination>: {}", e.getMessage(), e);
        }
        return null;
    }
}
