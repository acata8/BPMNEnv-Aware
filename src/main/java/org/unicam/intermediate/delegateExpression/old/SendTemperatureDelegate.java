package org.unicam.intermediate.delegateExpression.old;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.unicam.intermediate.config.GlobalEnvironment;
import org.unicam.intermediate.models.pojo.EnvironmentData;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

@Component
@Slf4j
public class SendTemperatureDelegate implements JavaDelegate {

    private final RuntimeService runtimeService;

    public final EnvironmentData data = GlobalEnvironment.getInstance().getData();

    public SendTemperatureDelegate(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        try {
            String endpoint = "http://www.randomnumberapi.com/api/v1.0/random?min=27&max=32&count=1";
            URL url = new URL(endpoint);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int status = con.getResponseCode();
            if (status != 200) {
                log.error("[SendTemperature] HTTP request failed with status: {}", status);
                throw new BpmnError("TemperatureRequestFailed", "Failed to retrieve temperature from API");
            }

            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                StringBuilder content = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }

                String response = content.toString().replaceAll("[\\[\\]]", "").trim();
                double temperature = Double.parseDouble(response);
                log.info("[SendTemperature] Retrieved simulated temperature: {} °C", temperature);

                runtimeService.startProcessInstanceByMessage("Message_Temperature", Map.of("valore", temperature));
                log.info("[SendTemperature] Started process with message 'Message_Temperature' and value: {}", temperature);
            }

        } catch (BpmnError bpmnError) {
            throw bpmnError;
        } catch (Exception e) {
            log.error("[SendTemperature] Unexpected error: {}", e.getMessage(), e);
            throw new BpmnError("TemperatureUnexpectedError", "Unexpected error while sending temperature");
        }
    }
}
