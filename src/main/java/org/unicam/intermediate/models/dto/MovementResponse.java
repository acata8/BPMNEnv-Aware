package org.unicam.intermediate.models.dto;

import lombok.Getter;

public class MovementResponse {
    @Getter
    private boolean success;
    @Getter
    private String message;
    @Getter
    private String destination;
    @Getter
    private String participantId;
    @Getter
    private String processId;

    public MovementResponse(boolean success, String message, String destination, String participantId, String processId) {
        this.success = success;
        this.message = message;
        this.destination = destination;
        this.participantId = participantId;
        this.processId = processId;
    }

}
