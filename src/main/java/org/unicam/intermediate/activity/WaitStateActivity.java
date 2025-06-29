package org.unicam.intermediate.activity;

import org.camunda.bpm.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.unicam.intermediate.service.LogService;

public class WaitStateActivity extends AbstractBpmnActivityBehavior {

    private final LogService logService;
    public  WaitStateActivity(LogService logService) {
        this.logService = logService;
    }

    @Override
    public void execute(ActivityExecution execution) {
        logService.addLog("[MOVEMENT: Wait state] Activity ID " + execution.getActivity().getId() + " is waiting for an event.");
        // ps, qui non metto nulla così si mette in attesa
    }

    @Override
    public void signal(ActivityExecution execution, String signalName, Object signalData) {
        logService.addLog("[MOVEMENT: Wait state] Activity ID " + execution.getActivity().getId() + " event has been signalled.");
        leave(execution);
    }
}

