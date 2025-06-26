package org.unicam.intermediate.Listener;

import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.util.xml.Element;
import org.camunda.bpm.engine.impl.util.xml.Namespace;
import org.springframework.stereotype.Component;
import org.unicam.intermediate.Activity.WaitStateActivity;

@Component
public class MovementTaskParseListener extends AbstractBpmnParseListener {

    private static final Namespace SPACE = new Namespace("space", "http://space");

    @Override
    public void parseServiceTask(Element serviceTaskElement, ScopeImpl scope, ActivityImpl activity) {
        parseMovementType(serviceTaskElement, activity);
    }

    @Override
    public void parseUserTask(Element userTaskElement, ScopeImpl scope, ActivityImpl activity) {
        parseMovementType(userTaskElement, activity);
    }

    @Override
    public void parseScriptTask(Element scriptTaskElement, ScopeImpl scope, ActivityImpl activity) {
        parseMovementType(scriptTaskElement, activity);
    }

    @Override
    public void parseTask(Element genericTaskElement, ScopeImpl scope, ActivityImpl activity) {
        parseMovementType(genericTaskElement, activity);
    }

    private void parseMovementType(Element element, ActivityImpl activity) {
        Element extensions = element.element("extensionElements");
        if (extensions != null) {
            Element typeElement = extensions.elementNS(SPACE, "type");
            if (typeElement != null && "movement".equals(typeElement.getText())) {
                activity.setActivityBehavior(new WaitStateActivity());
                activity.addListener(ExecutionListener.EVENTNAME_START, new MovementExecutionListener());
            }
        }
    }
}

