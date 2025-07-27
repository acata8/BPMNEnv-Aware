package org.unicam.intermediate.listener.movement;

import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.impl.bpmn.listener.DelegateExpressionExecutionListener;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.el.ExpressionManager;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.util.xml.Element;
import org.camunda.bpm.engine.impl.util.xml.Namespace;
import org.springframework.stereotype.Component;
import org.unicam.intermediate.activity.WaitStateActivity;
import org.unicam.intermediate.service.xml.XmlServiceDispatcher;
import org.unicam.intermediate.utils.Constants;


import static org.unicam.intermediate.utils.Constants.SPACE_NS;

import java.util.Collections;

@Component
public class MovementParseListener extends AbstractBpmnParseListener {



    @Override
    public void parseTask(Element taskElement, ScopeImpl scope, ActivityImpl activity) {
        Element extensions = taskElement.element("extensionElements");

        // recupera l’ExpressionManager corrente
        ExpressionManager exprMgr = Context
                .getProcessEngineConfiguration()
                .getExpressionManager();

        // crea l’espressione "${movementExecutionListener}" o "${destinationExecutionListener}"
        String exprString = "${movementExecutionListener}";
        var expression = exprMgr.createExpression(exprString);

        if (extensions != null) {
            Element typeElement = extensions.elementNS(SPACE_NS, "type");
            if (typeElement != null && "movement".equals(typeElement.getText())) {
                activity.setActivityBehavior(new WaitStateActivity());
                activity.addListener(ExecutionListener.EVENTNAME_START, new DelegateExpressionExecutionListener(expression, Collections.emptyList()));
                activity.addListener(ExecutionListener.EVENTNAME_END, new DelegateExpressionExecutionListener(expression, Collections.emptyList()));
            }
        }
    }
}
