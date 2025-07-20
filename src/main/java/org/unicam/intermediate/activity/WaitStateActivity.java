package org.unicam.intermediate.activity;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;

@Slf4j
public class WaitStateActivity extends AbstractBpmnActivityBehavior {


    @Override
    public void execute(ActivityExecution execution) {
        log.info("[MOVEMENT: Wait state] Activity ID {} is waiting for an event.", execution.getActivity().getId());
    }

    @Override
    public void signal(ActivityExecution execution, String signalName, Object signalData) {
        log.info("[MOVEMENT: Wait state] Activity ID {} event has been signalled.", execution.getActivity().getId());
        leave(execution);
    }
}

