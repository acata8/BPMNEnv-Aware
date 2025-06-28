package org.unicam.intermediate.activity;

import org.camunda.bpm.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;

public class WaitStateActivity extends AbstractBpmnActivityBehavior {
    @Override
    public void execute(ActivityExecution execution) {
        System.out.println("[MOVEMENT: Wait state] Activity ID " + execution.getActivity().getId() + " is waiting for an event.");
    }

    @Override
    public void signal(ActivityExecution execution, String signalName, Object signalData) {
        System.out.println("[MOVEMENT: Wait state] Activity ID " + execution.getActivity().getId() + " event has been signalled.");
        leave(execution);
    }
}

