package org.unicam.intermediate.models.dto;

public class ProcessStartResponse {
    private String pid;
    private String definitionKey;

    public ProcessStartResponse(String pid, String definitionKey) {
        this.pid = pid;
        this.definitionKey = definitionKey;
    }

    public String getPid() {
        return pid;
    }

    public String getDefinitionKey() {
        return definitionKey;
    }
}
