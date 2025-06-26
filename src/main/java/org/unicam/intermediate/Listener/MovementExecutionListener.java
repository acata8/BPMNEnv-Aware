package org.unicam.intermediate.Listener;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class MovementExecutionListener implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {
        execution.setVariable("destination", "Position_T1");
        System.out.println("Task 'movement' PID: " + execution.getActivityInstanceId());
    }
}
