package org.unicam.intermediate.service.xml;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import static org.unicam.intermediate.utils.Constants.SPACE_NS;

@Slf4j
@Component
public class DestinationXmlService extends AbstractXmlService {

    public DestinationXmlService() {
        super("destination", SPACE_NS.getNamespaceUri());
    }

    @Override
    public String getTypeKey() {
        return "movement";
    }

    @Override
    public String getNamespaceUri() {
        return namespaceUri;
    }

    @Override
    public String getLocalName() {
        return localName;
    }

    @Override
    public String extractRaw(DelegateExecution execution) {
        String raw = super.extractRaw(execution);
        log.debug("[DestinationXmlService] extractRaw on {} → {}",
                execution.getCurrentActivityId(), raw);
        return raw;
    }

    @Override
    public void patchInstanceValue(DelegateExecution execution, String newValue) {
        super.patchInstanceValue(execution, newValue);
        log.info("[DestinationXmlService] patched <space:destination>='{}' on {}",
                newValue, execution.getCurrentActivityId());
    }

    @Override
    public void restoreInstanceValue(DelegateExecution execution, String rawValue) {
        super.restoreInstanceValue(execution, rawValue);
        log.info("[DestinationXmlService] restored <space:destination>='{}' on {}",
                rawValue, execution.getCurrentActivityId());
    }
}
