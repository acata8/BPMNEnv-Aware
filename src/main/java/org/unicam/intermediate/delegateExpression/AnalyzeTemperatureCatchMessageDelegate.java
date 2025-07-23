package org.unicam.intermediate.delegateExpression;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("analyzeTemperatureCatchMessageDelegate")
@Slf4j
public class AnalyzeTemperatureCatchMessageDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        try {
            Object rawMap = execution.getVariable("temperatureMap");

            if (!(rawMap instanceof Map)) {
                log.warn("[AnalyzeTemperature] 'temperatureMap' is missing or not a valid Map<String, Double>");
                execution.setVariable("isCritical", false);
                return;
            }

            Map<String, Double> temperatureMap = (Map<String, Double>) rawMap;

            if (temperatureMap.isEmpty()) {
                log.warn("[AnalyzeTemperature] 'temperatureMap' is empty");
                execution.setVariable("isCritical", false);
                return;
            }

            String criticalPlaceId = null;

            for (Map.Entry<String, Double> entry : temperatureMap.entrySet()) {
                String placeId = entry.getKey();
                Double temp = entry.getValue();

                if (temp == null) {
                    log.warn("[AnalyzeTemperature] Skipping place '{}' due to null temperature", placeId);
                    continue;
                }

                if (temp >= 30.0 && temp < 31.0) {
                    criticalPlaceId = placeId;
                    break;
                }
            }

            boolean isCritical = criticalPlaceId != null;
            execution.setVariable("isCritical", isCritical);

            if (isCritical) {
                execution.setVariable("destination", criticalPlaceId);
                log.info("[AnalyzeTemperature] Critical temperature detected at '{}'. Destination set.", criticalPlaceId);
            } else {
                log.info("[AnalyzeTemperature] No critical temperature detected.");
            }

        } catch (Exception e) {
            log.error("[AnalyzeTemperature] Unexpected error: {}", e.getMessage(), e);
            throw new BpmnError("AnalyzeTemperatureError", "Internal error while analyzing temperatures");
        }
    }
}
