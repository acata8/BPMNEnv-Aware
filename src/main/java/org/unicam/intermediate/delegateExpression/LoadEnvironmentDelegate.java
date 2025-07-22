package org.unicam.intermediate.delegateExpression;

import lombok.AllArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.unicam.intermediate.service.EnvironmentService;

@Component("loadEnvironmentDelegate")
@AllArgsConstructor
public class LoadEnvironmentDelegate implements JavaDelegate {

    private final EnvironmentService environmentService;

    @Override
    public void execute(DelegateExecution execution) {
        environmentService.reloadFromLatestDeployment();
    }
}
