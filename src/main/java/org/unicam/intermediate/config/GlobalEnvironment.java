package org.unicam.intermediate.config;

import lombok.Getter;
import lombok.Setter;
import org.unicam.intermediate.models.pojo.EnvironmentData;

@Getter
@Setter
public class GlobalEnvironment {

    private static final GlobalEnvironment INSTANCE = new GlobalEnvironment();
    private EnvironmentData data;

    private GlobalEnvironment() {}

    public static GlobalEnvironment getInstance() {
        return INSTANCE;
    }

}
