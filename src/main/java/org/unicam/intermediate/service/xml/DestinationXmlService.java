package org.unicam.intermediate.service.xml;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.camunda.bpm.model.xml.instance.DomElement;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DestinationXmlService extends AbstractXmlService {

    private static final String SPACE_NS = "http://space";

    public DestinationXmlService() {
        super("movement", SPACE_NS);
    }

    /**
     * Qui facciamo override per:
     * 1) cercare sia <space:type> che <space:destination>
     * 2) leggere rawExpr e type insieme
     */
    @Override
    public String extractRaw(DelegateExecution execution) {
        ExtensionElements ext = ((Task)execution.getBpmnModelElementInstance())
                .getExtensionElements();
        if (ext == null) {
            return null;
        }

        String rawDest = null;
        String type    = null;
        for (DomElement dom : ext.getDomElement().getChildElements()) {
            if ("destination".equals(dom.getLocalName())
                    && SPACE_NS.equals(dom.getNamespaceURI())) {
                rawDest = dom.getTextContent().trim();
            }
            if ("type".equals(dom.getLocalName())
                    && SPACE_NS.equals(dom.getNamespaceURI())) {
                type = dom.getTextContent().trim();
            }
        }


        return rawDest;
    }

    /**
     * Sovrascrivo updateValue per loggare diversamente
     * o per inserire elementi aggiuntivi, se serve.
     */
    @Override
    public void updateValue(DelegateExecution execution, String newValue) {
        super.updateValue(execution, newValue);
        log.info("DestinationXmlService ha scritto '{}' su <space:destination> "
                + "nell'attività {}", newValue, execution.getCurrentActivityId());
    }
}
