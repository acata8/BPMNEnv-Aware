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

    public static TaskType fromXmlValue(String xmlValue) {
        if (xmlValue == null || xmlValue.trim().isEmpty()) {
            return null;
        }
        
        for (TaskType type : values()) {
            if (type.xmlValue.equalsIgnoreCase(xmlValue.trim())) {
                return type;
            }
        }
        return null;
    }


}