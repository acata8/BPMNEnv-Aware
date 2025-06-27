package org.unicam.intermediate.config;

import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.unicam.intermediate.listener.MovementTaskParseListener;

@Configuration
public class CamundaParseConfig {

    @Bean
    public ProcessEnginePlugin registerMovementParseListener(MovementTaskParseListener listener) {
        return new AbstractProcessEnginePlugin() {
            @Override
            public void preInit(ProcessEngineConfigurationImpl config) {
                config.getCustomPreBPMNParseListeners().add(listener);
            }
        };
    }
}