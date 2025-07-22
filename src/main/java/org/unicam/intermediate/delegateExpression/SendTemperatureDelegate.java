package org.unicam.intermediate.delegateExpression;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SendTemperatureDelegate implements JavaDelegate {

    private final RuntimeService runtimeService;

    public SendTemperatureDelegate(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        //double temperature = Math.round((29.0 + (Math.random() * 3.0)) * 10.0) / 10.0;
        runtimeService.startProcessInstanceByMessage("Message_Temperature", Map.of("valore", 30.4));
    }
}
