package org.unicam.intermediate.listener.log;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;

@Slf4j
public class LogExecutionListener implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {
        String eventName = execution.getEventName();
        String activityId = execution.getCurrentActivityId();
        String activityName = execution.getCurrentActivityName();
        String instanceId = execution.getProcessInstanceId();

        String logMessage = "";
        if (ExecutionListener.EVENTNAME_START.equals(eventName)) {
            logMessage = "TASK STARTED Activity: " + activityId + " - " + activityName + " | Instance: " + instanceId;
        } else if (ExecutionListener.EVENTNAME_END.equals(eventName)) {
            logMessage = "TASK ENDED Activity: " + activityId + " - " + activityName + " | Instance: " + instanceId;
        }
        log.info(logMessage);

    }
}
