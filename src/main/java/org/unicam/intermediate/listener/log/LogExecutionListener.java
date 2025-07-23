package org.unicam.intermediate.listener.log;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;

@Slf4j
public class LogExecutionListener implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {
        try {
            String eventName = execution.getEventName();
            String activityId = execution.getCurrentActivityId();
            String activityName = execution.getCurrentActivityName();
            String instanceId = execution.getProcessInstanceId();

            activityId = activityId != null ? activityId : "(unknown)";
            activityName = activityName != null ? activityName : "(unnamed)";

            if (ExecutionListener.EVENTNAME_START.equals(eventName)) {
                log.info("[LogExecutionListener] TASK STARTED | Activity: {} - {} | Instance: {}",
                        activityId, activityName, instanceId);
            } else if (ExecutionListener.EVENTNAME_END.equals(eventName)) {
                log.info("[LogExecutionListener] TASK ENDED | Activity: {} - {} | Instance: {}",
                        activityId, activityName, instanceId);
            }

        } catch (Exception e) {
            log.error("[LogExecutionListener] Error while logging execution event: {}", e.getMessage(), e);
        }
    }
}
