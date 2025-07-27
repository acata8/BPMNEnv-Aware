package org.unicam.intermediate.listener.message;

import lombok.Setter;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.delegate.Expression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("throwMessageListener")
public class ThrowMessageListener implements ExecutionListener {

    private final RuntimeService runtimeService;

    public ThrowMessageListener(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Setter
    private Expression messageNameExpr;
    @Setter
    private Expression setVariableValue;

    @Override
    public void notify(DelegateExecution execution) {
        String messageName = (String) messageNameExpr.getValue(execution);
        String varValue = (String) setVariableValue.getValue(execution);
        String correlationKey =  execution.getBusinessKey();

        Object value = execution.getVariable(varValue);

        runtimeService.createMessageCorrelation(messageName)
                      .processInstanceBusinessKey(correlationKey)
                      .setVariable(varValue, value)
                      .correlate();
    }
}
