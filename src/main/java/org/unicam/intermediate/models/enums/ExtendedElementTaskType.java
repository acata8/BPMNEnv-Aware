package org.unicam.intermediate.models.enums;

import lombok.Getter;

@Getter
public enum ExtendedElementTaskType {
    DESTINATION("destination"),
    BINDING("binding"),
    TYPE("type");

    private final String xmlValue;

    ExtendedElementTaskType(String xmlValue) {
        this.xmlValue = xmlValue;
    }

    @Override
    public String toString() {
        return xmlValue.toLowerCase();
    }
}
