package org.unicam.intermediate.service.xml;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.camunda.bpm.model.xml.instance.DomElement;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Super‐classe generica per leggere/scrivere un tag <ns:tagName> in extensionElements.
 */
@Slf4j
public abstract class AbstractXmlService {

    @Autowired
    protected RepositoryService repositoryService;

    /** il nome locale del tag, es. "destination" */
    protected final String localName;
    /** il namespace URI, es. "http://space" */
    protected final String namespaceUri;

    protected AbstractXmlService(String localName, String namespaceUri) {
        this.localName    = localName;
        this.namespaceUri = namespaceUri;
    }

    /**
     * Estrae il contenuto raw di <ns:localName> da extensionElements.
     * @return testo (trimmed) o null
     */
    public String extractRaw(DelegateExecution execution) {
        ModelElementInstance elem = execution.getBpmnModelElementInstance();
        if (!(elem instanceof Task task)) {
            return null;
        }
        ExtensionElements ext = task.getExtensionElements();
        if (ext == null) {
            return null;
        }
        for (DomElement dom : ext.getDomElement().getChildElements()) {
            if (localName.equals(dom.getLocalName()) &&
                    namespaceUri.equals(dom.getNamespaceURI())) {
                String txt = dom.getTextContent();
                return (txt != null ? txt.trim() : null);
            }
        }
        return null;
    }

    /**
     * Scrive newValue in ogni <ns:localName> esistente in extensionElements.
     */
    public void updateValue(DelegateExecution execution, String newValue) {
        String defId = execution.getProcessDefinitionId();
        BpmnModelInstance model = repositoryService.getBpmnModelInstance(defId);

        ModelElementInstance elem = model.getModelElementById(execution.getCurrentActivityId());
        if (!(elem instanceof Task task)) {
            log.warn("[XMLService] L’elemento {} non è un Task", execution.getCurrentActivityId());
            return;
        }

        ExtensionElements ext = task.getExtensionElements();
        if (ext == null) {
            log.warn("[XMLService] Nessuna ExtensionElements su Task {}", execution.getCurrentActivityId());
            return;
        }

        ext.getDomElement().getChildElements().stream()
                .filter(dom -> localName.equals(dom.getLocalName())
                        && namespaceUri.equals(dom.getNamespaceURI()))
                .forEach(dom -> {
                    dom.setTextContent(newValue);
                    log.info("[XMLService] <{} xmlns='{}'> aggiornato con: {}", localName, namespaceUri, newValue);
                });
    }
}
