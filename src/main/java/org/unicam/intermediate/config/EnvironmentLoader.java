package org.unicam.intermediate.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.unicam.intermediate.models.pojo.EnvironmentData;

import java.io.InputStream;
import java.util.List;

@Component
@Slf4j
@AllArgsConstructor
public class EnvironmentLoader implements ApplicationRunner {

    private final RepositoryService repositoryService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<Deployment> deployments = repositoryService.createDeploymentQuery().orderByDeploymentName().desc().list();

        for (Deployment deployment : deployments) {
            List<String> resources = repositoryService.getDeploymentResourceNames(deployment.getId());
            for (String res : resources) {
                if (res.equals("environment.json")) {
                    try (InputStream is = repositoryService.getResourceAsStream(deployment.getId(), res)) {
                        ObjectMapper mapper = new ObjectMapper();
                        EnvironmentData environment = mapper.readValue(is, EnvironmentData.class);
                        GlobalEnvironment.getInstance().setData(environment);
                        System.out.println("Environment loaded with " + environment.getPlaces().size() + " places.");
                        return;
                    }
                }
            }
        }
    }
}
