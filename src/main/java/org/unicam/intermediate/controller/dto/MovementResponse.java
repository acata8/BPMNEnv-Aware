package org.unicam.intermediate.controller.dto;

public class MovementResponse {
    private boolean success;
    private String message;
    private String destination;
    private String participantId;

    public MovementResponse(boolean success, String message, String destination, String participantId) {
        this.success = success;
        this.message = message;
        this.destination = destination;
        this.participantId = participantId;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getDestination() { return destination; }
    public String getParticipantId() { return participantId; }
}
