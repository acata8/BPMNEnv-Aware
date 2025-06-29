package org.unicam.intermediate.listener.log;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.unicam.intermediate.service.LogService;

public class LogExecutionListener implements ExecutionListener {

    private final LogService logService;

    public LogExecutionListener(LogService logService) {
        this.logService = logService;
    }

    @Override
    public void notify(DelegateExecution execution) {
        String eventName = execution.getEventName();
        String activityId = execution.getCurrentActivityId();
        String activityName = execution.getCurrentActivityName();
        String instanceId = execution.getProcessInstanceId();

        if (ExecutionListener.EVENTNAME_START.equals(eventName)) {
            String logMessage = "TASK STARTED Activity: " + activityId + " - " + activityName + " | Instance: " + instanceId;
            logService.addLog(logMessage);
        } else if (ExecutionListener.EVENTNAME_END.equals(eventName)) {
            String logMessage = "TASK ENDED Activity: " + activityId + " - " + activityName + " | Instance: " + instanceId;
            logService.addLog(logMessage);
        }

    }
}
