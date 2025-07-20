package org.unicam.intermediate.config;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.unicam.intermediate.listener.movement.MovementParseListener;

@Configuration
@Slf4j
public class CamundaConfig extends AbstractCamundaConfiguration {

    @Autowired
    private MovementParseListener movementParseListener;

    @Bean
    public ProcessEnginePlugin processEnginePlugin() {
        return new AbstractProcessEnginePlugin() {
            @Override
            public void preInit(ProcessEngineConfigurationImpl configuration) {
                configuration.getCustomPreBPMNParseListeners().add(movementParseListener);
            }
        };
    }
}
