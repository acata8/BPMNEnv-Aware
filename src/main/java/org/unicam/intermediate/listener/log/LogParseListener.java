package org.unicam.intermediate.listener.log;

import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.util.xml.Element;
import org.springframework.stereotype.Component;

@Component
public class LogParseListener extends AbstractBpmnParseListener {

    @Override
    public void parseTask(Element taskElement, ScopeImpl scope, ActivityImpl activity) {
        activity.addListener(ExecutionListener.EVENTNAME_START, new LogExecutionListener());
        activity.addListener(ExecutionListener.EVENTNAME_END, new LogExecutionListener());
    }
}
