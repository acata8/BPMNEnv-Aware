package org.unicam.intermediate.service;

import lombok.AllArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.el.ExpressionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.unicam.intermediate.config.GlobalEnvironment;
import org.unicam.intermediate.models.pojo.Place;
import org.unicam.intermediate.service.TemperatureService.TemperatureService;
import org.unicam.intermediate.utils.BooleanEvaluation.BooleanExpressionEvaluator;
import org.unicam.intermediate.utils.BooleanEvaluation.PlaceExpressionParser;

import java.util.Optional;

@Component("decisioneService")
@AllArgsConstructor
public class DecisioneService {

    private final ProcessEngineConfigurationImpl config;

    public void evaluatePlaceCondition(DelegateExecution execution, String expressionText, String outputvariableName) {

        PlaceExpressionParser.ConditionParts cp =  PlaceExpressionParser.parse(expressionText);
        double temperature = new TemperatureService().getTemperatureFromPlace(cp.placeId);
        double threshold = Double.parseDouble(cp.value);
        var evaluation = BooleanExpressionEvaluator.evaluate(temperature, cp.operator, threshold);
        execution.setVariable(outputvariableName, evaluation);
    }
}