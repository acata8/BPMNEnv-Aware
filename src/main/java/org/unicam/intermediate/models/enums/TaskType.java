package org.unicam.intermediate.models.enums;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
public enum TaskType {
    MOVEMENT("movement"),
    BINDING("binding"),
    UNBINDING("unbinding"),
    UNDEFINED("undefined");

    private final String xmlValue;

    TaskType(String xmlValue) {
        this.xmlValue = xmlValue;
    }

    @Override
    public String toString() {
        return xmlValue.toLowerCase();
    }
}
