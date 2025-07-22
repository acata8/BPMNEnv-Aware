package org.unicam.intermediate.delegateExpression;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class AnalyzeTemperatureDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        double temp = (double) execution.getVariable("valore");

        boolean isCritical = temp >= 30.0 && temp < 31.0;
        execution.setVariable("isCritical", isCritical);
        log.info("isCritical: {}", isCritical);
    }
}
