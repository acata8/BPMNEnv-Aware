package org.unicam.intermediate.listener.message;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.delegate.Expression;
import org.springframework.stereotype.Component;

@Component("catchMessageListener")
public class CatchMessageListener implements ExecutionListener {

    private Expression getVariableValue;
    private Expression setVariableExpr;

    public void setPlaceVariableExpr(Expression placeVariableExpr) {
        this.getVariableValue = placeVariableExpr;
    }
    public void setDestinationVariableExpr(Expression setVariableExpr) {
        this.setVariableExpr = setVariableExpr;
    }

    @Override
    public void notify(DelegateExecution execution) {
        String varName = (String) getVariableValue.getValue(execution);
        String setVariableName = (String) setVariableExpr.getValue(execution);
        Object varValue = execution.getVariable(varName);
        execution.setVariable(setVariableName, varValue);
    }
}
