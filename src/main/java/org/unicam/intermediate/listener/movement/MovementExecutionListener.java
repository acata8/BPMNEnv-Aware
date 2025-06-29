package org.unicam.intermediate.listener.movement;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.unicam.intermediate.service.LogService;

@Component
public class MovementExecutionListener implements ExecutionListener {

    private final LogService logService;

    public MovementExecutionListener(LogService logService) {
        this.logService = logService;
    }

    @Override
    public void notify(DelegateExecution execution) {
        execution.setVariable("destination", "CS@Unicam");
        String logMessage = ">>> Task type: movement. Execute custom logic.";
        logService.addLog(logMessage);
    }
}
