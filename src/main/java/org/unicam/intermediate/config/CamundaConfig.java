package org.unicam.intermediate.config;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.unicam.intermediate.listener.log.LogParseListener;
import org.unicam.intermediate.listener.movement.MovementParseListener;

@Configuration
@Slf4j
@AllArgsConstructor
public class CamundaConfig extends AbstractCamundaConfiguration {

    private final MovementParseListener movementParseListener;
    private final LogParseListener logParseListener;

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
