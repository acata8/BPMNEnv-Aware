package org.unicam.intermediate.delegateExpression;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("throwMessageDelegate")
@Slf4j
public class ThrowMessageDelegate implements JavaDelegate {

    private final RuntimeService runtimeService;

    private final ProcessEngineConfigurationImpl config;

    public ThrowMessageDelegate(RuntimeService runtimeService, ProcessEngineConfigurationImpl config) {
        this.runtimeService = runtimeService;
        this.config = config;
    }

    @Override
    public void execute(DelegateExecution execution) {
        // Recupera la business key dinamicamente se specificata
        String messageName       = (String) execution.getVariable("messageName");
        String placeVariable     = (String) execution.getVariable("placeVariable");
        String businessKeyVariable    = (String) execution.getVariable("businessKeyVariable");


        String correlationKey;
        if (businessKeyVariable != null && !businessKeyVariable.isEmpty()) {
            Object bk = execution.getVariable(businessKeyVariable);
            correlationKey = bk != null ? bk.toString() : execution.getVariable(businessKeyVariable).toString();
        } else {
            correlationKey = execution.getBusinessKey();
        }

        Object value = execution.getVariable(placeVariable);
        runtimeService.createMessageCorrelation(messageName)
                .processInstanceBusinessKey(correlationKey)
                .setVariable(placeVariable, value)
                .correlate();
    }
}