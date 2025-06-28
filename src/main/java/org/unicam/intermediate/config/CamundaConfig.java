package org.unicam.intermediate.config;

import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.unicam.intermediate.listener.log.LogParseListener;
import org.unicam.intermediate.listener.movement.MovementParseListener;

@Configuration
public class CamundaConfig extends AbstractCamundaConfiguration {

    @Autowired
    private MovementParseListener movementParseListener;

    @Autowired
    private LogParseListener logParseListener;

    @Bean
    public ProcessEnginePlugin processEnginePlugin() {
        return new AbstractProcessEnginePlugin() {
            @Override
            public void preInit(ProcessEngineConfigurationImpl configuration) {
                configuration.getCustomPreBPMNParseListeners().add(logParseListener);
                configuration.getCustomPreBPMNParseListeners().add(movementParseListener);
            }
        };
    }
}
