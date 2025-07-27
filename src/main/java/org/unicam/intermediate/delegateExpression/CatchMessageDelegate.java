package org.unicam.intermediate.delegateExpression;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;


@Setter
@Slf4j
@Component("catchMessageDelegate")
public class CatchMessageDelegate implements JavaDelegate {



    @Override
    public void execute(DelegateExecution execution) {

        String messageName       = (String) execution.getVariable("messageName");
        String placeVariable     = (String) execution.getVariable("placeVariable");
        String businessKeyVariable    = (String) execution.getVariable("businessKeyVariable");

        Object place = execution.getVariable(placeVariable);
        //execution.setVariable(destinationVariable, place);
    }
}