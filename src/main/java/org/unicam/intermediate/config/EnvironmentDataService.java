// src/main/java/org/unicam/intermediate/service/environmental/EnvironmentDataService.java
package org.unicam.intermediate.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.springframework.stereotype.Service;
import org.unicam.intermediate.models.pojo.EnvironmentData;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.List;

@Service
@Slf4j
@Getter
public class EnvironmentDataService {
    
    private final RepositoryService repositoryService;
    private final ObjectMapper objectMapper;
    private EnvironmentData data;
    
    public EnvironmentDataService(RepositoryService repositoryService, ObjectMapper objectMapper) {
        this.repositoryService = repositoryService;
        this.objectMapper = objectMapper;
    }
    
    @PostConstruct
    public void initialize() {
        loadEnvironmentData();
    }
    
    public void loadEnvironmentData() {
        List<Deployment> deployments = repositoryService
                .createDeploymentQuery()
                .orderByDeploymentTime().desc()
                .list();

        for (Deployment deployment : deployments) {
            List<String> resources = repositoryService.getDeploymentResourceNames(deployment.getId());
            for (String res : resources) {
                if ("environment.json".equals(res)) {
                    try (InputStream is = repositoryService.getResourceAsStream(deployment.getId(), res)) {
                        this.data = objectMapper.readValue(is, EnvironmentData.class);
                        log.info("[EnvironmentDataService] Loaded {} places from deployment '{}'", 
                                data.getPlaces().size(), deployment.getName());
                        return;
                    } catch (Exception e) {
                        log.error("[EnvironmentDataService] Failed to load environment.json: {}", e.getMessage());
                    }
                }
            }
        }
        
        // Initialize with empty data if nothing found
        this.data = new EnvironmentData();
        log.warn("[EnvironmentDataService] No environment.json found, initialized with empty data");
    }
    
    public void reloadEnvironment() {
        loadEnvironmentData();
        log.info("[EnvironmentDataService] Environment data reloaded");
    }
}