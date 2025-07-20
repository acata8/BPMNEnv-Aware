package org.unicam.intermediate.listener.movement;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class MovementExecutionListener implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {
        execution.setVariable("destination", "CS@Unicam");
    }
}
