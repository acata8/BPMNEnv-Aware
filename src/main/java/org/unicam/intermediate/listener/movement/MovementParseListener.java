package org.unicam.intermediate.listener.movement;

import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.util.xml.Element;
import org.camunda.bpm.engine.impl.util.xml.Namespace;
import org.springframework.stereotype.Component;
import org.unicam.intermediate.activity.WaitStateActivity;

@Component
public class MovementParseListener extends AbstractBpmnParseListener {

    private static final Namespace SPACE_NAMESPACE = new Namespace("space", "http://space");

    @Override
    public void parseTask(Element taskElement, ScopeImpl scope, ActivityImpl activity) {
        Element extensions = taskElement.element("extensionElements");
        if (extensions != null) {
            Element typeElement = extensions.elementNS(SPACE_NAMESPACE, "type");
            if (typeElement != null && "movement".equals(typeElement.getText())) {
                activity.setActivityBehavior(new WaitStateActivity());
                activity.addListener(ExecutionListener.EVENTNAME_START, new MovementExecutionListener());
            }
        }
    }
}
