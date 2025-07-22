package org.unicam.intermediate.delegateExpression;

import org.camunda.bpm.engine.RuntimeService;
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

@Component("sendTemperatureThrowMessageDelegate")
public class SendTemperatureThrowMessageDelegate implements JavaDelegate {

    private final RuntimeService runtimeService;

    public final EnvironmentData data = GlobalEnvironment.getInstance().getData();

    public SendTemperatureThrowMessageDelegate(RuntimeService runtimeService) {
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
            if (status == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuilder content = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                con.disconnect();

                // La risposta è un array JSON, es: [28]
                String response = content.toString();
                response = response.replaceAll("[\\[\\]]", ""); // rimuove le parentesi
                int temperature = Integer.parseInt(response.trim());

                System.out.println("Temperatura simulata: " + temperature + " °C");
            } else {
                System.err.println("Errore nella richiesta: HTTP " + status);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        runtimeService
                .createMessageCorrelation("Message_Temperature")
                .processInstanceBusinessKey("manda-temperatura")
                .setVariable("valore", 30.4)
                .correlate();

        //double temperature = Math.round((29.0 + (Math.random() * 3.0)) * 10.0) / 10.0;
    }
}
