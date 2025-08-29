package org.unicam.intermediate.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.unicam.intermediate.models.enums.TaskType;

import java.time.Instant;

/**
 * Represents a binding state during a process execution, where a specific task
 * type defines the type of operation (e.g., binding, unbinding). Each binding instance
 * is uniquely identified by a combination of business key and participant IDs, allowing
 * tracking of interactions between participants within the defined process.
 *
 * This class provides methods to generate unique keys for waiting and checking states
 * associated with the binding process. The toString representation includes details
 * of the task type, participants involved, business key, and creation timestamp.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WaitingBinding {
    private String processDefinitionKey;
    private String targetParticipantId;
    private String currentParticipantId;
    private String businessKey;
    private String executionId;
    private TaskType taskType;
    private Instant createdAt;
    
    public String getWaitingKey() {
        return businessKey + ":" + targetParticipantId;
    }
    
    public String getLookupKey() {
        return businessKey + ":" + currentParticipantId;
    }

    @Override
    public String toString() {
        return String.format("%s waiting: %s ↔ %s (key: %s, created: %s)",
                taskType != null ? taskType.toString() : "Unknown",
                currentParticipantId,
                targetParticipantId,
                businessKey,
                createdAt);
    }
}