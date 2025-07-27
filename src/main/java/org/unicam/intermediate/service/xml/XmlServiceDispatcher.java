package org.unicam.intermediate.service.xml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mantiene tutti gli AbstractXmlService in mappa keyed by "namespace#localName".
 */
@Component
public class XmlServiceDispatcher {
    private final Map<String,AbstractXmlService> services;
    @Autowired
    public XmlServiceDispatcher(List<AbstractXmlService> svcList) {
        this.services = svcList.stream()
                .collect(Collectors.toMap(
                        s -> s.getNamespaceUri() + "#" + s.getTypeKey(),
                        s -> s
                ));
    }
    public AbstractXmlService get(String namespaceUri, String typeKey) {
        return services.get(namespaceUri + "#" + typeKey);
    }
}

