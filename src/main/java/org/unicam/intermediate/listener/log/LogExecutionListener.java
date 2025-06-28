package org.unicam.intermediate.listener.log;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;

public class LogExecutionListener implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {
        String eventName = execution.getEventName();
        String activityId = execution.getCurrentActivityId();
        String activityName = execution.getCurrentActivityName();
        String instanceId = execution.getProcessInstanceId();

        if (ExecutionListener.EVENTNAME_START.equals(eventName)) {
            System.out.println("[Log] TASK STARTED Activity: " + activityId + " - " + activityName + " | Instance: " + instanceId);
        } else if (ExecutionListener.EVENTNAME_END.equals(eventName)) {
            System.out.println("[Log] TASK ENDED Activity: " + activityId + " - " + activityName + " | Instance: " + instanceId);
        }

    }
}
