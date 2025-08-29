package org.unicam.intermediate.delegateExpression.old;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AnalyzeTemperatureDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        try {
            Object rawValue = execution.getVariable("valore");

            if (!(rawValue instanceof Number)) {
                log.warn("[AnalyzeTemperature] Variable 'valore' is missing or not numeric");
                execution.setVariable("isCritical", false);
                return;
            }

            double temp = ((Number) rawValue).doubleValue();
            boolean isCritical = temp >= 30.0 && temp < 31.0;

            execution.setVariable("isCritical", isCritical);
            log.info("[AnalyzeTemperature] Read temperature: {}. isCritical: {}", temp, isCritical);

        } catch (Exception e) {
            log.error("[AnalyzeTemperature] Unexpected error: {}", e.getMessage(), e);
            throw new BpmnError("AnalyzeTemperatureError", "Internal error while analyzing temperature value");
        }
    }
}
