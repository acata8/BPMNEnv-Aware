package org.unicam.intermediate.config;

import org.unicam.intermediate.models.pojo.EnvironmentData;

public class GlobalEnvironment {
    private static final GlobalEnvironment INSTANCE = new GlobalEnvironment();
    private EnvironmentData data;

    private GlobalEnvironment() {}

    public static GlobalEnvironment getInstance() {
        return INSTANCE;
    }

    public EnvironmentData getData() {
        return data;
    }

    public void setData(EnvironmentData data) {
        this.data = data;
    }
}
