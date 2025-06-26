package org.unicam.intermediate.Config;

import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.unicam.intermediate.Listener.MovementTaskParseListener;

@Configuration
public class CamundaConfig extends AbstractCamundaConfiguration {

    @Autowired
    private MovementTaskParseListener movementTaskParseListener;

    @Override
    public void preInit(SpringProcessEngineConfiguration config) {
        config.getCustomPreBPMNParseListeners().add(movementTaskParseListener);
    }
}
