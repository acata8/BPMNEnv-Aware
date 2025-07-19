package org.unicam.intermediate.delegateExpression;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AnalyzeTemperatureDelegate implements JavaDelegate {
    @Autowired
    private RuntimeService runtimeService;

    @Override
    public void execute(DelegateExecution execution) {
        double temp = (double) execution.getVariable("valore");

        boolean isCritical = temp >= 30.0 && temp < 31.0;
        execution.setVariable("isCritical", isCritical);
        System.out.println("isCritical: " + isCritical);
    }
}
