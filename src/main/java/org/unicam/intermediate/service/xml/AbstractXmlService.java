package org.unicam.intermediate.service.xml;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.camunda.bpm.model.xml.instance.DomElement;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.unicam.intermediate.models.enums.ExtendedElementTaskType;
import org.unicam.intermediate.models.enums.TaskType;

@Slf4j
public abstract class AbstractXmlService {

    @Autowired
    protected RepositoryService repositoryService;

    /** il nome locale del tag, es. "destination" */
    protected final ExtendedElementTaskType localName;
    /** il namespace URI, es. "http://space" */
    protected final String namespaceUri;

    protected AbstractXmlService(ExtendedElementTaskType localName, String namespaceUri) {
        this.localName    = localName;
        this.namespaceUri = namespaceUri;
    }

    /** Chiave usata per il dispatch (es. "movement") */
    public abstract TaskType getTypeKey();
    public abstract String getNamespaceUri();
    public abstract ExtendedElementTaskType getLocalName();


    public String extractRaw(DelegateExecution execution) {
        ModelElementInstance elem = execution.getBpmnModelElementInstance();
        if (!(elem instanceof Task task)) return null;
        ExtensionElements ext = task.getExtensionElements();
        if (ext == null) return null;

        var matchingName = localName.toString().toUpperCase();

        return ext.getDomElement().getChildElements().stream()
                .filter(dom -> matchingName.equals(dom.getLocalName().toUpperCase())
                        && namespaceUri.equals(dom.getNamespaceURI()))
                .map(DomElement::getTextContent)
                .map(String::trim)
                .findFirst()
                .orElse(null);
    }

    public void patchInstanceValue(DelegateExecution execution, String newValue) {
        doUpdateOnInstance(execution, newValue);
        log.debug("[XMLService] patched <{}>='{}' on {}", localName, newValue, execution.getCurrentActivityId());
    }


    public void restoreInstanceValue(DelegateExecution execution, String rawValue) {
        doUpdateOnInstance(execution, rawValue);
        log.debug("[XMLService] restored <{}>='{}' on {}", localName, rawValue, execution.getCurrentActivityId());
    }

    private void doUpdateOnInstance(DelegateExecution execution, String value) {
        ModelElementInstance elem = execution.getBpmnModelElementInstance();
        if (!(elem instanceof Task task)) return;
        ExtensionElements ext = task.getExtensionElements();
        if (ext == null) return;

        var matchingName = localName.toString().toUpperCase();

        ext.getDomElement().getChildElements().stream()
                .filter(dom -> matchingName.equals(dom.getLocalName().toUpperCase())
                        && namespaceUri.equals(dom.getNamespaceURI()))
                .forEach(dom -> dom.setTextContent(value));
    }
}
