package org.unicam.intermediate.Activity;

import org.camunda.bpm.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;

public class WaitStateActivity extends AbstractBpmnActivityBehavior {
    @Override
    public void execute(ActivityExecution execution) {
        System.out.println("Waiting signal for: " + execution.getActivity().getId());
    }

    @Override
    public void signal(ActivityExecution execution, String signalName, Object signalData) {
        System.out.println("Signal arrives! Device is inside the coordinates! " + execution.getActivity().getId());
        leave(execution);
    }
}

