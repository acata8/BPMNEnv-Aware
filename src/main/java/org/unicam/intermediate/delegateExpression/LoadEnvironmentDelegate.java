package org.unicam.intermediate.delegateExpression;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.unicam.intermediate.service.EnvironmentService;

@Component("loadEnvironmentDelegate")
@Slf4j
@AllArgsConstructor
public class LoadEnvironmentDelegate implements JavaDelegate {

    private final EnvironmentService environmentService;

    @Override
    public void execute(DelegateExecution execution) {
        try {
            environmentService.reloadFromLatestDeployment();
            log.info("[LoadEnvironment] Environment loaded successfully.");
        } catch (Exception e) {
            log.error("[LoadEnvironment] Failed to load environment: {}", e.getMessage(), e);
            throw new BpmnError("LoadEnvironmentError", "Failed to load environment from deployment");
        }
    }
}
