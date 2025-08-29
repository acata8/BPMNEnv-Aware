package org.unicam.intermediate.service.environmental;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.springframework.stereotype.Service;
import org.unicam.intermediate.config.GlobalEnvironment;
import org.unicam.intermediate.models.pojo.EnvironmentData;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
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
                if ("environment.json".equals(res)) {
                    try (InputStream is = repositoryService.getResourceAsStream(deployment.getId(), res)) {
                        ObjectMapper mapper = new ObjectMapper();
                        EnvironmentData environment = mapper.readValue(is, EnvironmentData.class);
                        GlobalEnvironment.getInstance().setData(environment);
                        log.info("[EnvironmentService] Environment loaded from deployment '{}'", deployment.getName());
                        return;
                    } catch (IOException e) {
                        log.error("[EnvironmentService] Failed to read environment.json from deployment '{}': {}", deployment.getName(), e.getMessage(), e);
                        throw new IllegalStateException("Failed to parse environment.json", e);
                    }
                }
            }
        }

        log.warn("[EnvironmentService] No environment.json found in any deployment");
    }
}
