package org.unicam.intermediate.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.unicam.intermediate.config.GlobalEnvironment;
import org.unicam.intermediate.models.pojo.EnvironmentData;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@AllArgsConstructor
public class EnvironmentService {

    private final RepositoryService repositoryService;

    public void reloadFromLatestDeployment() {
        List<Deployment> deployments = repositoryService
            .createDeploymentQuery()
            .orderByDeploymentTime().desc()
            .list();

        for (Deployment deployment : deployments) {
            List<String> resources = repositoryService.getDeploymentResourceNames(deployment.getId());
            for (String res : resources) {
                if (res.equals("environment.json")) {
                    try (InputStream is = repositoryService.getResourceAsStream(deployment.getId(), res)) {
                        ObjectMapper mapper = new ObjectMapper();
                        EnvironmentData environment = mapper.readValue(is, EnvironmentData.class);
                        GlobalEnvironment.getInstance().setData(environment);
                        System.out.println("Environment ricaricato da deployment " + deployment.getName());
                        return;
                    } catch (IOException e) {
                        throw new RuntimeException("Errore nella lettura di environment.json", e);
                    }
                }
            }
        }
    }
}
