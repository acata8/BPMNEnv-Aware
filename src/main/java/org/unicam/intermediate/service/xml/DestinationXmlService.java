package org.unicam.intermediate.service.xml;

import org.camunda.bpm.engine.impl.util.xml.Namespace;
import org.springframework.stereotype.Component;


@Component
public class DestinationXmlService extends AbstractXmlService {

    private static final Namespace SPACE_NAMESPACE = new Namespace("http://space");


    public DestinationXmlService() {
        super("movement", SPACE_NAMESPACE.getNamespaceUri());
    }


}
