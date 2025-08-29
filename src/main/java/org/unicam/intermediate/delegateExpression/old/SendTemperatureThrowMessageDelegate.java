package org.unicam.intermediate.delegateExpression.old;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.unicam.intermediate.config.GlobalEnvironment;
import org.unicam.intermediate.models.pojo.EnvironmentData;
import org.unicam.intermediate.models.pojo.Place;
import org.unicam.intermediate.service.environmental.TemperatureService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component("sendTemperatureThrowMessageDelegate")
@AllArgsConstructor
@Slf4j
public class SendTemperatureThrowMessageDelegate implements JavaDelegate {

    private final RuntimeService runtimeService;

    @Override
    public void execute(DelegateExecution execution) {
        EnvironmentData data = GlobalEnvironment.getInstance().getData();
        Map<String, Double> temperatureMap = new HashMap<>();

        try {
            List<Place> places = data.getPlaces();
            for (Place place : places) {
                Map<String, Object> attributes = place.getAttributes();
                if (attributes == null || !attributes.containsKey("temperature")) {
                    continue;
                }

                Object rawEndpoint = attributes.get("temperature");
                if (!(rawEndpoint instanceof String endpoint) || endpoint.isBlank()) {
                    log.warn("[SendTemperature] Invalid or empty endpoint for place '{}'", place.getId());
                    continue;
                }

                try {
                    double temperature = new TemperatureService().fetchTemperatureFromEndpoint(endpoint);
                    temperatureMap.put(place.getId(), temperature);
                    log.info("[SendTemperature] Retrieved temperature {}°C for place '{}'", temperature, place.getId());
                } catch (Exception e) {
                    log.warn("[SendTemperature] Failed to fetch temperature for '{}': {}", place.getId(), e.getMessage());
                }
            }
String businessKey = execution.getBusinessKey();

            // Correlate message with the temperature map
            runtimeService
                    .createMessageCorrelation("Message_Temperature")
                    .processInstanceBusinessKey(businessKey)
                    .setVariable("temperatureMap", temperatureMap)
                    .correlate();

            log.info("[SendTemperature] Message_Temperature correlated with {} entries.", temperatureMap.size());

        } catch (Exception e) {
            log.error("[SendTemperature] Unexpected error: {}", e.getMessage(), e);
            throw new BpmnError("SendTemperatureError", "Unexpected error during temperature collection");
        }
    }


}
